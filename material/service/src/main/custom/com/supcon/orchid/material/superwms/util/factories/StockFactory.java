package com.supcon.orchid.material.superwms.util.factories;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.material.entities.MaterialBatchInfo;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseBatchType;
import com.supcon.orchid.fooramework.util.Entities;
import org.springframework.util.Assert;

import java.math.BigDecimal;


public class StockFactory {

    /**
     * 创建现存量信息
     * @param wareId 基础仓库Id
     * @param placeId 基础货位Id
     * @param good 货物(需要全部字段)
     * @param batchInfo 批次信息(需要全部字段)
     * @param quantity 数量
     * @param unitName 单位
     * @return 现存量
     */
    public static MaterialStandingcrop of(
            Long wareId,
            Long placeId,
            BaseSetMaterial good,
            MaterialBatchInfo batchInfo,
            BigDecimal quantity,
            String unitName
    ){
        MaterialStandingcrop stock = new MaterialStandingcrop();
        stock.setWare(Entities.ofId(BaseSetWarehouse.class,wareId));
        stock.setPlaceSet(placeId!=null?
                Entities.ofId(BaseSetStoreSet.class,placeId): null
        );
        stock.setGood(good);
        stock.setGoodCode(good.getCode());
        stock.setGoodName(good.getName());
        stock.setGoodUnit(good.getSaleUnit().getName());
        stock.setOnhand(quantity);
        stock.setAvailiQuantity(quantity);
        stock.setUnit(unitName);

        if (BaseBatchType.isEnable(good.getIsBatch())){
            Assert.notNull(batchInfo,"找不到批次信息，不能生成现存量！");//todo 国际化
            stock.setMaterBatchInfo(batchInfo);
            stock.setAvailableDate(batchInfo.getAvailableDate());
            stock.setBatchText(batchInfo.getBatchNum());
            stock.setApprochTime(batchInfo.getApprochTime());
            stock.setCheckDate(batchInfo.getCheckDate());
            stock.setCheckState(batchInfo.getCheckState());
            stock.setCheckResult(batchInfo.getCheckResult());
            stock.setInStoreTime(batchInfo.getInStoreDate());
            stock.setIsAvailable(batchInfo.getIsAvailable());
        } else {
            stock.setIsAvailable(true);
        }
        return stock;
    }
}
