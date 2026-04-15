package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.entity.dto.TokenUserInfoDto;
import com.easypan.entity.enums.VipPackageEnum;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.entity.vo.VipPackageVO;
import com.easypan.entity.vo.VipStatusVO;
import com.easypan.exception.BusinessException;
import com.easypan.redis.RedisComponent;
import com.easypan.service.VipOrderService;
import com.easypan.service.UserInfoService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author licheng
 * @since 2026-03-19
 */
@RestController
@RequestMapping("/vip-order")
public class VipOrderController extends ABaseController{
    @Resource
    private VipOrderService vipOrderService;
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private UserInfoService userInfoService;

    @PostMapping("/createPayOrder")
    @GlobalInterceptor
    public ResponseVO createPayOrder(@RequestHeader String token,Integer months) {
        return getSuccessResponseVO(vipOrderService.createPayOrder(token,months));
    }

    @GetMapping("/payReturn")
    public ResponseVO payReturn(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> req = request.getParameterMap();
        for (String key : req.keySet()) {
            params.put(key, request.getParameter(key));
        }
        return getSuccessResponseVO("支付成功，VIP已开通");
    }

    /**
     * 获取VIP套餐列表
     */
    @GetMapping("/packages")
    @GlobalInterceptor
    public ResponseVO getVipPackages() {
        List<VipPackageVO> packages = new ArrayList<>();
        // 从枚举中获取所有VIP套餐
        for (VipPackageEnum pkg : VipPackageEnum.values()) {
            VipPackageVO vo = new VipPackageVO();
            vo.setPackageId(pkg.name());
            vo.setName(pkg.getName());
            vo.setMonths(pkg.getMonths());
            vo.setPrice(pkg.getPrice());
            packages.add(vo);
        }
        return getSuccessResponseVO(packages);
    }

    /**
     * 获取VIP状态
     */
    @GetMapping("/status")
    @GlobalInterceptor
    public ResponseVO getVipStatus(@RequestHeader String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        if (userInfo == null) {
            return getBusinessErrorResponseVO(new BusinessException("登录态失效"), null);
        }
        VipStatusVO vipStatus = userInfoService.getVipStatusVO(userInfo.getUserId());
        return getSuccessResponseVO(vipStatus);
    }

    /**
     * 创建VIP订单
     */
    @PostMapping("/createOrder")
    @GlobalInterceptor
    public ResponseVO createVipOrder(@RequestHeader String token, @RequestParam String packageId) {
        // 从packageId解析出月数
        VipPackageEnum pkg = VipPackageEnum.valueOf(packageId);
        return getSuccessResponseVO(vipOrderService.createPayOrder(token, pkg.getMonths()));
    }
}