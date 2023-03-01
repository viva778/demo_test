package com.supcon.orchid.fooramework.annotation.staticautowired;

import java.lang.annotation.*;

/**
 * 标注在非final的静态字段上，会自动注入bean
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StaticAutowired {
}
