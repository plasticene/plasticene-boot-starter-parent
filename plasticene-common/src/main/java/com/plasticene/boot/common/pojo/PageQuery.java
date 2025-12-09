package com.plasticene.boot.common.pojo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


import java.util.Objects;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 11:44
 */
@Data
public class PageQuery {

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小值为 1")
    private Integer pageNo = 1;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "页码最小值为 1")
    @Max(value = 2000, message = "页码最大值为 2000")
    private Integer pageSize = 10;

    public PageQuery() {

    }

    public PageQuery(Integer pageNo, Integer pageSize) {
        if (Objects.nonNull(pageNo)) {
            this.pageNo = pageNo;
        }
       if (Objects.nonNull(pageSize)) {
           this.pageSize = pageSize;
       }
    }
}
