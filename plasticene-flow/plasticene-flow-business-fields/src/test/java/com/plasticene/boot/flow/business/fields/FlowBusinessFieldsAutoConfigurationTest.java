package com.plasticene.boot.flow.business.fields;

import com.plasticene.boot.flow.business.fields.autoconfigure.FlowBusinessFieldsAutoConfiguration;
import com.plasticene.boot.flow.business.fields.persistence.dao.FlowBusinessFieldsQueryDAO;
import com.plasticene.boot.flow.business.fields.persistence.dao.FlowCategoryFieldDAO;
import com.plasticene.boot.flow.business.fields.persistence.dao.FlowInstanceFieldValueDAO;
import com.plasticene.boot.flow.core.api.service.FlowBusinessFieldsApi;
import com.plasticene.boot.flow.core.api.spi.FlowBusinessFieldsLifecycle;
import com.plasticene.boot.flow.core.impl.application.FlowAccess;
import com.plasticene.boot.flow.core.impl.application.FlowBusinessFieldsSupportProvider;
import com.plasticene.boot.flow.core.impl.application.FlowVariables;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowCategoryDAO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowDefinitionDAO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowFormDAO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowTaskDAO;
import jakarta.validation.Validator;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowModelDAO;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证业务字段扩展只在开启时装配，并在启动时检查扩展表。
 * @author ZFJ
 * @since 2026-11-20
 */
class FlowBusinessFieldsAutoConfigurationTest {

    private final SqlSession session = mock(SqlSession.class);
    private final SqlSessionFactory factory = mock(SqlSessionFactory.class);

    @Test
    void leavesBaseDeploymentFreeOfExtensionMappersAndSql() {
        runner().run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(FlowBusinessFieldsApi.class)
                    .doesNotHaveBean(FlowCategoryFieldDAO.class)
                    .doesNotHaveBean(FlowInstanceFieldValueDAO.class)
                    .doesNotHaveBean(FlowBusinessFieldsQueryDAO.class);
            verifyNoInteractions(session);
        });
    }

    @Test
    void bindsExtensionOnlyWhenEnabledAndChecksBothTables() throws Exception {
        var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), mock(DataSource.class)));
        for (String resource : List.of("mapper/FlowInstanceMapper.xml", "mapper/FlowTaskMapper.xml")) {
            try (var input = Resources.getResourceAsStream(resource)) {
                new XMLMapperBuilder(input, config, resource, config.getSqlFragments()).parse();
            }
        }
        when(factory.getConfiguration()).thenReturn(config);
        when(factory.openSession(any(ExecutorType.class))).thenReturn(session);
        when(session.getConfiguration()).thenReturn(config);
        String categoryCheck = FlowCategoryFieldDAO.class.getName() + ".checkSchema";
        String valueCheck = FlowInstanceFieldValueDAO.class.getName() + ".checkSchema";
        when(session.selectOne(categoryCheck, null)).thenReturn(0);
        when(session.selectOne(valueCheck, null)).thenReturn(0);
        runner().withPropertyValues("ptc.flow.business-fields.enabled=true", "ptc.flow.business-fields.max-display-fields=7")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(FlowBusinessFieldsApi.class)
                            .hasSingleBean(FlowBusinessFieldsLifecycle.class)
                            .hasSingleBean(FlowBusinessFieldsQueryDAO.class);
                    assertThat(context.getBean(FlowBusinessFieldsApi.class).limits().maxDisplayFields()).isEqualTo(7);
                    verify(session).selectOne(categoryCheck, null);
                    verify(session).selectOne(valueCheck, null);
                });
    }

    @Test
    void reportsMissingModuleWhenTheFeatureIsExplicitlyEnabled() {
        new ApplicationContextRunner().withUserConfiguration(FlowBusinessFieldsSupportProvider.class)
                .withPropertyValues("ptc.flow.business-fields.enabled=true")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("plasticene-flow-business-fields"));
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FlowBusinessFieldsAutoConfiguration.class))
                .withBean(FlowAccess.class, () -> mock(FlowAccess.class))
                .withBean(FlowVariables.class, () -> mock(FlowVariables.class))
                .withBean(FlowCategoryDAO.class, () -> mock(FlowCategoryDAO.class))
                .withBean(FlowDefinitionDAO.class, () -> mock(FlowDefinitionDAO.class))
                .withBean(FlowModelDAO.class, () -> mock(FlowModelDAO.class))
                .withBean(FlowInstanceDAO.class, () -> mock(FlowInstanceDAO.class))
                .withBean(FlowTaskDAO.class, () -> mock(FlowTaskDAO.class))
                .withBean(FlowFormDAO.class, () -> mock(FlowFormDAO.class))
                .withBean(Validator.class, () -> mock(Validator.class))
                .withBean(SqlSessionFactory.class, () -> factory);
    }
}
