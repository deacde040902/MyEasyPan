package com.easypan.entity.dto;
import lombok.Data;
@Data
public class FileMoveDTO {
    private String fileIds; // 多个文件用逗号分隔
    private String targetPid;
}