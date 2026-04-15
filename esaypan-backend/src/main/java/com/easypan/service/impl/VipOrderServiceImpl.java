package com.easypan.service.impl;

import com.alipay.easysdk.factory.Factory;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easypan.entity.dto.TokenUserInfoDto;
import com.easypan.entity.enums.VipPackageEnum;
import com.easypan.entity.po.VipOrder;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.VipOrderMapper;
import com.easypan.redis.RedisComponent;
import com.easypan.service.VipOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Service
public class VipOrderServiceImpl extends ServiceImpl<VipOrderMapper, VipOrder> implements VipOrderService {

    private static final Logger log =LoggerFactory.getLogger(VipOrderServiceImpl.class);

    @Resource
    private RedisComponent redisComponent;

    /**
     * 修复：现在是 2 个参数 (token, months)
     */
    @Override
    public String createPayOrder(String token, Integer months) {
        // 1. 获取用户
        TokenUserInfoDto user = redisComponent.getTokenUserInfoDto(token);
        if (user == null) {
            throw new BusinessException("登录态失效");
        }

        // 2. 根据月数获取套餐（这里你可以扩展多档位，目前用 MONTH 单价）
        VipPackageEnum pkg = VipPackageEnum.MONTH;
        // 总金额 = 单价 * 月数
        BigDecimal totalAmount = BigDecimal.valueOf(pkg.getPrice()).multiply(BigDecimal.valueOf(months));

        // 3. 构建订单
        VipOrder order = new VipOrder();
        order.setOrderId(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(user.getUserId());
        order.setProductName(months + "个月VIP");
        order.setAmount(totalAmount.setScale(2, BigDecimal.ROUND_HALF_UP));
        order.setMonths(months); // 保存开通月数
        order.setPayStatus((byte) 0); // 0-待支付
        order.setCreateTime(new Date());
        this.save(order);
        log.info("✅ 创建订单成功：{}，金额：{}元", order.getOrderId(), totalAmount);

        // 4. 支付宝支付
        try {
            // 同步回调地址（支付成功页面跳转）
            String returnUrl = "https://lisa-overartificial-janet.ngrok-free.dev/api/vip-order/payReturn";
            // 调用支付宝SDK生成支付HTML
            return Factory.Payment.Page().pay(
                    order.getProductName(), // 标题
                    order.getOrderId(),     // 订单号
                    order.getAmount().toString(), // 金额
                    returnUrl
            ).getBody();
        } catch (Exception e) {
            log.error("❌ 创建支付宝订单失败", e);
            throw new BusinessException("支付接口异常");
        }
    }
}