package com.supcon.orchid.fooramework.util;

import com.supcon.orchid.fooramework.annotation.initiator.Initiator;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@Initiator
public class _SpringInitiator implements ApplicationContextAware {
    private static ApplicationContext APPLICATION_CONTEXT;
    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        APPLICATION_CONTEXT = applicationContext;
    }

    public static void init(){
        Springs.setContext(APPLICATION_CONTEXT);
    }
}
