package com.supcon.orchid.material.superwms.util.adptor.in;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.MaterialPurchInPart;
import com.supcon.orchid.material.entities.MaterialPurchInSingle;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.material.superwms.util.adptor.InboundTypeAdaptor;
import com.supcon.orchid.orm.entities.CodeEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

public class PurchaseInTypeAdaptor implements InboundTypeAdaptor<MaterialPurchInSingle, MaterialPurchInPart> {

    @Override
    public String getRedBlue(MaterialPurchInSingle table) {
        return Optional.ofNullable(table.getRedBlue()).map(CodeEntity::getId).orElse(null);
    }

    @Override
    public Date getStorageDate(MaterialPurchInSingle table) {
        return table.getInStorageDate();
    }

    @Override
    public BaseSetWarehouse getWarehouse(MaterialPurchInSingle table) {
        return table.getWareId();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialPurchInSingle table) {
        return table.getInCome();
    }

    @Override
    public Staff getPerson(MaterialPurchInSingle table) {
        return table.getInPerson();
    }

    @Override
    public BaseSetCooperate getVendor(MaterialPurchInSingle table) {
        return table.getVendor();
    }

    @Override
    public boolean $isGenTask(MaterialPurchInPart detail) {
        return true;
    }

    @Override
    public boolean $isGenPrintInfo(MaterialPurchInPart detail) {
        return false;
    }

    @Override
    public SystemCode $getCheckResult(MaterialPurchInPart detail) {
        return detail.getCheckResult();
    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialPurchInPart detail) {
        return detail.getGood();
    }

    @Override
    public BaseSetStoreSet $getPlace(MaterialPurchInPart detail) {
        return detail.getPlaceSet();
    }

    @Override
    public BigDecimal $getInQuantity(MaterialPurchInPart detail) {
        return detail.getInQuantity();
    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialPurchInPart detail) {
        return detail.getApplyQuantity();
    }

    @Override
    public String $getBatchNum(MaterialPurchInPart detail) {
        return $isEnableBatch(detail)?detail.getBatch():null;
    }

    @Override
    public Date $getProduceDate(MaterialPurchInPart detail) {
        return detail.getProductionDate();
    }

    @Override
    public Date $getPurchaseDate(MaterialPurchInPart detail) {
        return detail.getProductionDate();
    }

    @Override
    public Long $getRedReferId(MaterialPurchInPart detail) {
        return null;
    }

    @Override
    public BigDecimal $getBill(MaterialPurchInPart detail) {
        return detail.getInMoney();
    }

    @Override
    public Long $getReturnSrcId(MaterialPurchInPart detail) {
        return null;
    }

    @Override
    public String $getOrderNo(MaterialPurchInPart detail) {
        return detail.getPurOrderNo();
    }

    @Override
    public String $getMemoField(MaterialPurchInPart detail) {
        return detail.getOutMemo();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialPurchInPart detail) {
        return null;
    }

    @Override
    public void $setRedQuantity(MaterialPurchInPart detail, BigDecimal quantity) {

    }

    @Override
    public Long $getBatchInfoId(MaterialPurchInPart detail) {
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
    public void $setBatchInfoId(MaterialPurchInPart detail, Long batchId) { detail.setBatchinfoId(batchId); }

    @Override
    public void $setInQuantity(MaterialPurchInPart detail, BigDecimal quantity) {
        detail.setInQuantity(quantity);
    }


}
