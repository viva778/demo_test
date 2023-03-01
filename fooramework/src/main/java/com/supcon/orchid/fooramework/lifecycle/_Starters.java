package com.supcon.orchid.fooramework.lifecycle;

import java.util.HashSet;
import java.util.Set;

class _Starters {
    private static final Set<String> clazz_set = new HashSet<>();

    private static final Set<String> object_set = new HashSet<>();

    /**
     * 类级别，这个方法只执行一次
     */
    public static void run_with_clazz(Class<?> caller, Runnable runnable){
        if(clazz_set.add(caller.getName()+runnable.hashCode())){
            runnable.run();
        }
    }

    /**
     * 对象级别，这个方法只执行一次
     */
    public static void run_with_object(Object caller, Runnable runnable){
        if(object_set.add(caller.getClass().getName()+caller+":"+runnable.hashCode())){
            runnable.run();
        }
    }
}