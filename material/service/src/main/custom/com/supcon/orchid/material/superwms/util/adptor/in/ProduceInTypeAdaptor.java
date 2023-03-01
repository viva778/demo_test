package com.supcon.orchid.material.superwms.util.adptor.in;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.MaterialProduceInDetai;
import com.supcon.orchid.material.entities.MaterialProduceInSingl;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.material.superwms.util.adptor.InboundTypeAdaptor;
import com.supcon.orchid.orm.entities.CodeEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

public class ProduceInTypeAdaptor implements InboundTypeAdaptor<MaterialProduceInSingl, MaterialProduceInDetai> {

    @Override
    public String getRedBlue(MaterialProduceInSingl table) {
        return Optional.ofNullable(table.getRedBlue()).map(CodeEntity::getId).orElse(null);
    }

    @Override
    public Date getStorageDate(MaterialProduceInSingl table) {
        return table.getInStorageDate();
    }

    @Override
    public BaseSetWarehouse getWarehouse(MaterialProduceInSingl table) {
        return table.getWare();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialProduceInSingl table) {
        return table.getInCome();
    }

    @Override
    public Staff getPerson(MaterialProduceInSingl table) {
        return table.getInPerson();
    }

    @Override
    public BaseSetCooperate getVendor(MaterialProduceInSingl table) {
        return null;
    }

    @Override
    public boolean $isGenTask(MaterialProduceInDetai detail) {
        return true;
    }

    @Override
    public boolean $isGenPrintInfo(MaterialProduceInDetai detail) {
        return Boolean.TRUE.equals(detail.getGenPrintInfo());
    }

    @Override
    public SystemCode $getCheckResult(MaterialProduceInDetai detail) {
        return detail.getCheckResult();
    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialProduceInDetai detail) {
        return detail.getGood();
    }

    @Override
    public BaseSetStoreSet $getPlace(MaterialProduceInDetai detail) {
        return detail.getPlaceSet();
    }

    @Override
    public BigDecimal $getInQuantity(MaterialProduceInDetai detail) {
        return detail.getInQuantity();
    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialProduceInDetai detail) {
        return detail.getAppliQuanlity();
    }

    @Override
    public String $getBatchNum(MaterialProduceInDetai detail) {
        return $isEnableBatch(detail)?detail.getBatchText():null;
    }

    @Override
    public Date $getProduceDate(MaterialProduceInDetai detail) {
        return detail.getProductionDate();
    }

    @Override
    public Date $getPurchaseDate(MaterialProduceInDetai detail) {
        return detail.getProductionDate();
    }

    @Override
    public Long $getRedReferId(MaterialProduceInDetai detail) {
        return detail.getRedPartID();
    }

    @Override
    public BigDecimal $getBill(MaterialProduceInDetai detail) {
        return detail.getInMoney();
    }

    @Override
    public Long $getReturnSrcId(MaterialProduceInDetai detail) {
        return null;
    }

    @Override
    public String $getOrderNo(MaterialProduceInDetai detail) {
        return null;
    }

    @Override
    public String $getMemoField(MaterialProduceInDetai detail) {
        return detail.getInMemo();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialProduceInDetai detail) {
        return detail.getRedNumber();
    }

    @Override
    public void $setRedQuantity(MaterialProduceInDetai detail, BigDecimal quantity) {
        detail.setRedNumber(quantity);
    }

    @Override
    public Long $getBatchInfoId(MaterialProduceInDetai detail) {
        return detail.getBatchinfoId();
    }

    /**
     * 设置批次id回填字段,用于记录业务明细关联的批次信息
     * @param detail 入库单明细
     * @param batchId 批次信息id
     * @modify
     *  1.新建 modify by yaoyao
     */
    @Override
    public void $setBatchInfoId(MaterialProduceInDetai detail, Long batchId) { detail.setBatchinfoId(batchId); }

    @Override
    public void $setInQuantity(MaterialProduceInDetai detail, BigDecimal quantity) {
        detail.setInQuantity(quantity);
    }


}
