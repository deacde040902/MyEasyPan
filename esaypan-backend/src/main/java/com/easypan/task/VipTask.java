package com.easypan.task;

import com.easypan.entity.po.UserInfo;
import com.easypan.entity.enums.UserVipLevelEnum;
import com.easypan.service.UserInfoService;
import com.easypan.utils.VipUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * VIP到期定时任务（每天凌晨1点执行）
 */
@Component
public class VipTask {

    @Resource
    private UserInfoService userInfoService;
    @Resource
    private VipUtils vipUtils;

    /**
     * 每天凌晨1点检查VIP到期用户
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkVipExpire() {
        // 1. 查询所有VIP用户
        List<UserInfo> vipUsers = userInfoService.lambdaQuery()
                .eq(UserInfo::getVip, 1)
                .isNotNull(UserInfo::getVipExpireTime)
                .list();
        // 2. 遍历校验过期
        Date now = new Date();
        for (UserInfo user : vipUsers) {
            if (user.getVipExpireTime().before(now)) {
                // 到期降级
                vipUtils.expireVip(user);
            }
        }
    }
}