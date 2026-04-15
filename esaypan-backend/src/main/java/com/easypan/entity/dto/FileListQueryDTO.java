package com.easypan.entity.dto;

import lombok.Data;

@Data
public class FileListQueryDTO {
    private String filePid = "0"; // 默认根目录
    private String category = "all"; // all/image/video/document
    private String fileName; // 模糊搜索
    private Integer pageNo = 1;
    private Integer pageSize = 15;
}