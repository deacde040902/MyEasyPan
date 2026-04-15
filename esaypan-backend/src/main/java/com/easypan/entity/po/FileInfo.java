package com.easypan.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 文件信息实体类
 */
@Data
@TableName("file_info")
public class FileInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "file_id", type = IdType.INPUT)
    private String fileId;

    @TableField("user_id")
    private String userId;

    @TableField("file_pid")
    private String filePid;

    @TableField("file_name")
    private String fileName;

    @TableField("file_suffix")
    private String fileSuffix;

    @TableField("file_size")
    private Long fileSize;

    @TableField("is_folder")
    private Integer isFolder;

    @TableField("file_md5")
    private String fileMd5;

    @TableField("file_url")
    private String fileUrl;

    @TableField("cover_url")
    private String coverUrl;

    @TableField("status")
    private Integer status;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField("delete_time")
    private Date deleteTime;

    @TableField("last_op_time")
    private Date lastOpTime;

    // 非数据库字段（前端展示用）
    @TableField(exist = false)
    private String fileSizeStr;
}