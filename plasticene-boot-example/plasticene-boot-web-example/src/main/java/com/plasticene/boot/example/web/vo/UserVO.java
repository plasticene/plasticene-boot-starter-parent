package com.plasticene.boot.example.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/21
 */
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
}
