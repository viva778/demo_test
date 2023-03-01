package com.supcon.orchid.fooramework.util;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Function;

public class TransactionCaches {
    public static void set(String key, Object value) {
        RequestCaches.set(getInnerKey(key),value);
    }

    public static <X> X get(String key) {
        return RequestCaches.get(getInnerKey(key));
    }

    public static <V> V computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction){
        return RequestCaches.computeIfAbsent(getInnerKey(key),mappingFunction);
    }

    private static String getInnerKey(String key){
        return key+TransactionSynchronizationManager.getCurrentTransactionName();
    }
}
