package com.supcon.orchid.material.superwms.constants.systemcode;

import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.orm.entities.CodeEntity;

import java.util.Optional;

public class BaseBatchType {
    /**
     * 不启用
     */
    public static final String DISABLE = "BaseSet_isBatch/nobatch";

    /**
     * 按批
     */
    public static final String BY_BATCH = "BaseSet_isBatch/batch";

    /**
     * 按次
     */
    public static final String BY_PIECE = "BaseSet_isBatch/piece";

    public static boolean isEnable(String sys_code){
        return sys_code!=null&&!DISABLE.equals(sys_code);
    }

    public static boolean isEnable(SystemCode sys_code){
        return isEnable(Optional.ofNullable(sys_code).map(CodeEntity::getId).orElse(null));
    }
}
