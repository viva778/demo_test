package com.supcon.orchid.material.superwms.services.impl;

/**
 * 由于自由活动的报错问题、回滚问题（会在afterSubmit前触发
 * 所以只在自由活动处做标记，在afterSubmit后，触发之前标记的活动代码
 */
@Deprecated
public class PlatformAutoActivityServiceImpl{

}
