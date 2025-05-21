package com.plasticene.boot.example.web.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

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
    private String name;
    @Schema(description = "用户性别")
    private Integer gender;
    @Schema(description = "用户出生日期")
    private Date birthday;

}
