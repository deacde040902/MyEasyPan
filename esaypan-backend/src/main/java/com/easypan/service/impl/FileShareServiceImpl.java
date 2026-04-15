package com.easypan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.enums.FileStatusEnum;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.po.FileShare;
import com.easypan.entity.vo.FileShareVO;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.FileShareMapper;
import com.easypan.service.FileInfoService;
import com.easypan.service.FileShareService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件分享表 服务实现类（完整修复版）
 */
@Service
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare> implements FileShareService {

    @Resource
    private FileInfoService fileInfoService;

    /**
     * 创建分享链接
     */
    /**
     */
    @Override
    public FileShareVO createShare(String fileId, Integer expireType, String userId) {
        // 1. 校验文件是否存在且属于该用户
        FileInfo fileInfo = fileInfoService.getById(fileId);
        if (fileInfo == null || !userId.equals(fileInfo.getUserId())) {
            throw new BusinessException("文件不存在或无权限");
        }

        // 2. 生成分享信息
        FileShare fileShare = new FileShare();
        String shareId = UUID.randomUUID().toString().replace("-", "");
        fileShare.setShareId(shareId);
        fileShare.setFileId(fileId);
        fileShare.setUserId(userId);

        // 3. 生成6位随机提取码
        String shareCode = generateVerifyCode(6);
        fileShare.setShareCode(shareCode);

        // 4. 设置过期时间
        LocalDateTime now = LocalDateTime.now();
        switch (expireType) {
            case 1: // 1天
                fileShare.setExpireTime(now.plusDays(1));
                break;
            case 2: // 7天
                fileShare.setExpireTime(now.plusDays(7));
                break;
            default: // 永久
                fileShare.setExpireTime(null);
                break;
        }

        fileShare.setStatus((byte) 0);
        fileShare.setCreateTime(now);
        fileShare.setUpdateTime(now);

        // 5. 保存分享记录
        this.save(fileShare);

        // 6. ✅ 关键修改：构建并返回包含 shareId 和 shareCode 的 VO 对象
        FileShareVO vo = new FileShareVO();
        vo.setShareId(shareId);
        vo.setShareCode(shareCode); // 携带提取码返回
        vo.setFileName(fileInfo.getFileName()); // 可选：带上文件名方便前端显示
        vo.setExpireTime(fileShare.getExpireTime());

        return vo;
    }

    /**
     * 取消分享
     */
    @Override
    public void cancelShare(String shareId, String userId) {
        FileShare fileShare = this.getById(shareId);
        if (fileShare == null || !userId.equals(fileShare.getUserId())) {
            throw new BusinessException("分享不存在或无权限");
        }

        // 逻辑删除：更新状态为1（取消）
        fileShare.setStatus((byte) 1);
        fileShare.setUpdateTime(LocalDateTime.now());
        this.updateById(fileShare);
    }

    /**
     * 校验分享码
     */
    @Override
    public void verifyShareCode(String shareId, String shareCode) {
        FileShare fileShare = this.getById(shareId);
        if (fileShare == null) {
            throw new BusinessException("分享链接不存在");
        }

        // 校验状态
        if (fileShare.getStatus() == 1) {
            throw new BusinessException("分享已取消");
        }

        // 校验过期时间
        if (fileShare.getExpireTime() != null && LocalDateTime.now().isAfter(fileShare.getExpireTime())) {
            throw new BusinessException("分享已过期");
        }

        // 校验提取码
        if (!shareCode.equals(fileShare.getShareCode())) {
            throw new BusinessException("提取码错误");
        }
    }

    /**
     * 获取分享文件信息
     */
    @Override
    public FileShare getShareFileInfo(String shareId) {
        FileShare fileShare = this.getById(shareId);
        if (fileShare == null) {
            throw new BusinessException("分享不存在");
        }

        // 校验状态和过期时间
        if (fileShare.getStatus() == 1) {
            throw new BusinessException("分享已取消");
        }
        if (fileShare.getExpireTime() != null && LocalDateTime.now().isAfter(fileShare.getExpireTime())) {
            throw new BusinessException("分享已过期");
        }

        return fileShare;
    }

    // ========== 新增：实现 getShareList 方法 ==========
    /**
     * 获取用户的分享列表（仅返回正常状态的分享）
     */
    @Override
    public List<FileShareVO> getShareList(String userId) {
        // 1. 构建查询条件：当前用户 + 正常状态 + 未过期
        LambdaQueryWrapper<FileShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileShare::getUserId, userId)
                .eq(FileShare::getStatus, (byte) 0) // 0-正常分享
                .orderByDesc(FileShare::getCreateTime);

        // 2. 查询并转换为VO（隐藏敏感信息，如提取码）
        List<FileShare> shareList = this.list(wrapper);
        return shareList.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    // ========== 新增：实现 saveShareFile 方法 ==========
    /**
     * 保存分享文件到我的网盘
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveShareFile(String shareId, String filePid, String userId) {
        // 1. 校验分享链接有效性
        FileShare share = this.getShareFileInfo(shareId);
        if (share == null) {
            throw new BusinessException("分享链接无效");
        }

        // 2. 获取分享的源文件
        FileInfo sourceFile = fileInfoService.getById(share.getFileId());
        if (sourceFile == null || sourceFile.getStatus() != FileStatusEnum.NORMAL.getCode()) {
            throw new BusinessException("分享的文件不存在或已被删除");
        }

        // 3. 校验目标目录是否存在（filePid=0 表示根目录）
        if (!"0".equals(filePid)) {
            FileInfo targetFolder = fileInfoService.getById(filePid);
            if (targetFolder == null || !userId.equals(targetFolder.getUserId())
                    || Constants.ZERO.equals(targetFolder.getIsFolder())) {
                throw new BusinessException("目标目录不存在或无权限");
            }
        }

        // 4. 检查目标目录是否已有同名文件
        LambdaQueryWrapper<FileInfo> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getFilePid, filePid)
                .eq(FileInfo::getFileName, sourceFile.getFileName())
                .eq(FileInfo::getIsFolder, sourceFile.getIsFolder());
        if (fileInfoService.count(checkWrapper) > 0) {
            throw new BusinessException("目标目录已存在同名文件/文件夹");
        }

        // 5. 复制文件到当前用户的网盘（复用文件存储路径，避免重复存储）
        FileInfo newFile = new FileInfo();
        newFile.setFileId(UUID.randomUUID().toString().replace("-", "").substring(0, 15));
        newFile.setUserId(userId);
        newFile.setFilePid(filePid);
        newFile.setFileName(sourceFile.getFileName());
        newFile.setFileSuffix(sourceFile.getFileSuffix());
        newFile.setFileSize(sourceFile.getFileSize());
        newFile.setFileUrl(sourceFile.getFileUrl()); // 复用源文件路径
        newFile.setIsFolder(sourceFile.getIsFolder());
        newFile.setStatus(FileStatusEnum.NORMAL.getCode());
        newFile.setCreateTime(new Date());
        newFile.setLastOpTime(new Date());

        fileInfoService.save(newFile);
    }

    // ==================== 私有工具方法 ====================
    /**
     * 生成随机提取码
     */
    private String generateVerifyCode(int len) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(random.nextInt(10)); // 0-9 数字
        }
        return sb.toString();
    }

    /**
     * FileShare PO 转换为 VO（隐藏敏感信息）
     */
    private FileShareVO convertToVO(FileShare share) {
        FileShareVO vo = new FileShareVO();
        vo.setShareId(share.getShareId());
        vo.setFileId(share.getFileId());

        // 补充文件名称（从FileInfo中获取）
        FileInfo fileInfo = fileInfoService.getById(share.getFileId());
        if (fileInfo != null) {
            vo.setFileName(fileInfo.getFileName());
            vo.setIsFolder(fileInfo.getIsFolder());
            vo.setFileSize(fileInfo.getFileSize());
            vo.setFileSizeStr(formatFileSize(fileInfo.getFileSize()));
        }

        vo.setExpireTime(share.getExpireTime());
        vo.setCreateTime(share.getCreateTime());
        // 隐藏提取码（只返回后两位，保护隐私）
        String shareCode = share.getShareCode();
        if (shareCode != null && shareCode.length() >= 2) {
            vo.setShareCode("****" + shareCode.substring(shareCode.length() - 2));
        } else {
            vo.setShareCode("无");
        }
        return vo;
    }

    /**
     * 格式化文件大小（复用 FileInfoServiceImpl 的逻辑）
     */
    private String formatFileSize(Long size) {
        if (size == null || size == 0) return "0B";
        if (size < 1024) return size + "B";
        if (size < 1024 * 1024) return String.format("%.2fKB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2fMB", size / (1024.0 * 1024));
        return String.format("%.2fGB", size / (1024.0 * 1024 * 1024));
    }
}