package com.plasticene.boot.flow.core.model.vo;

import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.dto.FlowFieldPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流程实例详情
 *
 * @author ZFJ
 * @since 2026-09-15
 */
@Data
@Schema(description = "流程实例详情")
public class FlowInstanceDetailVO {

    @Schema(description = "实例基本信息")
    private FlowInstanceVO instance;
    @Schema(description = "发起人id")
    private Long startUserId;
    @Schema(description = "发起人名称")
    private String startUserName;
    @Schema(description = "流程发布版本")
    private Integer version;
    @Schema(description = "实例创建时的表单配置快照")
    private Map<String, Object> formConfig;
    @Schema(description = "实例提交的表单数据")
    private Map<String, Object> formData;
    @Schema(description = "当前用户在本次详情上下文中的有效字段权限")
    private Map<String, FlowFieldPermission.Permission> fieldPermissions;
    @Schema(description = "实例使用的流程模型快照")
    private FlowNode modelNode;
    @Schema(description = "根据实例表单数据计算的实际路径节点key")
    private List<String> routeNodeKeys;
    @Schema(description = "实例任务流转记录")
    private List<FlowTaskVO> tasks;
    @Schema(description = "实例加签批次记录")
    private List<FlowTaskAddSignVO> addSigns;
    @Schema(description = "当前用户有权查看的表单变更记录")
    private List<FlowFormChangeRecordVO> formChangeRecords;
}

