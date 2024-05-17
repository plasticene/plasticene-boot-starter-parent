package com.plasticene.boot.web.core.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/5/14 15:09
 */
public class IpUtil {
    private static final String UNKNOWN = "unknown";

    public static String getIpAddress() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (StringUtils.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (StringUtils.isNotBlank(ip) && ip.indexOf(',') > 0) {
            ip = ip.substring(0, ip.indexOf(","));
        }
        return ip;
    }
}
