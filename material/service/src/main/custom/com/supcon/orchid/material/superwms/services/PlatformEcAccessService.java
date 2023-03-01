package com.supcon.orchid.material.superwms.services;

import java.util.Map;

public interface PlatformEcAccessService {

    String getModelCodeByDgCode(String dgCode);

    String getModelCodeByViewCode(String viewCode);

    /**
     * 获取页面多选字段信息
     * @param viewCode viewCode
     * @param methodName methodName
     * @return Map<propertyName,fieldCode>
     */
    Map<String,String> findMultiFieldsInfo(String viewCode, String methodName);

    String getDgIncludeWithFieldMapper(String dgCode, Map<String, String> mapper);
}
