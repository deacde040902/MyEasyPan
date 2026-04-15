package com.easypan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.easypan.entity.po.FileChunk;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件分片上传记录表 服务接口（纯接口版）
 */
public interface FileChunkService extends IService<FileChunk> {

    /**
     * 初始化分片上传
     */
    String initUpload(String fileMd5, String fileName, Long fileSize, Integer chunkSize, String filePid, String userId);

    /**
     * 上传分片
     */
    void uploadChunk(String uploadId, Integer chunkIndex, MultipartFile file, String userId);

    /**
     * 合并分片
     */
    void mergeChunk(String uploadId, String userId);

    /**
     * 查询已上传分片
     */
    List<Integer> getUploadedChunks(String uploadId, String userId);
}