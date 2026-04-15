package com.easypan.controller;

import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.TokenUserInfoDto;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.redis.RedisComponent;
import com.easypan.utils.StringTools;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 1. 未登录状态（登录/注册页）：直接返回成功，不做任何操作
 * 2. 已登录状态（系统内页）：自动清理Redis中的Token登录态
 */
@RestController
@RequestMapping("/api")
public class LogoutController extends ABaseController {

    @Resource
    private RedisComponent redisComponent;

    /**
     * 用户退出登录
     */
    @GetMapping("/logout")
    public ResponseVO logout(HttpServletRequest request) {
        // 1. 从请求头获取Token（可能为空，兼容未登录场景）
        String token = request.getHeader(Constants.REQUEST_HEADER_TOKEN);

        // 2. 仅当Token存在且有效时，才清理登录态（未登录场景直接跳过）
        if (StringTools.isNotEmpty(token)) {
            TokenUserInfoDto tokenUserInfo = redisComponent.getTokenUserInfoDto(token);
            if (tokenUserInfo != null) {
                // 已登录：彻底清理该用户的所有Token（WS_TOKEN + USERID映射双Key）
                redisComponent.cleanUserTokenByUserId(tokenUserInfo.getUserId());
            }
            // 兜底：直接删除Token对应的Key，确保Token失效
            redisComponent.deleteTokenUserInfoDto(token);
        }

        // 3. 无论是否登录，统一返回成功响应（完全匹配文档）
        return getSuccessResponseVO(null);
    }
}