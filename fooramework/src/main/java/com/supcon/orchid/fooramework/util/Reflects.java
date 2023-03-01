package com.supcon.orchid.fooramework.util;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 反射工具
 */
@SuppressWarnings("unchecked")
public class Reflects {

    private static final Logger log = LoggerFactory.getLogger(Reflects.class);


    //----------------------------------------------------------字段----------------------------------------------------------START
    private static final Map<Class<?>,List<Field>> fieldsCache = new HashMap<>();

    /**
     * 通过名称找Field
     */
    @SneakyThrows
    public static Field getField(Class<?> cls, String fieldName){
        for(Class<?> cur = cls;;){
            try {
                return cur.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                cur=cur.getSuperclass();
                if(cur!=null) continue;
                throw e;
            }
        }
    }

    /**
     * 通过全路径获取字段Getter
     * @param clazz 类
     * @param fieldPath 字段全路径
     * @return 字段
     */
    public static <T> Function<T,Object> getFieldGetterByFullPath(Class<T> clazz, String fieldPath){
        Class<?> cur_clazz = clazz;
        List<Field> fields = new LinkedList<>();
        for (String fieldName : fieldPath.split("\\.")) {
            Field field = getField(cur_clazz,fieldName);
            fields.add(field);
            cur_clazz = field.getType();
        }
        return t->{
            Object v = t;
            for (Field f : fields) {
                if(v!=null){
                    v = Reflects.getValue(v,f);
                } else {
                    return null;
                }
            }
            return v;
        };
    }

    /**
     * 通过全路径获取字段
     * @param clazz 类
     * @param fieldPath 字段全路径
     * @return 字段
     */
    public static Field getFieldByFullPath(Class<?> clazz, String fieldPath){
        Class<?> cur_clazz = clazz;
        Field field = null;
        for (String fieldName : fieldPath.split("\\.")) {
            field = getField(cur_clazz,fieldName);
            cur_clazz = field.getType();
        }
        return field;
    }
    /**
     * 获取包含父类的所有非静态字段
     */
    public static List<Field> getFields(Class<?> cls){
        List<Field> fieldList = fieldsCache.get(cls);
        if(fieldList==null){
            int size = 0;
            Stack<List<Field>> stack = new Stack<>();
            for (Class<?> cur = cls;cur!=null;cur=cur.getSuperclass()){
                List<Field> fields = Stream.of(cur.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).collect(Collectors.toList());
                if(fields.size()!=0){
                    stack.push(fields);
                    size+=fields.size();
                }
            }
            fieldList = new ArrayList<>(size);
            //先父后子
            while (!stack.empty()){
                List<Field> fields = stack.pop();
                fieldList.addAll(fields);
            }
            fieldsCache.put(cls,fieldList);
        }
        return fieldList;
    }

    public static Object getValue(Object entity, String fieldName){
        Field field;
        Object target;
        if(entity instanceof Class){
            field = getField((Class<?>) entity,fieldName);
            target = null;
        } else {
            field = getField(entity.getClass(),fieldName);
            target = entity;
        }
        return getValue(target,field);
    }

    @SneakyThrows
    public static Object getValue(Object entity, Field field) {
        if(field!=null){
            field.setAccessible(true);
            return field.get(entity);
        }
        return null;
    }

    /**
     * 设置对象字段值
     */
    @SneakyThrows
    public static void setValue(Object entity, Field field, Object value){
        boolean flag = field.isAccessible();
        field.setAccessible(true);
        field.set(entity,value);
        field.setAccessible(flag);
    }

    //----------------------------------------------------------字段----------------------------------------------------------END



    //----------------------------------------------------------方法----------------------------------------------------------START
    /**
     * 获取包含父类的所有非静态字段
     */
    public static List<Method> getMethods(Class<?> cls){
        int size = 0;
        Stack<List<Method>> stack = new Stack<>();
        for (Class<?> cur = cls;cur!=null;cur=cur.getSuperclass()){
            List<Method> methods = Stream.of(cur.getDeclaredMethods()).filter(method -> !Modifier.isStatic(method.getModifiers())).collect(Collectors.toList());
            if(methods.size()!=0){
                stack.push(methods);
                size+=methods.size();
            }
        }
        List<Method> methodList = new ArrayList<>(size);
        //先父后子
        while (!stack.empty()){
            List<Method> methods = stack.pop();
            methodList.addAll(methods);
        }
        return methodList;
    }

