package com.easypan;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import com.baomidou.mybatisplus.annotation.IdType;

import java.util.Collections;

/**
 * MyBatis-Plus 代码生成器
 */
public class CodeGenerator {
    public static void main(String[] args) {
        // 1. 数据库连接配置（请确认你的数据库信息正确）
        String url = "jdbc:mysql://127.0.0.1:3306/easypan?serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&useSSL=false";
        String username = "root"; // 替换为你的数据库用户名
        String password = "123456"; // 替换为你的数据库密码

        // 2. 快速生成器核心配置（修复所有错误）
        FastAutoGenerator.create(url, username, password)
                // 全局配置
                .globalConfig(builder -> {
                    builder.author("licheng") // 设置作者
                          //  .enableSwagger() // 开启 Swagger 注解（接口文档）
                            //.fileOverride() // 覆盖已生成的文件（放开注释，避免重复生成时手动删除）
                            .outputDir(System.getProperty("user.dir") + "/src/main/java"); // 生成文件输出目录
                })
                // 包配置（匹配你的项目结构）
                .packageConfig(builder -> {
                    builder.parent("com.easypan") // 项目根包
                            .moduleName("") // 无模块名，留空
                            .entity("entity.po") // 实体类包名（对应数据库POJO）
                            .mapper("mappers") // Mapper接口包名
                            .service("service") // Service接口包名
                            .serviceImpl("service.impl") // Service实现类包名
                            .controller("controller") // Controller包名
                            // Mapper XML 文件输出到 resources/mappers 目录
                            .pathInfo(Collections.singletonMap(OutputFile.xml,
                                    System.getProperty("user.dir") + "/src/main/resources/mappers"));
                })
                // 策略配置（核心修复：表名、主键类型）
                .strategyConfig(builder -> {
                    // 修复1：使用英文逗号分隔表名，去掉重复的 file_chunk
                    builder.addInclude("sys_config")
                            .addTablePrefix("") // 表名无前缀，留空
                            // Entity 实体类策略（修复2：分表配置主键类型）
                            .entityBuilder()
                            .enableLombok() // 启用 Lombok，生成 @Data 注解
                            .enableTableFieldAnnotation() // 生成字段注解 @TableField
                            // 全局主键类型（默认 ASSIGN_ID，适配大部分场景）
                            .idType(IdType.ASSIGN_ID)
                            // 针对 file_chunk 表单独配置自增主键（可选，如需精准匹配）
                            .logicDeleteColumnName("status") // 逻辑删除字段（可选）
                            // Controller 策略
                            .controllerBuilder()
                            .enableRestStyle() // 生成 @RestController
                            .enableHyphenStyle() // URL 驼峰转连字符（如 fileInfo → file-info）
                            // Service 策略
                            .serviceBuilder()
                            .formatServiceFileName("%sService") // Service接口名：FileInfoService
                            .formatServiceImplFileName("%sServiceImpl") // Service实现类名：FileInfoServiceImpl
                            // Mapper 策略
                            .mapperBuilder()
                            .enableBaseResultMap() // 生成 XML 结果映射
                            .enableBaseColumnList(); // 生成 XML 通用查询列
                })
                // 模板引擎（使用 Velocity，需确保依赖存在）
                .templateEngine(new VelocityTemplateEngine())
                // 执行生成
                .execute();

        System.out.println("✅ 代码生成完成！请查看 src/main/java/com/easypan 目录");
    }
}