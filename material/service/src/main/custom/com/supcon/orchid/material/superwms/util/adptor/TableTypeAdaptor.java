package com.supcon.orchid.material.superwms.util.adptor;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseBatchType;
import com.supcon.orchid.material.superwms.util.BatchNumberConvertor;

public interface TableTypeAdaptor<T extends AbstractEcFullEntity,D extends AbstractEcPartEntity> {

    BaseSetMaterial $getMaterial(D detail);

    default String $getOriginBatchNum(D detail){
        return BatchNumberConvertor.getOriginBatchNum($getMaterial(detail),$getBatchNum(detail));
    }

    String $getBatchNum(D detail);

    default boolean $isEnableBatch(D detail){
        return BaseBatchType.isEnable(Dbs.getProp($getMaterial(detail),BaseSetMaterial::getIsBatch));
    }

}
