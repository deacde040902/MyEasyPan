package com.easypan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.FileListQueryDTO;
import com.easypan.entity.enums.FileStatusEnum;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.redis.RedisUtils;
import com.easypan.service.FileInfoService;
import com.easypan.utils.StringTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件信息表 服务实现类（完整修正版）
 */
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoService {

    @Resource
    private RedisUtils redisUtils;


    @Value("${easypan.file.base-path:E:/Workspace-java/easypan/easypan-backend/file}")
    private String fileBasePath;

    // ========== 原有方法保留，仅修正错误 & 补全缺失方法 ==========

    /**
     * 分页加载文件列表
     */
    @Override
    public IPage<FileInfoVO> loadFileList(FileListQueryDTO queryDTO, String userId) {
        Page<FileInfo> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getStatus, FileStatusEnum.NORMAL.getCode())
                .eq(FileInfo::getFilePid, queryDTO.getFilePid())
                .orderByDesc(FileInfo::getLastOpTime);

        if (!"all".equals(queryDTO.getCategory())) {
            List<String> suffixList = getSuffixByCategory(queryDTO.getCategory());
            if (!suffixList.isEmpty()) {
                wrapper.in(FileInfo::getFileSuffix, suffixList);
            }
        }
        if (StringTools.isNotEmpty(queryDTO.getFileName())) {
            wrapper.like(FileInfo::getFileName, queryDTO.getFileName());
        }
        IPage<FileInfo> filePage = this.page(page, wrapper);
        return filePage.convert(this::convertToVO);
    }

    /**
     * 新建文件夹
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo createFolder(String filePid, String folderName, String userId) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getFilePid, filePid)
                .eq(FileInfo::getFileName, folderName)
                .eq(FileInfo::getIsFolder, Constants.ONE);
        if (this.count(wrapper) > 0) {
            throw new BusinessException("文件夹名称已存在");
        }
        FileInfo folder = new FileInfo();
        folder.setFileId(UUID.randomUUID().toString().replace("-", "").substring(0, 15));
        folder.setUserId(userId);
        folder.setFilePid(filePid);
        folder.setFileName(folderName);
        folder.setIsFolder(Constants.ONE);
        folder.setFileSize(0L);
        folder.setStatus(FileStatusEnum.NORMAL.getCode());
        folder.setCreateTime(new Date());
        folder.setLastOpTime(new Date());
        this.save(folder);
        return folder;
    }

    /**
     * 文件重命名
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameFile(String fileId, String newFileName, String userId) {
        FileInfo file = this.getById(fileId);
        if (file == null || !userId.equals(file.getUserId())) {
            throw new BusinessException("文件不存在或无操作权限");
        }
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getFilePid, file.getFilePid())
                .eq(FileInfo::getFileName, newFileName)
                .ne(FileInfo::getFileId, fileId);
        if (this.count(wrapper) > 0) {
            throw new BusinessException("文件名已存在");
        }
        file.setFileName(newFileName);
        file.setLastOpTime(new Date());
        this.updateById(file);
    }

    /**
     * 移动文件/文件夹
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveFile(String fileId, String targetPid, String userId) {
        FileInfo file = this.getById(fileId);
        if (file == null || !userId.equals(file.getUserId())) {
            throw new BusinessException("文件不存在或无操作权限");
        }
        if (!"0".equals(targetPid)) {
            FileInfo targetFolder = this.getById(targetPid);
            if (targetFolder == null || !userId.equals(targetFolder.getUserId())
                    || Constants.ZERO.equals(targetFolder.getIsFolder())) {
                throw new BusinessException("目标目录不存在");
            }
        }
        if (isChildFolder(fileId, targetPid, userId)) {
            throw new BusinessException("不能移动到自身子目录");
        }
        file.setFilePid(targetPid);
        file.setLastOpTime(new Date());
        this.updateById(file);
    }

    /**
     * 删除文件（移入回收站）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(String fileId, String userId) {
        FileInfo file = this.getById(fileId);
        if (file == null || !userId.equals(file.getUserId())) {
            throw new BusinessException("文件不存在或无操作权限");
        }
        file.setStatus(FileStatusEnum.RECYCLE.getCode());
        file.setDeleteTime(new Date());
        file.setLastOpTime(new Date());
        this.updateById(file);
        if (Constants.ONE.equals(file.getIsFolder())) {
            deleteChildFiles(file.getFileId(), userId);
        }
    }

    /**
     * 获取文件路径（面包屑）
     */
    @Override
    public List<FileInfoVO> getFilePath(String fileId, String userId) {
        List<FileInfoVO> pathList = new ArrayList<>();
        String currentFileId = fileId;
        while (!"0".equals(currentFileId)) {
            FileInfo file = this.getById(currentFileId);
            if (file == null || !userId.equals(file.getUserId())) {
                break;
            }
            pathList.add(0, convertToVO(file));
            currentFileId = file.getFilePid();
        }
        return pathList;
    }

    /**
     * 获取目录树
     */
    @Override
    public List<FileInfoVO> getFolderTree(String userId) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getIsFolder, Constants.ONE)
                .eq(FileInfo::getStatus, FileStatusEnum.NORMAL.getCode())
                .orderByAsc(FileInfo::getCreateTime);
        List<FileInfo> folderList = this.list(wrapper);
        return folderList.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 显示文件封面
     */
    @Override
    public void getFileCover(String fileId, String userId, HttpServletResponse response) {
        FileInfo file = this.getById(fileId);
        if (file == null || !userId.equals(file.getUserId()) || !FileStatusEnum.NORMAL.getCode().equals(file.getStatus())) {
            throw new BusinessException("文件不存在或无权限");
        }

        // ====== 如果是文件夹，直接返回默认封面 ======
        if (Constants.ONE.equals(file.getIsFolder())) {
            returnDefaultCover(response);
            return;
        }

        // ====== 获取文件真实路径 ======
        String filePath = fileBasePath + File.separator + file.getFileUrl();
        File coverFile = new File(filePath);

        // ====== 文件不存在 → 返回默认封面 ======
        if (!coverFile.exists()) {
            returnDefaultCover(response);
            return;
        }

        // ====== 图片 → 输出真实图片封面 ======
        String suffix = file.getFileSuffix() == null ? "" : file.getFileSuffix().toLowerCase();
        try (InputStream is = new FileInputStream(coverFile);
             OutputStream os = response.getOutputStream()) {

            switch (suffix) {
                case "jpg":
                case "jpeg":
                    response.setContentType(MediaType.IMAGE_JPEG_VALUE);
                    break;
                case "png":
                    response.setContentType(MediaType.IMAGE_PNG_VALUE);
                    break;
                case "gif":
                    response.setContentType(MediaType.IMAGE_GIF_VALUE);
                    break;
                // 不是图片 → 返回默认封面
                default:
                    returnDefaultCover(response);
                    return;
            }
            FileCopyUtils.copy(is, os);
        } catch (Exception e) {
            throw new BusinessException("获取封面失败：" + e.getMessage());
        }
    }

    /**
     * 预览文件
     */
    @Override
    public void previewFile(String fileId, String userId, HttpServletResponse response) {
        FileInfo file = this.getById(fileId);
        if (file == null || !userId.equals(file.getUserId()) || !FileStatusEnum.NORMAL.getCode().equals(file.getStatus())) {
            throw new BusinessException("文件不存在或无权限");
        }
        if (Constants.ONE.equals(file.getIsFolder())) {
            throw new BusinessException("文件夹无法预览");
        }
        String filePath = fileBasePath + File.separator + file.getFileUrl();
        File previewFile = new File(filePath);
        if (!previewFile.exists()) {
            throw new BusinessException("文件不存在");
        }
        try (InputStream is = new FileInputStream(previewFile);
             OutputStream os = response.getOutputStream()) {
            String suffix = file.getFileSuffix().toLowerCase();
            setPreviewContentType(response, suffix);
            FileCopyUtils.copy(is, os);
        } catch (Exception e) {
            throw new BusinessException("预览文件失败：" + e.getMessage());
        }
    }

    /**
     * 创建文件下载链接（修正RedisUtils.set参数错误）
     */
    @Override
    public String createDownloadUrl(String fileId, String userId) {
        FileInfo file = this.getById(fileId);
        if (file == null || !userId.equals(file.getUserId()) || !FileStatusEnum.NORMAL.getCode().equals(file.getStatus())) {
            throw new BusinessException("文件不存在或无权限");
        }
        String downloadCode = StringTools.getRandomString(16);
        String redisKey = Constants.REDIS_KEY_DOWNLOAD_CODE + downloadCode;
        // ✅ 修正：如果RedisUtils.set仅支持2参数，先存值再单独设置过期
        redisUtils.set(redisKey, fileId);
        redisUtils.expire(redisKey, 3600L); // 单独设置1小时过期
        return downloadCode;
    }

    /**
     * 下载文件
     */
    @Override
    public void downloadFile(String fileId, String code, HttpServletResponse response) {
        String redisKey = Constants.REDIS_KEY_DOWNLOAD_CODE + code;
        String redisFileId = (String) redisUtils.get(redisKey);
        if (redisFileId == null || !redisFileId.equals(fileId)) {
            throw new BusinessException("下载链接无效或已过期");
        }
        FileInfo file = this.getById(fileId);
        if (file == null || !FileStatusEnum.NORMAL.getCode().equals(file.getStatus())) {
            throw new BusinessException("文件不存在");
        }
        String filePath = fileBasePath + File.separator + file.getFileUrl();
        File downloadFile = new File(filePath);
        if (!downloadFile.exists()) {
            throw new BusinessException("文件不存在");
        }
        try (InputStream is = new FileInputStream(downloadFile);
             OutputStream os = response.getOutputStream()) {
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment;filename=" + URLEncoder.encode(file.getFileName(), "UTF-8"));
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(downloadFile.length()));
            FileCopyUtils.copy(is, os);
            redisUtils.delete(redisKey);
        } catch (Exception e) {
            throw new BusinessException("下载文件失败：" + e.getMessage());
        }
    }

    /**
     * 获取回收站文件列表
     */
    @Override
    public IPage<FileInfoVO> getRecycleFileList(FileListQueryDTO queryDTO, String userId) {
        Page<FileInfo> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getStatus, FileStatusEnum.RECYCLE.getCode())
                .orderByDesc(FileInfo::getDeleteTime);
        if (StringTools.isNotEmpty(queryDTO.getFileName())) {
            wrapper.like(FileInfo::getFileName, queryDTO.getFileName());
        }
        IPage<FileInfo> filePage = this.page(page, wrapper);
        return filePage.convert(this::convertToVO);
    }

    /**
     * 恢复回收站文件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFile(String fileId, String userId) {
        FileInfo file = this.getById(fileId);
        if (file == null || !userId.equals(file.getUserId()) || !FileStatusEnum.RECYCLE.getCode().equals(file.getStatus())) {
            throw new BusinessException("文件不存在或非回收站文件");
        }
        file.setStatus(FileStatusEnum.NORMAL.getCode());
        file.setDeleteTime(null);
        file.setLastOpTime(new Date());
        this.updateById(file);
        if (Constants.ONE.equals(file.getIsFolder())) {
            recoverChildFiles(file.getFileId(), userId);
        }
    }

    /**
     * 永久删除文件（解决抽象方法未实现报错）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permanentDeleteFile(String fileId, String userId) {
        FileInfo file = this.getById(fileId);
        if (file == null || !userId.equals(file.getUserId())) {
            throw new BusinessException("文件不存在或无权限");
        }
        // 物理删除文件
        if (Constants.ZERO.equals(file.getIsFolder())) {
            String filePath = fileBasePath + File.separator + file.getFileUrl();
            File physicalFile = new File(filePath);
            if (physicalFile.exists()) {
                physicalFile.delete();
            }
        }
        // 递归删除子文件
        if (Constants.ONE.equals(file.getIsFolder())) {
            permanentDeleteChildFiles(file.getFileId(), userId);
        }
        // 删除数据库记录
        this.removeById(fileId);
    }

    // ========== 私有工具方法（全部补全） ==========
    private FileInfoVO convertToVO(FileInfo file) {
        FileInfoVO vo = new FileInfoVO();
        vo.setFileId(file.getFileId());
        vo.setFilePid(file.getFilePid());
        vo.setFileName(file.getFileName());
        vo.setFileSuffix(file.getFileSuffix());
        vo.setFileSize(file.getFileSize());
        vo.setFileSizeStr(formatFileSize(file.getFileSize()));
        vo.setIsFolder(file.getIsFolder());
        vo.setCreateTime(file.getCreateTime());
        vo.setLastOpTime(file.getLastOpTime());
        return vo;
    }

    private String formatFileSize(Long size) {
        if (size == null || size == 0) return "0B";
        if (size < 1024) return size + "B";
        if (size < 1024 * 1024) return String.format("%.2fKB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2fMB", size / (1024.0 * 1024));
        return String.format("%.2fGB", size / (1024.0 * 1024 * 1024));
    }

    private List<String> getSuffixByCategory(String category) {
        Map<String, List<String>> categoryMap = new HashMap<>();
        categoryMap.put("image", Arrays.asList(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp", ".svg", ".ico"));
        categoryMap.put("video", Arrays.asList(".mp4", ".avi", ".mkv", ".mov", ".flv", ".wmv", ".rmvb", ".webm"));
        categoryMap.put("audio", Arrays.asList(".mp3", ".wav", ".flac", ".aac", ".ogg", ".wma", ".m4a"));
        categoryMap.put("document", Arrays.asList(".doc", ".docx", ".pdf", ".txt", ".xls", ".xlsx", ".ppt", ".pptx", ".md"));
        categoryMap.put("other", Arrays.asList(".zip", ".rar", ".7z", ".exe", ".apk", ".iso", ".dmg", ".log"));
        return categoryMap.getOrDefault(category.toLowerCase(), Collections.emptyList());
    }

    private boolean isChildFolder(String sourceFolderId, String targetPid, String userId) {
        if (sourceFolderId.equals(targetPid)) return true;
        String currentPid = targetPid;
        while (!"0".equals(currentPid)) {
            FileInfo folder = this.getById(currentPid);
            if (folder == null || !userId.equals(folder.getUserId())) break;
            if (sourceFolderId.equals(folder.getFileId())) return true;
            currentPid = folder.getFilePid();
        }
        return false;
    }

    private void deleteChildFiles(String parentId, String userId) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getFilePid, parentId)
                .eq(FileInfo::getStatus, FileStatusEnum.NORMAL.getCode());
        List<FileInfo> childFiles = this.list(wrapper);
        for (FileInfo child : childFiles) {
            child.setStatus(FileStatusEnum.RECYCLE.getCode());
            child.setDeleteTime(new Date());
            child.setLastOpTime(new Date());
            this.updateById(child);
            if (Constants.ONE.equals(child.getIsFolder())) {
                deleteChildFiles(child.getFileId(), userId);
            }
        }
    }

    private void returnDefaultCover(HttpServletResponse response) {
        try {
            response.setContentType(MediaType.IMAGE_PNG_VALUE);
            // 1×1 透明小图片（不会空白）
            String base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
            byte[] pngBytes = Base64.getDecoder().decode(base64);
            response.getOutputStream().write(pngBytes);
        } catch (Exception e) {
            throw new BusinessException("获取默认封面失败");
        }
    }

    private void setPreviewContentType(HttpServletResponse response, String suffix) {
        switch (suffix.toLowerCase()) {
            case "jpg":
            case "jpeg":
                response.setContentType(MediaType.IMAGE_JPEG_VALUE);
                break;
            case "png":
                response.setContentType(MediaType.IMAGE_PNG_VALUE);
                break;
            case "gif":
                response.setContentType(MediaType.IMAGE_GIF_VALUE);
                break;
            case "pdf":
                response.setContentType("application/pdf");
                break;
            case "txt":
                response.setContentType("text/plain;charset=UTF-8");
                break;
            case "mp4":
                response.setContentType("video/mp4");
                break;
            case "mp3":
                response.setContentType("audio/mpeg");
                break;
            default:
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        }
    }

    private void recoverChildFiles(String parentId, String userId) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getFilePid, parentId)
                .eq(FileInfo::getStatus, FileStatusEnum.RECYCLE.getCode());
        List<FileInfo> childFiles = this.list(wrapper);
        for (FileInfo child : childFiles) {
            child.setStatus(FileStatusEnum.NORMAL.getCode());
            child.setDeleteTime(null);
            child.setLastOpTime(new Date());
            this.updateById(child);
            if (Constants.ONE.equals(child.getIsFolder())) {
                recoverChildFiles(child.getFileId(), userId);
            }
        }
    }

    private void permanentDeleteChildFiles(String parentId, String userId) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getFilePid, parentId);
        List<FileInfo> childFiles = this.list(wrapper);
        for (FileInfo child : childFiles) {
            if (Constants.ZERO.equals(child.getIsFolder())) {
                String filePath = fileBasePath + File.separator + child.getFileUrl();
                File physicalFile = new File(filePath);
                if (physicalFile.exists()) {
                    physicalFile.delete();
                }
            }
            if (Constants.ONE.equals(child.getIsFolder())) {
                permanentDeleteChildFiles(child.getFileId(), userId);
            }
            this.removeById(child.getFileId());
        }
    }
}