package com.easypan.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * RAG 文档切片表
 * <p>
 * chunk_id = fileId + "_" + chunkIndex（确定性 ID，与向量库中的 Document id 一致，
 * 以便按文件删除向量）。
 */
@Data
@TableName("doc_chunk")
public class DocChunk implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "chunk_id", type = IdType.INPUT)
    private String chunkId;

    @TableField("file_id")
    private String fileId;

    @TableField("user_id")
    private String userId;

    @TableField("chunk_index")
    private Integer chunkIndex;

    /** 切片文本内容 */
    private String content;

    /** 字符数 */
    @TableField("char_count")
    private Integer charCount;

    private Date createTime;
}
