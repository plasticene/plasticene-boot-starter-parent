package com.plasticene.boot.example.flow.purchase;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageQuery;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.param.FlowStartByCodeParam;
import com.plasticene.boot.flow.core.api.service.FlowInstanceApi;
import com.plasticene.boot.flow.core.api.service.FlowQueryApi;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceVO;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证采购提交变量、幂等、参数约束与数据访问权限。
 * @author ZFJ
 * @since 2026-11-18
 */
class PurchaseApplyServiceTest {
    private final PurchaseApplyMapper mapper = mock(PurchaseApplyMapper.class);
    private final FlowInstanceApi instances = mock(FlowInstanceApi.class);
    private final FlowQueryApi queries = mock(FlowQueryApi.class);
    private ValidatorFactory validators;
    private PurchaseApplyService service;

    @BeforeEach
    void prepare() {
        validators = Validation.buildDefaultValidatorFactory();
        service = new PurchaseApplyService(mapper, instances, queries, validators.getValidator());
    }

    @AfterEach
    void close() {
        validators.close();
    }

    @Test
    void shouldSubmitOnlyTrustedSavedVariables() {
        PurchaseApply saved = saved();
        when(mapper.lockByRequest(3L, param().getRequestNo())).thenReturn(saved);
        when(instances.startInstanceByCode(any(), any())).thenReturn(21L);
        var detail = new FlowInstanceDetailVO();
        var instance = new FlowInstanceVO();
        instance.setCategory("purchase");
        detail.setInstance(instance);
        when(queries.getInstance(21L, null, actor())).thenReturn(detail);
        var result = service.submit(param(), actor());
        var captor = ArgumentCaptor.forClass(FlowStartByCodeParam.class);
        verify(instances).startInstanceByCode(captor.capture(), eq(actor()));
        assertEquals("purchase-approval", captor.getValue().getProcessCode());
        assertEquals("purchase:7", captor.getValue().getBusinessId());
        assertEquals(new BigDecimal("125.50"), captor.getValue().getVarMap().get("amount"));
        assertEquals(1, captor.getValue().getVarMap().get("urgent"));
        assertEquals(21L, result.instanceId());
        verify(mapper).bindInstance(3L, 7L, 21L);
    }

    @Test
    void shouldReturnOriginalInstanceForRepeatedRequest() {
        PurchaseApply saved = saved();
        saved.setInstanceId(21L);
        when(mapper.lockByRequest(3L, param().getRequestNo())).thenReturn(saved);
        assertEquals(21L, service.submit(param(), actor()).instanceId());
        verifyNoInteractions(instances, queries);
    }

    @Test
    void shouldRejectRequestOwnedByAnotherUser() {
        PurchaseApply saved = saved();
        saved.setCreatedBy(99L);
        when(mapper.lockByRequest(3L, param().getRequestNo())).thenReturn(saved);
        assertThrows(BizException.class, () -> service.submit(param(), actor()));
        verifyNoInteractions(instances);
    }

    @Test
    void shouldPropagateStartFailureWithoutBindingAnInstance() {
        when(mapper.lockByRequest(3L, param().getRequestNo())).thenReturn(saved());
        when(instances.startInstanceByCode(any(), any())).thenThrow(new BizException("流程未发布"));
        assertThrows(BizException.class, () -> service.submit(param(), actor()));
        verify(mapper, never()).bindInstance(any(), any(), any());
    }

    @Test
    void shouldRejectModelCategoryWithoutPurchaseCallback() {
        when(mapper.lockByRequest(3L, param().getRequestNo())).thenReturn(saved());
        when(instances.startInstanceByCode(any(), any())).thenReturn(21L);
        var detail = new FlowInstanceDetailVO();
        var instance = new FlowInstanceVO();
        instance.setCategory("other");
        detail.setInstance(instance);
        when(queries.getInstance(21L, null, actor())).thenReturn(detail);
        assertThrows(BizException.class, () -> service.submit(param(), actor()));
        verify(mapper, never()).bindInstance(any(), any(), any());
    }

    @Test
    void shouldValidateAmountDateAndAllowedOptionsBeforePersistence() {
        var invalidAmount = param();
        invalidAmount.setAmount(BigDecimal.ZERO);
        assertThrows(BizException.class, () -> service.submit(invalidAmount, actor()));
        var invalid = param();
        invalid.setExpectedDate(LocalDate.now().minusDays(1));
        PurchaseApplyParam invalidDate = invalid;
        assertThrows(BizException.class, () -> service.submit(invalidDate, actor()));
        invalid = param();
        invalid.setPurposes(List.of("unknown"));
        PurchaseApplyParam invalidOptions = invalid;
        assertThrows(BizException.class, () -> service.submit(invalidOptions, actor()));
        verifyNoInteractions(mapper, instances);
    }

    @Test
    void shouldRequireInstanceReadPermissionForBusinessDetail() {
        PurchaseApply saved = saved();
        saved.setInstanceId(21L);
        when(mapper.find(3L, 7L, false)).thenReturn(saved);
        when(queries.getInstance(21L, null, actor())).thenThrow(new BizException("无权查看"));
        assertThrows(BizException.class, () -> service.detail(7L, actor()));
    }

    @Test
    void shouldScopeListToCurrentOrganizationAndApplicant() {
        when(mapper.count(3L, 8L)).thenReturn(23L);
        when(mapper.page(3L, 8L, 20, 20)).thenReturn(List.of(saved()));
        var result = service.page(new PageQuery(2, 20), actor());
        assertEquals(23L, result.getTotal());
        assertEquals(2L, result.getPages());
        assertEquals(1, result.getList().size());
    }

    private LoginUser actor() {
        var actor = new LoginUser();
        actor.setId(8L);
        actor.setOrgId(3L);
        return actor;
    }

    private PurchaseApplyParam param() {
        var param = new PurchaseApplyParam();
        param.setRequestNo("2f573377-fd8f-4cd3-97bb-7eb686ed785d");
        param.setTitle("采购电脑");
        param.setDescription("用于日常办公");
        param.setAmount(new BigDecimal("999.00"));
        param.setCategory("electronics");
        param.setPurposes(List.of("office", "project"));
        param.setUrgent(true);
        param.setExpectedDate(LocalDate.now().plusDays(1));
        return param;
    }

    private PurchaseApply saved() {
        var apply = new PurchaseApply();
        apply.setId(7L);
        apply.setOrgId(3L);
        apply.setCreatedBy(8L);
        apply.setTitle("已保存采购单");
        apply.setAmount(new BigDecimal("125.50"));
        apply.setCategory("office");
        apply.setUrgent(true);
        apply.setStatus(0);
        return apply;
    }
}
