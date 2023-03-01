package com.supcon.orchid.fooramework.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.collect.Sets;
import com.supcon.orchid.fooramework.support.Pair;
import lombok.SneakyThrows;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.data.util.CastUtils.cast;


public class Jacksons {


    private static final ObjectMapper DEFAULT_MAPPER;

    static {
        DEFAULT_MAPPER = new ObjectMapper();
        DEFAULT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
    }

    public static String writeValue(Object obj) {
        return _WriteValue(DEFAULT_MAPPER,obj);
    }

    public static String writeValueWithIncludes(Object obj, String includes){
        return _WriteValueWithIncludes(DEFAULT_MAPPER, obj, includes);
    }


    public static <X> X readValue(String jsonString, Class<?> cls){
        return _ReadValue(DEFAULT_MAPPER,jsonString,cls);
    }

    public static <T> T readValue(String jsonString, TypeReference<T> valueTypeRef){
        return _ReadValue(DEFAULT_MAPPER,jsonString,valueTypeRef);
    }

    public static <T> T convert(Object fromValue,TypeReference<T> valueTypeRef){
        return _Convert(DEFAULT_MAPPER,fromValue,valueTypeRef);
    }

    public static <X> X convert(Object fromValue, Class<?> cls){
        return _Convert(DEFAULT_MAPPER,fromValue,cls);
    }

    public static WithConfig config(){
        return new WithConfig();
    }

    public static class WithConfig {
        private final ObjectMapper mapper_with_config = new ObjectMapper();

        private WithConfig(){}

        public WithConfig setDateFormat(String dateFormat){
            mapper_with_config.setDateFormat(new SimpleDateFormat(dateFormat));
            return this;
        }

        public WithConfig include(JsonInclude.Include...includes){
            Stream.of(includes).forEach(mapper_with_config::setSerializationInclusion);
            return this;
        }

        public WithConfig includes(String beanId, String...properties){
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter(beanId, SimpleBeanPropertyFilter.filterOutAllExcept(Sets.newHashSet(properties)));
            mapper_with_config.setFilterProvider(filterProvider);
            return this;
        }

        public WithConfig enable(DeserializationFeature...features){
            ArrayOperator.of(features).forEach(mapper_with_config::enable);
            return this;
        }

        public WithConfig disable(DeserializationFeature...features){
            ArrayOperator.of(features).forEach(mapper_with_config::disable);
            return this;
        }

        public WithConfig setSerializationInclusion(JsonInclude.Include...includes){
            ArrayOperator.of(includes).forEach(mapper_with_config::setSerializationInclusion);
            return this;
        }

        public String writeValue(Object obj) {
            return _WriteValue(mapper_with_config,obj);
        }

        public String writeValueWithIncludes(Object obj, String includes){
            return _WriteValueWithIncludes(mapper_with_config, obj, includes);
        }

        public <X> X readValue(String jsonString, Class<?> cls){
            return _ReadValue(mapper_with_config,jsonString,cls);
        }

        public <T> T readValue(String jsonString, TypeReference<T> valueTypeRef){
            return _ReadValue(mapper_with_config,jsonString,valueTypeRef);
        }

        public <T> T convert(Object fromValue,TypeReference<T> valueTypeRef){
            return _Convert(mapper_with_config,fromValue,valueTypeRef);
        }

        public <X> X convert(Object fromValue, Class<?> cls){
            return _Convert(mapper_with_config,fromValue,cls);
        }
    }

    //------------------------------方法原型------------------------------

    @SneakyThrows
    private static String _WriteValue(ObjectMapper mapper, Object obj) {
        if(obj instanceof String) {
            return (String)obj;
        }
        return mapper.writeValueAsString(obj);
    }

    private static String _WriteValueWithIncludes(ObjectMapper mapper, Object obj, String includes){
        Pair<String[],Map<String,List<String>>> propInfo = _PropertyLayer.info(includes.split(","));
        Object cleanObj =  _RecConvertToMapWithIncludes(obj,propInfo.getFirst(),propInfo.getSecond());
        return _WriteValue(mapper,cleanObj);
    }

    /**
     * 只保留includes中的字段，将对象转为Map、List组合
     * （递归）
     */
    @SneakyThrows
    private static Object _RecConvertToMapWithIncludes(Object obj, String[] plainIncludes, Map<String, List<String>> innerIncludes){
        if(obj!=null){
            Object mappedValue;
            if(obj instanceof Collection) {
                //为集合时
                Collection<?> collection = (Collection<?>)obj;
                mappedValue = collection.stream().map(ele-> _RecConvertToMapWithIncludes(ele,plainIncludes,innerIncludes)).collect(Collectors.toList());
            } else if(obj.getClass().isArray()) {
                //为数组时
                mappedValue = Stream.of((Object[]) obj).map(ele-> _RecConvertToMapWithIncludes(ele,plainIncludes,innerIncludes)).collect(Collectors.toList());
            } else if(obj instanceof Map) {
                //为Map时
                Map<String,Object> copy = new LinkedHashMap<>();
                Map<String,Object> origin = cast(obj);
                //1).拷贝浅层
                Stream.of(plainIncludes).forEach(key->copy.put(key,origin.get(key)));
                //2).拷贝内部
                if(innerIncludes!=null){
                    innerIncludes.forEach((name,list)->{
                        if(origin.get(name)!=null){
                            Pair<String[],Map<String,List<String>>> subParams = _PropertyLayer.info(list.toArray(new String[0]));
                            //递归设置
                            copy.put(name, _RecConvertToMapWithIncludes(origin.get(name),subParams.getFirst(),subParams.getSecond()));
                        }
                    });
                }
                mappedValue = copy;
            } else {
                Map<String,Object> objectMap = new LinkedHashMap<>();
                //1).拷贝浅层
                for (String name : plainIncludes) {
                    objectMap.put(name, Reflects.getValue(obj,name));
                }
                //2).内部拷贝
                if(innerIncludes!=null){
                    for (Map.Entry<String, List<String>> entry : innerIncludes.entrySet()) {
                        String name = entry.getKey();
                        List<String> list = entry.getValue();

                        Object innerValue = Reflects.getValue(obj,name);
                        if (innerValue != null) {
                            Pair<String[], Map<String, List<String>>> subParams = _PropertyLayer.info(list.toArray(new String[0]));
                            //递归设置
                            objectMap.put(name,_RecConvertToMapWithIncludes(innerValue,subParams.getFirst(),subParams.getSecond()));
                        }
                    }
                }
                mappedValue = objectMap;
            }
            return cast(mappedValue);
        }
        return null;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private static <X> X _ReadValue(ObjectMapper mapper, String jsonString, Class<?> cls){
        return (X)mapper.readValue(jsonString,cls);
    }

    @SneakyThrows
    private static <T> T _ReadValue(ObjectMapper mapper, String jsonString, TypeReference<T> valueTypeRef){
        return mapper.readValue(jsonString,valueTypeRef);
    }

    private static <T> T _Convert(ObjectMapper mapper,Object fromValue,TypeReference<T> valueTypeRef){
        return mapper.convertValue(fromValue,valueTypeRef);
    }

    @SuppressWarnings("unchecked")
    private static <X> X _Convert(ObjectMapper mapper, Object fromValue, Class<?> cls){
        return (X)mapper.convertValue(fromValue,cls);
    }
}
