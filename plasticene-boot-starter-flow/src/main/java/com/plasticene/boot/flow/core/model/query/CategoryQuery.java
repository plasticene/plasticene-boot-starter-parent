package com.plasticene.boot.flow.core.model.query;

import com.plasticene.boot.common.pojo.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ZFJ
 * @since 2026/2/9
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CategoryQuery extends PageQuery {
    private String name;
}
