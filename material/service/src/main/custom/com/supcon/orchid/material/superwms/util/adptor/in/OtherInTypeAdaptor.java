package com.supcon.orchid.material.superwms.util.adptor.in;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.MaterialInSingleDetail;
import com.supcon.orchid.material.entities.MaterialOtherInSingle;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.material.superwms.util.adptor.InboundTypeAdaptor;
import com.supcon.orchid.orm.entities.CodeEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

public class OtherInTypeAdaptor implements InboundTypeAdaptor<MaterialOtherInSingle, MaterialInSingleDetail> {

    @Override
    public String getRedBlue(MaterialOtherInSingle table) {
        return Optional.ofNullable(table.getRedBlue()).map(CodeEntity::getId).orElse(null);
    }

    @Override
    public Date getStorageDate(MaterialOtherInSingle table) {
        return table.getInStorageDate();
    }

    @Override
    public BaseSetWarehouse getWarehouse(MaterialOtherInSingle table) {
        return table.getWare();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialOtherInSingle table) {
        return table.getInCome();
    }

    @Override
    public Staff getPerson(MaterialOtherInSingle table) {
        return table.getInPerson();
    }

    @Override
    public BaseSetCooperate getVendor(MaterialOtherInSingle table) {
        return null;
    }

    @Override
    public boolean $isGenTask(MaterialInSingleDetail detail) {
        return true;
    }

    @Override
    public boolean $isGenPrintInfo(MaterialInSingleDetail detail) {
        return Boolean.TRUE.equals(detail.getGenPrintInfo());
    }

    @Override
    public SystemCode $getCheckResult(MaterialInSingleDetail detail) {
        return detail.getCheckResult();
    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialInSingleDetail detail) {
        return detail.getGood();
    }

    @Override
    public BaseSetStoreSet $getPlace(MaterialInSingleDetail detail) {
        return detail.getPlaceSet();
    }

    @Override
    public BigDecimal $getInQuantity(MaterialInSingleDetail detail) {
        return detail.getInQuantity();
    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialInSingleDetail detail){
        return detail.getAppliQuanlity();
    }

    @Override
    public String $getBatchNum(MaterialInSingleDetail detail) {
        return $isEnableBatch(detail)?detail.getBatchText():null;
    }

    @Override
    public Date $getProduceDate(MaterialInSingleDetail detail) {
        return detail.getProductionDate();
    }

    @Override
    public Date $getPurchaseDate(MaterialInSingleDetail detail) {
        return detail.getProductionDate();
    }

    @Override
    public Long $getRedReferId(MaterialInSingleDetail detail) {
        return detail.getRedPartID();
    }

    @Override
    public BigDecimal $getBill(MaterialInSingleDetail detail) {
        return detail.getInMoney();
    }

    @Override
    public Long $getReturnSrcId(MaterialInSingleDetail detail) {
        return null;
    }

    @Override
    public String $getOrderNo(MaterialInSingleDetail detail) {
        return null;
    }

    @Override
    public String $getMemoField(MaterialInSingleDetail detail) {
        return detail.getInMemo();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialInSingleDetail detail) {
        return detail.getRedNumber();
    }

    @Override
    public void $setRedQuantity(MaterialInSingleDetail detail, BigDecimal quantity) {
        detail.setRedNumber(quantity);
    }

    @Override
    public Long $getBatchInfoId(MaterialInSingleDetail detail) {
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
    public void $setBatchInfoId(MaterialInSingleDetail detail, Long batchId) { detail.setBatchinfoId(batchId); }

    @Override
    public void $setInQuantity(MaterialInSingleDetail detail, BigDecimal quantity){
        detail.setInQuantity(quantity);
    }


}
