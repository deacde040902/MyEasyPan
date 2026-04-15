package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.entity.dto.QQCallbackDTO;
import com.easypan.entity.dto.QQLoginDTO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.service.QQLoginService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/qqlogin")
public class QQLoginController extends ABaseController {

    @Resource
    private QQLoginService qqLoginService;

    /**
     * 获取QQ授权登录链接
     */
    @PostMapping
    public ResponseVO qqLogin(@Validated @RequestBody(required = false) QQLoginDTO dto) {
        String authorizeUrl = qqLoginService.getQQAuthorizeUrl(dto);
        return getSuccessResponseVO(authorizeUrl);
    }

    /**
     * QQ授权回调处理
     */
    @PostMapping("/callback")
    public ResponseVO qqCallback(@Validated @RequestBody QQCallbackDTO dto) {
        Map<String, Object> result = qqLoginService.handleQQCallback(dto);
        return getSuccessResponseVO(result);
    }
}