package com.easypan.entity.vo;

import lombok.Data;
import java.util.Date;

@Data
public class FileInfoVO {
    private String fileId;
    private String filePid;
    private String fileName;
    private String fileSuffix;
    private Long fileSize;
    private String fileSizeStr; // 格式化后的文件大小
    private Integer isFolder;
    private Date createTime;
    private Date lastOpTime;
    private String userId;
}