package com.supcon.orchid.material.superwms.util.adptor.out;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.material.entities.MaterialPlaceAjustInfo;
import com.supcon.orchid.material.entities.MaterialPlaceAjustPart;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.fooramework.annotation.staticautowired.StaticAutowired;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseRedBlue;
import com.supcon.orchid.material.superwms.services.ModelBizTypeService;
import com.supcon.orchid.material.superwms.util.adptor.OutboundTypeAdaptor;

import java.math.BigDecimal;
import java.util.Date;

public class PlaceAdjustOutTypeAdaptor implements OutboundTypeAdaptor<MaterialPlaceAjustInfo, MaterialPlaceAjustPart> {

    @StaticAutowired
    private static ModelBizTypeService bizTypeService;


    @Override
    public String getRedBlue(MaterialPlaceAjustInfo table) {
        return BaseRedBlue.BLUE;
    }

    @Override
    public Date getOutDate(MaterialPlaceAjustInfo table) {
        return table.getAjustTime();
    }

    @Override
    public Staff getStaff(MaterialPlaceAjustInfo table) {
        return table.getCreateStaff();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialPlaceAjustInfo table) {
        return bizTypeService.getStorageType("adjustOut");
    }

    @Override
    public BaseSetWarehouse getWarehouse(MaterialPlaceAjustInfo table) {
        return table.getWare();
    }

    @Override
    public boolean $isGenTask(MaterialPlaceAjustPart detail) {
        return false;
    }

    @Override
    public BigDecimal $getStoreQuantity(MaterialPlaceAjustPart detail) {
        return detail.getAjustAmount();
    }

    @Override
    public BigDecimal $getOutQuantity(MaterialPlaceAjustPart detail) {
        return detail.getAjustAmount();
    }

    @Override
    public void $setOutQuantity(MaterialPlaceAjustPart detail, BigDecimal quantity) {

    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialPlaceAjustPart detail) {
        return detail.getAjustAmount();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialPlaceAjustPart detail) {
        return null;
    }

    @Override
    public BigDecimal $getBill(MaterialPlaceAjustPart detail) {
        return null;
    }

    @Override
    public Long $getRedReferId(MaterialPlaceAjustPart detail) {
        return null;
    }

    @Override
    public MaterialStandingcrop $getStock(MaterialPlaceAjustPart detail) {
        return detail.getOnhand();
    }

    @Override
    public String $getMemoField(MaterialPlaceAjustPart detail) {
        return detail.getRemark();
    }

    @Override
    public BaseSetCooperate $getCustomer(MaterialPlaceAjustPart detail) {
        return null;
    }

    @Override
    public void $setRedQuantity(MaterialPlaceAjustPart detail, BigDecimal quantity) {

    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialPlaceAjustPart detail) {
        return detail.getGood();
    }
}