    @SneakyThrows
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>...parameterTypes){
        List<Method> methods = Stream.of(clazz.getDeclaredMethods()).filter(m->m.getName().equals(methodName)).collect(Collectors.toList());
        if(methods.size()>1){
            return clazz.getDeclaredMethod(methodName,parameterTypes);
        } else if(methods.size()>0){
            return methods.get(0);
        } else {
            return null;
        }
    }

    @SneakyThrows
    public static Object call(Object entity, String methodName, Object...args){
        boolean is_clazz = entity instanceof Class;
        Method method = _FindMethodForCall(is_clazz? (Class<?>) entity :entity.getClass(), methodName, args);
        if (method != null) {
            return call(is_clazz?null:entity, method, args);
        } else if(!is_clazz){
            //尝试查找被代理方法
            if (Aops.isAopProxy(entity)) {
                Object target = Aops.getTarget(entity);
                Method targetMethod = _FindMethodForCall(target.getClass(), methodName, args);
                if (targetMethod != null) {
                    return call(target, targetMethod, args);
                }
            }
        }
        throw new NoSuchMethodException(entity.getClass().getName() + "." + methodName);
    }

    @SneakyThrows
    public static Object call(Object entity, Method method, Object...args){
        boolean flag = method.isAccessible();
        method.setAccessible(true);
        try {
            Object res = method.invoke(entity,args);
            method.setAccessible(flag);
            return res;
        } catch ( InvocationTargetException e) {
            log.error("执行 '"+method.getName()+"' 方法时发生异常：");
            throw e.getTargetException();
        }
    }

    @SneakyThrows
    private static Method _FindMethodForCall(Class<?> clazz, String methodName, Object...args){
        Method method;
        if(args!=null&&args.length>0){
            List<Method> methods = Stream.of(clazz.getDeclaredMethods()).filter(m->m.getName().equals(methodName)).collect(Collectors.toList());
            Method found = null;
            for (Method mtd : methods) {
                Class<?>[] paramTypes = mtd.getParameterTypes();
                if(args.length==paramTypes.length){
                    boolean success = true;
                    for (int i=0;i<args.length;i++){
                        if(args[i]!=null&&!paramTypes[i].isAssignableFrom(args[i].getClass())){
                            success = false;
                            break;
                        }
                    }
                    if(success){
                        found = mtd;
                        break;
                    }
                }
            }
            method = found;
        } else {
            method = clazz.getMethod(methodName);
        }
        return method;
    }
    //----------------------------------------------------------方法----------------------------------------------------------END




    //----------------------------------------------------------注解----------------------------------------------------------START


    /**
     * 从类中选取注解值
     */
    public static <T extends Annotation,R> R getValueFromAnnotation(Class<?> cls, Class<T> annotationClass, Function<? super T, ? extends R> getter){
        T annotation = cls.getAnnotation(annotationClass);
        return  getter.apply(annotation);
    }

    /**
     * 从字段中选取注解值
     */
    public static <T extends Annotation,R> R getValueFromAnnotation(Field field, Class<T> annotationClass, Function<? super T, ? extends R> getter){
        T annotation = field.getAnnotation(annotationClass);
        return getter.apply(annotation);
    }

    /**
     * 从方法中选取注解值
     */
    public static <T extends Annotation,R> R getValueFromAnnotation(Method method, Class<T> annotationClass, Function<? super T, ? extends R> getter){
        T annotation = method.getAnnotation(annotationClass);
        return getter.apply(annotation);
    }

    /**
     * 设置注解值
     */
    public static void setAnnotationValue(Annotation annotation, String property, Object value) {
        InvocationHandler handler = Proxy.getInvocationHandler(annotation);
        Map<String,Object> memberValues = (Map<String,Object>)getValue(handler,"memberValues");
        if(memberValues!=null){
            memberValues.put(property, value);
        }
    }

    //----------------------------------------------------------注解----------------------------------------------------------END


}
