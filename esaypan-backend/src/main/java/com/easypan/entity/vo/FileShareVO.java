package com.easypan.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 文件分享 VO（前端展示）
 */
@Data
public class FileShareVO {
    private String shareId;        // 分享ID
    private String fileId;         // 文件ID
    private String fileName;       // 文件名称
    private Integer isFolder;      // 是否文件夹（0-文件，1-文件夹）
    private Long fileSize;         // 文件大小
    private String fileSizeStr;    // 格式化后的文件大小
    private LocalDateTime expireTime; // 过期时间
    private LocalDateTime createTime; // 创建时间
    private String shareCode;      // 提取码（脱敏展示）
}