package com.plasticene.boot.flow.core.dao;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 流程数据总览Mapper配置测试
 *
 * @author ZFJ
 * @since 2026-09-17
 */
class FlowDashboardMapperTest {

    @Test
    void shouldParseDashboardMapper() {
        String resource = "mapper/FlowDashboardMapper.xml";
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource);
        assertNotNull(inputStream);

        Configuration configuration = new Configuration();
        XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
                inputStream, configuration, resource, configuration.getSqlFragments());
        mapperBuilder.parse();
    }
}
