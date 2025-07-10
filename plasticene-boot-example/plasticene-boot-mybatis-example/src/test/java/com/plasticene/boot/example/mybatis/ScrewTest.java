package com.plasticene.boot.example.mybatis;

import cn.smallbun.screw.core.Configuration;
import cn.smallbun.screw.core.engine.EngineConfig;
import cn.smallbun.screw.core.engine.EngineFileType;
import cn.smallbun.screw.core.engine.EngineTemplateType;
import cn.smallbun.screw.core.execute.DocumentationExecute;
import cn.smallbun.screw.core.process.ProcessConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fjzheng
 * @date 2025/7/9
 * @version 1.0
 *
 * 官网地址：<a href="https://gitee.com/leshalv/screw">...</a>
 */
@SpringBootTest
public class ScrewTest {
    @Resource
    private ApplicationContext applicationContext;

    @Test
    public void test() {
//        HikariConfig hikariConfig = new HikariConfig();
//        hikariConfig.setDriverClassName("com.mysql.jdbc.Driver");
//        hikariConfig.setJdbcUrl("jdbc:mysql://127.0.0.2:3306/anmi?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&allowMultiQueries=true");
//        hikariConfig.setUsername("root");
//        hikariConfig.setPassword("root");
//        // 设置可以获取tables remarks信息
//        hikariConfig.addDataSourceProperty("useInformationSchema", "true");
//        hikariConfig.setMinimumIdle(2);
//        hikariConfig.setMaximumPoolSize(5);
//        DataSource dataSource = new HikariDataSource(hikariConfig);

        DataSource dataSource = applicationContext.getBean(DataSource.class);

        // 生成文件配置
        EngineConfig engineConfig = EngineConfig.builder()
                // 生成文件路径，自己mac本地的地址，这里需要自己更换下路径
                .fileOutputDir("/Users/shepherdmy/Desktop")
                // 打开目录
                .openOutputDir(false)
                // 文件类型
                .fileType(EngineFileType.MD)
                // 生成模板实现
                .produceType(EngineTemplateType.freemarker).build();

        // 生成文档配置（包含以下自定义版本号、描述等配置连接）
        Configuration config = Configuration.builder()
                .version("3.0.x")
                .description("这是我测试的生成的数据库文档呀😄")
                .dataSource(dataSource)
                .engineConfig(engineConfig)
                .produceConfig(getProcessConfig())
                .build();

        // 执行生成
        new DocumentationExecute(config).execute();
    }

    /**
     * 配置想要生成的表+ 配置想要忽略的表
     * @return 生成表配置
     */
    public ProcessConfig getProcessConfig(){
        // 忽略表名
        List<String> ignoreTableName = List.of("test");
        // 忽略表前缀，如忽略a开头的数据库表
        List<String> ignorePrefix = List.of("heros_");
        // 忽略表后缀
        List<String> ignoreSuffix = List.of("grades");

        return ProcessConfig.builder()
                //指定生成逻辑、当存在指定表、指定表前缀、指定表后缀时，将生成指定表，其余表不生成、并跳过忽略表配置
                //根据名称指定表生成
                .designatedTableName(new ArrayList<>())
                //根据表前缀生成
                .designatedTablePrefix(new ArrayList<>())
                //根据表后缀生成
                .designatedTableSuffix(new ArrayList<>())
                //忽略表名
                .ignoreTableName(ignoreTableName)
                //忽略表前缀
                .ignoreTablePrefix(ignorePrefix)
                //忽略表后缀
                .ignoreTableSuffix(ignoreSuffix).build();
    }

}
