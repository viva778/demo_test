package com.supcon.orchid.fooramework.annotation.initiator;

import java.lang.annotation.*;

/**
 * Fooramework初始化前，会执行被此注解标注类的public静态方法
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Initiator {
}
