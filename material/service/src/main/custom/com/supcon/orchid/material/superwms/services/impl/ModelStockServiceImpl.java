package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.material.superwms.services.ModelStockService;
import com.supcon.orchid.material.superwms.util.MaterialExceptionThrower;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 现存量服务
 */
@Service
public class ModelStockServiceImpl implements ModelStockService {

    /**
     * 现存量增长/减少
     *
     * @param stock          现存量
     * @param offsetQuantity 增幅(负数则减少)
     */
    @Override
    public void quantityOffset(MaterialStandingcrop stock, BigDecimal offsetQuantity) {
        //如果之前存在现存量，则更新现存量
        BigDecimal availQuantity = stock.getAvailiQuantity() != null ?
                stock.getAvailiQuantity() : BigDecimal.ZERO;
        BigDecimal stockQuantity = stock.getOnhand() != null ?
                stock.getOnhand() : BigDecimal.ZERO;
        BigDecimal updAvailQuantity = availQuantity.add(offsetQuantity);
        BigDecimal updStockQuantity = stockQuantity.add(offsetQuantity);
        if (updAvailQuantity.compareTo(BigDecimal.ZERO) >= 0 && updStockQuantity.compareTo(BigDecimal.ZERO) >= 0) {
            stock.setAvailiQuantity(updAvailQuantity);
            stock.setOnhand(updStockQuantity);
        } else {
            //现存量不足
            MaterialExceptionThrower.stock_insufficient();
        }
    }

    @Transactional
    @Override
    public MaterialStandingcrop get(Long materialId, String batchNum) {
        return Dbs.load(
                MaterialStandingcrop.class,
                "VALID=1 AND ONHAND>0 AND GOOD=? AND BATCH_TEXT=?",
                materialId, batchNum
        );
    }


    @Transactional
    @Override
    public MaterialStandingcrop get(Long materialId, Long batchId, Long warehouseId, Long placeId) {
        List<String> conditions = new LinkedList<>();
        if (warehouseId != null) {
            conditions.add("WARE=?");
        }
        if (materialId != null) {
            conditions.add("GOOD=?");
        }
        if (batchId != null) {
            conditions.add("MATER_BATCH_INFO=?");
        } else {
            conditions.add("MATER_BATCH_INFO IS NULL");
        }
        if (placeId != null) {
            conditions.add("PLACE_SET=?");
        } else {
            conditions.add("PLACE_SET IS NULL");
        }
        return Dbs.load(
                MaterialStandingcrop.class,
                "VALID=1 AND ONHAND>0 AND " + String.join(" AND ", conditions),
                Stream.of(
                        warehouseId,
                        materialId,
                        batchId,
                        placeId
                ).filter(Objects::nonNull).toArray()
        );
    }


    @Transactional
    @Override
    public MaterialStandingcrop get(Long materialId, String batchNum, Long warehouseId, Long placeId) {
        List<String> conditions = new LinkedList<>();
        if (warehouseId != null) {
            conditions.add("WARE=?");
        }
        if (materialId != null) {
            conditions.add("GOOD=?");
        }
        if (Strings.valid(batchNum)) {
            conditions.add("BATCH_TEXT=?");
        } else {
            conditions.add("BATCH_TEXT IS NULL");
            batchNum = null;
        }
        if (placeId != null) {
            conditions.add("PLACE_SET=?");
        } else {
            conditions.add("PLACE_SET IS NULL");
        }
        return Dbs.load(
                MaterialStandingcrop.class,
                "VALID=1 AND ONHAND>0 AND " + String.join(" AND ", conditions),
                Stream.of(
                        warehouseId,
                        materialId,
                        batchNum,
                        placeId
                ).filter(Objects::nonNull).toArray()
        );
    }

    @Transactional
    @Override
    public Pair<BigDecimal, BigDecimal> getQuantityTotal(Long warehouseId, Long materialId) {
        Pair<BigDecimal, BigDecimal> sPair = Dbs.pair(
                "SELECT SUM(ONHAND) s,SUM(AVAILI_QUANTITY) a FROM " + MaterialStandingcrop.TABLE_NAME + " WHERE VALID=1 AND ONHAND>0 AND WARE=? AND GOOD=?",
                BigDecimal.class, BigDecimal.class,
                warehouseId, materialId
        );
        if (sPair == null) {
            return Pair.of(BigDecimal.ZERO, BigDecimal.ZERO);
        } else {
            return Pair.of(
                    Optional.ofNullable(sPair.getFirst()).orElse(BigDecimal.ZERO),
                    Optional.ofNullable(sPair.getSecond()).orElse(BigDecimal.ZERO)
            );
        }
    }

}
