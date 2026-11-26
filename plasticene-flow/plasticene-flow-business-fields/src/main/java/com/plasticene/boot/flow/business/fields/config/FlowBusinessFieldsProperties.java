package com.plasticene.boot.flow.business.fields.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定审批业务字段的部署配置。
 * @author ZFJ
 * @since 2026-11-20
 */
@Data
@ConfigurationProperties("ptc.flow.business-fields")
public class FlowBusinessFieldsProperties {

    /** 接入方是否开启业务字段扩展。 */
    private boolean enabled;

    /** 每个分类允许配置的展示字段数。 */
    private int maxDisplayFields = 5;

    /** 每个分类允许配置的搜索字段数。 */
    private int maxSearchFields = 5;
}
