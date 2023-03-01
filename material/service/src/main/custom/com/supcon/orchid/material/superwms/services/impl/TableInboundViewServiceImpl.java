package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.material.entities.MaterialEirNumber;
import com.supcon.orchid.material.entities.MaterialEirNumberList;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.entities.MaterialWasteInDetail;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.Dates;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Elements;
import com.supcon.orchid.fooramework.util.Entities;
import com.supcon.orchid.material.superwms.services.TableInboundViewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TableInboundViewServiceImpl implements TableInboundViewService {

    @Transactional
    @Override
    public List<MaterialWasteInDetail> wasteCreateDetailsByWarehouseAndMaterials(Long warehouseId, List<Long> materialIds){
        //查找今年的环评信息
        Pair<Date,Date> yearRange = Dates.getYearRange();
        Map<Long, MaterialEirNumber> material$eirNumber = Dbs.findByCondition(
                MaterialEirNumber.class,
                "VALID=1 AND EIR_NUMBER_LIST IN(SELECT ID FROM "+ MaterialEirNumberList.TABLE_NAME+" WHERE EIA_YEAR BETWEEN ? AND ?) AND "+Dbs.inCondition("WASTE_GOOD",materialIds.size()),
                Elements.toArray(yearRange.getFirst(),yearRange.getSecond(),materialIds)
        ).stream().collect(Collectors.toMap(
                en->en.getWasteGood().getId(),
                en->en
        ));
        //查找仓库现存量汇总信息
        Map<Long,BigDecimal> material$stock = warehouseId!=null?Dbs.binaryMap(
                "SELECT GOOD,SUM(ONHAND) FROM "+ MaterialStandingcrop.TABLE_NAME+" WHERE WARE=? AND "+Dbs.inCondition("GOOD",materialIds.size())+" GROUP BY GOOD",
                Long.class, BigDecimal.class,
                Elements.toArray(warehouseId,materialIds)
        ): Collections.emptyMap();
        return materialIds.stream().map(materialId->{
            MaterialWasteInDetail detail = new MaterialWasteInDetail();
            detail.setGood(Entities.ofId(BaseSetMaterial.class,materialId));
            detail.setEiaNumber(material$eirNumber.get(materialId));
            detail.setNowQuanlity(material$stock.getOrDefault(materialId, BigDecimal.ZERO));
            return detail;
        }).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public List<MaterialWasteInDetail> wasteRefreshStockByWarehouse(Long warehouseId, List<Long> materialIds){
        //查找仓库现存量汇总信息（刷新表头仓库
        Map<Long,BigDecimal> material$stock = Dbs.binaryMap(
                "SELECT GOOD,SUM(ONHAND) FROM "+ MaterialStandingcrop.TABLE_NAME+" WHERE WARE=? AND "+Dbs.inCondition("GOOD",materialIds.size())+" GROUP BY GOOD",
                Long.class, BigDecimal.class,
                Elements.toArray(warehouseId,materialIds)
        );
        return materialIds.stream().map(materialId->{
            MaterialWasteInDetail detail = new MaterialWasteInDetail();
            detail.setGood(Entities.ofId(BaseSetMaterial.class,materialId));
            detail.setNowQuanlity(material$stock.getOrDefault(materialId, BigDecimal.ZERO));
            return detail;
        }).collect(Collectors.toList());
    }
}
