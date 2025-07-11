package com.plasticene.boot.example.mybatis;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;


/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/7/10
 */
public class CodeGeneratorTest {

    public static void main(String[] args) {
        // 使用 FastAutoGenerator 快速配置代码生成器
        FastAutoGenerator.create("jdbc:mysql://127.0.0.1:3306/db_test?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&allowMultiQueries=true",
                        "root",
                        "root")
                .globalConfig(builder -> {
                    builder.author("shepherd") // 设置作者
                            .commentDate("2025-07-10")
                            .enableSwagger()
                            .outputDir("src/main/java"); // 输出目录
                })
                .packageConfig(builder -> {
                    builder.parent("com.shepherd.example") // 设置父包名
                            .entity("entity") // 设置实体类包名
                            .mapper("dao") // 设置 Mapper 接口包名
                            .service("service") // 设置 Service 接口包名
                            .serviceImpl("service.impl") // 设置 Service 实现类包名
                            .xml("mappers"); // 设置 Mapper XML 文件包名
                })
                .strategyConfig(builder -> {
                    builder.addInclude("tb_user", "team_score") // 设置需要生成的表名
                            .entityBuilder()
                            .enableLombok() // 启用 Lombok
                            .enableTableFieldAnnotation() // 启用字段注解
                            .javaTemplate("/templates/entity.java") // 设置实体类模板
                            .serviceBuilder()
                            .serviceTemplate("/templates/service.java") // 设置 Service 模板
                            .serviceImplTemplate("/templates/serviceImpl.java") // 设置 ServiceImpl 模板
                            .mapperBuilder()
                            .mapperTemplate("/templates/mapper.java")
                            .controllerBuilder()
                            .enableRestStyle(); // 启用 REST 风格
                })
                .templateEngine(new FreemarkerTemplateEngine()) // 使用 Freemarker 模板引擎
                .execute(); // 执行生成
    }
}
