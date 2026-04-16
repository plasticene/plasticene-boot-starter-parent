package com.plasticene.boot.mybatis.core.generator;

import com.plasticene.boot.mybatis.core.utils.CodegenUtils;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.util.List;

/**
 * @author ZFJ
 * @date 2025/12/8
 */
public class CodeGenerator {
    @Resource
    private DataSource dataSource;
    @Resource
    private ApplicationContext applicationContext;


    private static volatile String basePackage = null;

    /**
     * 生成代码
     *
     * @param author      作者
     * @param tables      表名
     * @param tablePrefix 表前缀
     */
    public void generate(@NotNull String author, @NotNull List<String> tables, String tablePrefix) {
        GeneratorInfo info = new GeneratorInfo();
        info.setDataSource(dataSource);
        info.setBasePackage(getMainPackagePath());
        info.setAuthor(author);
        info.setTables(tables);
        info.setTablePrefix(tablePrefix);
        CodegenUtils.generate(info);
    }

    public String getMainPackagePath() {
        if (basePackage != null) {
            return basePackage;
        }
        // 方法1：获取主配置类
        String[] mainSources = applicationContext.getBeanNamesForAnnotation(SpringBootApplication.class);
        if (mainSources.length > 0) {
            Object mainSource = applicationContext.getBean(mainSources[0]);
            basePackage = mainSource.getClass().getPackage().getName();
            return basePackage;
        }
        return  null;
    }


}
