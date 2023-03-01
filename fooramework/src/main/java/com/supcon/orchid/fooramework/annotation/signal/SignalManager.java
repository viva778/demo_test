package com.supcon.orchid.fooramework.annotation.signal;

import com.supcon.orchid.fooramework.util.ArrayOperator;
import com.supcon.orchid.fooramework.util.Asynchronous;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class SignalManager {
    //信号名-->方法列表 映射
    private static final Map<String, SignalMethod[]> SIGNAL_METHODS_MAP = new HashMap<>();
    //匹配过后的信号缓存（因为支持正则表达式，所以不能简单用信号名-方法处理
    private static final Map<String,SignalMethod[]> MATCHED_SIGNAL_MAP_CACHE = new HashMap<>();

    static void _SetSignalMethodsMap(Map<String, List<SignalMethod>> signalMethodsMap){
        signalMethodsMap.forEach((signal,signal_methods)-> SIGNAL_METHODS_MAP.put(signal,signal_methods.toArray(new SignalMethod[0])));
    }

    public static int propagate(String signal, Map<String,Object> params){
        //调用注解方法
        SignalMethod[] signalMethods = _GetSignalMethods(signal);
        if(signalMethods.length>0){
            Map<String,Object> params_with_signal = null;
            for(SignalMethod signalMethod:signalMethods) {
                if(signalMethod.getSignal().signal_as_param()){
                    if(params_with_signal==null){
                        params_with_signal = new LinkedHashMap<>(params);
                        if(params.containsKey("signal")){
                            params_with_signal.put(String.valueOf(params_with_signal.hashCode()),signal);
                        } else {
                            params_with_signal.put("signal",signal);
                        }
                    }
                    signalMethod.call(params_with_signal);
                } else {
                    signalMethod.call(params);
                }
            }
        }
        return signalMethods.length;
    }

    public static int count(String signal){
        return _GetSignalMethods(signal).length;
    }

    /**
     * 触发信号
     */
    public static int propagate(String signal, Object...params){
        //调用注解方法
        SignalMethod[] signalMethods = _GetSignalMethods(signal);
        if(signalMethods.length>0){
            Object[] params_with_signal = null;
            for(SignalMethod signalMethod:signalMethods) {
                if(signalMethod.getSignal().signal_as_param()){
                    if(params_with_signal==null){
                        params_with_signal = ArrayOperator.of(params).concat(signal).get();
                    }
                    signalMethod.call(params_with_signal);
                } else {
                    signalMethod.call(params);
                }
            }
        }
        return signalMethods.length;
    }

    @SuppressWarnings("unchecked")
    public static <T> Future<T>[] collect_async(Class<T> collect_class, String signal, Object... params){
        return (Future<T>[])_Collect(collect_class,signal,true,params);
    }

    @SuppressWarnings("unchecked")
    public static <T> Future<T>[] collect_async(Class<T> collect_class, String signal, Map<String,Object> params){
        return (Future<T>[])_Collect(collect_class,signal,true,params);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] collect(Class<T> collect_class, String signal, Object... params){
        return (T[])_Collect(collect_class,signal,false,params);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] collect(Class<T> collect_class, String signal, Map<String,Object> params){
        return (T[])_Collect(collect_class,signal,false,params);
    }

    private static Object[] _Collect(Class<?> collect_class, String signal, boolean async, Object... params){
        SignalMethod[] signalMethods = _GetSignalMethods(signal);
        if(signalMethods.length>0){
            Object[] result = (Object[]) (async?new Future<?>[signalMethods.length]:Array.newInstance(collect_class,signalMethods.length));
            int index = 0;
            Object[] params_with_signal = null;
            for(SignalMethod signalMethod:signalMethods) {
                Object[] f_params;
                if(signalMethod.getSignal().signal_as_param()){
                    if(params_with_signal==null){
                        params_with_signal = ArrayOperator.of(params).concat(signal).get();
                    }
                    f_params = params_with_signal;
                } else {
                    f_params = params;
                }
                Object value = async? Asynchronous.submit(()->signalMethod.call(f_params)):signalMethod.call(f_params);
                if(!async&&value!=null&&!collect_class.isAssignableFrom(value.getClass())){
                    throw new IllegalArgumentException("收集信号类型不匹配");
                }
                result[index++] = value;
            }
            return result;
        }
        return (Object[]) Array.newInstance(collect_class,0);
    }

    private static Object[] _Collect(Class<?> collect_class, String signal, boolean async, Map<String,Object> params){
        SignalMethod[] signalMethods = _GetSignalMethods(signal);
        if(signalMethods.length>0){
            Object[] result = (Object[]) (async?new Future<?>[signalMethods.length]:Array.newInstance(collect_class,signalMethods.length));
            int index = 0;
            Map<String,Object> params_with_signal = null;
            for(SignalMethod signalMethod:signalMethods) {
                Map<String,Object> f_params;
                if(signalMethod.getSignal().signal_as_param()){
                    if(params_with_signal==null){
                        params_with_signal = new LinkedHashMap<>(params);
                        if(params.containsKey("signal")){
                            params_with_signal.put(String.valueOf(params_with_signal.hashCode()),signal);
                        } else {
                            params_with_signal.put("signal",signal);
                        }
                    }
                    f_params = params_with_signal;
                } else {
                    f_params = params;
                }
                Object value = async? Asynchronous.submit(()->signalMethod.call(f_params)):signalMethod.call(f_params);
                if(!async&&value!=null&&!collect_class.isAssignableFrom(value.getClass())){
                    throw new IllegalArgumentException("收集信号类型不匹配");
                }
                result[index++] = value;
            }
            return result;
        }
        return (Object[]) Array.newInstance(collect_class,0);
    }

    /**
     * 添加新的信号监听
     */
    public static void addSignalListener(String[] receive_signals, Object object, Method method, int priority, boolean signal_as_param){
        Signal v_signal = _GetVirtualSignal(receive_signals,priority,signal_as_param);
        SignalMethod addition = new SignalMethod(object,method,v_signal);
        for (String receive_signal : receive_signals) {
            //1.更新SIGNAL_METHODS_MAP
            {
                SignalMethod[] methods = Optional.ofNullable(SIGNAL_METHODS_MAP.get(receive_signal))
                        .map(origin_methods->ArrayOperator.of(origin_methods).concat(addition).get())
                        .orElseGet(()-> new SignalMethod[]{addition});
                SIGNAL_METHODS_MAP.put(receive_signal,methods);
            }
            //2.更新MATCHED_SIGNAL_MAP_CACHE
            {
                MATCHED_SIGNAL_MAP_CACHE.entrySet().forEach(entry->{
                    if(entry.getKey().matches(receive_signal)){
                        SignalMethod[] methods = ArrayOperator.of(entry.getValue()).concat(addition).sorted().get();
                        entry.setValue(methods);
                    }
                });
            }
        }
    }

    private static Signal _GetVirtualSignal(String[] receive_signals, int priority, boolean signal_as_param){
        return new Signal(){
            @Override
            public Class<? extends Annotation> annotationType() {
                return Signal.class;
            }

            @Override
            public String[] value() {
                return receive_signals;
            }

            @Override
            public int priority() {
                return priority;
            }

            @Override
            public boolean signal_as_param() {
                return signal_as_param;
            }
        };
    }

    private static SignalMethod[] _GetSignalMethods(String signal) {
        return MATCHED_SIGNAL_MAP_CACHE.computeIfAbsent(signal, s-> SIGNAL_METHODS_MAP
                .entrySet()
                .stream()
                .filter(entry->signal.matches(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Stream::of)
                .sorted()
                .toArray(SignalMethod[]::new)
        );
    }

}
