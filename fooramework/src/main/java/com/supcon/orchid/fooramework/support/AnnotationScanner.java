package com.supcon.orchid.fooramework.support;


import com.supcon.orchid.fooramework.lifecycle.Fooramework;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class AnnotationScanner {
    private static final Set<Class<? extends Annotation>> markedAnnotation = new HashSet<>();
    private static final Map<Class<? extends Annotation>, Set<Class<?>>> classAnnotationMap = new HashMap<>();
    private static final Map<Class<? extends Annotation>,Set<Field>> fieldAnnotationMap = new HashMap<>();
    private static final Map<Class<? extends Annotation>,Set<Field>> staticFieldAnnotationMap = new HashMap<>();
    private static final Map<Class<? extends Annotation>,Set<Method>> methodAnnotationMap = new HashMap<>();
    private static final Map<Class<? extends Annotation>,Set<Method>> staticMethodAnnotationMap = new HashMap<>();
    private static Set<Class<?>> classSet;
    private static Map<String,Class<?>> classMap;

    @SneakyThrows
    public static Set<Class<?>> scanClasses(String path) {
        String classpath = "classpath*:" + path.replace(".", "/") + "/**/*.class";
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metaReader = new CachingMetadataReaderFactory();
        Resource[] resources = resolver.getResources(classpath);
        ClassLoader loader = AnnotationScanner.class.getClassLoader();
        Set<Class<?>> classList = new HashSet<>();
        for (Resource resource : resources) {
            MetadataReader reader = metaReader.getMetadataReader(resource);
            String className = reader.getClassMetadata().getClassName();
            Class<?> clazz = loader.loadClass(className);
            classList.add(clazz);
        }
        return classList;
    }

    /**
     * 扫描项目所有类
     */
    @SneakyThrows
    private static Set<Class<?>> scanClasses(){
        Set<Class<?>> classList = new HashSet<>();
        Set<String> packList = Fooramework.getPackList();
        for (String packName : packList) {
            String classpath = "classpath*:" + packName.replace(".", "/") + "/**/*.class";
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            MetadataReaderFactory metaReader = new CachingMetadataReaderFactory();
            Resource[] resources = resolver.getResources(classpath);
            ClassLoader loader = AnnotationScanner.class.getClassLoader();
            for (Resource resource : resources) {
                MetadataReader reader = metaReader.getMetadataReader(resource);
                String className = reader.getClassMetadata().getClassName();
                Class<?> clazz = loader.loadClass(className);
                classList.add(clazz);
            }
        }
        return classList;
    }

    //添加需要扫描的注解
    public static void addScanAnnotation(Class<? extends Annotation> cls){
        markedAnnotation.add(cls);
    }

    public static Set<Class<?>> getClasses(){
        return classSet;
    }

    public static Map<String, Class<?>> getClassMap() {
        return classMap;
    }

    public static void scan(){
        classSet = scanClasses();
        classMap = classSet.stream().collect(Collectors.toMap(
                Class::getName,
                clazz->clazz
        ));
        for(Class<?> cls:classSet){
            //添加类上的所有注解map
            Annotation[] classAnnotations = cls.getDeclaredAnnotations();
            for(Annotation annotation:classAnnotations){
                if(markedAnnotation.contains(annotation.annotationType())){
                    Set<Class<?>> classSet = classAnnotationMap.computeIfAbsent(annotation.annotationType(), k -> new HashSet<>());
                    classSet.add(cls);
                }
            }
            //添加字段上的所有注解map，静态和非静态分开
            Field[] fields = cls.getDeclaredFields();
            for(Field field:fields){
                Annotation[] fieldAnnotations = field.getDeclaredAnnotations();
                if(fieldAnnotations.length>0){
                    Map<Class<? extends Annotation>,Set<Field>> curMap;
                    curMap = Modifier.isStatic(field.getModifiers())?staticFieldAnnotationMap:fieldAnnotationMap;
                    for (Annotation annotation:fieldAnnotations){
                        if(markedAnnotation.contains(annotation.annotationType())){
                            Set<Field> fieldSet = curMap.computeIfAbsent(annotation.annotationType(), k -> new HashSet<>());
                            fieldSet.add(field);
                        }
                    }
                }
            }
            //添加方法上的所有注解map，静态和非静态分开
            Method[] methods = cls.getDeclaredMethods();
            for(Method method:methods){
                Map<Class<? extends Annotation>,Set<Method>> curMap;
                curMap = Modifier.isStatic(method.getModifiers())?staticMethodAnnotationMap:methodAnnotationMap;
                Annotation[] annotations = method.getDeclaredAnnotations();
                for(Annotation annotation:annotations){
                    if(markedAnnotation.contains(annotation.annotationType())){
                        Set<Method> methodSet = curMap.computeIfAbsent(annotation.annotationType(), k -> new HashSet<>());
                        methodSet.add(method);
                    }
                }
            }
        }
    }

    public static Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> cls){
        Set<Class<?>> res = classAnnotationMap.get(cls);
        return res==null? Collections.emptySet() :new HashSet<>(res);
    }

    public static Set<Field> getAnnotatedStaticFields(Class<? extends Annotation> cls){
        Set<Field> res = staticFieldAnnotationMap.get(cls);
        return res==null? Collections.emptySet() :new HashSet<>(res);
    }

    public static Set<Field> getAnnotatedFields(Class<? extends Annotation> cls){
        Set<Field> res = fieldAnnotationMap.get(cls);
        return res==null? Collections.emptySet() :new HashSet<>(res);
    }

    public static Set<Method> getAnnotatedMethods(Class<? extends Annotation> cls){
        Set<Method> res = methodAnnotationMap.get(cls);
        return res==null? Collections.emptySet() :new HashSet<>(res);
    }

    public static Set<Method> getAnnotatedStaticMethods(Class<? extends Annotation> cls){
        Set<Method> res = staticMethodAnnotationMap.get(cls);
        return res==null? Collections.emptySet() :new HashSet<>(res);
    }
}
