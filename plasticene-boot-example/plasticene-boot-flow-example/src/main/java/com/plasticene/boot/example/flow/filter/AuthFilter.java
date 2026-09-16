package com.plasticene.boot.example.flow.filter;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.plasticene.boot.common.enums.ResponseCodeEnum;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.web.core.filter.BaseFilter;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author ZFJ
 * @date 2025/11/12
 */

@Component
@Slf4j
public class AuthFilter extends BaseFilter {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private final AuthProperties authProperties = new AuthProperties();

    private static final String TOKEN_KEY = "accessToken";

    @Override
    protected List<String> getExcludePathList () {
        return authProperties.getSkipUrls();
    }


    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) {
        try {
            // 1.跳过登录认证校验 由父类baseFilter处理
            // 2.获取token
            String token = getRequestToken(request);
            if (StrUtil.isBlank(token)) {
                unauthorized(response);
                return;
            }
            // 3.获取登录用户信息
            String userInfo = stringRedisTemplate.opsForValue().get("login:token:" + token);
            if (StrUtil.isBlank(userInfo)) {
                unauthorized(response);
                return;
            }
            // 4.设置登录用户信息上下文
            LoginUser loginUser = JSON.parseObject(userInfo, LoginUser.class);
            LoginUserHolder.set(loginUser);
            // 5.续期
            if (authProperties.isRenew()) {
                LocalDateTime expireTime = loginUser.getExpireTime();
                Integer tokenExpireTime = authProperties.getTokenExpireTime();
                int minutes = authProperties.getRenewRate().multiply(BigDecimal.valueOf(tokenExpireTime)).intValue();
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime nextTime = now.plusMinutes(minutes);
                if (expireTime.isBefore(nextTime)) {
                    loginUser.setExpireTime(now.plusMinutes(tokenExpireTime));
                    stringRedisTemplate.opsForValue().set("login:token:" + token,
                            JSON.toJSONString(loginUser),
                            tokenExpireTime,
                            TimeUnit.HOURS);
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("auth error: ", e);
        } finally {
            LoginUserHolder.remove();
        }
    }

    private String getRequestToken(HttpServletRequest request) {
        // 从header中查找
        String token = request.getHeader(TOKEN_KEY);
        if (StrUtil.isNotBlank(token)) {
            return token;
        }
        // header中没有，从parameter请求参数中获取
        token = request.getParameter(TOKEN_KEY);
        if (StrUtil.isNotBlank(token)) {
            return token;
        }
        // header和parameter中没有，从cookie中获取
        Cookie[] cookies = request.getCookies();
        if (Objects.nonNull(cookies)) {
            for (Cookie cookie : cookies) {
                if (Objects.equals(cookie.getName(), TOKEN_KEY)) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        return token;
    }

    void unauthorized(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(ResponseCodeEnum.UNAUTHORIZED.getCode());
        response.getWriter().write(JSON.toJSONString(ResponseVO.failure(ResponseCodeEnum.UNAUTHORIZED.getCode(),
                ResponseCodeEnum.UNAUTHORIZED.getMsg())));
    }

}
