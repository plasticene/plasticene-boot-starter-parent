package com.plasticene.boot.common.pojo;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 11:44
 */
@Data
public class PageParam {
    private static final Integer PAGE_NO = 1;
    private static final Integer PAGE_SIZE = 20;

//    @ApiModelProperty(value = "页码，从 1 开始", required = true,example = "1")
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小值为 1")
    private Integer pageNo = PAGE_NO;

//    @ApiModelProperty(value = "每页条数，最大值为 100", required = true, example = "10")
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "页码最小值为 1")
    @Max(value = 100, message = "页码最大值为 200")
    private Integer pageSize = PAGE_SIZE;
}
