package com.easypan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAsync
@SpringBootApplication(scanBasePackages = {"com.easypan"})
@MapperScan("com.easypan.mappers")
@EnableTransactionManagement
@EnableScheduling
public class EasyPanApplication {
    private static final Logger logger = LoggerFactory.getLogger(EasyPanApplication.class);
    public static void main(String[] args) {
        SpringApplication.run(EasyPanApplication.class, args);
        logger.info("✅ 项目启动成功");
    }
}