package com.easypan.service;

import com.easypan.entity.po.VipOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author licheng
 * @since 2026-03-19
 */
public interface VipOrderService extends IService<VipOrder> {
    String createPayOrder(String token, Integer months);
}
