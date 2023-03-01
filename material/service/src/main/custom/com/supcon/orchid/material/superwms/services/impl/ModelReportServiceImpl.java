package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.material.entities.MaterialBusinessDetail;
import com.supcon.orchid.material.entities.MaterialDaySettlement;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.fooramework.support.Converter;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.Dates;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Entities;
import com.supcon.orchid.fooramework.util.Hbs;
import com.supcon.orchid.material.services.MaterialDaySettlementService;
import com.supcon.orchid.material.superwms.constants.systemcode.OperateDirection;
import com.supcon.orchid.material.superwms.services.ModelReportService;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ModelReportServiceImpl implements ModelReportService {

    @Autowired
    private MaterialDaySettlementService daySettlementService;

    @Override
    @Transactional
    public void doStockReport( Pair<Date, Date> dayRange , Date today) {
//        Date today = new Date();
//        Pair<Date, Date> dayRange = Dates.getDayRange(today);
        //查询当日流水
//        List<MaterialBusinessDetail> bizDetails = Hbs.findByCriteriaWithIncludes(
//                MaterialBusinessDetail.class,
//                "direction.id,good.id,placeSet.id,ware.id,quantity",
//                Restrictions.between("trasactionDate",dayRange.getFirst(),dayRange.getSecond())
//        );
        List<MaterialBusinessDetail> bizDetails = Dbs.findByCondition(MaterialBusinessDetail.class, "VALID = 1 AND TRASACTION_DATE BETWEEN ? AND ?", dayRange.getFirst(), dayRange.getSecond());
        //---------------------------------------------------仓库级别日结算---------------------------------------------------
        //创建<仓库:物料>-流水映射
        Map<Pair<Long, Long>, List<MaterialBusinessDetail>> wm_bds = bizDetails.stream().filter(Objects::nonNull).collect(Collectors.groupingBy(
                val -> Pair.of(val.getWare().getId(), val.getGood().getId())
        ));
        //查询仓库级别库存
        Dbs.stream(
                "SELECT SUM(ONHAND),WARE,GOOD FROM " + MaterialStandingcrop.TABLE_NAME + " WHERE VALID=1 GROUP BY WARE,GOOD",
                Object[].class
        ).forEach(val -> {
            BigDecimal total = Converter.decimalConverter(val[0]);
            if (total != null && total.compareTo(BigDecimal.ZERO) != 0) {
                Long warehouseId = Converter.longConverter(val[1]);
                Long materialId = Converter.longConverter(val[2]);
                MaterialDaySettlement warehouseReport = new MaterialDaySettlement();
                warehouseReport.setWare(Entities.ofId(BaseSetWarehouse.class, warehouseId));
                warehouseReport.setGood(Entities.ofId(BaseSetMaterial.class, materialId));
                warehouseReport.setSettlementDate(today);
                warehouseReport.setDaySettlement(total);
                //过滤仓库物料流水
                List<MaterialBusinessDetail> bds = wm_bds.get(Pair.of(warehouseId, materialId));
                if (bds != null && !bds.isEmpty()) {
                    BigDecimal inTotal = bds.stream()
                            .filter(bd -> bd.getDirection() != null && OperateDirection.RECEIVE.equals(bd.getDirection().getId()))
                            .map(MaterialBusinessDetail::getQuantity)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal outTotal = bds.stream()
                            .filter(bd -> bd.getDirection() != null && OperateDirection.SEND.equals(bd.getDirection().getId()))
                            .map(MaterialBusinessDetail::getQuantity)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    warehouseReport.setTotalIn(inTotal);
                    warehouseReport.setTotalOut(outTotal);
                } else {
                    warehouseReport.setTotalIn(BigDecimal.ZERO);
                    warehouseReport.setTotalOut(BigDecimal.ZERO);
                }
                daySettlementService.saveDaySettlement(warehouseReport, null);
            }
        });
        //---------------------------------------------------货位级别日结算---------------------------------------------------
        //创建<货位:物料>-流水映射
        Map<Pair<Long, Long>, List<MaterialBusinessDetail>> pm_bds = bizDetails.stream().filter(val -> val.getPlaceSet() != null).collect(Collectors.groupingBy(
                val -> Pair.of(val.getPlaceSet().getId(), val.getGood().getId())
        ));
        //查询货位级别库存
        Dbs.stream(
                "SELECT SUM(ONHAND),WARE,PLACE_SET,GOOD FROM " + MaterialStandingcrop.TABLE_NAME + " WHERE VALID=1 AND PLACE_SET IS NOT NULL GROUP BY WARE,PLACE_SET,GOOD",
                Object[].class
        ).forEach(val -> {
            BigDecimal total = Converter.decimalConverter(val[0]);
            if (total != null && total.compareTo(BigDecimal.ZERO) != 0) {
                Long warehouseId = Converter.longConverter(val[1]);
                Long placeId = Converter.longConverter(val[2]);
                Long materialId = Converter.longConverter(val[3]);
                MaterialDaySettlement placeReport = new MaterialDaySettlement();
                placeReport.setWare(Entities.ofId(BaseSetWarehouse.class, warehouseId));
                placeReport.setPlaceSet(Entities.ofId(BaseSetStoreSet.class, placeId));
                placeReport.setGood(Entities.ofId(BaseSetMaterial.class, materialId));
                placeReport.setSettlementDate(today);
                placeReport.setDaySettlement(total);
                //过滤货位物料流水
                List<MaterialBusinessDetail> bds = pm_bds.get(Pair.of(placeId, materialId));
                if (bds != null && !bds.isEmpty()) {
                    BigDecimal inTotal = bds.stream()
                            .filter(bd -> bd.getDirection() != null && OperateDirection.RECEIVE.equals(bd.getDirection().getId()))
                            .map(MaterialBusinessDetail::getQuantity)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal outTotal = bds.stream()
                            .filter(bd -> bd.getDirection() != null && OperateDirection.SEND.equals(bd.getDirection().getId()))
                            .map(MaterialBusinessDetail::getQuantity)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    placeReport.setTotalIn(inTotal);
                    placeReport.setTotalOut(outTotal);
                } else {
                    placeReport.setTotalIn(BigDecimal.ZERO);
                    placeReport.setTotalOut(BigDecimal.ZERO);
                }
                daySettlementService.saveDaySettlement(placeReport, null);
            }
        });
    }
}
