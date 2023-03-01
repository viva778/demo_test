package com.supcon.orchid.material.superwms.constants;

import java.math.BigDecimal;

public class Tolerant {
    public static final BigDecimal BIG_DECIMAL_TOLERANT = new BigDecimal("0.001");

    /**
     * 等于
     */
    public static boolean equals(BigDecimal a, BigDecimal b){
        return a.subtract(b).abs().compareTo(BIG_DECIMAL_TOLERANT)<0;
    }

    /**
     * 大于等于
     */
    public static boolean ge(BigDecimal a, BigDecimal b){
        return a.subtract(b).compareTo(BIG_DECIMAL_TOLERANT.negate())>0;
    }

    /**
     * 小于等于
     */
    public static boolean le(BigDecimal a, BigDecimal b){
        return a.subtract(b).compareTo(BIG_DECIMAL_TOLERANT)<0;
    }

    /**
     * 大于
     */
    public static boolean greater(BigDecimal a, BigDecimal b){
        return a.subtract(b).compareTo(BIG_DECIMAL_TOLERANT)>0;
    }

    /**
     * 小于
     */
    public static boolean less(BigDecimal a, BigDecimal b){
        return b.subtract(a).compareTo(BIG_DECIMAL_TOLERANT)>0;
    }
}
