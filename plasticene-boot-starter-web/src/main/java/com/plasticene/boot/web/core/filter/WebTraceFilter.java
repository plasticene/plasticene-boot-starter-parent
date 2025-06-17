package com.plasticene.boot.web.core.filter;

import cn.hutool.core.util.StrUtil;
import com.plasticene.boot.web.core.utils.MDCTraceUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.List;


/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/13 18:20
 */
//
public class WebTraceFilter extends OncePerRequestFilter {

    private static final List<String> EXCLUDE_PATHS = List.of(
            "/doc.html",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/favicon.ico",
            "/webjars"
    );


    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws IOException, ServletException {
        String path = request.getRequestURI();
        // 如果路径在排除列表中，直接跳过
        if (EXCLUDE_PATHS.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String traceId = request.getHeader(MDCTraceUtils.TRACE_ID_HEADER);
            if (StrUtil.isEmpty(traceId)) {
                MDCTraceUtils.addTrace();
            } else {
                MDCTraceUtils.putTrace(traceId);
            }
            filterChain.doFilter(request, response);
        } finally {
            MDCTraceUtils.removeTrace();
        }
    }
}
