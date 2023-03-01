package com.supcon.orchid.material.superwms.util.adptor;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public abstract class InTypeContext {
    /**
     * 批次重复校验
     */
    private boolean batchConflictCheck;

    /**
     * 是否需要质检
     */
    private boolean needQualityCheck;

    public abstract String getDescription();
}
