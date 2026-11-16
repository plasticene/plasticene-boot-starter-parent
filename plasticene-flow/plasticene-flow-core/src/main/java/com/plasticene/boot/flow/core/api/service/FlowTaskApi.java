package com.plasticene.boot.flow.core.api.service;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.param.FlowTaskAddSignParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskFormParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.api.vo.FlowTaskReturnTargetVO;
import java.util.List;

/**
 * 审批任务处理及可退回节点查询的公开接口。
 * <p>所有方法均校验宿主提供的可信操作者、组织归属及相应权限。</p>
 * @author ZFJ
 * @since 2026-11-16
 */
public interface FlowTaskApi {
    /**
     * 以当前任务处理人的身份审批通过待办。
     *
     * @param param 任务 ID 和审批意见
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void approve(FlowTaskParam param, LoginUser actor);
    /**
     * 以当前任务处理人的身份拒绝待办。
     *
     * @param param 任务 ID 和拒绝意见
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void reject(FlowTaskParam param, LoginUser actor);
    /**
     * 保存当前待办允许编辑的 FLOW 自带表单字段；业务单据须通过宿主入口修改。
     *
     * @param param 任务 ID 和本次修改的表单字段
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void saveForm(FlowTaskFormParam param, LoginUser actor);
    /**
     * 按任务支持的加签规则增加审批人。
     *
     * @param param 任务 ID、加签方式和加签审批人
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void addSign(FlowTaskAddSignParam param, LoginUser actor);
    /**
     * 将当前待办转交给指定的有效审批人。
     *
     * @param param 任务 ID、接收人和转交意见
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void transfer(FlowTaskTransferParam param, LoginUser actor);
    /**
     * 将当前待办退回至允许的历史审批节点。
     *
     * @param param 任务 ID、退回目标节点和退回意见
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void returnTask(FlowTaskReturnParam param, LoginUser actor);
    /**
     * 撤回操作者已完成的审批；须满足任务及后续流程的可撤回条件。
     *
     * @param id 待撤回的已审批任务 ID
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void withdraw(Long id, LoginUser actor);
    /**
     * 查询当前待办允许退回的历史审批节点。
     *
     * @param id 当前待办任务 ID
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 可退回的节点列表
     */
    List<FlowTaskReturnTargetVO> listReturnTargets(Long id, LoginUser actor);
}
