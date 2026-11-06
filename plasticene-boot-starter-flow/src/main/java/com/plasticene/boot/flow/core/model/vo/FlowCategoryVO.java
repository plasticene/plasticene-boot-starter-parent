package com.plasticene.boot.flow.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author ZFJ
 * @date 2025/11/19
 */
@Data
public class FlowCategoryVO {
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "公司id")
    private Long orgId;
    @Schema(description = "分类名称")
    private String name;
    @Schema(description = "前端内置图标标识，空值使用默认图标")
    private String icon;
    @Schema(description = "分类标识，同一公司内唯一、不区分大小写")
    private String code;
    @Schema(description = "分类描述，最多100字")
    private String description;
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


}
