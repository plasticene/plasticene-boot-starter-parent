package com.plasticene.boot.web.core.utils;

import com.plasticene.boot.common.utils.JsonUtils;
import com.plasticene.boot.web.core.model.RequestInfo;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2023/4/26 18:40
 */
@Slf4j
@UtilityClass
public class SignUtil {

    public SortedMap beanToMap(Object obj) {
        if (Objects.isNull(obj)) {
            return new TreeMap();
        }
        String s = JsonUtils.toJsonString(obj);
        TreeMap treeMap = JsonUtils.parseObject(s, TreeMap.class);
        return treeMap;
    }

    public String getContent(SortedMap sortedMap, String nonce, String timestamp) {
        StringBuilder sb = new StringBuilder();
        sortedMap.forEach((k, v) -> {
            sb.append(k + "=" + v + "&");
        });
        // 这里是反其道而行之，不按常理出牌，给破解加大一丢丢难度
        sb.append(nonce).append("=nonce").append(timestamp).append("=timestamp");
        return sb.toString();
    }

    public static void main(String[] args) {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setRequestParams("can shu");
        requestInfo.setIp("127.0.0.1");
        requestInfo.setHttpMethod("post");
        requestInfo.setUrl("/test");
        requestInfo.setClassMethod("com.shepherd.test()");
        SortedMap sortedMap = beanToMap(requestInfo);
        sortedMap.forEach((k, v) -> {
            System.out.println(k + "----" + v);
        });
    }


}
