package com.supcon.orchid.material.superwms.config;

import com.supcon.orchid.fooramework.annotation.signal.SignalManager;
import com.supcon.orchid.fooramework.services.PlatformAutoActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 给groovy脚本提供信号触发入口
 */
@Component("signal")
public class SignalManagerBean {
    public void activate(String signal, Map<String,Object> params){
        SignalManager.propagate(signal,params);
    }

    public void activate(String signal, Object... params){
        SignalManager.propagate(signal,params);
    }

    @Autowired
    private PlatformAutoActivityService activityService;

    //标记后延期执行
    public void delay(String signal, Map<String,Object> params){
        activityService.addActivity(signal, params);
    }

    //标记后延期执行
    public void delay(String signal, Object... params){
        activityService.addActivity(signal, params);
    }

}
