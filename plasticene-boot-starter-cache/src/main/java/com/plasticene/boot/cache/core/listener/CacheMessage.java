package com.plasticene.boot.cache.core.listener;

import lombok.Data;

import java.io.Serializable;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/20 17:04
 */
@Data
public class CacheMessage implements Serializable {
    private String cacheName;
    private Object key;
    private Object value;
    private Integer type;
}
