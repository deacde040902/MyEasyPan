package com.easypan.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.enums.FileStatusEnum;
import com.easypan.entity.po.DocChunk;
import com.easypan.entity.po.FileInfo;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.DocChunkMapper;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.rag.config.AiConfig;
import com.easypan.rag.config.RagProperties;
import com.easypan.rag.service.DocChunkService;
import com.easypan.rag.service.DocIndexService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;

/**
 * 文档索引服务：Tika 解析 → 按段落分块（带重叠）→ 向量化入库（离线链路）
 */
@Service
public class DocIndexServiceImpl implements DocIndexService {

    private static final Logger logger = LoggerFactory.getLogger(DocIndexServiceImpl.class);

    private static final List<String> SUPPORT_SUFFIX = Arrays.asList(
            "txt", "md", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "html", "htm");

    @Resource
    private RagProperties ragProperties;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private DocChunkService docChunkService;

    @Value("${easypan.file.base-path}")
    private String fileBasePath;

    /** 按 fileId 串行化索引操作，防止并发重复索引（重复点击 / 上传钩子与手动触发并发） */
    private final Map<String, Object> fileLocks = new ConcurrentHashMap<>();

    @Override
    public int indexFile(String userId, String fileId) {
        // 同一文件串行；不同文件仍可并行
        Object lock = fileLocks.computeIfAbsent(fileId, k -> new Object());
        synchronized (lock) {
            try {
                return doIndexFile(userId, fileId);
            } finally {
                fileLocks.remove(fileId);
            }
        }
    }

    private int doIndexFile(String userId, String fileId) {
        FileInfo file = fileInfoMapper.selectById(fileId);
        if (file == null || !userId.equals(file.getUserId())) {
            throw new BusinessException("文件不存在或无操作权限");
        }
        if (Constants.ONE.equals(file.getIsFolder())) {
            throw new BusinessException("文件夹不支持建立索引");
        }
        if (!FileStatusEnum.NORMAL.getCode().equals(file.getStatus())) {
            throw new BusinessException("仅支持对正常状态的文件建立索引");
        }
        if (!isSupportedSuffix(file.getFileSuffix())) {
            throw new BusinessException("该文件类型不支持问答索引，支持：" + String.join("、", SUPPORT_SUFFIX));
        }

        String physicalPath = fileBasePath + File.separator + file.getFileUrl();
        File physicalFile = new File(physicalPath);
        if (!physicalFile.exists()) {
            throw new BusinessException("文件不存在：" + file.getFileName());
        }

        // 1. 先清理旧索引（覆盖式重建）
        removeFileIndex(fileId);

        // 2. Tika 解析为纯文本
        String text = parseToText(physicalFile, file.getFileName());
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(emptyTextMessage(physicalFile, file.getFileSuffix()));
        }

        // 3. 分块
        List<String> chunks = splitChunks(text);
        if (chunks.isEmpty()) {
            throw new BusinessException("文件内容过短，无需建立索引");
        }

