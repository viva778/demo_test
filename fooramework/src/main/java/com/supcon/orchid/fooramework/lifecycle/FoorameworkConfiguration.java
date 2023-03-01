package com.supcon.orchid.fooramework.lifecycle;

import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;

import java.util.Arrays;

/**
 * 框架启动配置
 */
public interface FoorameworkConfiguration extends ApplicationListener<ApplicationEvent>{

    String[] scan_path();

    @Override
    default void onApplicationEvent(@NonNull ApplicationEvent event) {
        if(event instanceof ApplicationPreparedEvent) {
            _Starters.run_with_object(this,()->Arrays.stream(scan_path()).forEach(Fooramework::addScanPackage));
        } else if(event instanceof ApplicationStartedEvent) {
            _Starters.run_with_clazz(FoorameworkConfiguration.class,Fooramework::start);
        }
    }
}
