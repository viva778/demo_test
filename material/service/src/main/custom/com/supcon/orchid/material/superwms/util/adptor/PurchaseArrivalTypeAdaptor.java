package com.supcon.orchid.material.superwms.util.adptor;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.material.entities.MaterialPurArrivalInfo;
import com.supcon.orchid.material.entities.MaterialPurArrivalPart;

public class PurchaseArrivalTypeAdaptor implements TableTypeAdaptor<MaterialPurArrivalInfo, MaterialPurArrivalPart>{

    @Override
    public BaseSetMaterial $getMaterial(MaterialPurArrivalPart detail) {
        return detail.getGood();
    }

    @Override
    public String $getBatchNum(MaterialPurArrivalPart detail) {
        return $isEnableBatch(detail)?detail.getBatch():null;
    }
}
