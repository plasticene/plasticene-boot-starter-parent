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
    protected List<String> getExcludePath() {
        return Collections.emptyList();
    }

    /**
     * 判断是否需要跳过
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // 合并公共路径和子类自定义路径
        List<String> allExcludePaths = new ArrayList<>(COMMON_EXCLUDE_PATHS);
        allExcludePaths.addAll(getExcludePath());
        return allExcludePaths.stream()
                .anyMatch(excludePath -> matchesPath(path, excludePath));
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
