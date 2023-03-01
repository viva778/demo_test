package com.supcon.orchid.fooramework.util;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.springframework.data.util.CastUtils.cast;

public class RequestCaches {
    private static final ThreadLocal<Map<String,Object>> localCaches = new ThreadLocal<>();


    public static void set(String key, Object value) {
        HttpServletRequest request = Https.getRequest();
        if(request!=null){
            request.setAttribute(key,value);
        } else {
            Map<String,Object> localMap;
            if(localCaches.get()==null){
                localMap = new HashMap<>();
                localCaches.set(localMap);
            } else {
                localMap = localCaches.get();
            }
            localMap.put(key, value);
        }
    }

    public static <X> X get(String key){
        HttpServletRequest request = Https.getRequest();
        if(request!=null){
            return cast(request.getAttribute(key));
        } else {
            if(localCaches.get()!=null){
                return cast(localCaches.get().get(key));
            } else {
                return null;
            }
        }
    }


    public static <V> V computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction){
        V cache = get(key);
        if(cache==null){
            V value = mappingFunction.apply(key);
            set(key,value);
            return value;
        } else {
            return cache;
        }
    }
}
