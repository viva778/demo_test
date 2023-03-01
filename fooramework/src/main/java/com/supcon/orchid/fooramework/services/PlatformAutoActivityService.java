package com.supcon.orchid.fooramework.services;

import java.util.Map;

/**
 * 由于自由活动的报错问题、回滚问题（会在afterSubmit前触发
 * 所以只在自由活动处做标记，在afterSubmit后，触发之前标记的活动代码
 */
public interface PlatformAutoActivityService {

    /**
     * 增加活动
     * 后以信号形式触发
     * @param signal 信号
     * @param params 参数
     */
    void addActivity(String signal, Object... params);

    /**
     * 增加活动
     * 后以信号形式触发
     * @param signal 信号
     * @param params 参数
     */
    void addActivity(String signal, Map<String, Object> params);


}
