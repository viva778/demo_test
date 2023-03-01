package com.supcon.orchid.material.superwms.util.adptor.in;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.material.entities.MaterialWasteInDetail;
import com.supcon.orchid.material.entities.MaterialWasteInSingle;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseRedBlue;
import com.supcon.orchid.material.superwms.util.adptor.InboundTypeAdaptor;

import java.math.BigDecimal;
import java.util.Date;

public class WasteInTypeAdaptor implements InboundTypeAdaptor<MaterialWasteInSingle, MaterialWasteInDetail> {

    @Override
    public String getRedBlue(MaterialWasteInSingle table) {
        return BaseRedBlue.BLUE;
    }

    @Override
    public Date getStorageDate(MaterialWasteInSingle table) {
        return table.getInStorageDate();
    }

    @Override
    public BaseSetWarehouse getWarehouse(MaterialWasteInSingle table) {
        return table.getWarehourse();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialWasteInSingle table) {
        return table.getStorageType();
    }

    @Override
    public Staff getPerson(MaterialWasteInSingle table) {
        return table.getToStaff();
    }

    @Override
    public BaseSetCooperate getVendor(MaterialWasteInSingle table) {
        return null;
    }

    @Override
    public boolean $isGenTask(MaterialWasteInDetail detail) {
        return false;
    }

    @Override
    public boolean $isGenPrintInfo(MaterialWasteInDetail detail) {
        return false;
    }

    @Override
    public SystemCode $getCheckResult(MaterialWasteInDetail detail) {
        return null;
    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialWasteInDetail detail) {
        return detail.getGood();
    }

    @Override
    public BaseSetStoreSet $getPlace(MaterialWasteInDetail detail) {
        return detail.getPlaceSet();
    }

    @Override
    public BigDecimal $getInQuantity(MaterialWasteInDetail detail) {
        return detail.getInQuanlity();
    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialWasteInDetail detail){
        return detail.getApplyNum();
    }

    @Override
    public String $getBatchNum(MaterialWasteInDetail detail) {
        return $isEnableBatch(detail)?detail.getBatchText():null;
    }

    @Override
    public Date $getProduceDate(MaterialWasteInDetail detail) {
        return null;
    }

    @Override
    public Date $getPurchaseDate(MaterialWasteInDetail detail) {
        return null;
    }

    @Override
    public Long $getRedReferId(MaterialWasteInDetail detail) {
        return null;
    }

    @Override
    public BigDecimal $getBill(MaterialWasteInDetail detail) {
        return null;
    }

    @Override
    public Long $getReturnSrcId(MaterialWasteInDetail detail) {
        return null;
    }

    @Override
    public String $getOrderNo(MaterialWasteInDetail detail) {
        return null;
    }

    @Override
    public String $getMemoField(MaterialWasteInDetail detail) {
        return detail.getInMemo();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialWasteInDetail detail) {
        return null;
    }

    @Override
    public void $setRedQuantity(MaterialWasteInDetail detail, BigDecimal quantity) {
    }

    @Override
    public Long $getBatchInfoId(MaterialWasteInDetail detail) {
        return detail.getBatchInfoId();
    }

    /**
     * 设置批次id回填字段,用于记录业务明细关联的批次信息
     * @param detail 入库单明细
     * @param batchId 批次信息id
     * @modify
     *  1.新建 modify by yaoyao
     */
    @Override
    public void $setBatchInfoId(MaterialWasteInDetail detail, Long batchId) {
        detail.setBatchInfoId(batchId);
    }

    @Override
    public void $setInQuantity(MaterialWasteInDetail detail, BigDecimal quantity) {
        detail.setInQuanlity(quantity);
    }

}
