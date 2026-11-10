package com.easypan.rag.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.annotation.Resource;

/**
 * doc_chunk 表自动初始化（无需手动执行 SQL）
 * <p>
 * 使用 CREATE TABLE IF NOT EXISTS，幂等可重复执行；
 * 表已存在时跳过，不影响已有数据。
 */
@Component
@Order(1)
public class DocChunkSchemaInitRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DocChunkSchemaInitRunner.class);

    private static final String CREATE_DOC_CHUNK_SQL =
            "CREATE TABLE IF NOT EXISTS `doc_chunk` (" +
            "  `chunk_id` varchar(64) NOT NULL COMMENT '切片ID(主键,与向量库Document id一致)'," +
            "  `file_id` varchar(32) NOT NULL COMMENT '所属文件ID'," +
            "  `user_id` varchar(15) NOT NULL COMMENT '所属用户ID'," +
            "  `chunk_index` int NOT NULL DEFAULT 0 COMMENT '切片序号(从0开始)'," +
            "  `content` text NOT NULL COMMENT '切片文本内容'," +
            "  `char_count` int NOT NULL DEFAULT 0 COMMENT '字符数'," +
            "  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
            "  PRIMARY KEY (`chunk_id`)," +
            "  INDEX `idx_file_id`(`file_id`)," +
            "  INDEX `idx_user_id`(`user_id`)" +
            ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'RAG文档切片表'";

    @Resource
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_DOC_CHUNK_SQL);
            logger.info("doc_chunk 表已就绪（自动初始化完成或已存在）");
        } catch (SQLException e) {
            // 不阻断启动，但提示用户排查（通常为数据库未启动或账号无建表权限）
            logger.error("doc_chunk 表自动初始化失败: {}", e.getMessage(), e);
        }
    }
}
