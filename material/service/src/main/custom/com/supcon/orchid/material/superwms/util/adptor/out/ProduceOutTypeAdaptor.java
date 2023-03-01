package com.supcon.orchid.material.superwms.util.adptor.out;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.material.entities.MaterialProduceOutDeta;
import com.supcon.orchid.material.entities.MaterialProduceOutSing;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.material.superwms.util.adptor.OutboundTypeAdaptor;
import com.supcon.orchid.orm.entities.CodeEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

public class ProduceOutTypeAdaptor implements OutboundTypeAdaptor<MaterialProduceOutSing, MaterialProduceOutDeta> {


    @Override
    public String getRedBlue(MaterialProduceOutSing table) {
        return Optional.ofNullable(table.getRedBlue()).map(CodeEntity::getId).orElse(null);
    }

    @Override
    public Date getOutDate(MaterialProduceOutSing table) {
        return table.getOutStorageDate();
    }

    @Override
    public Staff getStaff(MaterialProduceOutSing table) {
        return table.getOutPerson();
    }

    @Override
    public MaterialStorageType getStorageType(MaterialProduceOutSing table) {
        return table.getOutCome();
    }


    @Override
    public BaseSetWarehouse getWarehouse(MaterialProduceOutSing table) {
        return table.getWare();
    }

    @Override
    public boolean $isGenTask(MaterialProduceOutDeta detail) {
        return true;
    }

    @Override
    public BigDecimal $getStoreQuantity(MaterialProduceOutDeta detail) {
        return detail.getOutQuantity();
    }

    @Override
    public BigDecimal $getOutQuantity(MaterialProduceOutDeta detail) {
        return detail.getOutQuantity();
    }

    @Override
    public void $setOutQuantity(MaterialProduceOutDeta detail, BigDecimal quantity) {
        detail.setOutQuantity(quantity);
    }

    @Override
    public BigDecimal $getApplyQuantity(MaterialProduceOutDeta detail) {
        return detail.getAppliQuanlity();
    }

    @Override
    public BigDecimal $getRedQuantity(MaterialProduceOutDeta detail) {
        return detail.getRedNum();
    }

    @Override
    public BigDecimal $getBill(MaterialProduceOutDeta detail) {
        return detail.getOutMoney();
    }

    @Override
    public Long $getRedReferId(MaterialProduceOutDeta detail) {
        return detail.getRedPartID();
    }

    @Override
    public MaterialStandingcrop $getStock(MaterialProduceOutDeta detail) {
        return detail.getOnhand();
    }

    @Override
    public String $getMemoField(MaterialProduceOutDeta detail) {
        return detail.getPartMemo();
    }

    @Override
    public BaseSetMaterial $getMaterial(MaterialProduceOutDeta detail) {
        return detail.getGood();
    }

    @Override
    public BaseSetCooperate $getCustomer(MaterialProduceOutDeta detail) {
        return null;
    }

    @Override
    public void $setRedQuantity(MaterialProduceOutDeta detail, BigDecimal quantity) {
        detail.setRedNum(quantity);
    }

}
