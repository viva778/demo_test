package com.supcon.orchid.fooramework.util;



import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static org.springframework.data.util.CastUtils.cast;

/**
 * 本地缓存
 */
public class NativeCaches {
    private static final Map<String, Object> nativeCaches = new HashMap<>();

    private static final Map<String, Lock> lockMap = new ConcurrentHashMap<>();

    public static <X> X get(Object key) {
        return cast(nativeCaches.get(keyGetter(key)));
    }

    public static Long getLastUpdateTime(Object key){
        String sKey = keyGetter(key);
        return cast(nativeCaches.get(sKey+"_update_timestamp"));
    }

    public static void set(Object key,Object value) {
        String sKey = keyGetter(key);
        nativeCaches.put(sKey,value);
        nativeCaches.put(sKey+"_update_timestamp",System.currentTimeMillis());
    }

    public static <K,V> V lkComputeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        String sKey = keyGetter(key);
        Lock lock = lockMap.computeIfAbsent(sKey,k->new ReentrantLock());
        lock.lock();
        try {
            return computeIfAbsent(sKey,key,mappingFunction);
        } finally {
            lock.unlock();
            lockMap.remove(sKey);
        }
    }

    public static <K,V> V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction){
        return computeIfAbsent(keyGetter(key),key,mappingFunction);
    }

    private static <K,V> V computeIfAbsent(String sKey, K key, Function<? super K, ? extends V> mappingFunction){
        return cast(nativeCaches.computeIfAbsent(sKey, k->{
            nativeCaches.put(sKey+"_update_timestamp",System.currentTimeMillis());
            return mappingFunction.apply(key);
        }));
    }

    private static String keyGetter(Object key) {
        return key!=null?
                key.getClass() + key.toString() : null;
    }

}
