package com.supcon.orchid.material.superwms.util.adptor.out;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.material.entities.MaterialOtherOutSingle;
import com.supcon.orchid.material.entities.MaterialOutSingleDetai;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.material.superwms.util.adptor.OutboundTypeAdaptor;
import com.supcon.orchid.orm.entities.CodeEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

public class OtherOutTypeAdaptor implements OutboundTypeAdaptor<MaterialOtherOutSingle, MaterialOutSingleDetai> {
    @Override
    public String getRedBlue(MaterialOtherOutSingle table) {
        return Optional.ofNullable(table.getRedBlue()).map(CodeEntity::getId).orElse(null);
    }

    @Override
    public Date getOutDate(MaterialOtherOutSingle table) {
        return table.getOutStorageDate();
    }

    @Override
    public Staff getStaff(MaterialOtherOutSingle table) {
        return table.getOutPerson();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialOtherOutSingle table) {
        return table.getOutCome();
    }


    @Override
    public BaseSetWarehouse getWarehouse(MaterialOtherOutSingle table) {
        return table.getWare();
    }

    @Override
    public boolean $isGenTask(MaterialOutSingleDetai detail) {
        return true;
    }

    @Override
    public BigDecimal $getStoreQuantity(MaterialOutSingleDetai detail) {
        return detail.getOutQuantity();
    }

    @Override
    public BigDecimal $getOutQuantity(MaterialOutSingleDetai detail) {
        return detail.getOutQuantity();
    }

    @Override
    public void $setOutQuantity(MaterialOutSingleDetai detail, BigDecimal quantity) {
        detail.setOutQuantity(quantity);
    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialOutSingleDetai detail) {
        return detail.getAppliQuantity();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialOutSingleDetai detail) {
        return detail.getRedNumber();
    }

    @Override
    public BigDecimal $getBill(MaterialOutSingleDetai detail) {
        return detail.getOutMoney();
    }

    @Override
    public Long $getRedReferId(MaterialOutSingleDetai detail) {
        return detail.getRedPartID();
    }

    @Override
    public MaterialStandingcrop $getStock(MaterialOutSingleDetai detail) {
        return detail.getOnhand();
    }

    @Override
    public String $getMemoField(MaterialOutSingleDetai detail) {
        return detail.getOutMemo();
    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialOutSingleDetai detail) {
        return detail.getGood();
    }

    @Override
    public BaseSetCooperate $getCustomer(MaterialOutSingleDetai detail) {
        return null;
    }

    @Override
    public void $setRedQuantity(MaterialOutSingleDetai detail, BigDecimal quantity) {
        detail.setRedNumber(quantity);
    }

}
