package com.easypan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.easypan.entity.dto.FileListQueryDTO;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.vo.FileInfoVO;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 文件信息表 服务接口（纯接口版，正确定义）
 * 核心：仅继承 MyBatis-Plus 的 IService 接口，不继承任何实现类
 */
public interface FileInfoService extends IService<FileInfo> {

    /**
     * 分页加载文件列表
     */
    IPage<FileInfoVO> loadFileList(FileListQueryDTO queryDTO, String userId);

    /**
     * 新建文件夹
     */
    FileInfo createFolder(String filePid, String folderName, String userId);

    /**
     * 文件重命名
     */
    void renameFile(String fileId, String newFileName, String userId);

    /**
     * 移动文件/文件夹
     */
    void moveFile(String fileId, String targetPid, String userId);

    /**
     * 删除文件（移入回收站）
     */
    void deleteFile(String fileId, String userId);

    /**
     * 获取文件路径（面包屑）
     */
    List<FileInfoVO> getFilePath(String fileId, String userId);

    /**
     * 获取目录树
     */
    List<FileInfoVO> getFolderTree(String userId);

    void getFileCover(String fileId, String userId, HttpServletResponse response);
    void previewFile(String fileId, String userId, HttpServletResponse response);
    String createDownloadUrl(String fileId, String userId);
    void downloadFile(String fileId, String code, HttpServletResponse response);
    IPage<FileInfoVO> getRecycleFileList(FileListQueryDTO queryDTO, String userId);
    void recoverFile(String fileId, String userId);
    void permanentDeleteFile(String fileId, String userId);
}