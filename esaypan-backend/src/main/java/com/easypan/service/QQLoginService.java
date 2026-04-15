package com.easypan.service;

import com.easypan.entity.dto.QQCallbackDTO;
import com.easypan.entity.dto.QQLoginDTO;
import java.util.Map;

public interface QQLoginService {
    // 获取QQ授权链接
    String getQQAuthorizeUrl(QQLoginDTO dto);
    // 处理QQ回调
    Map<String, Object> handleQQCallback(QQCallbackDTO dto);
}