package com.supcon.orchid.material.superwms.util.factories;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.material.entities.MaterialCropGather;
import com.supcon.orchid.fooramework.util.Entities;

import java.math.BigDecimal;

public class StockSummaryFactory {
    /**
     * 新建存量汇总
     * @param wareId 仓库ID
     * @param goodId 货物ID
     * @param quantity 货物数量
     * @param unit 单位
     * @return 现存量汇总实体
     */
    public static MaterialCropGather of(
            Long wareId,
            Long goodId,
            BigDecimal quantity,
            String unit
    ){
        MaterialCropGather  stockSummary = new MaterialCropGather();
        stockSummary.setWare(Entities.ofId(BaseSetWarehouse.class,wareId));
        stockSummary.setGood(Entities.ofId(BaseSetMaterial.class,goodId));
        stockSummary.setStandingcrop(quantity);
        stockSummary.setUnit(unit);
        return stockSummary;
    }
}
