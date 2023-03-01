package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.fooramework.support.Pair;

import java.math.BigDecimal;

/**
 * 现存量服务
 */
public interface ModelStockService {

    /**
     * 现存量增长/减少
     * @param stock 现存量
     * @param offsetQuantity 增幅(负数则减少)
     */
    void quantityOffset(MaterialStandingcrop stock, BigDecimal offsetQuantity);

    MaterialStandingcrop get(Long materialId,String batchNum);

    MaterialStandingcrop get(Long goodId, Long batchId, Long warehouseId, Long placeId);

    MaterialStandingcrop get(Long materialId, String batchNum, Long warehouseId, Long placeId);

    /**
     * 获取仓库下物料库存
     * @param warehouseId 仓库ID
     * @param materialId 物料ID
     * @return Pair first:现存量 second:可用量
     */
    Pair<BigDecimal,BigDecimal> getQuantityTotal(Long warehouseId, Long materialId);
}
