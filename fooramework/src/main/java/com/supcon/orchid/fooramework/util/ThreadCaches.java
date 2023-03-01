package com.supcon.orchid.fooramework.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class ThreadCaches {

    private final static ThreadLocal<Map<String,Object>> tl = new ThreadLocal<>();

    public static void set(String key, Object value) {
        getCache().put(key, value);
    }

    @SuppressWarnings("unchecked")
    public static <X> X get(String key) {
        return ((X)getCache().get(key));
    }

    @SuppressWarnings("unchecked")
    public static <V> V computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction){
        return ((V)getCache().computeIfAbsent(key,mappingFunction));
    }

    private static Map<String,Object> getCache(){
        return Optional.ofNullable(tl.get()).orElseGet(()->{
            Map<String,Object> map = new HashMap<>();
            tl.set(map);
            return map;
        });
    }
}
