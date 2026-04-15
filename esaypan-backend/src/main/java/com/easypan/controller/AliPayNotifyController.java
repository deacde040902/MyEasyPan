package com.easypan.controller;

import com.alipay.easysdk.factory.Factory;
import com.easypan.annotation.GlobalInterceptor;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.po.VipOrder;
import com.easypan.entity.enums.VipPackageEnum;
import com.easypan.mappers.UserInfoMapper;
import com.easypan.service.VipOrderService;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付回调控制器
 * 功能：处理支付宝异步回调，完成订单状态更新+VIP开通
 */
@RestController
@RequestMapping("/aliPay")
public class AliPayNotifyController {

    // 日志组件
    private static final Logger log = LoggerFactory.getLogger(AliPayNotifyController.class);

    @Resource
    private VipOrderService vipOrderService;

    @Resource
    private UserInfoMapper userInfoMapper;

    /**
     * 支付宝异步回调接口
     * 注意：该接口必须是POST、公网可访问、无登录拦截
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        // 1. 解析支付宝回调参数
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> req = request.getParameterMap();
        for (String key : req.keySet()) {
            // 处理参数多值问题，取第一个值
            String[] values = req.get(key);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(key, valueStr);
        }
        log.info("接收支付宝回调参数：{}", params);

        try {
            // 2. 支付宝验签（核心：验证回调来源合法性）
            boolean signVerified = Factory.Payment.Common().verifyNotify(params);
            log.info("🔍 支付宝验签结果：{}", signVerified);
            if (!signVerified) {
                log.error("支付宝验签失败，回调参数不合法");
                return "fail";
            }

            // 3. 校验订单号参数
            String orderId = params.get("out_trade_no");
            if (orderId == null || orderId.isEmpty()) {
                log.error("回调参数中无订单号（out_trade_no）");
                return "fail";
            }

            // 4. 查询订单（校验订单存在性）
            VipOrder order = vipOrderService.getById(orderId);
            if (order == null) {
                log.error("订单不存在：{}", orderId);
                return "fail";
            }

            // 5. 幂等性校验（避免重复回调处理）
            if (order.getPayStatus() == 1) {
                log.info("订单已支付完成，无需重复处理：{}", orderId);
                return "success";
            }

            // 6. 更新订单状态
            order.setPayStatus((byte) 1);
            order.setPayTime(new Date());
            boolean updateOrder = vipOrderService.updateById(order);
            if (!updateOrder) {
                log.error("订单状态更新失败：{}", orderId);
                return "fail";
            }
            log.info("✅ 订单状态更新成功：{}", orderId);

            // 7. 开通VIP
            UserInfo user = userInfoMapper.selectById(order.getUserId());
            if (user == null) {
                log.error("订单关联用户不存在：{}", order.getUserId());
                return "fail";
            }

            // 计算VIP过期时间（支持续费叠加）
            Date now = new Date();
            Date expireTime;
            if (user.getVipExpireTime() == null || user.getVipExpireTime().before(now)) {
                // 新开通/已过期：从当前时间开始计算
                expireTime = DateUtils.addMonths(now, VipPackageEnum.MONTH.getMonths());
            } else {
                // 未过期：在原有过期时间上叠加
                expireTime = DateUtils.addMonths(user.getVipExpireTime(), VipPackageEnum.MONTH.getMonths());
            }

            // 更新用户VIP信息
            user.setVipLevel(1);
            user.setVipExpireTime(expireTime);
            user.setVip(1);
            user.syncVipSpace(); // 同步VIP空间
            userInfoMapper.updateById(user);
            log.info("用户{}开通VIP成功，过期时间：{}", user.getUserId(), expireTime);

            return "success";
        } catch (Exception e) {
            log.error("支付宝回调处理异常", e);
            return "fail";
        }
    }
}