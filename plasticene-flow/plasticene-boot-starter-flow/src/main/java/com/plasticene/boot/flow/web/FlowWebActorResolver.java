package com.plasticene.boot.flow.web;

import com.plasticene.boot.flow.core.model.FlowActor;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 将当前 HTTP 请求转换为流程操作人，业务应用可替换实现以接入自己的认证体系。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
@FunctionalInterface
public interface FlowWebActorResolver {

    FlowActor resolve(HttpServletRequest request);
}
