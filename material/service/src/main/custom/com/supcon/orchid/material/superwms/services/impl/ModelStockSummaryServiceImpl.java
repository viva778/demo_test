package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.material.entities.MaterialCropGather;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.material.superwms.services.ModelStockSummaryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 存量汇总服务
 */
@Service
public class ModelStockSummaryServiceImpl implements ModelStockSummaryService {

    /**
     * 现存量增长/减少
     * @param stockSummary 存量汇总
     * @param offsetQuantity 增幅(负数则减少)
     */
    public void quantityOffset(MaterialCropGather stockSummary, BigDecimal offsetQuantity) {
        BigDecimal stock = stockSummary.getStandingcrop();
        BigDecimal updStock = stock.add(offsetQuantity);
        if(updStock.compareTo(BigDecimal.ZERO)>=0){
            stockSummary.setStandingcrop(updStock);
        } else {
            stockSummary.setStandingcrop(BigDecimal.ZERO);
        }
    }

    @Transactional
    public MaterialCropGather get(Long wareId, Long goodId) {
        return Dbs.load(
                MaterialCropGather.class,
                "WARE=? AND GOOD=? AND VALID=1",
                wareId,goodId
        );
    }
}
