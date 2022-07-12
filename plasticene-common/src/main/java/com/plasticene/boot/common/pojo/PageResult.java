package com.plasticene.boot.common.pojo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 11:43
 */
public final class PageResult<T> implements Serializable {

//    @ApiModelProperty(value = "数据", required = true)
    private List<T> list;

//    @ApiModelProperty(value = "总量", required = true)
    private Long total;

    private Long pages;



    public PageResult() {
    }

    public PageResult(List<T> list, Long total, Long pages) {
        this.list = list;
        this.total = total;
        this.pages = pages;
    }

    public PageResult(Long total, Long pages) {
        this.list = new ArrayList<>();
        this.total = total;
        this.pages = pages;
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L, 0L);
    }


}

