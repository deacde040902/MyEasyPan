package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.entity.dto.TokenUserInfoDto;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import com.easypan.redis.RedisComponent;
import com.easypan.service.FileChunkService;
import com.easypan.service.VipOrderService;
import com.easypan.utils.VipUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

@RestController
@RequestMapping("/vip")
public class VipController extends ABaseController {

    @Resource
    private VipUtils vipUtils;
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private FileChunkService fileChunkService;
    @Resource
    private VipOrderService vipOrderService;

    /**
     * VIP专属：超大文件上传（完整实现）
     * 逻辑：VIP不限速+支持100GB文件，普通用户仅支持5GB
     */
    @PostMapping("/upload/bigFile/init")
    @GlobalInterceptor
    public ResponseVO<String> initBigFileUpload(
            @RequestHeader("token") String token,
            @RequestParam String fileMd5,
            @RequestParam String fileName,
            @RequestParam Long fileSize,
            @RequestParam(defaultValue = "500") Integer chunkSize) {
        // 1. 校验VIP权限
        vipUtils.checkVipPermission(token);
        // 2. 校验空间
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        vipUtils.checkSpaceEnough(userInfo.getUserId(), fileSize);
        // 3. VIP专属：分片大小500MB（普通用户100MB）
        // 调用分片上传初始化方法
        String uploadId = fileChunkService.initUpload(
                fileMd5, fileName, fileSize, chunkSize,
                "/", userInfo.getUserId()
        );
        return getSuccessResponseVO(uploadId);
    }

    /**
     * VIP专属：分片上传（不限速）
     */
    @PostMapping("/upload/bigFile/chunk")
    @GlobalInterceptor
    public ResponseVO<Void> uploadBigFileChunk(
            @RequestHeader("token") String token,
            @RequestParam String uploadId,
            @RequestParam Integer chunkIndex,
            @RequestParam MultipartFile file) {
        // 1. 校验VIP权限
        vipUtils.checkVipPermission(token);
        // 2. 调用分片上传（VIP不限速，无额外逻辑）
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        fileChunkService.uploadChunk(uploadId, chunkIndex, file, userInfo.getUserId());
        return getSuccessResponseVO(null);
    }

    /**
     * VIP专属：合并分片
     */
    @PostMapping("/upload/bigFile/merge")
    @GlobalInterceptor
    public ResponseVO<Void> mergeBigFile(
            @RequestHeader("token") String token,
            @RequestParam String uploadId) {
        // 1. 校验VIP权限
        vipUtils.checkVipPermission(token);
        // 2. 合并分片
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        fileChunkService.mergeChunk(uploadId, userInfo.getUserId());
        return getSuccessResponseVO(null);
    }

    /**
     * 开通VIP（对接支付模拟）
     */
    @PostMapping("/open")
    @GlobalInterceptor
    public ResponseVO<String> openVip(
            @RequestHeader("token") String token,
            @RequestParam Integer months) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        if (userInfo == null) {
            return getBusinessErrorResponseVO(new BusinessException("登录态失效"), null);
        }
        // 1. 生成订单（调用 VipOrderService）
        String orderNo = vipOrderService.createPayOrder(token, months);
        // 2. 返回订单号 + 支付链接（真实场景返回支付宝支付参数）
        return getSuccessResponseVO("订单创建成功，订单号：" + orderNo + "，请前往支付");
    }
}