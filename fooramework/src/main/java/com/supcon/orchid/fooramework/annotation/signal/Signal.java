package com.supcon.orchid.fooramework.annotation.signal;

import java.lang.annotation.*;

/**
 * 信号
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Signal {
    String[] value();

    int priority() default 0;

    boolean signal_as_param() default false;
}
