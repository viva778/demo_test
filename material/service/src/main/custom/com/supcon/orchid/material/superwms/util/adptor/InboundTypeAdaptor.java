package com.supcon.orchid.material.superwms.util.adptor;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.MaterialStorageType;

import java.math.BigDecimal;
import java.util.Date;

public interface InboundTypeAdaptor<T extends AbstractEcFullEntity,D extends AbstractEcPartEntity> extends TableTypeAdaptor<T,D>{

    String getRedBlue(T table);

    Date getStorageDate(T table);

    BaseSetWarehouse getWarehouse(T table);

    MaterialStorageType getStorageType(T table);

    Staff getPerson(T table);

    BaseSetCooperate getVendor(T table);

    boolean $isGenTask(D detail);

    boolean $isGenPrintInfo(D detail);

    SystemCode $getCheckResult(D detail);

    BaseSetMaterial $getMaterial(D detail);

    BaseSetStoreSet $getPlace(D detail);

    BigDecimal $getInQuantity(D detail);

    BigDecimal $getApplyQuantity(D detail);

    Date $getProduceDate(D detail);

    Date $getPurchaseDate(D detail);

    Long $getRedReferId(D detail);

    BigDecimal $getBill(D detail);

    /**
     * 退料单ONLY
     */
    Long $getReturnSrcId(D detail);

    /**
     * 采购入库ONLY
     */
    String $getOrderNo(D detail);

    String $getMemoField(D detail);

    BigDecimal $getRedQuantity(D detail);

    void $setRedQuantity(D detail, BigDecimal quantity);

    Long $getBatchInfoId(D detail);

    /***
     * 只包含其他入库，生产入库和采购入库
     * @param detail 表体明细
     * @param batchId 批次信息表id
     */
    void $setBatchInfoId(D detail, Long batchId);

    void $setInQuantity(D detail, BigDecimal quantity);

}
