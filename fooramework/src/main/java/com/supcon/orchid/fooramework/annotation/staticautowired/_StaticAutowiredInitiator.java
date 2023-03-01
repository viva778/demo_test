package com.supcon.orchid.fooramework.annotation.staticautowired;

import com.supcon.orchid.fooramework.annotation.initiator.Initiator;
import com.supcon.orchid.fooramework.lifecycle.Fooramework;
import com.supcon.orchid.fooramework.support.AnnotationScanner;
import com.supcon.orchid.fooramework.util.Reflects;
import com.supcon.orchid.fooramework.util.Springs;

import java.lang.reflect.Field;
import java.util.Set;

@Initiator
class _StaticAutowiredInitiator {
    public static void init(){
        //注册注解到扫描器
        AnnotationScanner.addScanAnnotation(StaticAutowired.class);
        //扫描之后注入bean，优先级高于普通事件
        Fooramework.addPostEvent(10,()->{
            Set<Field> fieldSet = AnnotationScanner.getAnnotatedStaticFields(StaticAutowired.class);
            for(Field field:fieldSet){
                Object value = Springs.getBean(field.getName(),field.getType());
                Reflects.setValue(null,field, value);
            }
        });
    }
}
