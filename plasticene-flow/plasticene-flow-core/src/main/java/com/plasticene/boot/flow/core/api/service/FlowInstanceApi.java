package com.plasticene.boot.flow.core.api.service;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.param.FlowStartByCodeParam;
import com.plasticene.boot.flow.core.api.param.FlowStartByIdParam;
import com.plasticene.boot.flow.core.api.param.FlowStartParam;
import com.plasticene.boot.flow.core.api.param.FlowRoutePreviewParam;
import com.plasticene.boot.flow.core.api.vo.FlowRouteNodeVO;
import java.util.List;

/**
 * 流程申请发起、路线预览和取消的公开接口。
 * <p>所有方法均校验宿主提供的可信操作者、组织归属及相应权限。</p>
 * @author ZFJ
 * @since 2026-11-16
 */
public interface FlowInstanceApi {
    /**
     * 按当前组织的流程编码选择生效定义，发起业务单据审批。
     * 宿主应先保存业务单据，并提交可信的审批变量；单据后续修改不会自动同步。
     *
     * @param param 流程编码、业务单据 ID 和已保存的审批变量
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 新建流程实例 ID
     */
    Long startInstanceByCode(FlowStartByCodeParam param, LoginUser actor);
    /**
     * 使用指定的业务表单定义版本发起审批，并按该版本的变量快照校验数据。
     *
     * @param param 发布定义 ID、业务单据 ID 和已保存的审批变量
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 新建流程实例 ID
     */
    Long startInstanceById(FlowStartByIdParam param, LoginUser actor);
    /**
     * 使用指定的 FLOW 自带表单定义发起申请，并保存本次表单数据。
     *
     * @param param 发布定义 ID 和申请表单数据
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 新建流程实例 ID
     */
    Long startInstance(FlowStartParam param, LoginUser actor);
    /**
     * 按指定定义版本、审批变量和当前发起人预览审批路线，不创建实例或任务。
     *
     * @param param 发布定义 ID 和待预览的审批变量
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 计算得到的路线节点列表
     */
    List<FlowRouteNodeVO> previewRoute(FlowRoutePreviewParam param, LoginUser actor);
    /**
     * 校验取消权限及实例状态后取消流程申请。
     *
     * @param id 流程实例 ID
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void cancel(Long id, LoginUser actor);
}
