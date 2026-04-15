package com.easypan.entity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Web相关配置（注册RestTemplate Bean）
 */
@Configuration // 标记为配置类，Spring启动时扫描并注册Bean
public class WebConfig {

    /**
     * 注册RestTemplate Bean，供QQ登录/第三方接口调用使用
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}