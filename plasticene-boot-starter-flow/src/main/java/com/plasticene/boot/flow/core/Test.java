package com.plasticene.boot.flow.core;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public class Test {

    public static void main(String[] args) {
        int a = 16;
        String s = format(a);
        System.out.println(s);
    }




    public static String format(Object obj) {
        return switch (obj) {
            case Integer i -> String.format("int %d", i);
            case Long l    -> String.format("long %d", l);
            case Double d  -> String.format("double %f", d);
            case String s  -> String.format("String %s", s);
            case null      -> "null";
            default        -> obj.toString();
        };
    }
}
