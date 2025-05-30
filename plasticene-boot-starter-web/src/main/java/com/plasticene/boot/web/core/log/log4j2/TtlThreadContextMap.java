package com.plasticene.boot.web.core.log.log4j2;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.apache.logging.log4j.spi.ThreadContextMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/29
 */
public class TtlThreadContextMap implements ThreadContextMap {

    private final ThreadLocal<Map<String, String>> localMap;

    public TtlThreadContextMap() {
        this.localMap = new TransmittableThreadLocal<>();
    }

    @Override
    public void put(final String key, final String value) {
        Map<String, String> map = localMap.get();
        map = map == null ? new HashMap<>() : new HashMap<>(map);
        map.put(key, value);
        localMap.set(Collections.unmodifiableMap(map));
    }

    @Override
    public String get(final String key) {
        final Map<String, String> map = localMap.get();
        return map == null ? null : map.get(key);
    }

    @Override
    public void remove(final String key) {
        final Map<String, String> map = localMap.get();
        if (map != null) {
            final Map<String, String> copy = new HashMap<>(map);
            copy.remove(key);
            localMap.set(Collections.unmodifiableMap(copy));
        }
    }

    @Override
    public void clear() {
        localMap.remove();
    }

    @Override
    public boolean containsKey(final String key) {
        final Map<String, String> map = localMap.get();
        return map != null && map.containsKey(key);
    }

    @Override
    public Map<String, String> getCopy() {
        final Map<String, String> map = localMap.get();
        return map == null ? new HashMap<>() : new HashMap<>(map);
    }

    @Override
    public Map<String, String> getImmutableMapOrNull() {
        return localMap.get();
    }

    @Override
    public boolean isEmpty() {
        final Map<String, String> map = localMap.get();
        return map == null || map.isEmpty();
    }

    @Override
    public String toString() {
        final Map<String, String> map = localMap.get();
        return map == null ? "{}" : map.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        final Map<String, String> map = this.localMap.get();
        result = prime * result + ((map == null) ? 0 : map.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TtlThreadContextMap other)) {
            return false;
        }
        final Map<String, String> map = this.localMap.get();
        final Map<String, String> otherMap = other.getImmutableMapOrNull();
        if (map == null) {
            return otherMap == null;
        } else {
            return map.equals(otherMap);
        }
    }
}
