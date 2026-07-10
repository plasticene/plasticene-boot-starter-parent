package com.plasticene.boot.web.core.filter;

import com.plasticene.boot.web.core.global.RequestBodyWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2023/5/4 23:23
 */
@Slf4j
public class BodyTransferFilter extends BaseFilter {

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/")) {
            filterChain.doFilter(request, response);
            return;
        }
        RequestBodyWrapper requestBodyWrapper = null;
        try {
            requestBodyWrapper = new RequestBodyWrapper(request);
        }catch (Exception e){
            log.warn("requestBodyWrapper Error:", e);
        }
        filterChain.doFilter((Objects.isNull(requestBodyWrapper) ? request : requestBodyWrapper), response);
    }
}
