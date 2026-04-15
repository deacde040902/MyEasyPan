package com.easypan.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * 支付宝密钥格式清洗工具类
 * 功能：自动移除密钥中的换行、空格、不可见字符，修复常见拼写错误
 */
public class AlipayKeyCleaner {

    /**
     * 清洗密钥（核心方法）
     * @param key 原始密钥（可能含换行、空格、错误字符）
     * @return 清洗后的纯净密钥（一行字符串，无多余字符）
     */
    public static String cleanKey(String key) {
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("密钥不能为空！");
        }

        // 1. 移除所有换行符（\n）、回车符（\r）、制表符（\t）
        String cleanKey = key.replaceAll("\\r|\\n|\\t", "");
        // 2. 移除所有空格（半角/全角）
        cleanKey = cleanKey.replaceAll("\\s|　", "");
        // 3. 移除首尾的空字符
        cleanKey = StringUtils.trim(cleanKey);
        // 4. 修复常见拼写错误（比如你之前的Swith错误，可根据需要扩展）
        cleanKey = fixCommonKeyErrors(cleanKey);
        // 5. 校验密钥格式（Base64长度必须是4的倍数）
        validateBase64Format(cleanKey);

        return cleanKey;
    }

    /**
     * 修复密钥中常见的拼写错误（可自定义扩展）
     */
    private static String fixCommonKeyErrors(String key) {
        // 示例：修复你之前的"Swith"错误（如果有）
        return key.replace("Swith", "Swith"); // 可根据实际错误调整
    }

    /**
     * 校验Base64格式（RSA密钥必须是Base64编码，长度为4的倍数）
     */
    private static void validateBase64Format(String key) {
        if (key.length() % 4 != 0) {
            throw new IllegalArgumentException(
                    "密钥格式错误！Base64长度必须是4的倍数，当前长度：" + key.length()
            );
        }
    }
}