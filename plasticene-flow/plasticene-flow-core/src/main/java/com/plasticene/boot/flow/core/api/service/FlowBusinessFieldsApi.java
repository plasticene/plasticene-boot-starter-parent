package com.plasticene.boot.flow.core.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.api.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.api.vo.FlowBusinessFieldsVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskPageVO;

import java.util.List;

/**
 * 定义可选业务字段扩展的配置和分页能力。
 * @author ZFJ
 * @since 2026-11-20
 */
public interface FlowBusinessFieldsApi {

    /** 返回部署配置的字段数量上限。 */
    FlowBusinessFieldsVO.Limits limits();

    /** 为核心已授权的分类目录补充字段配置。 */
    List<FlowBusinessFieldsVO.Category> enrichCatalog(List<FlowBusinessFieldsVO.Category> categories, LoginUser actor);

    /** 查询指定分类配置与候选字段并校验分类读取权限。 */
    FlowBusinessFieldsVO.Configuration getFields(Long categoryId, LoginUser actor);

    /** 保存分类字段配置并校验分类更新权限。 */
    void saveFields(Long categoryId, List<FlowBusinessFieldsVO.Field> fields, LoginUser actor);

    /** 使用核心已限定的本人申请范围执行分页。 */
    IPage<FlowInstanceVO> pageInstances(FlowInstanceQuery scopedQuery);

    /** 使用核心已限定的本人任务范围执行分页。 */
    IPage<FlowTaskPageVO> pageTasks(FlowTaskQuery scopedQuery);
}
