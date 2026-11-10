package com.easypan.rag.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.enums.FileStatusEnum;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.vo.QaSourceVO;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.rag.config.RagProperties;
import com.easypan.rag.service.QaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;

/**
 * 文件问答服务（在线链路）：权限过滤 → 向量检索 → 置信拒答 → 可溯源生成（流式）
 */
@Service
public class QaServiceImpl implements QaService {

    private static final Logger logger = LoggerFactory.getLogger(QaServiceImpl.class);

    /** SSE 事件名 */
    public static final String EVENT_SOURCES = "sources";
    public static final String EVENT_MESSAGE = "message";
    public static final String EVENT_DONE = "done";

    private static final String SYSTEM_PROMPT =
            "你是私有云盘的文件问答助手。请严格根据下面提供的资料片段回答用户问题，要求：\n"
            + "1. 只使用资料中的内容，不要编造资料之外的信息；\n"
            + "2. 回答中引用资料时，在对应句子末尾标注来源编号，如 [1]、[2]；\n"
            + "3. 如果资料不足以回答问题，直接回答：根据你的文件，未找到与该问题相关的内容。";

    private static final ExecutorService QA_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "qa-stream-" + r.hashCode());
        t.setDaemon(true);
        return t;
    });

    @Resource
    private RagProperties ragProperties;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private OpenAiChatModel chatModel;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Value("${easypan.file.base-path}")
    private String fileBasePath;

    @Override
    public SseEmitter askStream(String userId, String question) {
        SseEmitter emitter = new SseEmitter(180_000L);
        QA_EXECUTOR.execute(() -> {
            try {
                doAsk(userId, question, emitter);
            } catch (Exception e) {
                logger.error("问答处理失败 userId={}", userId, e);
                try {
                    emitter.send(SseEmitter.event().name(EVENT_MESSAGE)
                            .data("问答服务异常：" + e.getMessage()));
                    emitter.send(SseEmitter.event().name(EVENT_DONE).data("[DONE]"));
                    emitter.complete();
                } catch (Exception ignore) {
                    emitter.completeWithError(e);
                }
            }
        });
        return emitter;
    }

    private void doAsk(String userId, String question, SseEmitter emitter) throws Exception {
        // 1. 权限感知：只取当前用户"正常状态"的文件集合，无权文件完全不进入召回池
        List<FileInfo> accessibleFiles = fileInfoMapper.selectList(
                new LambdaQueryWrapper<FileInfo>()
                        .select(FileInfo::getFileId)
                        .eq(FileInfo::getUserId, userId)
                        .eq(FileInfo::getIsFolder, Constants.ZERO)
                        .eq(FileInfo::getStatus, FileStatusEnum.NORMAL.getCode()));
        if (accessibleFiles.isEmpty()) {
            refuse(emitter, "当前没有可检索的文件，请先上传文档类文件（txt、pdf、word 等）。");
            return;
        }
        List<Object> fileIds = accessibleFiles.stream()
                .map(FileInfo::getFileId).collect(Collectors.toList());

        // 2. 向量检索（metadata filter 下推，仅在有权文件范围内召回）
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        Filter.Expression filter = builder.in("fileId", fileIds).build();
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(ragProperties.getTopK())
                .similarityThreshold(ragProperties.getScoreThreshold())
                .filterExpression(filter)
                .build();
        List<Document> docs = vectorStore.similaritySearch(request);

        // 3. 置信拒答：无片段命中说明知识库覆盖不足
        if (docs == null || docs.isEmpty()) {
            refuse(emitter, "根据你的文件，未找到与该问题相关的内容。");
            return;
        }

        // 4. 溯源信息先推给前端
        List<QaSourceVO> sources = new ArrayList<>();
        for (Document doc : docs) {
            QaSourceVO vo = new QaSourceVO();
            vo.setFileId((String) doc.getMetadata().get("fileId"));
            vo.setFileName((String) doc.getMetadata().get("fileName"));
            vo.setChunkIndex(doc.getMetadata().get("chunkIndex") instanceof Integer
                    ? (Integer) doc.getMetadata().get("chunkIndex") : null);
            vo.setScore(doc.getScore());
            String text = doc.getText() == null ? "" : doc.getText();
            vo.setSnippet(text.length() > 120 ? text.substring(0, 120) + "..." : text);
            sources.add(vo);
        }
        emitter.send(SseEmitter.event().name(EVENT_SOURCES).data(JSON.toJSONString(sources)));

        // 5. 组装上下文与提示词
        StringBuilder context = new StringBuilder();
        int totalChars = 0;
        for (int i = 0; i < docs.size(); i++) {
            String content = docs.get(i).getText();
            if (content == null || content.isEmpty()) {
                continue;
            }
            String fileName = String.valueOf(docs.get(i).getMetadata().getOrDefault("fileName", "未知文件"));
            String block = "[" + (i + 1) + "] 来源：" + fileName + "\n" + content + "\n\n";
            if (totalChars + block.length() > ragProperties.getMaxContextChars()) {
                break;
            }
            context.append(block);
            totalChars += block.length();
        }

        ChatClient client = ChatClient.builder(chatModel).build();
        StringBuilder answer = new StringBuilder();
        client.prompt()
                .system(SYSTEM_PROMPT)
                .user("资料片段如下：\n" + context + "\n用户问题：" + question)
                .stream()
                .content()
                .doOnNext(chunk -> {
                    answer.append(chunk);
                    try {
                        emitter.send(SseEmitter.event().name(EVENT_MESSAGE).data(chunk));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .doOnError(err -> {
                    logger.error("流式生成失败 userId={}", userId, err);
                    try {
                        emitter.send(SseEmitter.event().name(EVENT_MESSAGE).data("\n\n[生成中断]"));
                        emitter.send(SseEmitter.event().name(EVENT_DONE).data("[DONE]"));
                        emitter.complete();
                    } catch (Exception ignore) {
                        emitter.completeWithError(err);
                    }
                })
                .doOnComplete(() -> {
                    logger.info("问答完成 userId={} sources={} answerLen={}",
                            userId, sources.size(), answer.length());
                    try {
                        emitter.send(SseEmitter.event().name(EVENT_DONE).data("[DONE]"));
                        emitter.complete();
                    } catch (Exception ignore) {
                    }
                })
                .subscribe();
    }

    private void refuse(SseEmitter emitter, String message) throws java.io.IOException {
        emitter.send(SseEmitter.event().name(EVENT_SOURCES).data(JSON.toJSONString(new ArrayList<>())));
        emitter.send(SseEmitter.event().name(EVENT_MESSAGE).data(message));
        emitter.send(SseEmitter.event().name(EVENT_DONE).data("[DONE]"));
        emitter.complete();
    }
}
