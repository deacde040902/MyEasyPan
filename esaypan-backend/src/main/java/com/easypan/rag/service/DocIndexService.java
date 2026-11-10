package com.easypan.rag.service;

import java.util.List;
import java.util.Map;

public interface DocIndexService {

    /**
     * 解析、分块并向量化一个文件（同步）
     *
     * @return 生成的切片数量
     */
    int indexFile(String userId, String fileId);

    /**
     * 异步建索引（文件上传合并完成后调用）
     */
    void indexFileAsync(String userId, String fileId);

    /**
     * 为当前用户所有支持索引的文档批量建立索引（幂等，已索引的会重建）
     *
     * @return {total, success, failed, chunkCount}
     */
    Map<String, Object> indexAll(String userId);

    /**
     * 删除某文件的全部向量与切片记录（永久删除时调用）
     */
    void removeFileIndex(String fileId);

    /**
     * 是否为支持索引的文档类型
     */
    boolean isSupportedSuffix(String fileSuffix);

    /**
     * 用户已建立索引的文件列表：[{fileId, fileName, chunkCount}]
     */
    List<Map<String, Object>> listIndexedFiles(String userId);
}
