package com.supcon.orchid.material.superwms.util.adptor.in;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.staticautowired.StaticAutowired;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.material.superwms.services.ModelBizTypeService;
import com.supcon.orchid.material.superwms.util.adptor.InboundTypeAdaptor;
import com.supcon.orchid.orm.entities.CodeEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

public class AllocationInTypeAdaptor implements InboundTypeAdaptor<MaterialAppropriation, MaterialAppDetail> {

    @StaticAutowired
    private static ModelBizTypeService bizTypeService;

    @Override
    public String getRedBlue(MaterialAppropriation table) {
        return Optional.ofNullable(table.getRedBlue()).map(CodeEntity::getId).orElse(null);
    }

    @Override
    public Date getStorageDate(MaterialAppropriation table) {
        return new Date();
    }

    @Override
    public BaseSetWarehouse getWarehouse(MaterialAppropriation table) {
        return table.getToWare();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialAppropriation table) {
        return bizTypeService.getStorageType("allocateReasonIn");
    }

    @Override
    public Staff getPerson(MaterialAppropriation table) {
        return table.getCreateStaff();
    }

    @Override
    public BaseSetCooperate getVendor(MaterialAppropriation table) {
        return null;
    }

    @Override
    public boolean $isGenTask(MaterialAppDetail detail) {
        return true;
    }

    @Override
    public boolean $isGenPrintInfo(MaterialAppDetail detail) {
        return false;
    }

    @Override
    public SystemCode $getCheckResult(MaterialAppDetail detail) {
        return null;
    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialAppDetail detail) {
        return detail.getProductId();
    }

    @Override
    public BaseSetStoreSet $getPlace(MaterialAppDetail detail) {
        return detail.getInPlaceSet();
    }

    @Override
    public BigDecimal $getInQuantity(MaterialAppDetail detail) {
        return detail.getInQuantity();
    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialAppDetail detail){
        return detail.getAppliQuantity();
    }

    @Override
    public String $getBatchNum(MaterialAppDetail detail) {
        return Dbs.getProp(detail.getOnhand(),MaterialStandingcrop::getBatchText);
    }

    @Override
    public Date $getProduceDate(MaterialAppDetail detail) {
        return null;
    }

    @Override
    public Date $getPurchaseDate(MaterialAppDetail detail) {
        return null;
    }

    @Override
    public Long $getRedReferId(MaterialAppDetail detail) {
        return detail.getRedPartID();
    }

    @Override
    public BigDecimal $getBill(MaterialAppDetail detail) {
        return null;
    }

    @Override
    public Long $getReturnSrcId(MaterialAppDetail detail) {
        return null;
    }

    @Override
    public String $getOrderNo(MaterialAppDetail detail) {
        return null;
    }

    @Override
    public String $getMemoField(MaterialAppDetail detail) {
        return detail.getDetailMemo();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialAppDetail detail) {
        return detail.getRedNum();
    }

    @Override
    public void $setRedQuantity(MaterialAppDetail detail, BigDecimal quantity) {
        detail.setRedNum(quantity);
    }

    @Override
    public Long $getBatchInfoId(MaterialAppDetail detail) {
        return null;
    }

    /**
     * 设置批次id回填字段,用于记录业务明细关联的批次信息
     * @param detail 入库单明细
     * @param batchId 批次信息id
     * @modify
     *  1.新建 modify by yaoyao
     */
    @Override
    public void $setBatchInfoId(MaterialAppDetail detail, Long batchId) {  }

    @Override
    public void $setInQuantity(MaterialAppDetail detail, BigDecimal quantity){
        detail.setInQuantity(quantity);
    }


}
