package com.supcon.orchid.material.superwms.util.adptor.in;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.MaterialPlaceAjustInfo;
import com.supcon.orchid.material.entities.MaterialPlaceAjustPart;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.fooramework.annotation.staticautowired.StaticAutowired;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseRedBlue;
import com.supcon.orchid.material.superwms.services.ModelBizTypeService;
import com.supcon.orchid.material.superwms.util.adptor.InboundTypeAdaptor;

import java.math.BigDecimal;
import java.util.Date;

public class PlaceAdjustInTypeAdaptor implements InboundTypeAdaptor<MaterialPlaceAjustInfo, MaterialPlaceAjustPart> {

    @StaticAutowired
    private static ModelBizTypeService bizTypeService;

    @Override
    public String getRedBlue(MaterialPlaceAjustInfo table) {
        return BaseRedBlue.BLUE;
    }

    @Override
    public Date getStorageDate(MaterialPlaceAjustInfo table) {
        return table.getAjustTime();
    }

    @Override
    public BaseSetWarehouse getWarehouse(MaterialPlaceAjustInfo table) {
        return table.getWare();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialPlaceAjustInfo table) {
        return bizTypeService.getStorageType("adjustIn");
    }

    @Override
    public Staff getPerson(MaterialPlaceAjustInfo table) {
        return table.getCreateStaff();
    }

    @Override
    public BaseSetCooperate getVendor(MaterialPlaceAjustInfo table) {
        return null;
    }

    @Override
    public boolean $isGenTask(MaterialPlaceAjustPart detail) {
        return false;
    }

    @Override
    public boolean $isGenPrintInfo(MaterialPlaceAjustPart detail) {
        return false;
    }

    @Override
    public SystemCode $getCheckResult(MaterialPlaceAjustPart detail) {
        return null;
    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialPlaceAjustPart detail) {
        return detail.getGood();
    }

    @Override
    public String $getBatchNum(MaterialPlaceAjustPart detail) {
        return Dbs.getProp(detail.getOnhand(), MaterialStandingcrop::getBatchText);
    }

    @Override
    public BaseSetStoreSet $getPlace(MaterialPlaceAjustPart detail) {
        return detail.getToPlaceSet();
    }

    @Override
    public BigDecimal $getInQuantity(MaterialPlaceAjustPart detail) {
        return detail.getAjustAmount();
    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialPlaceAjustPart detail) {
        return detail.getAjustAmount();
    }

    @Override
    public Date $getProduceDate(MaterialPlaceAjustPart detail) {
        return null;
    }

    @Override
    public Date $getPurchaseDate(MaterialPlaceAjustPart detail) {
        return null;
    }

    @Override
    public Long $getRedReferId(MaterialPlaceAjustPart detail) {
        return null;
    }

    @Override
    public BigDecimal $getBill(MaterialPlaceAjustPart detail) {
        return null;
    }

    @Override
    public Long $getReturnSrcId(MaterialPlaceAjustPart detail) {
        return null;
    }

    @Override
    public String $getOrderNo(MaterialPlaceAjustPart detail) {
        return null;
    }

    @Override
    public String $getMemoField(MaterialPlaceAjustPart detail) {
        return detail.getRemark();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialPlaceAjustPart detail) {
        return null;
    }

    @Override
    public void $setRedQuantity(MaterialPlaceAjustPart detail, BigDecimal quantity) {

    }

    @Override
    public Long $getBatchInfoId(MaterialPlaceAjustPart detail) {
        return null;
    }

    @Override
    public void $setBatchInfoId(MaterialPlaceAjustPart detail, Long batchId) {

    }

    @Override
    public void $setInQuantity(MaterialPlaceAjustPart detail, BigDecimal quantity) {

    }
}
