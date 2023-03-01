package com.supcon.orchid.material.superwms.util.adptor.out;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.material.entities.MaterialAppDetail;
import com.supcon.orchid.material.entities.MaterialAppropriation;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.fooramework.annotation.staticautowired.StaticAutowired;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.material.superwms.services.ModelBizTypeService;
import com.supcon.orchid.material.superwms.util.adptor.OutboundTypeAdaptor;
import com.supcon.orchid.orm.entities.CodeEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

public class AllocationOutTypeAdaptor implements OutboundTypeAdaptor<MaterialAppropriation, MaterialAppDetail> {

    @StaticAutowired
    private static ModelBizTypeService bizTypeService;

    @Override
    public String getRedBlue(MaterialAppropriation table) {
        return Optional.ofNullable(table.getRedBlue()).map(CodeEntity::getId).orElse(null);
    }

    @Override
    public Date getOutDate(MaterialAppropriation table) {
        return new Date();
    }

    @Override
    public Staff getStaff(MaterialAppropriation table) {
        return table.getCreateStaff();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialAppropriation table) {
        return bizTypeService.getStorageType("allocateReasonOut");
    }


    @Override
    public BaseSetWarehouse getWarehouse(MaterialAppropriation table) {
        return table.getFromWare();
    }

    @Override
    public boolean $isGenTask(MaterialAppDetail detail) {
        return true;
    }

    @Override
    public BigDecimal $getStoreQuantity(MaterialAppDetail detail) {
        return detail.getOutQuantity();
    }

    @Override
    public BigDecimal $getOutQuantity(MaterialAppDetail detail) {
        return detail.getOutQuantity();
    }

    @Override
    public void $setOutQuantity(MaterialAppDetail detail, BigDecimal quantity) {
        detail.setOutQuantity(quantity);
    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialAppDetail detail) {
        return detail.getAppliQuantity();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialAppDetail detail) {
        return detail.getRedNum();
    }

    @Override
    public BigDecimal $getBill(MaterialAppDetail detail) {
        return null;
    }

    @Override
    public Long $getRedReferId(MaterialAppDetail detail) {
        return detail.getRedPartID();
    }

    @Override
    public MaterialStandingcrop $getStock(MaterialAppDetail detail) {
        return detail.getOnhand();
    }

    @Override
    public String $getMemoField(MaterialAppDetail detail) {
        return detail.getDetailMemo();
    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialAppDetail detail) {
        return detail.getProductId();
    }

    @Override
    public BaseSetCooperate $getCustomer(MaterialAppDetail detail) {
        return null;
    }

    @Override
    public void $setRedQuantity(MaterialAppDetail detail, BigDecimal quantity) {
        detail.setRedNum(quantity);
    }

}
