package com.supcon.orchid.fooramework.lifecycle;


import com.supcon.orchid.fooramework.annotation.initiator.Initiator;
import com.supcon.orchid.fooramework.support.AnnotationScanner;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.Reflects;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;


@SuppressWarnings("unused")
public class Fooramework {


    public static final Set<String> SCAN_PACK_LIST = new HashSet<>();

    public static void addScanPackage(String packName){
        SCAN_PACK_LIST.add(packName);
    }


    public static Set<String> getPackList(){
        return SCAN_PACK_LIST;
    }



    //第一个是方法，第二个是执行优先级，高的在前面
    private static final List<Pair<Runnable,Integer>> postEvent = new LinkedList<>();
    private static final List<Pair<Runnable,Integer>> preEvent = new LinkedList<>();

    public static void addPreEvent(Runnable runnable){
        preEvent.add(Pair.of(runnable,1));
    }
    public static void addPostEvent(Runnable runnable){
        postEvent.add(Pair.of(runnable,1));
    }

    public static void addPreEvent(int priority,Runnable runnable){
        preEvent.add(Pair.of(runnable,priority));
    }
    public static void addPostEvent(int priority,Runnable runnable){
        postEvent.add(Pair.of(runnable,priority));
    }


    private static boolean started = false;
    private static final Lock startLock = new ReentrantLock();

    /**
     * 框架开启
     */
    static void start() {
        if(!started&&startLock.tryLock()){
            try {
                if(!started) {
                    //1.扫描框架包
                    String thisPath = Fooramework.class.getName();
                    String foorameworkPath = thisPath.substring(0,thisPath.lastIndexOf("lifecycle")-1);
                    Set<Class<?>> classes = AnnotationScanner.scanClasses(foorameworkPath);

                    //2.执行所有initiator中的静态方法
                    classes.forEach(clazz->{
                        if(clazz.isAnnotationPresent(Initiator.class)){
                            Stream.of(clazz.getMethods())
                                    .filter(method -> Modifier.isStatic(method.getModifiers()))
                                    .filter(method -> method.getParameterCount()==0)
                                    .forEach(method-> Reflects.call(null,method));
                        }
                    });

                    //3.把扫描事件根据优先级排序
                    preEvent.sort((e1,e2)-> e2.getSecond()-e1.getSecond());
                    postEvent.sort((e1,e2)-> e2.getSecond()-e1.getSecond());

                    //4.运行扫描事件和扫描
                    for (Pair<Runnable,?> pair: preEvent)
                        pair.getFirst().run();
                    AnnotationScanner.scan();
                    for (Pair<Runnable,?> pair: postEvent)
                        pair.getFirst().run();

                    //5.启动完成
                    started = true;
                }
            } finally {
                startLock.unlock();
            }
        }
    }

    public static boolean isStarted() {
        return started;
    }

}
