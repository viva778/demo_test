package com.supcon.orchid.fooramework.util;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class Springs {

    public static <T> T getBean(Class<T> clazz) {
        return app_context.getBean(clazz);
    }

    public static <T> T getBean(String name, Class<T> clazz) {
        if(app_context.containsBean(name)){
            return app_context.getBean(name,clazz);
        } else {
            return app_context.getBean(clazz);
        }
    }

    public static Object getBean(String beanName) {
        return app_context.getBean(beanName);
    }

    public static <T> Map<String,T> getBeansOfType(Class<T> type){
        return app_context.getBeansOfType(type);
    }

    public static Map<String,Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType){
        return app_context.getBeansWithAnnotation(annotationType);
    }

    public static Object getBean(String beanName, String className) throws ClassNotFoundException {
        Class<?> clz = Class.forName(className);
        return app_context.getBean(beanName, clz);
    }

    public static boolean containsBean(String name) {
        return app_context.containsBean(name);
    }

    public static boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        return app_context.isSingleton(name);
    }

    public static Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        return app_context.getType(name);
    }

    public static String[] getAliases(String name) throws NoSuchBeanDefinitionException {
        return app_context.getAliases(name);
    }

    public static String[] getBeanNamesForType(Class<?> clazz){
        return app_context.getBeanNamesForType(clazz);
    }

    public static String[] getBeanDefinitionNames(){
        return app_context.getBeanDefinitionNames();
    }

    private static ApplicationContext app_context;

    private static DefaultListableBeanFactory listable_bean_factory;

    static void setContext(ApplicationContext context){
        app_context = context;
        listable_bean_factory = (DefaultListableBeanFactory)((ConfigurableApplicationContext)context).getBeanFactory();
    }


    /**
     * 注册bean到spring容器中
     */
    public static <T> void registerBean(String beanName, Class<T> clazz, Supplier<T> supplier) {
        // 通过BeanDefinitionBuilder创建bean定义
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz,supplier);
        // 尝试移除之前相同的bean
        if (listable_bean_factory.containsBean(beanName)) {
            listable_bean_factory.removeBeanDefinition(beanName);
        }
        // 注册bean
        listable_bean_factory
                .registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());
    }
}

