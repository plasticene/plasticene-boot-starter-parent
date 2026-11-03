package com.plasticene.boot.flow.core.enums;

/**
 * 定义加签类型与批次执行状态。
 * @author ZFJ
 * @since 2026-11-03
 */
public final class FlowAddSignEnum {
    private FlowAddSignEnum() {
    }

    public enum Type { BEFORE, AFTER, PARALLEL }

    public enum State { RUNNING, PASSED, REJECTED, CANCELLED, APPLIED }
}
