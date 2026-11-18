package com.plasticene.boot.example.flow.purchase;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageQuery;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.param.FlowStartByCodeParam;
import com.plasticene.boot.flow.core.api.service.FlowInstanceApi;
import com.plasticene.boot.flow.core.api.service.FlowQueryApi;
import jakarta.validation.Validator;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 同事务保存采购单并通过 FLOW 公开接口发起业务审批。
 * @author ZFJ
 * @since 2026-11-18
 */
@Service
public class PurchaseApplyService {
    public static final String PROCESS_CODE = "purchase-approval";
    public static final String CATEGORY = "purchase";
    public static final String BUSINESS_PREFIX = "purchase:";
    private final PurchaseApplyMapper mapper;
    private final FlowInstanceApi instances;
    private final FlowQueryApi queries;
    private final Validator validator;

    public PurchaseApplyService(PurchaseApplyMapper mapper, FlowInstanceApi instances,
                                FlowQueryApi queries, Validator validator) {
        this.mapper = mapper;
        this.instances = instances;
        this.queries = queries;
        this.validator = validator;
    }

    /** 保存采购单并按固定业务流程编码提交；同一请求标识返回原申请。 */
    @Transactional(rollbackFor = Exception.class)
    public PurchaseApplyVO submit(PurchaseApplyParam param, LoginUser actor) {
        requireActor(actor);
        validate(param);
        mapper.insertIfAbsent(param, actor.getOrgId(), actor.getId());
        PurchaseApply saved = mapper.lockByRequest(actor.getOrgId(), param.getRequestNo());
        if (saved == null || !Objects.equals(saved.getCreatedBy(), actor.getId())) {
            throw new BizException("无权使用该采购申请请求标识");
        }
        if (saved.getInstanceId() != null) {
            return PurchaseApplyVO.from(saved);
        }
        var start = new FlowStartByCodeParam();
        start.setProcessCode(PROCESS_CODE);
        start.setBusinessId(BUSINESS_PREFIX + saved.getId());
        start.setVarMap(Map.of("amount", saved.getAmount(), "category", saved.getCategory(),
                "urgent", Boolean.TRUE.equals(saved.getUrgent()) ? 1 : 0));
        Long instanceId = instances.startInstanceByCode(start, actor);
        var detail = queries.getInstance(instanceId, null, actor);
        if (detail == null || detail.getInstance() == null
                || !CATEGORY.equals(detail.getInstance().getCategory())) {
            throw new BizException("采购审批模型须使用 purchase 分类，请调整模型并重新发布");
        }
        mapper.bindInstance(actor.getOrgId(), saved.getId(), instanceId);
        saved.setInstanceId(instanceId);
        return PurchaseApplyVO.from(saved);
    }

    /** 查询当前用户的采购申请列表，状态来自事件回调更新的业务表。 */
    public PageResult<PurchaseApplyVO> page(PageQuery query, LoginUser actor) {
        requireActor(actor);
        if (query == null || !validator.validate(query).isEmpty()) {
            throw new BizException("采购申请分页参数不正确");
        }
        long offset = (long) (query.getPageNo() - 1) * query.getPageSize();
        if (offset > Integer.MAX_VALUE) {
            throw new BizException("页码超出范围");
        }
        long total = mapper.count(actor.getOrgId(), actor.getId());
        var list = mapper.page(actor.getOrgId(), actor.getId(), (int) offset, query.getPageSize())
                .stream().map(PurchaseApplyVO::from).toList();
        return new PageResult<>(list, total, (total + query.getPageSize() - 1) / query.getPageSize());
    }

    /** 通过 FLOW 公开查询接口验证实例查看权限，再返回对应组织的采购单。 */
    public PurchaseApplyVO detail(Long id, LoginUser actor) {
        requireActor(actor);
        PurchaseApply saved = mapper.find(actor.getOrgId(), id, false);
        if (saved == null || saved.getInstanceId() == null) {
            throw new BizException("采购申请不存在");
        }
        queries.getInstance(saved.getInstanceId(), null, actor);
        return PurchaseApplyVO.from(saved);
    }

    private void requireActor(LoginUser actor) {
        if (actor == null || actor.getId() == null || actor.getOrgId() == null) {
            throw new BizException("缺少可信登录身份");
        }
    }

    private void validate(PurchaseApplyParam param) {
        if (param == null) {
            throw new BizException("采购申请不能为空");
        }
        var violations = validator.validate(param);
        if (!violations.isEmpty()) {
            throw new BizException(violations.stream().map(item -> item.getPropertyPath() + ": " + item.getMessage())
                    .sorted().findFirst().orElseThrow());
        }
    }
}
