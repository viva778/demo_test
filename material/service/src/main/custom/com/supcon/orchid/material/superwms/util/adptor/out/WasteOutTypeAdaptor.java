package com.supcon.orchid.material.superwms.util.adptor.out;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.material.entities.MaterialWasteOutDetail;
import com.supcon.orchid.material.entities.MaterialWasteOutSingle;
import com.supcon.orchid.material.superwms.util.adptor.OutboundTypeAdaptor;
import com.supcon.orchid.orm.entities.CodeEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

public class WasteOutTypeAdaptor implements OutboundTypeAdaptor<MaterialWasteOutSingle, MaterialWasteOutDetail> {


    @Override
    public String getRedBlue(MaterialWasteOutSingle table) {
        return Optional.ofNullable(table.getRedBlue()).map(CodeEntity::getId).orElse(null);
    }

    @Override
    public Date getOutDate(MaterialWasteOutSingle table) {
        return table.getApplyDate();
    }

    @Override
    public Staff getStaff(MaterialWasteOutSingle table) {
        return table.getApplyPerson();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialWasteOutSingle table) {
        return table.getReason();
    }


    @Override
    public BaseSetWarehouse getWarehouse(MaterialWasteOutSingle table) {
        return table.getWare();
    }

    @Override
    public boolean $isGenTask(MaterialWasteOutDetail detail) {
        return false;
    }

    @Override
    public BigDecimal $getStoreQuantity(MaterialWasteOutDetail detail) {
        return detail.getOutNumber();
    }

    @Override
    public BigDecimal $getOutQuantity(MaterialWasteOutDetail detail) {
        return detail.getOutNumber();
    }

    @Override
    public void $setOutQuantity(MaterialWasteOutDetail detail, BigDecimal quantity) {
        detail.setOutNumber(quantity);
    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialWasteOutDetail detail) {
        return detail.getApplyNumber();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialWasteOutDetail detail) {
        return null;
    }

    @Override
    public BigDecimal $getBill(MaterialWasteOutDetail detail) {
        return null;
    }

    @Override
    public Long $getRedReferId(MaterialWasteOutDetail detail) {
        return null;
    }

    @Override
    public MaterialStandingcrop $getStock(MaterialWasteOutDetail detail) {
        return detail.getNowStock();
    }

    @Override
    public String $getMemoField(MaterialWasteOutDetail detail) {
        return detail.getNote();
    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialWasteOutDetail detail) {
        return detail.getWaste();
    }

    @Override
    public BaseSetCooperate $getCustomer(MaterialWasteOutDetail detail) {
        return null;
    }

    @Override
    public void $setRedQuantity(MaterialWasteOutDetail detail, BigDecimal quantity) {

    }

}
