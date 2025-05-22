package com.plasticene.boot.example.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/21
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "用户VO")
public class UserVO {
    @Schema(description = "用户id")
    private Long userId;
    @Schema(description = "姓名")
    private String userName;
    @Schema(description = "年龄")
    private Integer age;
    @Schema(description = "出生日期")
    private Date birthday;
}
