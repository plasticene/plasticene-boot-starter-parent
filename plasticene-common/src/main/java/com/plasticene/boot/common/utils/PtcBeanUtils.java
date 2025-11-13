package com.plasticene.boot.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/8/17 16:21
 */
public class PtcBeanUtils {

    private static final Logger logger = LoggerFactory.getLogger(PtcBeanUtils.class);

    public static <A, B> B copy(A a, Class<B> clazz) {
        if (a == null || clazz == null) {
            return null;
        }

        try {
            B b = clazz.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(a, b);
            return b;
        } catch (Exception e) {
            logger.error("PtcBeanUtils#copy error.", e);
        }
        return null;
    }

    public static <A, B> void copy(A a, B b) {
        if (a == null || b == null) {
            return;
        }
        try {
            BeanUtils.copyProperties(a, b);
        } catch (Exception e) {
            logger.error("PtcBeanUtils#copy error.", e);
        }
    }

    /**
     * 拷贝属性，忽略源对象中的null值
     */
    public static void copyPropertiesIgnoreNull(Object source, Object target) {
        BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
    }

    /**
     * 获取源对象中为null的属性名数组
     */
    private static String[] getNullPropertyNames(Object source) {
        if (source == null) {
            return new String[0];
        }
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            String propertyName = pd.getName();
            // 跳过class属性
            if ("class".equals(propertyName)) {
                continue;
            }
            Object srcValue = src.getPropertyValue(propertyName);
            if (srcValue == null) {
                emptyNames.add(propertyName);
            }
        }
        return emptyNames.toArray(new String[0]);
    }

    /**
     * 拷贝属性，不覆盖target中已有的非null值
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyPropertiesSkipExisting(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        // 获取target中非null的字段名
        String[] nonNullProperties = getNonNullPropertyNames(target);
        // 使用Spring的BeanUtils，忽略target中非null的字段
        BeanUtils.copyProperties(source, target, nonNullProperties);
    }

    /**
     * 获取对象中非null的属性名数组
     */
    private static String[] getNonNullPropertyNames(Object target) {
        if (target == null) {
            return new String[0];
        }

        final BeanWrapper beanWrapper = new BeanWrapperImpl(target);
        PropertyDescriptor[] pds = beanWrapper.getPropertyDescriptors();

        Set<String> nonNullNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            String propertyName = pd.getName();
            // 跳过class属性
            if ("class".equals(propertyName)) {
                continue;
            }
            Object propertyValue = beanWrapper.getPropertyValue(propertyName);
            if (propertyValue != null) {
                nonNullNames.add(propertyName);
            }
        }

        return nonNullNames.toArray(new String[0]);
    }
}
