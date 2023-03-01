package com.supcon.orchid.fooramework.annotation.signal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.orchid.fooramework.support.Converter;
import com.supcon.orchid.fooramework.util.Aops;
import com.supcon.orchid.fooramework.util.Reflects;
import lombok.Getter;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.function.Function;

class SignalMethod implements Comparable<SignalMethod>{

    private final Object target;

    private final Method method;

    @Getter
    private final Signal signal;

    private final Parameter[] parameters;


    public SignalMethod(Object target, Method method, Signal signal) {
        this.target = target;
        this.method = method;
        this.signal = signal;
        //由于需要根据变量名调用，需要获取正确的方法对象
        if(target!=null&& !target.equals(Aops.getTarget(target))){
            Class<?> targetClass = Aops.getTarget(target).getClass();
            Method targetMethod = Reflects.getMethod(targetClass, method.getName(), method.getParameterTypes());
            assert targetMethod != null;
            this.parameters = targetMethod.getParameters();
        } else {
            this.parameters = method.getParameters();
        }
    }

    /**
     * 根据类型调用
     * @param arguments 全参数
     * @return 方法调用返回值
     */
    public Object call(Object... arguments){
        return Reflects.call(target,method,_match_parameter(parameters,arguments));
    }

    /**
     * 根据MAP调用，参数根据KEY名称和类型匹配
     * @param arguments 参数MAP
     * @return 方法调用返回值
     */
    public Object call(Map<String,Object> arguments){
        return Reflects.call(target,method,_match_parameter(parameters,arguments));
    }


    private Object[] _match_parameter(Parameter[] parameters, Object[] arguments){
        boolean[] used_arg_idx = new boolean[arguments.length];
        Object[] receive_params = new Object[parameters.length];

        //进行收发参数匹配
        matching:{
            int matched_cnt = 0;
            //根据值类型匹配
            for (int i = 0; i<parameters.length; i++) {
                if(receive_params[i]==null){
                    Parameter parameter = parameters[i];
                    for(int j=0; j<arguments.length; j++){
                        if(!used_arg_idx[j]){
                            Object value = arguments[j];
                            if(value!=null&&parameter.getType().isAssignableFrom(value.getClass())){
                                used_arg_idx[j] = true;
                                receive_params[i] = value;
                                matched_cnt++;
                                if(matched_cnt==parameters.length||matched_cnt==arguments.length){
                                    break matching;
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return receive_params;
    }

    private Object[] _match_parameter(Parameter[] parameters, Map<String,Object> arguments){
        Object[] receive_params = new Object[parameters.length];

        //进行收发参数匹配
        matching:{
            int matched_cnt = 0;
            //先匹配名称能被对应的参数(优先使用名称匹配，否则需要消费额外资源匹配参数
            for (int i=0; i<parameters.length; i++) {
                Parameter parameter = parameters[i];
                String key = parameter.getName();
                if(arguments.containsKey(key)){
                    receive_params[i] = convertType(parameter,arguments.get(key));
                    matched_cnt++;
                    if(matched_cnt==parameters.length||matched_cnt==arguments.size()){
                        break matching;
                    }
                }
            }

            //将parameter中的名称，在remain_key中全部移除
            String[] remain_keys = arguments.keySet().toArray(new String[0]);
            if(matched_cnt!=0){
                for (Parameter parameter : parameters) {
                    for(int j=0; j<remain_keys.length; j++){
                        if(remain_keys[j]!=null&&parameter.getName().equals(remain_keys[j])){
                            remain_keys[j] = null;
                            break;
                        }
                    }
                }
            }

            //如果未匹配完成，则继续根据值类型匹配
            for (int i = 0; i<parameters.length; i++) {
                if(receive_params[i]==null){
                    Parameter parameter = parameters[i];
                    for(int j=0; j<remain_keys.length; j++){
                        if(remain_keys[j]!=null){
                            String key = remain_keys[j];
                            Object value = arguments.get(key);
                            if(value!=null){
                                if(parameter.getType().isAssignableFrom(value.getClass())){
                                    remain_keys[j] = null;
                                    receive_params[i] = value;
                                    matched_cnt++;
                                    if(matched_cnt==parameters.length||matched_cnt==arguments.size()){
                                        break matching;
                                    } else {
                                        break;
                                    }
                                }
                            } else {
                                remain_keys[j] = null;
                            }
                        }
                    }
                }
            }
        }
        return receive_params;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static Object convertType(Parameter parameter, Object value){
        if(value!=null){
            if (!parameter.getType().isAssignableFrom(value.getClass())) {
                Function<Object, ?> converter = Converter.getConverter(parameter.getType());
                Object valueFx = converter.apply(value);
                if(valueFx!=null){
                    if (parameter.getType().isAssignableFrom(valueFx.getClass())) {
                        return valueFx;
                    } else {
                        //转换失败尝试使用jackson转换
                        return objectMapper.convertValue(value,parameter.getType());
                    }
                }
            }
            return value;
        }
        return null;
    }

    @Override
    public int compareTo(SignalMethod that) {
        return this.signal.priority()-that.signal.priority();
    }
}
