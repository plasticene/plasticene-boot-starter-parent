package com.plasticene.boot.example.flow.filter;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.web.core.filter.BaseFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ZFJ
 * @date 2025/11/12
 */

@Component
@Slf4j
public class AuthFilter extends BaseFilter {
    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) {
        // 模拟登录认证

        try {
            LoginUser loginUser = new LoginUser();
            loginUser.setId(8L);
            loginUser.setOrgId(3L);
            loginUser.setDeptId(10L);
            loginUser.setRoleIds(List.of(4L, 5L));
            loginUser.setUsername("admin");
            loginUser.setNickname("哈哈😄");
            loginUser.setGender(0);
            LoginUserHolder.set(loginUser);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("auth error", e);
        } finally {
            LoginUserHolder.remove();
        }


    }
}
