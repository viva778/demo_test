package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.material.entities.MaterialCropGather;

import java.math.BigDecimal;

/**
 * 存量汇总服务
 */
public interface ModelStockSummaryService {

    /**
     * 现存量增长/减少
     * @param stockSummary 存量汇总
     * @param offsetQuantity 增幅(负数则减少)
     */
    void quantityOffset(MaterialCropGather stockSummary, BigDecimal offsetQuantity);

    /**
     * 获取现存量汇总
     * @param wareId 仓库ID
     * @param goodId 物料ID
     * @return 现存量汇总
     */
    MaterialCropGather get(Long wareId, Long goodId);

}
