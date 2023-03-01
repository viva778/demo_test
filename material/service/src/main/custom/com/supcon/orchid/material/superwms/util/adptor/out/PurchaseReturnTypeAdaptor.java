package com.supcon.orchid.material.superwms.util.adptor.out;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.material.entities.MaterialPurReturn;
import com.supcon.orchid.material.entities.MaterialPurReturnPart;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseRedBlue;
import com.supcon.orchid.material.superwms.util.adptor.OutboundTypeAdaptor;

import java.math.BigDecimal;
import java.util.Date;

public class PurchaseReturnTypeAdaptor implements OutboundTypeAdaptor<MaterialPurReturn, MaterialPurReturnPart> {

    @Override
    public String getRedBlue(MaterialPurReturn table) {
        return BaseRedBlue.BLUE;
    }

    @Override
    public Date getOutDate(MaterialPurReturn table) {
        return table.getReturnTime();
    }

    @Override
    public Staff getStaff(MaterialPurReturn table) {
        return table.getReturnStaff();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialPurReturn table) {
        return table.getOutType();
    }

    @Override
    public BaseSetWarehouse getWarehouse(MaterialPurReturn table) {
        return table.getWarehouse();
    }

    @Override
    public boolean $isGenTask(MaterialPurReturnPart detail) {
        return true;
    }

    @Override
    public BigDecimal $getStoreQuantity(MaterialPurReturnPart detail) {
        return detail.getReturnNum();
    }

    @Override
    public BigDecimal $getOutQuantity(MaterialPurReturnPart detail) {
        return detail.getReturnNum();
    }

    @Override
    public void $setOutQuantity(MaterialPurReturnPart detail, BigDecimal quantity) {
        detail.setReturnNum(quantity);
    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialPurReturnPart detail) {
        return detail.getApplyNum();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialPurReturnPart detail) {
        return null;
    }

    @Override
    public BigDecimal $getBill(MaterialPurReturnPart detail) {
        return null;
    }

    @Override
    public Long $getRedReferId(MaterialPurReturnPart detail) {
        return null;
    }

    @Override
    public MaterialStandingcrop $getStock(MaterialPurReturnPart detail) {
        return detail.getStockOnHand();
    }

    @Override
    public String $getMemoField(MaterialPurReturnPart detail) {
        return detail.getReturnMemo();
    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialPurReturnPart detail) {
        return detail.getProduct();
    }

    @Override
    public BaseSetCooperate $getCustomer(MaterialPurReturnPart detail) {
        return null;
    }

    @Override
    public void $setRedQuantity(MaterialPurReturnPart detail, BigDecimal quantity) {

    }


}
