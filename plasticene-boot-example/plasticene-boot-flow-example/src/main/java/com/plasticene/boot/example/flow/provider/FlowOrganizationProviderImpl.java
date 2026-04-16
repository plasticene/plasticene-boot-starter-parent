package com.plasticene.boot.example.flow.provider;

import com.plasticene.boot.flow.core.provider.FlowOrganizationProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author ZFJ
 * @since 2026/4/14
 */
@Component
public class FlowOrganizationProviderImpl implements FlowOrganizationProvider {
    @Override
    public Map<Long, String> getUserMap() {
        // 1. 创建 RestClient
        RestClient restClient = RestClient.create();

        // 2. 调用接口并解析
        Result<List<User>> response = restClient.get()
                .uri("http://localhost:9000/api/user/list")
                .header("accessToken", "da6a35ab48d942e9a56c0c15cc452d50")
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
                .header("accessToken", "da6a35ab48d942e9a56c0c15cc452d50")
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
                .header("accessToken", "da6a35ab48d942e9a56c0c15cc452d50")
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
}
