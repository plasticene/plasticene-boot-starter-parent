package com.plasticene.boot.example.flow.provider;

import cn.hutool.core.util.StrUtil;
import com.plasticene.boot.flow.core.provider.FlowOrganizationProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author ZFJ
 * @since 2026/4/14
 */
@Component
public class FlowOrganizationProviderImpl implements FlowOrganizationProvider {
    private static final String TOKEN_KEY = "accessToken";
    @Override
    public Map<Long, String> getUserMap() {
        // 1. 创建 RestClient
        RestClient restClient = RestClient.create();

        // 2. 调用接口并解析
        Result<List<User>> response = restClient.get()
                .uri("http://localhost:9000/api/user/list")
                .header("accessToken", getRequestToken())
                .retrieve()
                .body(new ParameterizedTypeReference<Result<List<User>>>() {});

        // 3. 转换为 Map<id, nickname>
        return Optional.ofNullable(response)
                .map(Result::data)
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(User::id, User::nickname));
    }

    @Override
    public Map<Long, String> getDeptMap() {
        // 1. 创建 RestClient
        RestClient restClient = RestClient.create();

        // 2. 调用接口并解析
        Result<List<Dept>> response = restClient.get()
                .uri("http://localhost:9000/api/dept/list")
                .header("accessToken", getRequestToken())
                .retrieve()
                .body(new ParameterizedTypeReference<Result<List<Dept>>>() {});

        // 3. 转换为 Map<id, nickname>
        return Optional.ofNullable(response)
                .map(Result::data)
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(Dept::id, Dept::name));
    }

    @Override
    public Map<Long, String> getRoleMap() {
        // 1. 创建 RestClient
        RestClient restClient = RestClient.create();

        // 2. 调用接口并解析
        Result<List<Role>> response = restClient.get()
                .uri("http://localhost:9000/api/role/list")
                .header("accessToken", getRequestToken())
                .retrieve()
                .body(new ParameterizedTypeReference<Result<List<Role>>>() {});

        // 3. 转换为 Map<id, nickname>
        return Optional.ofNullable(response)
                .map(Result::data)
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(Role::id, Role::name));
    }

    // 用户信息
    public record User(Long id, String nickname) {}

    public record Role(Long id, String name) {}

    public record Dept(Long id, String name) {}

    // 通用返回包装类
    public record Result<T>(int code, String msg, T data) {}

    private String getRequestToken() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert requestAttributes != null;
        HttpServletRequest request = requestAttributes.getRequest();
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

    public static void main(String[] args) throws IOException, InterruptedException {
        RestTemplate restTemplate = new RestTemplate();
        String urlPattern = "https://ylink-uat.geniusafc.com/admin/voice-recognize/real-recognize/{id}";
        String id = "b2a823de-59f4-4f4e-a1d6-3da3a7ff18d2";

        // 假设接口返回的是 String，也可以替换为具体的 DTO 对象类（如 ResultDto.class）
        String response = restTemplate.getForObject(urlPattern, String.class, id);
        System.out.println("响应内容：" + response);
    }
}
