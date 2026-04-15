package com.easypan.utils;

import com.easypan.entity.dto.TokenUserInfoDto;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.enums.UserVipLevelEnum;
import com.easypan.exception.BusinessException;
import com.easypan.redis.RedisComponent;
import com.easypan.service.UserInfoService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * VIP 权限/空间工具类（完整实现）
 */
@Component
public class VipUtils {

    @Resource
    private RedisComponent redisComponent;
    @Resource
    private UserInfoService userInfoService;

    /**
     * 校验用户是否为VIP（接口权限用）
     * 修复：从DB查询最新VIP状态，解决Redis信息过期问题
     */
    public void checkVipPermission(String token) {
        // 1. 获取登录用户ID
        TokenUserInfoDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(token);
        if (tokenUserInfoDto == null) {
            throw new BusinessException("登录态失效，请重新登录");
        }
        // 2. 从DB查询最新用户信息（核心修复）
        UserInfo user = userInfoService.getUserInfoByUserId(tokenUserInfoDto.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 3. 校验VIP状态（含过期校验）
        checkVipStatus(user);
        if (!user.getVip().equals(1)) {
            throw new BusinessException("该功能仅VIP会员可用，请开通会员");
        }
    }

    /**
     * 获取用户总空间（区分VIP/普通）
     * 实现：对接DB，支持空间校验
     */
    public Long getUserTotalSpace(String userId) {
        UserInfo user = userInfoService.getUserInfoByUserId(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 实时校验VIP状态
        checkVipStatus(user);
        return user.getTotalSpace();
    }

    /**
     * 开通VIP（核心业务）
     * 实现：对接DB，支持按月续费，同步空间
     */
    public void openVip(String userId, Integer months) {
        if (months == null || months < 1 || months > 12) {
            throw new BusinessException("请选择1-12个月的续费时长");
        }
        // 1. 查询用户
        UserInfo user = userInfoService.getUserInfoByUserId(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 2. 计算过期时间（续费叠加）
        Date now = new Date();
        Date expireTime;
        if (user.getVipExpireTime() == null || user.getVipExpireTime().before(now)) {
            // 新开通/已过期：从现在开始算
            expireTime = new Date(now.getTime() + months * 30L * 24 * 60 * 60 * 1000);
        } else {
            // 未过期：叠加时长
            expireTime = new Date(user.getVipExpireTime().getTime() + months * 30L * 24 * 60 * 60 * 1000);
        }
        // 3. 更新VIP状态
        user.setVipLevel(1);
        user.setVipExpireTime(expireTime);
        user.setVip(1);
        user.syncVipSpace(); // 同步VIP空间
        // 4. 保存到DB
        userInfoService.updateById(user);
    }

    /**
     * VIP到期处理
     * 实现：降级VIP，恢复普通用户空间
     */
    public void expireVip(UserInfo user) {
        user.setVipLevel(0);
        user.setVip(0);
        user.setVipExpireTime(null);
        user.syncVipSpace(); // 恢复普通空间
        userInfoService.updateById(user);
    }

    /**
     * 内部方法：校验VIP是否过期（通用逻辑）
     */
    private void checkVipStatus(UserInfo user) {
        if (user.getVip().equals(1) && user.getVipExpireTime() != null) {
            Date now = new Date();
            // VIP已过期：自动降级
            if (user.getVipExpireTime().before(now)) {
                expireVip(user);
            }
        }
    }

    /**
     * 扩展：校验用户上传文件空间是否足够
     * 对接文件上传功能时调用
     */
    public void checkSpaceEnough(String userId, Long fileSize) {
        Long totalSpace = getUserTotalSpace(userId);
        UserInfo user = userInfoService.getUserInfoByUserId(userId);
        // 已用空间 + 文件大小 > 总空间：空间不足
        if (user.getUseSpace() + fileSize > totalSpace) {
            throw new BusinessException("存储空间不足，当前已用：" +
                    FileSizeUtils.formatFileSize(user.getUseSpace()) +
                    "，总空间：" + FileSizeUtils.formatFileSize(totalSpace));
        }
    }
}