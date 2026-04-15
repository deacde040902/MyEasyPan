package com.easypan.service.impl;

import com.easypan.entity.dto.QQCallbackDTO;
import com.easypan.entity.dto.QQLoginDTO;
import com.easypan.entity.dto.TokenUserInfoDto;
import com.easypan.entity.po.UserInfo;
import com.easypan.exception.BusinessException;
import com.easypan.redis.RedisComponent;
import com.easypan.redis.RedisUtils;
import com.easypan.service.QQLoginService;
import com.easypan.service.UserInfoService;
import com.easypan.utils.StringTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class QQLoginServiceImpl implements QQLoginService {

    @Value("${qq.oauth.client-id}")
    private String clientId;
    @Value("${qq.oauth.client-secret}")
    private String clientSecret;
    @Value("${qq.oauth.redirect-uri}")
    private String redirectUri;
    @Value("${qq.oauth.authorize-url}")
    private String authorizeUrl;
    @Value("${qq.oauth.token-url}")
    private String tokenUrl;
    @Value("${qq.oauth.user-info-url}")
    private String userInfoUrl;

    @Resource
    private UserInfoService userInfoService;
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private RestTemplate restTemplate;

    private static final String STATE_PREFIX = "qq_login_state:";

    @Override
    public String getQQAuthorizeUrl(QQLoginDTO dto) {
        // 生成防CSRF的state，存入Redis（有效期5分钟）
        String state = UUID.randomUUID().toString().replace("-", "");
        String callbackUrl = dto == null ? "/index" : dto.getCallbackUrl();
        redisComponent.setex(STATE_PREFIX + state, callbackUrl, 300);

        // 拼接QQ授权链接
        return String.format("%s?response_type=code&client_id=%s&redirect_uri=%s&state=%s",
                authorizeUrl, clientId, redirectUri, state);
    }

    @Override
    public Map<String, Object> handleQQCallback(QQCallbackDTO dto) {
        // 1. 校验state有效性
        String stateKey = STATE_PREFIX + dto.getState();
        Object callbackUrlObj = redisComponent.get(stateKey);
        if (callbackUrlObj == null) {
            throw new BusinessException("state无效或已过期，请重新登录");
        }
        String callbackUrl = callbackUrlObj.toString();
        redisUtils.delete(stateKey);

        // 2. 通过code获取access_token
        String accessToken = getAccessToken(dto.getCode());
        if (StringTools.isEmpty(accessToken)) {
            throw new BusinessException("获取QQ授权失败");
        }

        // 3. 获取openId（QQ用户唯一标识）
        String openId = getOpenId(accessToken);
        if (StringTools.isEmpty(openId)) {
            throw new BusinessException("获取QQ用户标识失败");
        }

        // 4. 获取QQ用户信息
        Map<String, Object> qqUserInfo = getQQUserInfo(accessToken, openId);
        if (qqUserInfo == null || !"0".equals(qqUserInfo.get("ret"))) {
            throw new BusinessException("获取QQ用户信息失败");
        }

        // 5. 查找或创建用户（通过openId绑定）
        UserInfo userInfo = findOrCreateUserByQQOpenId(openId, qqUserInfo);

        // 6. 生成系统登录Token
        String token = generateUserToken(userInfo);

        // 7. 封装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("callbackUrl", callbackUrl);
        result.put("token", token);
        result.put("userInfo", userInfo);
        return result;
    }

    // 获取access_token
    private String getAccessToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("redirect_uri", redirectUri);
        params.add("fmt", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, Map.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return (String) response.getBody().get("access_token");
        }
        return null;
    }

    // 获取openId
    private String getOpenId(String accessToken) {
        String url = String.format("https://graph.qq.com/oauth2.0/me?access_token=%s&fmt=json", accessToken);
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return (String) response.getBody().get("openid");
        }
        return null;
    }

    // 获取QQ用户信息
    private Map<String, Object> getQQUserInfo(String accessToken, String openId) {
        String url = String.format("%s?access_token=%s&oauth_consumer_key=%s&openid=%s",
                userInfoUrl, accessToken, clientId, openId);
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        }
        return null;
    }

    // 查找或创建用户
    private UserInfo findOrCreateUserByQQOpenId(String openId, Map<String, Object> qqUserInfo) {
        // 假设UserInfo表新增qqOpenId字段，用于绑定QQ账号
        UserInfo userInfo = userInfoService.getUserInfoByQQOpenId(openId);
        if (userInfo == null) {
            // 自动注册新用户
            userInfo = new UserInfo();
            userInfo.setUserId(UUID.randomUUID().toString().replace("-", "").substring(0, 15));
            userInfo.setNickName((String) qqUserInfo.get("nickname"));
            userInfo.setQqOpenId(openId);
            userInfo.setAvatarPath((String) qqUserInfo.get("figureurl_qq_2")); // QQ头像
            userInfo.setStatus(1);
            userInfo.setCreateTime(new java.util.Date());
            userInfoService.add(userInfo);
        }
        return userInfo;
    }

    // 生成登录Token
    private String generateUserToken(UserInfo userInfo) {
        // 清理旧Token
        redisComponent.cleanUserTokenByUserId(userInfo.getUserId());
        String token = UUID.randomUUID().toString().replace("-", "");

        // 保存Token信息
        TokenUserInfoDto tokenDto = new TokenUserInfoDto();
        tokenDto.setToken(token);
        tokenDto.setUserId(userInfo.getUserId());
        tokenDto.setNickName(userInfo.getNickName());
        tokenDto.setAvatarPath(userInfo.getAvatarPath());
        tokenDto.setAdmin(false);
        redisComponent.saveTokenUserInfoDto(tokenDto);

        return token;
    }
}