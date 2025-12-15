package com.plasticene.boot.web.core.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author ZFJ
 * @since 2025/12/15
 */
public abstract class BaseFilter extends OncePerRequestFilter {

    // 公共默认排除路径
    protected static final List<String> COMMON_EXCLUDE_PATHS = List.of(
            "/doc.html",
            "/swagger-ui.html",
            "/swagger-ui/*",
            "/v3/api-docs",
            "/v3/api-docs/*",
            "/v3/api-docs/swagger-config",
            "/favicon.ico",
            "/webjars/*",
            "/swagger-resources",
            "/swagger-resources/*"
    );

    /**
     * 每个子类可以自定义自己的排除路径
     */
    protected List<String> getExcludePathList() {
        return Collections.emptyList();
    }

    /**
     * 判断是否需要跳过<br>
     * 校验子类自定义的排除路径时，考虑到请求遵从restful风格，需要分两种情况：不加请求方式路径或加上请求方法路径<br>
     * 1. 不加请求方式路径：/api/user<br>
     * 2. 加请求方式路径：POST/api/user
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // 校验默认排除路径
        for (String excludePath : COMMON_EXCLUDE_PATHS) {
            if (matchesPath(path, excludePath)) {
                return true;
            }
        }
        String method = request.getMethod();
        String methodPath = method + path;
        List<String> excludePathList = getExcludePathList();
        // 校验子类自定义的排除路径
        for (String excludePath : excludePathList) {
            if (matchesPath(path, excludePath) || matchesPath(methodPath, excludePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 路径匹配方法，支持通配符
     */
    private boolean matchesPath(String requestPath, String pattern) {
        if (pattern.endsWith("/*")) {
            String basePattern = pattern.substring(0, pattern.length() - 2);
            return requestPath.startsWith(basePattern);
        }
        return requestPath.equals(pattern);
    }
}
