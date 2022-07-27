package com.plasticene.boot.web.core.interceptor;

import cn.hutool.core.util.StrUtil;
import com.plasticene.boot.web.core.utils.MDCTraceUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Objects;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/26 17:09
 */
public class FeignInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
         // 传递header
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    // 跳过 content-length
                    if (Objects.equals("content-length", name)){
                        continue;
                    }
                    String value = request.getHeader(name);
                    requestTemplate.header(name, value);
                }
            }
        }
        // 传递日志traceId
        String traceId = MDCTraceUtils.getTraceId();
        if (StrUtil.isNotEmpty(traceId)) {
            requestTemplate.header(MDCTraceUtils.TRACE_ID_HEADER, traceId);
            requestTemplate.header(MDCTraceUtils.SPAN_ID_HEADER, MDCTraceUtils.getNextSpanId());
        }
    }
}
