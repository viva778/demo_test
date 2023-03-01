package com.supcon.orchid.fooramework.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Asynchronous {
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    public static <V> Future<V> submit(Callable<V> callable) {
        return EXECUTOR_SERVICE.submit(callable);
    }
}
