package com.supcon.orchid.material.superwms.util.adptor;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.fooramework.util.Dbs;

import java.math.BigDecimal;
import java.util.Date;

public interface OutboundTypeAdaptor<T extends AbstractEcFullEntity,D extends AbstractEcPartEntity> extends TableTypeAdaptor<T,D>{
    String getRedBlue(T table);

    Date getOutDate(T table);

    Staff getStaff(T table);

    MaterialStorageType getStorageType(T table);

    BaseSetWarehouse getWarehouse(T table);

    boolean $isGenTask(D detail);

    BigDecimal $getStoreQuantity(D detail);

    BigDecimal $getOutQuantity(D detail);

    void  $setOutQuantity(D detail, BigDecimal quantity);

    BigDecimal $getApplyQuantity(D detail);

    BigDecimal $getRedQuantity(D detail);

    BigDecimal $getBill(D detail);

    Long $getRedReferId(D detail);

    MaterialStandingcrop $getStock(D detail);

    String $getMemoField(D detail);

    default String $getBatchNum(D detail){
        return Dbs.getProp($getStock(detail),MaterialStandingcrop::getBatchText);
    }

    BaseSetCooperate $getCustomer(D detail);

    void $setRedQuantity(D detail, BigDecimal quantity);
}
