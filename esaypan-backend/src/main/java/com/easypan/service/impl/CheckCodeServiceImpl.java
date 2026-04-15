package com.easypan.service.impl;

import cn.hutool.core.lang.UUID;
import com.easypan.entity.constants.Constants;
import com.easypan.redis.RedisUtils;
import com.easypan.service.CheckCodeService;
import com.wf.captcha.ArithmeticCaptcha;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class CheckCodeServiceImpl implements CheckCodeService {

    @Resource
    private RedisUtils redisUtils;

    @Override
    public Map<String, String> generateCheckCode() {
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 42);
        String code = captcha.text();
        String checkCodeKey = UUID.randomUUID().toString();
        // 验证码10分钟内有效，存储到Redis
        redisUtils.setex(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey, code, Constants.REDIS_TIME_1MIN * 10);
        String checkCodeBase64 = captcha.toBase64();
        Map<String, String> result = new HashMap<>();
        result.put("checkCode", checkCodeBase64);
        result.put("checkCodeKey", checkCodeKey);
        return result;
    }

    @Override
    public boolean verifyCheckCode(String checkCodeKey, String checkCode) {
        // 校验参数非空
        if (checkCodeKey == null || checkCodeKey.isEmpty() || checkCode == null || checkCode.isEmpty()) {
            return false;
        }
        // 从Redis获取正确的验证码
        String redisKey = Constants.REDIS_KEY_CHECK_CODE + checkCodeKey;
        String correctCode = (String) redisUtils.get(redisKey);
        // 验证码不存在（过期或key错误）
        if (correctCode == null) {
            return false;
        }
        // 校验通过后删除Redis中的验证码，防止重复使用
        boolean verifyResult = correctCode.equalsIgnoreCase(checkCode);
        if (verifyResult) {
            redisUtils.delete(redisKey);
        }
        return verifyResult;
    }
}