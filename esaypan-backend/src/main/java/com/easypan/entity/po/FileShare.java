package com.easypan.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("file_share")
public class FileShare {


    @TableId(value = "share_id", type = IdType.ASSIGN_ID)
    private String shareId;

    private String fileId;
    private String userId;
    private String shareCode;
    private LocalDateTime expireTime;
    private Byte status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}