package com.plasticene.boot.flow.core.impl.service.form;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowForm;
import com.plasticene.boot.flow.core.impl.model.param.FlowFormParam;
import com.plasticene.boot.flow.core.impl.model.query.FlowFormQuery;
import com.plasticene.boot.flow.core.impl.model.vo.FlowFormVO;

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
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    Long create(FlowFormParam param, LoginUser loginUser);

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
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    PageResult<FlowFormVO> page(FlowFormQuery query, LoginUser loginUser);

    /**
     * 查询所有表单
     * @return 表单列表
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    List<FlowFormVO> listAll(LoginUser loginUser);

    /**
     * 获取表单详情
     * @param id 表单id
     * @return 表单详情
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    FlowFormVO detail(Long id, LoginUser loginUser);
}
