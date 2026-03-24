package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.flow.core.entity.Form;
import com.plasticene.boot.flow.core.model.param.FormParam;
import com.plasticene.boot.flow.core.model.query.FormQuery;
import com.plasticene.boot.flow.core.model.vo.FormVO;

import java.util.List;

/**
 * @author ZFJ
 * @since 2026/2/24
 */
public interface FormService extends IService<Form> {

    /**
     * 创建表单
     * @param param 表单参数
     * @return 表单id
     */
    Long create(FormParam param);

    /**
     * 修改表单
     * @param param 表单参数
     */
    void update(FormParam param);

    /**
     * 删除表单
     * @param ids 表单id
     */
    void delete(List<Long> ids);

    /**
     * 分页查询表单
     * @param query 查询参数
     * @return 表单列表
     */
    PageResult<FormVO> page(FormQuery query);

    /**
     * 查询所有表单
     * @return 表单列表
     */
    List<FormVO> listAll();

    /**
     * 获取表单详情
     * @param id 表单id
     * @return 表单详情
     */
    FormVO detail(Long id);
}
