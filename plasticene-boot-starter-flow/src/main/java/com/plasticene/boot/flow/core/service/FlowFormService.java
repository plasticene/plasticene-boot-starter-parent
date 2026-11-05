package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.flow.core.entity.FlowForm;
import com.plasticene.boot.flow.core.model.param.FlowFormParam;
import com.plasticene.boot.flow.core.model.query.FlowFormQuery;
import com.plasticene.boot.flow.core.model.vo.FlowFormVO;

import java.util.List;

/**
 * 定义流程表单管理服务。
 * @author ZFJ
 * @since 2026-11-05
 */
public interface FlowFormService extends IService<FlowForm> {

    /**
     * 创建表单
     * @param param 表单参数
     * @return 表单id
     */
    Long create(FlowFormParam param);

    /**
     * 修改表单
     * @param param 表单参数
     */
    void update(FlowFormParam param);

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
    PageResult<FlowFormVO> page(FlowFormQuery query);

    /**
     * 查询所有表单
     * @return 表单列表
     */
    List<FlowFormVO> listAll();

    /**
     * 获取表单详情
     * @param id 表单id
     * @return 表单详情
     */
    FlowFormVO detail(Long id);
}
