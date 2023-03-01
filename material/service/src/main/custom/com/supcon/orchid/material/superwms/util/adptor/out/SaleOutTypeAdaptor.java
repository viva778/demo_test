package com.supcon.orchid.material.superwms.util.adptor.out;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.material.entities.MaterialSaleOutDetail;
import com.supcon.orchid.material.entities.MaterialSaleOutSingle;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.material.superwms.util.adptor.OutboundTypeAdaptor;
import com.supcon.orchid.orm.entities.CodeEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

public class SaleOutTypeAdaptor implements OutboundTypeAdaptor<MaterialSaleOutSingle, MaterialSaleOutDetail> {


    @Override
    public String getRedBlue(MaterialSaleOutSingle table) {
        return Optional.ofNullable(table.getRedBlue()).map(CodeEntity::getId).orElse(null);
    }

    @Override
    public Date getOutDate(MaterialSaleOutSingle table) {
        return table.getOutStorageDate();
    }

    @Override
    public Staff getStaff(MaterialSaleOutSingle table) {
        return table.getOutPerson();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialSaleOutSingle table) {
        return table.getOutCome();
    }

    @Override
    public BaseSetWarehouse getWarehouse(MaterialSaleOutSingle table) {
        return table.getWare();
    }

    @Override
    public boolean $isGenTask(MaterialSaleOutDetail detail) {
        return true;
    }

    @Override
    public BigDecimal $getStoreQuantity(MaterialSaleOutDetail detail) {
        return detail.getOutQuantity();
    }

    @Override
    public BigDecimal $getOutQuantity(MaterialSaleOutDetail detail) {
        return detail.getOutQuantity();
    }

    @Override
    public void $setOutQuantity(MaterialSaleOutDetail detail, BigDecimal quantity) {
        detail.setOutQuantity(quantity);
    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialSaleOutDetail detail) {
        return detail.getAppliQuanlity();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialSaleOutDetail detail) {
        return detail.getRedNum();
    }

    @Override
    public BigDecimal $getBill(MaterialSaleOutDetail detail) {
        return detail.getOutMoney();
    }

    @Override
    public Long $getRedReferId(MaterialSaleOutDetail detail) {
        return detail.getRedPartID();
    }

    @Override
    public MaterialStandingcrop $getStock(MaterialSaleOutDetail detail) {
        return detail.getOnhand();
    }

    @Override
    public String $getMemoField(MaterialSaleOutDetail detail) {
        return detail.getOutMemo();
    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialSaleOutDetail detail) {
        return detail.getGood();
    }

    @Override
    public BaseSetCooperate $getCustomer(MaterialSaleOutDetail detail) {
        return detail.getCustomer();
    }

    @Override
    public void $setRedQuantity(MaterialSaleOutDetail detail, BigDecimal quantity) {
        detail.setRedNum(quantity);
    }

}
