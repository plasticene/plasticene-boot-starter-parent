package com.plasticene.boot.common.utils;

import com.plasticene.boot.common.pojo.MaskRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/4/30 09:49
 */
@Slf4j
public class MaskUtils {


    /**
     * [中文姓名] 只显示第一个汉字，其他隐藏为2个星号<例子：李**>
     */
    public static String chineseName(final String fullName) {
        if (StringUtils.isBlank(fullName)) {
            return "";
        }
        final String name = StringUtils.left(fullName, 1);
        return StringUtils.rightPad(name, StringUtils.length(fullName), "*");
    }

    /**
     * [中文姓名] 只显示第一个汉字，其他隐藏为2个星号<例子：李**>
     */
    public static String chineseName(final String familyName, final String givenName) {
        if (StringUtils.isBlank(familyName) || StringUtils.isBlank(givenName)) {
            return "";
        }
        return chineseName(familyName + givenName);
    }

    /**
     * [身份证号] 显示最后四位，其他隐藏。共计18位或者15位。<例子：420**********5762>
     */
    public static String idCardNum(final String id) {
        if (StringUtils.isBlank(id)) {
            return "";
        }

        return StringUtils.left(id, 3).concat(StringUtils
                .removeStart(StringUtils.leftPad(StringUtils.right(id, 4), StringUtils.length(id), "*"),
                        "***"));
    }

    /**
     * [固定电话] 后四位，其他隐藏<例子：****1234>
     */
    public static String fixedPhone(final String num) {
        if (StringUtils.isBlank(num)) {
            return "";
        }
        return StringUtils.leftPad(StringUtils.right(num, 4), StringUtils.length(num), "*");
    }

    /**
     * [手机号码] 前三位，后四位，其他隐藏<例子:138******1234>
     */
    public static String mobilePhone(final String num) {
        if (StringUtils.isBlank(num)) {
            return "";
        }
        return StringUtils.left(num, 3).concat(StringUtils
                .removeStart(StringUtils.leftPad(StringUtils.right(num, 4), StringUtils.length(num), "*"),
                        "***"));

    }

    /**
     * [地址] 只显示到地区，不显示详细地址；我们要对个人信息增强保护<例子：北京市海淀区****>
     *
     * @param sensitiveSize 敏感信息长度
     */
    public static String address(final String address, final int sensitiveSize) {
        if (StringUtils.isBlank(address)) {
            return "";
        }
        final int length = StringUtils.length(address);
        return StringUtils.rightPad(StringUtils.left(address, length - sensitiveSize), length, "*");
    }

    /**
     * [电子邮箱] 邮箱前缀仅显示第一个字母，前缀其他隐藏，用星号代替，@及后面的地址显示<例子:g**@163.com>
     */
    public static String email(final String email) {
        if (StringUtils.isBlank(email)) {
            return "";
        }
        final int index = StringUtils.indexOf(email, "@");
        if (index <= 1) {
            return email;
        } else {
            return StringUtils.rightPad(StringUtils.left(email, 1), index, "*")
                    .concat(StringUtils.mid(email, index, StringUtils.length(email)));
        }
    }

    /**
     * [银行卡号] 前六位，后四位，其他用星号隐藏每位1个星号<例子:6222600**********1234>
     */
    public static String bankCard(final String cardNum) {
        if (StringUtils.isBlank(cardNum)) {
            return "";
        }
        return StringUtils.left(cardNum, 6).concat(StringUtils.removeStart(
                StringUtils.leftPad(StringUtils.right(cardNum, 4), StringUtils.length(cardNum), "*"),
                "******"));
    }

    /**
     * [api秘钥] 前3位，后3位，其他用星号隐藏每位1个星号<例子:Aj3**********8Kl>
     */
    public static String apiSecret(final String cardNum) {
        if (StringUtils.isBlank(cardNum)) {
            return "";
        }
        return StringUtils.left(cardNum, 3).concat(StringUtils.removeStart(
                StringUtils.leftPad(StringUtils.right(cardNum, 3), StringUtils.length(cardNum), "*"),
                "******"));
    }

    public static String commonMask(String text, MaskRule rule) {
        if (StringUtils.isBlank(text) || Objects.isNull(rule)) {
            return text;
        }
        int length = text.length();
        // 0：隐藏，1：显示
        Integer type = rule.getType();
        // 开头:0 中间:1 末尾: -1 全部: 2 区间：3
        Integer scope = rule.getScope();
        Integer count = rule.getCount();
        Integer start = rule.getStart();
        Integer end = rule.getEnd();

        try {
            StringBuilder ms = new StringBuilder();
            // 开头count位
            if (scope == 0) {
                for (int i = 0; i < length; i++) {
                    if (i < count) {
                        if (type == 0) ms.append("*");
                        if (type == 1) ms.append(text.charAt(i));
                    } else {
                        if (type == 0) ms.append(text.charAt(i));
                        if (type == 1) ms.append("*");
                    }
                }
            }
            if (scope == 1) {
                // 中间count位
                int mid = length/2;
                int left = mid - count/2;
                left = left < 0 ? 0 : left;
                int right = mid + count/2 - (count%2==0? 1: 0);
                for (int i = 0; i < length; i++) {
                    if (i >= left && i <= right) {
                        if (type == 0) ms.append("*");
                        if (type == 1) ms.append(text.charAt(i));
                    } else {
                        if (type == 0) ms.append(text.charAt(i));
                        if (type == 1) ms.append("*");
                    }
                }
            }
            if (scope == -1) {
                // 末尾屏蔽count位
                int n = length - count;
                n = n < 0 ? 0 : n;
                for (int i = 0 ; i < length; i++) {
                    if (i >= n) {
                        if (type == 0) ms.append("*");
                        if (type == 1) ms.append(text.charAt(i));
                    } else {
                        if (type == 0) ms.append(text.charAt(i));
                        if (type == 1) ms.append("*");
                    }
                }
            }
            if (scope == 2) {
                // 全部
                for (int i = 0; i < length; i++) {
                    if (type == 0) ms.append("*");
                    if (type == 1) ms.append(text.charAt(i));
                }
            }
            if (scope == 3) {
                // 区间
                for (int i = 0; i < length; i++) {
                    if (i >= start -1  && i <= end - 1) {
                        if (type == 0) ms.append("*");
                        if (type == 1) ms.append(text.charAt(i));
                    } else {
                        if (type == 0) ms.append(text.charAt(i));
                        if (type == 1) ms.append("*");
                    }
                }
            }
            return ms.toString();
        } catch (Exception e) {
            log.error("脱敏转换失败：", e);
        }
        return text;
    }

    public static void main(String[] args) {
        String text = "17812345939";
        MaskRule rule = new MaskRule();
        rule.setType(0);
        rule.setScope(1);
        rule.setCount(8);
        String s = commonMask(text, rule);
        System.out.println(s);

    }


}
