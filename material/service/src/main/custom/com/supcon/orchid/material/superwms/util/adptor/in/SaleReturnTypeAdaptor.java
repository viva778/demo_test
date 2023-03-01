package com.supcon.orchid.material.superwms.util.adptor.in;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.MaterialSaleReturn;
import com.supcon.orchid.material.entities.MaterialSaleReturnGood;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseRedBlue;
import com.supcon.orchid.material.superwms.util.adptor.InboundTypeAdaptor;

import java.math.BigDecimal;
import java.util.Date;

public class SaleReturnTypeAdaptor implements InboundTypeAdaptor<MaterialSaleReturn, MaterialSaleReturnGood> {

    @Override
    public String getRedBlue(MaterialSaleReturn table) {
        return BaseRedBlue.BLUE;
    }

    @Override
    public Date getStorageDate(MaterialSaleReturn table) {
        return table.getReturnDate();
    }

    @Override
    public BaseSetWarehouse getWarehouse(MaterialSaleReturn table) {
        return table.getWare();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialSaleReturn table) {
        return table.getStorageType();
    }

    @Override
    public Staff getPerson(MaterialSaleReturn table) {
        return table.getReturnStaff();
    }

    @Override
    public BaseSetCooperate getVendor(MaterialSaleReturn table) {
        return null;//todo 移动到表体
        //return table.getCustomer();
    }

    @Override
    public boolean $isGenTask(MaterialSaleReturnGood detail) {
        return true;
    }

    @Override
    public boolean $isGenPrintInfo(MaterialSaleReturnGood detail) {
        return false;
    }

    @Override
    public SystemCode $getCheckResult(MaterialSaleReturnGood detail) {
        return null;
    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialSaleReturnGood detail) {
        return detail.getGood();
    }

    @Override
    public BaseSetStoreSet $getPlace(MaterialSaleReturnGood detail) {
        return detail.getPlaceSet();
    }

    @Override
    public BigDecimal $getInQuantity(MaterialSaleReturnGood detail) {
        return detail.getReturnNum();
    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialSaleReturnGood detail){
        return detail.getApplyNum();
    }

    @Override
    public String $getBatchNum(MaterialSaleReturnGood detail) {
        return $isEnableBatch(detail)?detail.getBatchText():null;
    }

    @Override
    public Date $getProduceDate(MaterialSaleReturnGood detail) {
        return null;
    }

    @Override
    public Date $getPurchaseDate(MaterialSaleReturnGood detail) {
        return null;
    }

    @Override
    public Long $getRedReferId(MaterialSaleReturnGood detail) {
        return null;
    }

    @Override
    public BigDecimal $getBill(MaterialSaleReturnGood detail) {
        return null;
    }

    @Override
    public Long $getReturnSrcId(MaterialSaleReturnGood detail) {
        return null;
    }

    @Override
    public String $getOrderNo(MaterialSaleReturnGood detail) {
        return null;
    }

    @Override
    public String $getMemoField(MaterialSaleReturnGood detail) {
        return detail.getReturnMemo();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialSaleReturnGood detail) {
        return null;
    }

    @Override
    public void $setRedQuantity(MaterialSaleReturnGood detail, BigDecimal quantity) {

    }

    @Override
    public Long $getBatchInfoId(MaterialSaleReturnGood detail) {
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
    public void $setBatchInfoId(MaterialSaleReturnGood detail, Long batchId) {
        detail.setBatchInfoId(batchId);
    }

    @Override
    public void $setInQuantity(MaterialSaleReturnGood detail, BigDecimal quantity) {
        detail.setReturnNum(quantity);
    }

}
