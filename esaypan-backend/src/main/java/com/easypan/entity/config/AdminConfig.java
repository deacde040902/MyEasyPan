package com.easypan.entity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 管理员配置类：读取配置文件中的admin.emails
 */
@Data
@Component
@ConfigurationProperties(prefix = "admin")
public class AdminConfig {
    /**
     * 超级管理员邮箱列表（支持多个，逗号分隔）
     */
    private String emails;

    public List<String> getAdminEmailList() {
        List<String> adminEmails = new ArrayList<>();
        if (emails != null && !emails.isEmpty()) {
            adminEmails.addAll(Arrays.asList(emails.split(",")));
        }
        return adminEmails;
    }
}