package com.supcon.orchid.fooramework.util;


import com.supcon.orchid.fooramework.support.GenericTypeInfo;
import com.supcon.orchid.fooramework.support.Pair;
import lombok.SneakyThrows;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.FeatureDescriptor;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.util.CastUtils.cast;

public class Beans {

    private static String[] getNullPropertyNames (Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for(java.beans.PropertyDescriptor pd : pds) {
            if(pd.getReadMethod()!=null){
                Object srcValue = src.getPropertyValue(pd.getName());
                if (srcValue == null) emptyNames.add(pd.getName());
            }
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    public static void copyPropertiesIgnoreNull(Object src, Object target){
        BeanUtils.copyProperties(src, target, getNullPropertyNames(src));
    }


    public static void copyProperties(Object src, Object target){
        BeanUtils.copyProperties(src, target);
    }

    public static Object getPropertyValue(Object source, String propertyName){
        final BeanWrapper src = new BeanWrapperImpl(source);
        int dotIdx = propertyName.indexOf(".");
        if(dotIdx==-1){
            return src.getPropertyValue(propertyName);
        } else {
            return getPropertyValue(src.getPropertyValue(propertyName.substring(0,dotIdx)),propertyName.substring(dotIdx+1));
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T> T getCopy(T bean){
        if(bean!=null){
            T copy = (T) bean.getClass().newInstance();
            BeanUtils.copyProperties(bean,copy);
            return copy;
        }
        return null;
    }

    /**
     * 复制对象，但只保留包含属性
     * @param bean 对象
     * @param includes 包含属性
     * @return 对象拷贝
     */
    public static <T> T getCopyWithIncludes(T bean, String includes){
        Pair<String[],Map<String,List<String>>> propInfo = _PropertyLayer.info(includes.split(","));
        return cast(_RecGetCopy(GenericTypeInfo.of(bean.getClass()),bean,propInfo.getFirst(),propInfo.getSecond()));
    }

    /**
     * 复制对象，但只保留包含属性
     * @param bean 对象
     * @param plainIncludes 外部属性
     * @param innerIncludes 内部包含
     * @return 对象拷贝
     */
    @SneakyThrows
    private static Object _RecGetCopy(GenericTypeInfo typeInfo, Object bean, String[] plainIncludes, Map<String,List<String>> innerIncludes){
        Object mappedValue;
        Class<?> beanClass = typeInfo.getType();
        if(bean instanceof Collection) {
            //为集合时
            Collection<?> collection = (Collection<?>)bean;
            GenericTypeInfo genericParameterTypeInfo = ArrayOperator.of(typeInfo.getGenericParametersInfo()).get(0);
            mappedValue = collection.stream().map(ele-> ele!=null? _RecGetCopy(Optional.ofNullable(genericParameterTypeInfo).orElse(GenericTypeInfo.of(ele.getClass())),ele,plainIncludes,innerIncludes):null).collect(Collectors.toCollection(()-> cast(collectionInstance(beanClass,bean.getClass()))));
        } else if(beanClass.isArray()) {
            //为数组时
            mappedValue = ArrayOperator.of((Object[]) bean)
                    .map(beanClass.getComponentType(),ele-> ele!=null?cast(_RecGetCopy(typeInfo.getComponentTypeInfo(),ele,plainIncludes,innerIncludes)):null)
                    .get();
        } else if(bean instanceof Map) {
            //为Map时
            Map<String,Object> origin = cast(bean);
            Map<String,Object> copy = cast(mapInstance(beanClass,bean.getClass(), origin.size()));
            //1).拷贝浅层
            for (String key : plainIncludes) {
                copy.put(key,origin.get(key));
            }
            //2).拷贝内部
            if(innerIncludes!=null){
                GenericTypeInfo genericParameterTypeInfo = ArrayOperator.of(typeInfo.getGenericParametersInfo()).get(1);
                innerIncludes.forEach((name,list)->{
                    Object val = origin.get(name);
                    if(val!=null){
                        Pair<String[],Map<String,List<String>>> subParams = _PropertyLayer.info(list.toArray(new String[0]));
                        //递归设置
                        copy.put(name, _RecGetCopy(Optional.ofNullable(genericParameterTypeInfo).orElse(GenericTypeInfo.of(val.getClass())),val,subParams.getFirst(),subParams.getSecond()));
                    }
                });
            }
            mappedValue = copy;
        } else {
            mappedValue = tryInstance(beanClass,bean.getClass(),null,null);
            final BeanWrapper srcWrapper = new BeanWrapperImpl(bean);
            java.beans.PropertyDescriptor[] pds = srcWrapper.getPropertyDescriptors();
            //1).拷贝浅层
            if(plainIncludes.length!=0){
                //获取差集
                String[] difference = Elements.difference(ArrayOperator.of(pds).map(String.class,FeatureDescriptor::getName).get(),plainIncludes).toArray(new String[0]);
                BeanUtils.copyProperties(bean,mappedValue,difference);
            }

            //2).内部拷贝
            if(innerIncludes!=null){
                final BeanWrapper targetWrapper = new BeanWrapperImpl(mappedValue);
                ArrayOperator.of(pds).filter(pd->innerIncludes.containsKey(pd.getName())).forEach(pd -> {
                    if(pd.getReadMethod()!=null&&pd.getWriteMethod()!=null){
                        Object val = srcWrapper.getPropertyValue(pd.getName());
                        if(val!=null){
                            String signature = (String) Reflects.getValue(Reflects.getField(beanClass,pd.getName()),"signature");
                            GenericTypeInfo genericParameterTypeInfo = signature!=null?GenericTypeInfo.of(signature):null;
                            _PropertyLayer.info(innerIncludes.get(pd.getName()).toArray(new String[0])).consume((subPlain,subInner)->{
                                targetWrapper.setPropertyValue(pd.getName(), _RecGetCopy(Optional.ofNullable(genericParameterTypeInfo).orElse(GenericTypeInfo.of(pd.getPropertyType())),val,subPlain,subInner));
                            });
                        }
                    }
                });
            }
        }
        return cast(mappedValue);
    }

    @SneakyThrows
    private static Object collectionInstance(Class<?> baseClass, Class<?> implClass){
        try {
            return tryInstance(baseClass,implClass,null,null);
        } catch (NoSuchMethodException e) {
            if(baseClass.isAssignableFrom(ArrayList.class)){
                return new ArrayList<>();
            } else if(baseClass.isAssignableFrom(LinkedHashSet.class)){
                return new LinkedHashSet<>();
            }
            throw e;
        }
    }


    @SneakyThrows
    private static Object mapInstance(Class<?> baseClass, Class<?> implClass,int size){
        try {
            return tryInstance(baseClass,implClass,new Class<?>[]{Integer.class},new Object[size]);
        } catch (NoSuchMethodException e) {
            if(baseClass.isAssignableFrom(LinkedHashMap.class)){
                return new LinkedHashMap<>(size);
            }
            throw e;
        }
    }


    @SneakyThrows
    private static Object tryInstance(Class<?> baseClass, Class<?> implClass, Class<?>[] parameterTypes, Object[] parameter) throws NoSuchMethodException{
        try {
            if(parameterTypes!=null){
                Constructor<?> constructor = implClass.getConstructor(parameterTypes);
                return constructor.newInstance(parameter);
            } else {
                return implClass.getConstructor().newInstance();
            }
        } catch (NoSuchMethodException e) {
            try {
                if(parameterTypes!=null){
                    Constructor<?> constructor = baseClass.getConstructor(parameterTypes);
                    return constructor.newInstance(parameter);
                } else {
                    return baseClass.getConstructor().newInstance();
                }
            } catch (NoSuchMethodException ignored) {
                throw e;
            }
        }
    }
}
