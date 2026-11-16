package com.plasticene.boot.flow.core.impl.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证历史变量转换失败能定位定义且不会泄露完整表单配置。
 * @author ZFJ
 * @since 2026-11-16
 */
class FlowVarInfoMigrationTest {
    @Test
    void shouldIdentifyDefinitionAndValidationReasonWithoutPrintingPayload() {
        String config = """
                {"rule":[{"type":"input","field":"department","title":"部门","value":"private-option-value"},
                {"type":"input","field":"department","title":"重复部门"}]}
                """;
        var failure = assertThrows(IllegalStateException.class,
                () -> FlowVarInfoMigration.convert(42L, config, new ObjectMapper()));
        assertTrue(failure.getMessage().contains("definitionId=42"));
        assertTrue(failure.getMessage().contains("表单字段标识不能重复"));
        assertFalse(failure.getMessage().contains("private-option-value"));
        assertNotNull(failure.getCause());
    }

    @Test
    void shouldPreserveLegacyEmptyOptionValuesWithoutRelaxingFormSaveValidation() throws Exception {
        String config = """
                {"rule":[{"type":"checkbox","field":"hobby","title":"多选框",
                "options":[{"label":"有效选项","value":"a"},{"label":"历史空值选项","value":""}]}]}
                """;
        var mapper = new ObjectMapper();
        var source = mapper.readValue(config, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() { });
        assertThrows(com.plasticene.boot.common.exception.BizException.class,
                () -> com.plasticene.boot.flow.core.impl.form.FlowFormConfig.validate(source));
        var result = mapper.readTree(FlowVarInfoMigration.convert(33L, config, mapper));
        var field = result.path("fields").get(0);
        assertEquals("hobby", field.path("field").asText());
        assertEquals(mapper.readTree(config).path("rule").get(0).path("options"), field.path("options"));
    }

    @Test
    void shouldExcludeSubformFieldsFromMigrationVariableScope() throws Exception {
        String config = """
                {"rule":[{"type":"inputNumber","field":"amount","title":"主金额"},
                {"type":"subForm","field":"details","title":"明细","props":{"rule":[
                {"type":"inputNumber","field":"amount","title":"明细金额"}]}}]}
                """;
        var mapper = new ObjectMapper();
        var fields = mapper.readTree(FlowVarInfoMigration.convert(37L, config, mapper)).path("fields");
        assertEquals(1, fields.size());
        assertEquals("amount", fields.get(0).path("field").asText());
        assertEquals("主金额", fields.get(0).path("title").asText());
    }

    @Test
    void shouldRetainOriginalFieldCodesWhenConverting() throws Exception {
        String config = """
                {"rule":[{"type":"select","field":"legacy_code","title":"部门",
                "options":[{"label":"部门A","value":"a"}]}]}
                """;
        var mapper = new ObjectMapper();
        var result = mapper.readTree(FlowVarInfoMigration.convert(43L, config, mapper));
        assertEquals("legacy_code", result.path("fields").get(0).path("field").asText());
        assertEquals("a", result.path("fields").get(0).path("options").get(0).path("value").asText());
    }
}
