package com.plasticene.boot.example.web.param;

import com.plasticene.boot.example.web.enums.GenderEnum;
import com.plasticene.boot.web.core.validator.CombineNotNull;
import com.plasticene.boot.web.core.validator.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/21
 */
@Data
@Schema(description = "用户参数")
public class UserParam {
    @Schema(description = "用户id")
    private Long id;
    @Schema(description = "用户姓名")
    @NotBlank(message = "名字不能为空")
    private String name;
    @Schema(description = "用户性别")
    @EnumValue(message = "性别枚举值不对", linkEnum = GenderEnum.class)
    private Integer gender;
    @Schema(description = "用户出生日期")
    @CombineNotNull(message = "女生出生日期不能为空", condition = "#this.gender == 1")
    private Date birthday;
    @Schema(description = "用户上传文件")
    private List<MultipartFile> files;

}
