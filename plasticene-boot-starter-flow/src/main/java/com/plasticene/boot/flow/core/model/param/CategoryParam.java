package com.plasticene.boot.flow.core.model.param;

import com.plasticene.boot.web.core.validator.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @author ZFJ
 * @date 2025/11/19
 */
@Data
public class CategoryParam {
    @Schema(description = "分组名称")
    @NotBlank(message = "分组名称不能为空")
    @Size(max = 32, message = "分组名称长度不能超过32个字符")
    private String name;
    @Schema(description = "分组code，不区分大小写")
    @Pattern(regexp = "^[A-Za-z]+$", message = "code只能包含大小写字母")
    @Size(max = 16, message = "分类长度不能超过16个字符")
    private String code;
    @Schema(description = "分组id")
    @NotNull(message = "id不能为空", groups = Update.class)
    private Long id;
    @Schema(description = "id集合，修改排序时传")
    private List<Long> ids;
}
