package com.plasticene.boot.web.core.filter;

import com.plasticene.boot.web.core.global.RequestBodyWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Objects;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2023/5/4 23:23
 */
@Slf4j
public class BodyTransferFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        RequestBodyWrapper requestBodyWrapper = null;
        try {
            HttpServletRequest req = (HttpServletRequest)request;
            requestBodyWrapper = new RequestBodyWrapper(req);

        }catch (Exception e){
            log.warn("requestBodyWrapper Error:", e);
        }
        chain.doFilter((Objects.isNull(requestBodyWrapper) ? request : requestBodyWrapper), response);
    }
}
