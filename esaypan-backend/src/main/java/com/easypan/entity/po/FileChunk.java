package com.easypan.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 文件分片上传记录表
 * </p>
 *
 * @author licheng
 * @since 2026-03-21
 */
@Getter
@Setter
@TableName("file_chunk")

public class FileChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    
    @TableField("upload_id")
    private String uploadId;

    
    @TableField("user_id")
    private String userId;

    
    @TableField("file_md5")
    private String fileMd5;

    
    @TableField("file_name")
    private String fileName;

    
    @TableField("total_size")
    private Long totalSize;

    
    @TableField("chunk_size")
    private Integer chunkSize;

    
    @TableField("total_chunks")
    private Integer totalChunks;

    
    @TableField("uploaded_chunks")
    private Integer uploadedChunks;

    
    @TableField("file_pid")
    private String filePid;

    
    @TableField("status")
    @TableLogic
    private Byte status;

    
    @TableField("create_time")
    private LocalDateTime createTime;

    
    @TableField("update_time")
    private LocalDateTime updateTime;

    
    @TableField("expire_time")
    private LocalDateTime expireTime;
}
