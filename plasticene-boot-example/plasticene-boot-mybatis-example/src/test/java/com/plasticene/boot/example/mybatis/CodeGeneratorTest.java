package com.plasticene.boot.example.mybatis;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.model.ClassAnnotationAttributes;


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
                .globalConfig(builder -> {  // 全局配置
                    builder.author("ZFJ") // 设置作者
                            .commentDate("yyyy-MM-dd")  // 设置日期
                            .enableSpringdoc()  // 开启openapi3文档注释
                            .outputDir("src/main/java")  // 输出目录
                            .disableOpenDir(); // 不打开路径
                })
                .packageConfig(builder -> {
                    builder.parent("com.shepherd.example") // 设置父包名
                            .entity("entity") // 设置entity实体类包名
                            .mapper("dao") // 设置Mapper接口包名
                            .service("service") // 设置Service接口包名
                            .serviceImpl("service.impl") // 设置Service实现类包名
                            .xml("mappers"); // 设置 MapperXML文件包名
                })
                .strategyConfig(builder -> {
                    builder.addInclude("tb_user", "team_score") // 设置需要生成的表名
                            // 设置实体类
                            .entityBuilder()
                            .enableLombok(new ClassAnnotationAttributes("@Data","lombok.Data")) // 启用 Lombok
                            .enableTableFieldAnnotation() // 启用字段注解
                            .javaTemplate("/templates/entity.java") // 设置实体类模板
                            // 设置mapper接口
                            .mapperBuilder()
                            .mapperTemplate("/templates/mapper.java") // 设置mapper目标
                            .convertMapperFileName((entityName -> entityName + "DAO")) // 设置mapper接口文件名
                            .enableBaseResultMap()
                            .enableBaseColumnList()
                            // 设置service接口
                            .serviceBuilder()
                            .serviceTemplate("/templates/service.java") // 设置Service模板
                            .convertServiceFileName((entityName -> entityName + "Service")) // 设置service文件名
                            .serviceImplTemplate("/templates/serviceImpl.java") // 设置ServiceImpl模板
                            // 设置controller类
                            .controllerBuilder()
                            .template("/templates/controller.java")
                            .enableRestStyle(); // 启用 REST 风格
                })
                .templateEngine(new FreemarkerTemplateEngine()) // 使用 Freemarker 模板引擎
                .execute(); // 执行生成
    }
}
