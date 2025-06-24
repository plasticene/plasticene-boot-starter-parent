package com.plasticene.boot.example.web.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.ttl.threadpool.TtlExecutors;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.example.web.param.UserParam;
import com.plasticene.boot.example.web.vo.UserVO;
import com.plasticene.boot.web.core.anno.ApiLog;
import com.plasticene.boot.web.core.anno.ApiSecurity;
import com.plasticene.boot.web.core.anno.ResponseResultBody;
import com.plasticene.boot.web.core.prop.ApiSecurityProperties;
import com.plasticene.boot.web.core.utils.AESUtil;
import com.plasticene.boot.web.core.utils.MDCTraceUtils;
import com.plasticene.boot.web.core.utils.RSAUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/21
 */
@Tag(name = "测试示例管理")
@RestController
@RequestMapping("/test")
@Slf4j
@ApiLog
@ResponseResultBody
public class TestController {
    @Resource
    private ApiSecurityProperties apiSecurityProperties;

    private final ExecutorService executorService = TtlExecutors.getTtlExecutorService(Executors.newFixedThreadPool(1));
//    private final ExecutorService executorService = Executors.newFixedThreadPool(1);


    @Operation(summary = "获取消息")
    @GetMapping("/msg")
    public ResponseVO<String> getMessage() {
        return ResponseVO.success("哈哈😄");
    }

    @Operation(summary = "测试swagger3文档、traceId、api接口出入参数输出")
    @PostMapping("/user")
    public ResponseVO<UserVO> addUser(@RequestBody UserParam param) {
        UserVO vo = UserVO.builder()
                .userId(param.getId())
                .userName(param.getName())
                .age(27)
                .build();
        log.info("vo===={}", vo);
        return ResponseVO.success(vo);
    }

    @Operation(summary = "获取用户信息(测试响应结果结构统一格式)")
    @GetMapping("/user")
    public UserVO getUser() {
        return UserVO.builder().userId(10L).userName("plasticene").build();
    }

    @Operation(summary = "加密出入参数接口，供调用测试(加解密接口必须是post)")
    @PostMapping("/user/list")
    @ApiSecurity(isSign = false, decryptRequest = true, encryptResponse = true)
    public ResponseVO<List<UserVO>> listUsers(@RequestBody UserParam param) {
        String name = StringUtils.isNotBlank(param.getName()) ? param.getName() : "张三";
        Integer gender = param.getGender();
        List<UserVO> voList = List.of(
                UserVO.builder().userId(10L).userName(name).age(gender == 1 ? 18 : 20).build(),
                UserVO.builder().userId(20L).userName(name + "20").age(gender == 0 ? 28 : 30).build()
        );
        log.info("voList===={}", voList);
        return ResponseVO.success(voList);
    }

    @Operation(summary = "测试调加密接口")
    @GetMapping("/api/security")
    public ResponseVO<List<UserVO>> testSecurityApi() throws IOException, InterruptedException {
        UserParam param = new UserParam();
        param.setName("哈哈");
        param.setGender(1);

        String aesKey = AESUtil.generateAESKey();
        String data = AESUtil.encrypt(JSON.toJSONString(param), aesKey);
        String key = RSAUtil.encryptByPublicKey(aesKey, apiSecurityProperties.getRsaPublicKey());
        JSONObject req = new JSONObject();
        req.put("key", key);
        req.put("data", data);
        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(req.toJSONString());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:8882/test/user/list"))
                .header("Content-Type", "application/json")
                .POST(body)
                .timeout(Duration.ofSeconds(5))
                .build();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ResponseVO<?> responseVO = JSON.parseObject(httpResponse.body(), ResponseVO.class);
        Object rst = responseVO.getData();
        JSONObject res = JSON.parseObject(rst.toString());
        String decryptKey = RSAUtil.decryptByPrivateKey((String) res.get("key"), apiSecurityProperties.getRsaPrivateKey());
        String resData = AESUtil.decrypt((String) res.get("data"), decryptKey);
        log.info("resData===={}", resData);
        return ResponseVO.success(JSON.parseArray(resData, UserVO.class));
    }

    @Operation(summary = "测试参数校验")
    @PostMapping("/param/valid")
    public void testValidator(@RequestBody @Validated UserParam param) {
        System.out.println(param);
    }

    @Operation(summary = "测试traceId多线程上下文传递")
    @GetMapping("/log/traceId")
    public void testLogTraceId() {
        // webTraceFilter会写入traceId
        log.info("主线程traceId: {}", MDCTraceUtils.getTraceId());
        Objects.requireNonNull(executorService, "executorService is null");
        executorService.submit(() -> {
            log.info("子线程traceId: {}", MDCTraceUtils.getTraceId());
            try {
                // 睡眠2秒，等webTraceFilter清除traceId
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("睡眠结束子线程traceId: {}", MDCTraceUtils.getTraceId());
        });
        MDCTraceUtils.removeTrace();
    }

    @Operation(summary = "测试错误日志打印")
    @GetMapping("/log/error")
    public ResponseVO<?> testLog() {
        var traceId = MDCTraceUtils.getTraceId();
        UserVO vo = new UserVO();
        try {
            int length = vo.getUserName().length();
            log.info("length===={}", length);
        } catch (Exception e) {
            log.error("错误异常日志输出：traceId:{}", traceId, e);
        }
       return ResponseVO.success(vo);
    }

}
