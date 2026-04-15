package com.easypan.service.impl;

import com.easypan.entity.po.FileChunk;
import com.easypan.entity.po.FileInfo;
import com.easypan.mappers.FileChunkMapper;
import com.easypan.service.FileChunkService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easypan.service.FileInfoService;
import com.easypan.utils.StringTools;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 文件分片上传记录表 服务实现类（LocalDateTime 适配版）
 */
@Service
public class FileChunkServiceImpl extends ServiceImpl<FileChunkMapper, FileChunk> implements FileChunkService {

    private static final String CHUNK_BASE_PATH = "E:/Workspace-java/easypan/esaypan-backend/file/chunks/files/";

    @Resource
    private FileInfoService fileInfoService;

    /**
     * 初始化分片上传（修复时间类型）
     */
    @Override
    public String initUpload(String fileMd5, String fileName, Long fileSize, Integer chunkSize, String filePid, String userId) {
        // 1. 检查是否已有相同上传任务
        FileChunk existChunk = this.lambdaQuery()
                .eq(FileChunk::getFileMd5, fileMd5)
                .eq(FileChunk::getUserId, userId)
                .eq(FileChunk::getStatus, (byte) 0)
                .one();

        if (existChunk != null) {
            return existChunk.getUploadId();
        }

        // 2. 创建新的上传记录（核心修复：LocalDateTime 赋值）
        FileChunk fileChunk = new FileChunk();
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        fileChunk.setUploadId(uploadId);
        fileChunk.setFileMd5(fileMd5);
        fileChunk.setFileName(fileName);
        fileChunk.setTotalSize(fileSize);
        fileChunk.setChunkSize(chunkSize);
        fileChunk.setFilePid(filePid);
        fileChunk.setUserId(userId);
        fileChunk.setStatus((byte) 0);
        fileChunk.setTotalChunks((int) Math.ceil((double) fileSize / chunkSize));
        fileChunk.setUploadedChunks(0);

        // 修复：设置过期时间为 LocalDateTime（1天后）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusDays(1); // 1天后过期
        fileChunk.setExpireTime(expireTime);

        // 修复：创建/更新时间也用 LocalDateTime
        fileChunk.setCreateTime(now);
        fileChunk.setUpdateTime(now);

        // 3. 保存记录
        this.save(fileChunk);

        // 4. 创建本地文件夹
        String uploadFolder = CHUNK_BASE_PATH + uploadId + "/";
        File folder = new File(uploadFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        return uploadId;
    }

    /**
     * 上传分片（修复更新时间）
     */
    @Override
    public void uploadChunk(String uploadId, Integer chunkIndex, MultipartFile file, String userId) {
        FileChunk fileChunk = this.lambdaQuery()
                .eq(FileChunk::getUploadId, uploadId)
                .eq(FileChunk::getUserId, userId)
                .eq(FileChunk::getStatus, (byte) 0)
                .one();

        if (fileChunk == null) {
            throw new RuntimeException("上传任务不存在或已过期");
        }

        // 保存分片文件
        try (InputStream is = file.getInputStream();
             FileOutputStream os = new FileOutputStream(CHUNK_BASE_PATH + uploadId + "/" + chunkIndex)) {

            byte[] buffer = new byte[1024 * 1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            // 修复：更新时间用 LocalDateTime
            fileChunk.setUploadedChunks(fileChunk.getUploadedChunks() + 1);
            fileChunk.setUpdateTime(LocalDateTime.now());
            this.updateById(fileChunk);

        } catch (IOException e) {
            throw new RuntimeException("分片上传失败", e);
        }
    }

    /**
     * 合并分片（修复更新时间）
     */
    @Override
    public void mergeChunk(String uploadId, String userId) {
        FileChunk fileChunk = this.lambdaQuery()
                .eq(FileChunk::getUploadId, uploadId)
                .eq(FileChunk::getUserId, userId)
                .eq(FileChunk::getStatus, (byte) 0)
                .one();

        if (fileChunk == null) {
            throw new RuntimeException("上传任务不存在");
        }

        if (!fileChunk.getUploadedChunks().equals(fileChunk.getTotalChunks())) {
            throw new RuntimeException("分片未上传完整");
        }

        // 合并文件（自动存到正确目录：easypan/file/chunks/files/）
        String finalFilePath = mergeChunks(uploadId, fileChunk.getFileName(), fileChunk.getFileMd5());

        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(UUID.randomUUID().toString().replace("-", "").substring(0, 15));
        fileInfo.setUserId(userId);
        fileInfo.setFilePid(fileChunk.getFilePid());
        fileInfo.setFileName(fileChunk.getFileName());

        String fileSuffix = getSuffix(fileChunk.getFileName());
        fileInfo.setFileSuffix(fileSuffix);
        fileInfo.setFileSize(fileChunk.getTotalSize());
        fileInfo.setIsFolder(0);
        fileInfo.setFileMd5(fileChunk.getFileMd5());

        String fileUrl = "chunks/files/" + fileChunk.getFileMd5() + fileSuffix;
        fileInfo.setFileUrl(fileUrl);

        fileInfo.setStatus(0);
        fileInfo.setCreateTime(new Date());
        fileInfo.setLastOpTime(new Date());
        fileInfoService.save(fileInfo);

        fileChunk.setStatus((byte) 1);
        fileChunk.setUpdateTime(LocalDateTime.now());
        this.updateById(fileChunk);
    }

    /**
     * 查询已上传分片
     */
    @Override
    public List<Integer> getUploadedChunks(String uploadId, String userId) {
        FileChunk fileChunk = this.lambdaQuery()
                .eq(FileChunk::getUploadId, uploadId)
                .eq(FileChunk::getUserId, userId)
                .one();

        if (fileChunk == null) {
            return Collections.emptyList();
        }

        List<Integer> uploadedChunks = new ArrayList<>();
        File chunkFolder = new File(CHUNK_BASE_PATH + uploadId + "/");
        if (chunkFolder.exists()) {
            File[] chunks = chunkFolder.listFiles();
            if (chunks != null) {
                for (File chunk : chunks) {
                    try {
                        int index = Integer.parseInt(chunk.getName());
                        uploadedChunks.add(index);
                    } catch (NumberFormatException e) {
                        // 忽略
                    }
                }
            }
        }

        Collections.sort(uploadedChunks);
        return uploadedChunks;
    }

    private String mergeChunks(String uploadId, String fileName, String fileMd5) {
        String chunkFolderPath = CHUNK_BASE_PATH + uploadId + "/";
        File chunkFolder = new File(chunkFolderPath);

        if (!chunkFolder.exists()) {
            throw new RuntimeException("分片文件夹不存在");
        }

        File[] chunks = chunkFolder.listFiles();
        if (chunks == null || chunks.length == 0) {
            throw new RuntimeException("未找到分片文件");
        }

        Arrays.sort(chunks, (f1, f2) -> {
            try {
                return Integer.compare(Integer.parseInt(f1.getName()), Integer.parseInt(f2.getName()));
            } catch (NumberFormatException e) {
                return 0;
            }
        });

        String finalFolderPath = "E:/Workspace-java/easypan/esaypan-backend/file/chunks/files/";
        File finalFolder = new File(finalFolderPath);
        if (!finalFolder.exists()) {
            finalFolder.mkdirs();
        }

        String finalFilePath = finalFolderPath + fileMd5 + getSuffix(fileName);
        File finalFile = new File(finalFilePath);

        try (FileOutputStream os = new FileOutputStream(finalFile)) {
            byte[] buffer = new byte[1024 * 1024];

            for (File chunk : chunks) {
                try (InputStream is = new java.io.FileInputStream(chunk)) {
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("文件合并失败", e);
        }

        return finalFilePath;
    }

    private String getSuffix(String fileName) {
        if (StringTools.isEmpty(fileName) || !fileName.contains(".")) {
            return "";
        }
        return "." + fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}