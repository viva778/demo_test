package com.supcon.orchid.material.superwms.util.adptor.in;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.MaterialProdReturn;
import com.supcon.orchid.material.entities.MaterialProdReturnDeta;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseRedBlue;
import com.supcon.orchid.material.superwms.util.adptor.InboundTypeAdaptor;

import java.math.BigDecimal;
import java.util.Date;

public class ProduceReturnTypeAdaptor implements InboundTypeAdaptor<MaterialProdReturn, MaterialProdReturnDeta> {

    @Override
    public String getRedBlue(MaterialProdReturn table) {
        return BaseRedBlue.BLUE;
    }

    @Override
    public Date getStorageDate(MaterialProdReturn table) {
        return table.getReturnDate();
    }

    @Override
    public BaseSetWarehouse getWarehouse(MaterialProdReturn table) {
        return table.getWarehouse();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialProdReturn table) {
        return table.getStorageType();
    }

    @Override
    public Staff getPerson(MaterialProdReturn table) {
        return table.getReturnStaff();
    }

    @Override
    public BaseSetCooperate getVendor(MaterialProdReturn table) {
        return null;
    }

    @Override
    public boolean $isGenTask(MaterialProdReturnDeta detail) {
        return true;
    }

    @Override
    public boolean $isGenPrintInfo(MaterialProdReturnDeta detail) {
        return false;
    }

    @Override
    public SystemCode $getCheckResult(MaterialProdReturnDeta detail) {
        return null;
    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialProdReturnDeta detail) {
        return detail.getGood();
    }

    @Override
    public BaseSetStoreSet $getPlace(MaterialProdReturnDeta detail) {
        return detail.getPlaceSet();
    }

    @Override
    public BigDecimal $getInQuantity(MaterialProdReturnDeta detail) {
        return detail.getReturnNum();
    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialProdReturnDeta detail) {
        return detail.getApplyNum();
    }

    @Override
    public String $getBatchNum(MaterialProdReturnDeta detail) {
        return $isEnableBatch(detail)?detail.getBatchText():null;
    }

    @Override
    public Date $getProduceDate(MaterialProdReturnDeta detail) {
        return detail.getProdOutDate();
    }

    @Override
    public Date $getPurchaseDate(MaterialProdReturnDeta detail) {
        return null;
    }

    @Override
    public Long $getRedReferId(MaterialProdReturnDeta detail) {
        return null;
    }

    @Override
    public BigDecimal $getBill(MaterialProdReturnDeta detail) {
        return null;
    }

    @Override
    public Long $getReturnSrcId(MaterialProdReturnDeta detail) {
        return null;
    }

    @Override
    public String $getOrderNo(MaterialProdReturnDeta detail) {
        return null;
    }

    @Override
    public String $getMemoField(MaterialProdReturnDeta detail) {
        return detail.getMemoField();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialProdReturnDeta detail) {
        return null;
    }

    @Override
    public void $setRedQuantity(MaterialProdReturnDeta detail, BigDecimal quantity) {

    }

    @Override
    public Long $getBatchInfoId(MaterialProdReturnDeta detail) {
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
    public void $setBatchInfoId(MaterialProdReturnDeta detail, Long batchId) {
        detail.setBatchInfoId(batchId);
    }

    @Override
    public void $setInQuantity(MaterialProdReturnDeta detail, BigDecimal quantity) {
        detail.setReturnNum(quantity);
    }


}
