package com.plasticene.boot.flow.core.impl.form;

import com.plasticene.boot.common.exception.BizException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证表单保存校验与发布时可引用的基础字段集合。
 * @author ZFJ
 * @since 2026-11-05
 */
class FlowFormConfigTest {
    @Test
    void shouldExtractOnlyBasicFieldsAndStopAtSubforms() {
        var input = Map.of("type", "input", "field", "inside", "title", "内部");
        var rules = new ArrayList<Object>();
        for (String type : List.of("group", "subForm", "tableForm", "tableFormColumn")) {
            rules.add(Map.of("type", type, "field", type, "title", type, "children", List.of(input), "props", Map.of("rule", List.of(input))));
        }
        rules.add(Map.of("type", "fcTitle", "field", "title", "title", "标题"));
        rules.add(Map.of("type", "row", "field", "layout", "children", List.of(input)));
        rules.add(Map.of("type", "switch", "field", "enabled", "control", List.of(Map.of("rule", List.of(
                Map.of("type", "input", "field", "extra"))))));
        assertEquals(Set.of("inside", "enabled", "extra"), FlowFormConfig.referenceFields(Map.of("rule", rules)));
    }

    @Test
    void shouldRejectDuplicateFieldsAcrossLayoutsAndSubformDefinitions() {
        var input = field("input", "amount", "金额");
        assertThrows(BizException.class, () -> FlowFormConfig.validate(config(input,
                Map.of("type", "row", "children", List.of(input)))));
        assertThrows(BizException.class, () -> FlowFormConfig.validate(config(input,
                Map.of("type", "subForm", "field", "detail", "title", "分组", "props", Map.of("rule", List.of(input))))));
        assertThrows(BizException.class, () -> FlowFormConfig.validate(config(input,
                Map.of("type", "tableForm", "field", "table", "title", "表格", "props", Map.of("columns", List.of(Map.of("rule", List.of(input))))))));
    }

    @Test
    void shouldRequireFieldNamesAndValidOptionsButAcceptZeroAndFalse() {
        assertThrows(BizException.class, () -> FlowFormConfig.validate(config(field("input", "a", "  "))));
        for (String type : List.of("select", "radio", "checkbox")) {
            var rule = field(type, "a", "选项");
            assertThrows(BizException.class, () -> FlowFormConfig.validate(config(rule)));
            rule.put("options", List.of(Map.of("label", "零", "value", 0), Map.of("label", "否", "value", false)));
            assertDoesNotThrow(() -> FlowFormConfig.validate(config(rule)));
            rule.put("options", List.of(Map.of("label", " ", "value", 1)));
            assertThrows(BizException.class, () -> FlowFormConfig.validate(config(rule)));
            rule.put("options", List.of(Map.of("label", "选项", "value", " ")));
            assertThrows(BizException.class, () -> FlowFormConfig.validate(config(rule)));
        }
        assertDoesNotThrow(() -> FlowFormConfig.validate(config(field("switch", "enabled", "启用"))));
    }

    @Test
    void shouldValidateTreeAndTransferOptionLocationsRecursively() {
        for (String type : List.of("cascader", "tree", "aTreeSelect", "aTransfer")) {
            var rule = field(type, "a", "选项");
            assertThrows(BizException.class, () -> FlowFormConfig.validate(config(rule)));
            String source = "cascader".equals(type) ? "options" : "aTransfer".equals(type) ? "dataSource" : "treeData";
            String key = "tree".equals(type) ? "id" : "aTransfer".equals(type) ? "key" : "value";
            rule.put("props", Map.of(source, List.of(Map.of("label", "根", key, 0, "children", List.of(Map.of("label", "子", key, 1))))));
            assertDoesNotThrow(() -> FlowFormConfig.validate(config(rule)));
            rule.put("props", Map.of(source, List.of(Map.of("label", "根", key, 0, "children", List.of(Map.of("label", "子"))))));
            assertThrows(BizException.class, () -> FlowFormConfig.validate(config(rule)));
        }
    }

    @Test
    void shouldAllowAbsentFormAndParseSerializedRules() {
        assertDoesNotThrow(() -> FlowFormConfig.validate(null));
        assertDoesNotThrow(() -> FlowFormConfig.validate(Map.of("rule", List.of("{\"type\":\"input\",\"field\":\"a\",\"title\":\"名称\"}"))));
        assertThrows(BizException.class, () -> FlowFormConfig.validate(Map.of("rule", "bad")));
        assertThrows(BizException.class, () -> FlowFormConfig.validate(Map.of("rule", List.of("{bad"))));
        // 发布只做字段提取，不重复执行名称和选项完整性校验。
        assertEquals(Set.of("a"), FlowFormConfig.referenceFields(config(field("select", "a", ""))));
    }

    @Test
    void shouldKeepLayoutMetadataAndIgnoreDisplayTextWhenSaving() {
        var input = field("input", "name", "名称");
        assertDoesNotThrow(() -> FlowFormConfig.validate(config(
                Map.of("type", "table", "props", Map.of("rule", Map.of("row", 3, "col", 4)), "children", List.of(input)),
                Map.of("type", "div", "title", "", "children", List.of("显示文本")))));
        var controlled = field("switch", "enabled", "启用");
        controlled.put("control", List.of(Map.of("rule", List.of(field("input", "extra", "")))));
        assertThrows(BizException.class, () -> FlowFormConfig.validate(config(controlled)));
    }

    private Map<String, Object> field(String type, String field, String title) {
        return new HashMap<>(Map.of("type", type, "field", field, "title", title));
    }

    private Map<String, Object> config(Object... rules) {
        return Map.of("rule", List.of(rules));
    }
}
