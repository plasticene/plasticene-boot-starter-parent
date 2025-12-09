package com.plasticene.boot.mybatis.core.generator;

import lombok.Data;

import javax.sql.DataSource;
import java.util.List;

/**
 * @author ZFJ
 * @date 2025/12/9
 */
@Data
public class GeneratorInfo {
    /**
     * 数据源
     */
    private DataSource dataSource;
    /**
     * 基础包名
     */
    private String basePackage;
    /**
     * 作者
     */
    private String author;
    /**
     * 表名
     */
    private List<String> tables;
    /**
     * 表前缀
     */
    private String tablePrefix;

}
