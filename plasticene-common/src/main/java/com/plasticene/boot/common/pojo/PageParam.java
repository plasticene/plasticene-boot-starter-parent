package com.plasticene.boot.common.pojo;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 11:44
 */
@Data
public class PageParam {
    private static final Integer PAGE_NO = 1;
    private static final Integer PAGE_SIZE = 20;

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小值为 1")
    private Integer pageNo = PAGE_NO;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "页码最小值为 1")
    @Max(value = 200, message = "页码最大值为 200")
    private Integer pageSize = PAGE_SIZE;

    public PageParam() {

    }

    public PageParam(Integer pageNo, Integer pageSize) {
        if (Objects.nonNull(pageNo)) {
            this.pageNo = pageNo;
        }
       if (Objects.nonNull(pageSize)) {
           this.pageSize = pageSize;
       }
    }
}
