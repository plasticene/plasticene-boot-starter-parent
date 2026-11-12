package com.plasticene.boot.flow.core.model.param;

import com.plasticene.boot.flow.core.validation.Insert;
import com.plasticene.boot.flow.core.validation.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * @author ZFJ
 * @date 2025/11/19
 */
@Data
public class FlowCategoryParam {
    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空", groups = {Insert.class, Update.class})
    @Size(max = 32, message = "分类名称长度不能超过32个字符", groups = {Insert.class, Update.class})
    private String name;
    @Schema(description = "前端内置图标标识，空值使用默认图标")
    @Size(max = 64, message = "图标标识长度不能超过64个字符", groups = {Insert.class, Update.class})
    private String icon;
    @Schema(description = "分类标识，新增时5～32位，同一公司内唯一、不区分大小写，创建后不可修改")
    @NotBlank(message = "分类标识不能为空", groups = Insert.class)
    @Size(min = 5, max = 32, message = "分类标识须为5～32位", groups = Insert.class)
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "分类标识仅支持字母、数字、下划线或连字符，最多32位",
            groups = {Insert.class, Update.class})
    private String code;
    @Schema(description = "分类描述，最多100字")
    @Size(max = 100, message = "分类描述不能超过100字", groups = {Insert.class, Update.class})
    private String description;
    @Schema(description = "分组id")
    @NotNull(message = "id不能为空", groups = Update.class)
    private Long id;
    @Schema(description = "id集合，修改排序时传")
    private List<Long> ids;
}
