package com.easypan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.easypan.entity.po.FileShare;
import com.easypan.entity.vo.FileShareVO;

import java.util.List;

/**
 * 文件分享表 服务接口（补全版）
 */
public interface FileShareService extends IService<FileShare> {

    /**
     * 创建分享链接
     */
    FileShareVO createShare(String fileId, Integer expireType, String userId);

    /**
     * 取消分享
     */
    void cancelShare(String shareId, String userId);

    /**
     * 校验分享码
     */
    void verifyShareCode(String shareId, String shareCode);

    /**
     * 获取分享文件信息
     */
    FileShare getShareFileInfo(String shareId);

    // ========== 新增：缺失的两个核心方法 ==========
    /**
     * 获取用户的分享列表
     */
    List<FileShareVO> getShareList(String userId);

    /**
     * 保存分享文件到我的网盘
     */
    void saveShareFile(String shareId, String filePid, String userId);
}