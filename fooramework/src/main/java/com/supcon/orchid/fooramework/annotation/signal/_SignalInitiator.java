package com.supcon.orchid.fooramework.annotation.signal;

import com.supcon.orchid.fooramework.annotation.initiator.Initiator;
import com.supcon.orchid.fooramework.lifecycle.Fooramework;
import com.supcon.orchid.fooramework.support.AnnotationScanner;
import com.supcon.orchid.fooramework.util.Aops;
import com.supcon.orchid.fooramework.util.Reflects;
import com.supcon.orchid.fooramework.util.Springs;

import java.lang.reflect.Method;
import java.util.*;

@Initiator
class _SignalInitiator {
    public static void init(){
        //注册注解到扫描器
        AnnotationScanner.addScanAnnotation(Signal.class);
        //扫描后初始化
        Fooramework.addPostEvent(2, _SignalInitiator::setup);
    }

    private static void setup(){
        //初始化SIGNAL_METHODS_MAP
        if(!Fooramework.isStarted()){
            Map<String, List<SignalMethod>> signalMethodsMap = new HashMap<>();
            //静态的signal直接加到map
            Set<Method> staticMethods = AnnotationScanner.getAnnotatedStaticMethods(Signal.class);
            for(Method method:staticMethods){
                Signal a_signal = method.getAnnotation(Signal.class);
                String[] sig_val = a_signal.value();
                if(sig_val!=null){
                    for(String signalString:sig_val){
                        List<SignalMethod> signalMethods = signalMethodsMap.computeIfAbsent(signalString, k -> new LinkedList<>());
                        signalMethods.add(new SignalMethod(null,method,a_signal));
                    }
                }
            }
            //非静态则查找bean，之后连对象一起加到map
            Set<Method> methods = AnnotationScanner.getAnnotatedMethods(Signal.class);
            for(Method method:methods){
                Signal a_signal = method.getAnnotation(Signal.class);
                String[] sig_val = a_signal.value();
                if(sig_val!=null) {
                    Object p_target = Springs.getBean(method.getDeclaringClass());
                    Method p_method = Reflects.getMethod(p_target.getClass(),method.getName(),method.getParameterTypes());
                    boolean isProxy = p_method!=null&&!method.equals(p_method);
                    Method r_method = isProxy?
                            p_method:method;
                    Object r_target = isProxy?
                            p_target: Aops.getTarget(p_target);
                    for (String signalString : sig_val) {
                        List<SignalMethod> signalMethods = signalMethodsMap.computeIfAbsent(signalString, k -> new LinkedList<>());
                        signalMethods.add(new SignalMethod(r_target,r_method,a_signal));
                    }
                }
            }
            signalMethodsMap.forEach((signal,signal_methods)-> SignalManager._SetSignalMethodsMap(signalMethodsMap));
            SignalManager.propagate("startup");
        }
    }
}
