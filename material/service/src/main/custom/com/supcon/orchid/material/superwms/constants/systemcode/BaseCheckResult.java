package com.supcon.orchid.material.superwms.constants.systemcode;

import com.supcon.orchid.foundation.entities.SystemCode;

public class BaseCheckResult {
    /**
     * 合格
     */
    public static final String QUALIFIED = "BaseSet_checkResult/qualified";

    public static boolean isQualified(SystemCode systemCode){
        return systemCode!=null&&QUALIFIED.equals(systemCode.getId());
    }
}
