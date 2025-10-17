package com.plasticene.boot.delay.core.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author ZFJ
 * @date 2025/10/16
 */
@Data
@ConfigurationProperties(prefix = "ptc.delay")
public class DelayProperties {
    /**
     * 节点心跳续期保活周期，单位：秒，默认30s
     */
    private Integer heartbeatPeriod = 30;
    /**
     * 拉取数据初始化延迟时间，单位：秒，默认0s
     */
    private Integer pullInitialDelay = 0;
    /**
     * 拉取数据的周期，单位：秒，默认10m
     */
    private Integer pullPeriod = 10*60;
    /**
     * 健康检查初始化延迟时间，单位：秒，默认2m
     */
    private Integer healthInitialDelay = 2*60;
    /**
     * 健康检查周期，单位：秒，默认3m
     */
    private Integer healthPeriod = 3*60;
    /**
     * 删除数据的初始化延迟时间，单位：秒，默认1h
     */
    private Integer removeInitialDelay = 60*60;
    /**
     * 删除数据周期，单位：秒，默认1h
     */
    private Integer removePeriod = 60*60;
}
