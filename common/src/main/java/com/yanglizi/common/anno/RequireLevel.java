package com.yanglizi.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author win11
 */
@Target({ElementType.METHOD, ElementType.TYPE})  // 可放在方法或类上
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireLevel {

    // 接口的唯一编码（用于动态查配置）
    String apiKey() default "";

    // 默认等级（如果动态配置没查到，用这个兜底）
    int value() default 1;

    String message() default "等级还不够";
}