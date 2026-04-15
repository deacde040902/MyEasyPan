package com.easypan.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * 加密工具类（MD5加密，适配密码存储）
 */
public class EncryptUtils {

    // 加密盐值（自定义，提高安全性）
    private static final String SALT = "EasyPan@2026";

    /**
     * MD5加密（密码+盐值）
     * @param password 原始密码
     * @return 加密后的密码（32位小写）
     */
    public static String md5Encrypt(String password) {
        if (!StringUtils.hasText(password)) {
            return "";
        }
        // 密码 + 盐值 后加密，防止彩虹表破解
        return DigestUtils.md5Hex(password + SALT);
    }

    /**
     * 验证密码是否匹配
     * @param inputPwd 输入的原始密码
     * @param encryptPwd 数据库中加密后的密码
     * @return true=匹配，false=不匹配
     */
    public static boolean verifyPwd(String inputPwd, String encryptPwd) {
        if (!StringUtils.hasText(inputPwd) || !StringUtils.hasText(encryptPwd)) {
            return false;
        }
        return md5Encrypt(inputPwd).equals(encryptPwd);
    }
}