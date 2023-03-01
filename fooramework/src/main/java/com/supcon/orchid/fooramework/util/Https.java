package com.supcon.orchid.fooramework.util;


import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class Https {

    public static HttpServletRequest getRequest(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attributes!=null){
            return attributes.getRequest();
        } else {
            return null;
        }
    }

    public static HttpServletResponse getResponse(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attributes!=null){
            return attributes.getResponse();
        } else {
            return null;
        }
    }

    public static String getParameter(String key){
        HttpServletRequest request = getRequest();
        Assert.notNull(request, "request missing");
        return request.getParameter(key);
    }

    public static Map<String,String[]> getParameterMap(){
        HttpServletRequest request = getRequest();
        Assert.notNull(request, "request missing");
        return request.getParameterMap();
    }

    /**
     * 包含url参数，且该参数不等于
     * @param key 参数key
     * @param val 比较对象
     */
    public static boolean parameterContainsAndNotEqualTo(String key, String val){
        HttpServletRequest request = getRequest();
        Assert.notNull(request, "request missing");
        if(request.getParameterMap().containsKey(key)){
            return !val.equals(request.getParameter(key));
        }
        return false;
    }
}
