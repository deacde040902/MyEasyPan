package com.easypan.entity.vo;

import lombok.Data;

/**
 * 问答溯源引用条目
 */
@Data
public class QaSourceVO {

    private String fileId;

    private String fileName;

    /** 片段预览 */
    private String snippet;

    /** 相似度得分（0~1） */
    private Double score;

    /** 切片在原文件中的序号 */
    private Integer chunkIndex;
}
