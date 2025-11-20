package com.plasticene.boot.flow.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ZFJ
 * @date 2025/11/19
 */
@Data
public class CategoryVO {
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "公司id")
    private Long orgId;
    @Schema(description = "分组名称")
    private String name;
    @Schema(description = "分组code")
    private String code;
    @Schema(description = "分组顺序")
    private Integer seq;
    @Schema(description = "是否系统内置  0：否  1：是")
    private Integer isSys;
    @Schema(description = "创建人")
    private Long creator;
    @Schema(description = "更新人")
    private Long updater;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "分组下流程")
    private List<FlowProcessVO> processList = new ArrayList<>();


}
