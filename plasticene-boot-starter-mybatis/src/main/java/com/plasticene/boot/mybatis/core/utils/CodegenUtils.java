package com.plasticene.boot.mybatis.core.utils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.model.ClassAnnotationAttributes;
import com.plasticene.boot.mybatis.core.generator.GeneratorInfo;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;

import java.util.Collections;


/**
 * @author ZFJ
 * @date 2025/12/8
 */
public class CodegenUtils {

    public static void generate(GeneratorInfo info) {
        FastAutoGenerator.create(new DataSourceConfig.Builder(info.getDataSource()))
                // 全局配置
                .globalConfig(builder -> {
                    builder.author(info.getAuthor()) // 设置作者
                            .commentDate("yyyy-MM-dd")  // 设置日期
                            .outputDir("src/main/java")  // 输出目录
                            .disableOpenDir(); // 不打开路径
                })
                .packageConfig(builder -> {
                    builder.parent(info.getBasePackage()) // 设置父包名
                            .entity("entity") // 设置entity实体类包名
                            .mapper("dao") // 设置Mapper接口包名
                            .service("service") // 设置Service接口包名
                            .serviceImpl("service.impl") // 设置Service实现类包名
                            .pathInfo(Collections.singletonMap(OutputFile.xml, "src/main/resources/mapper")); // 设置路径配置信息

                })
                .strategyConfig(builder -> {
                    builder.addInclude(info.getTables()) // 设置需要生成的表名
                            // 设置实体类
                            .entityBuilder()
                            .disableSerialVersionUID()
                            .enableLombok(new ClassAnnotationAttributes("@Data","lombok.Data")) // 启用 Lombok
                            .enableTableFieldAnnotation() // 启用字段注解
                            .javaTemplate("/templates/entity.java") // 设置实体类模板
                            .superClass(BaseDO.class)
                            .addSuperEntityColumns("create_time", "update_time", "creator", "updater")
                            // 设置mapper接口
                            .mapperBuilder()
                            .mapperTemplate("/templates/mapper.java") // 设置mapper目标
                            .convertMapperFileName((entityName -> entityName + "DAO")) // 设置mapper接口文件名
                            .enableBaseResultMap()
                            .enableBaseColumnList()
                            .superClass(BaseMapperX.class)
                            // 设置service接口
                            .serviceBuilder()
                            .serviceTemplate("/templates/service.java") // 设置Service模板
                            .convertServiceFileName((entityName -> entityName + "Service")) // 设置service文件名
                            .serviceImplTemplate("/templates/serviceImpl.java") // 设置ServiceImpl模板
                            // 设置controller类
                            .controllerBuilder()
                            .template("/templates/controller.java");
                    if (StrUtil.isNotBlank(info.getTablePrefix())) {
                        builder.addTablePrefix(info.getTablePrefix()); // 添加表前缀
                    }
                })
                .injectionConfig(injectConfig -> {
                    injectConfig.customFile(new CustomFile.Builder()
                            .fileName("DTO.java") // 文件名称
                            .templatePath("templates/entityDTO.java.ftl") //指定生成模板路径
                            .packageName("model.dto") // 包名,
                            .build());
                    injectConfig.customFile(new CustomFile.Builder()
                            .fileName("VO.java") // 文件名称
                            .templatePath("templates/entityVO.java.ftl") // 指定生成模板路径
                            .packageName("model.vo") // 包名
                            .build());
                    injectConfig.customFile(new CustomFile.Builder()
                            .fileName("Param.java") // 文件名称
                            .templatePath("templates/entityParam.java.ftl") // 指定生成模板路径
                            .packageName("model.param") // 包名
                            .build());
                    injectConfig.customFile(new CustomFile.Builder()
                            .fileName("Query.java") // 文件名称
                            .templatePath("templates/entityQuery.java.ftl") // 指定生成模板路径
                            .packageName("model.query") // 包名
                            .build());

                })
                // 使用 Freemarker 模板引擎
                .templateEngine(new FreemarkerTemplateEngine())
                .execute(); // 执行生成
    }

}
