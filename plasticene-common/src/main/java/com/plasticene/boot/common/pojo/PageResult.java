package com.plasticene.boot.common.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 11:43
 */
@Data
public final class PageResult<T> implements Serializable {

    /**
     * 数据内容
     */
    private List<T> list;

    /**
     * 总数
     */
    private Long total;

    /**
     * 页数
     */
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

}