        // 4. 向量化入库（SimpleVectorStore.add 内部调用 EmbeddingModel）
        List<Document> documents = new ArrayList<>();
        List<DocChunk> chunkRecords = new ArrayList<>();
        Date now = new Date();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkId = fileId + "_" + i;
            String content = chunks.get(i);
            documents.add(Document.builder()
                    .id(chunkId)
                    .text(content)
                    .metadata("fileId", fileId)
                    .metadata("userId", userId)
                    .metadata("fileName", file.getFileName())
                    .metadata("chunkIndex", i)
                    .build());
            DocChunk record = new DocChunk();
            record.setChunkId(chunkId);
            record.setFileId(fileId);
            record.setUserId(userId);
            record.setChunkIndex(i);
            record.setContent(content);
            record.setCharCount(content.length());
            record.setCreateTime(now);
            chunkRecords.add(record);
        }
        vectorStore.add(documents);

        // 5. 切片元数据落库
        chunkRecords.forEach(docChunkService::save);
        saveVectorStore();

        logger.info("文件索引完成 fileId={} fileName={} chunks={}", fileId, file.getFileName(), chunks.size());
        return chunks.size();
    }

    @Async
    @Override
    public void indexFileAsync(String userId, String fileId) {
        try {
            indexFile(userId, fileId);
        } catch (Exception e) {
            // 异步索引失败不影响上传主流程
            logger.warn("异步索引失败 fileId={} : {}", fileId, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> indexAll(String userId) {
        List<FileInfo> files = fileInfoMapper.selectList(
                new LambdaQueryWrapper<FileInfo>()
                        .eq(FileInfo::getUserId, userId)
                        .eq(FileInfo::getIsFolder, Constants.ZERO)
                        .eq(FileInfo::getStatus, FileStatusEnum.NORMAL.getCode()));
        int total = 0;
        int success = 0;
        int failed = 0;
        int chunkCount = 0;
        StringBuilder failedNames = new StringBuilder();
        for (FileInfo file : files) {
            if (!isSupportedSuffix(file.getFileSuffix())) {
                continue;
            }
            if (file.getFileSize() == null || file.getFileSize() <= 0) {
                continue;
            }
            total++;
            try {
                chunkCount += indexFile(userId, file.getFileId());
                success++;
            } catch (Exception e) {
                failed++;
                logger.warn("批量索引失败 fileId={} fileName={} : {}", file.getFileId(), file.getFileName(), e.getMessage());
                if (failedNames.length() > 0) {
                    failedNames.append("、");
                }
                failedNames.append(file.getFileName());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("failed", failed);
        result.put("chunkCount", chunkCount);
        result.put("failedNames", failedNames.toString());
        return result;
    }

    @Override
    public void removeFileIndex(String fileId) {
        // 删除切片记录
        List<DocChunk> oldChunks = docChunkService.list(
                new LambdaQueryWrapper<DocChunk>().eq(DocChunk::getFileId, fileId));
        if (oldChunks.isEmpty()) {
            return;
        }
        List<String> oldIds = oldChunks.stream().map(DocChunk::getChunkId).collect(Collectors.toList());
        try {
            vectorStore.delete(oldIds);
        } catch (Exception e) {
            logger.warn("删除向量失败 fileId={} : {}", fileId, e.getMessage());
        }
        docChunkService.remove(new LambdaQueryWrapper<DocChunk>().eq(DocChunk::getFileId, fileId));
        saveVectorStore();
    }

    @Override
    public boolean isSupportedSuffix(String fileSuffix) {
        if (fileSuffix == null) {
            return false;
        }
        String suffix = fileSuffix.toLowerCase();
        if (suffix.startsWith(".")) {
            suffix = suffix.substring(1);
        }
        return SUPPORT_SUFFIX.contains(suffix);
    }

    @Override
    public List<Map<String, Object>> listIndexedFiles(String userId) {
        List<DocChunk> chunks = docChunkService.list(
                new LambdaQueryWrapper<DocChunk>().eq(DocChunk::getUserId, userId));
        Map<String, Long> countMap = chunks.stream()
                .collect(Collectors.groupingBy(DocChunk::getFileId, Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : countMap.entrySet()) {
            FileInfo file = fileInfoMapper.selectById(entry.getKey());
            Map<String, Object> item = new HashMap<>();
            item.put("fileId", entry.getKey());
            item.put("fileName", file == null ? "未知文件" : file.getFileName());
            item.put("chunkCount", entry.getValue());
            result.add(item);
        }
        return result;
    }

    /**
     * 文本为空时的针对性提示：对 PDF 进一步判断是否为扫描件/图片型文档（无文字层）
     */
    private String emptyTextMessage(File physicalFile, String suffix) {
        String s = suffix == null ? "" : suffix.toLowerCase().replace(".", "");
        if (s.equals("pdf")) {
            try (PDDocument doc = Loader.loadPDF(physicalFile)) {
                int pages = doc.getNumberOfPages();
                boolean hasImage = false;
                boolean hasFont = false;
                for (PDPage page : doc.getPages()) {
                    PDResources r = page.getResources();
                    if (r == null) {
                        continue;
                    }
                    for (COSName n : r.getXObjectNames()) {
                        try {
                            if (r.getXObject(n).getClass().getSimpleName().contains("Image")) {
                                hasImage = true;
                            }
                        } catch (Exception ignore) {
                        }
                    }
                    if (r.getFontNames().iterator().hasNext()) {
                        hasFont = true;
                    }
                }
                if (!hasFont && hasImage) {
                    return "该 PDF 为扫描件/图片型文档（共 " + pages
                            + " 页，无文字层），无法直接提取文本。请先用 WPS/Adobe 的 OCR 功能将其转为可复制文字的 PDF 或 Word，再建立索引。";
                }
            } catch (Exception ignore) {
            }
        }
        return "未能从文件中解析出文本内容（可能是纯图片/扫描件或空白文档）";
    }

    /**
     * Tika 统一解析 pdf/docx/xlsx 等为纯文本
     */
    private String parseToText(File physicalFile, String fileName) {
        try (InputStream is = new FileInputStream(physicalFile)) {
            Parser parser = new AutoDetectParser();
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            ParseContext context = new ParseContext();
            // 限制最大提取字符数，防止超大文件撑爆内存
            BodyContentHandler handler = new BodyContentHandler(new WriteOutContentHandler(3_000_000));
            parser.parse(is, handler, metadata, context);
            return handler.toString();
        } catch (Exception e) {
            throw new BusinessException("文档解析失败：" + e.getMessage());
        }
    }

    /**
     * 分块策略：按段落累积到目标长度；超长段落滑动窗口硬切；
     * 相邻分块之间保留 chunk-overlap 字符重叠，避免语义被切断。
     */
    private List<String> splitChunks(String text) {
        int chunkSize = ragProperties.getChunkSize();
        int overlap = ragProperties.getChunkOverlap();
        int minChars = ragProperties.getMinChunkChars();

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] paragraphs = normalized.split("\n+");

        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String para : paragraphs) {
            String p = para.trim();
            if (p.isEmpty()) {
                continue;
            }
            // 超长段落：滑动窗口硬切，窗口间保留 overlap
            if (p.length() > chunkSize) {
                flushChunk(cur, result, minChars);
                int step = Math.max(chunkSize - overlap, 1);
                for (int start = 0; start < p.length(); start += step) {
                    int end = Math.min(start + chunkSize, p.length());
                    String piece = p.substring(start, end);
                    if (piece.length() >= minChars) {
                        result.add(piece);
                    }
                    if (end == p.length()) {
                        break;
                    }
                }
                continue;
            }
            if (cur.length() > 0 && cur.length() + p.length() + 1 > chunkSize) {
                String flushed = flushChunk(cur, result, minChars);
                // 新块以上一块结尾的 overlap 字符开头，保证上下文连续
                if (flushed != null && overlap > 0 && flushed.length() > overlap) {
                    cur.append(flushed, flushed.length() - overlap, flushed.length()).append('\n');
                }
            }
            cur.append(p).append('\n');
        }
        flushChunk(cur, result, minChars);
        return result;
    }

    private String flushChunk(StringBuilder cur, List<String> result, int minChars) {
        String flushed = cur.toString().trim();
        cur.setLength(0);
        if (!flushed.isEmpty() && flushed.length() >= minChars) {
            result.add(flushed);
            return flushed;
        }
        return flushed.isEmpty() ? null : flushed;
    }

    private void saveVectorStore() {
        try {
            File persistFile = AiConfig.vectorStoreFile(fileBasePath);
            if (vectorStore instanceof SimpleVectorStore) {
                ((SimpleVectorStore) vectorStore).save(persistFile);
            }
        } catch (Exception e) {
            logger.warn("向量库持久化失败: {}", e.getMessage());
        }
    }
}
