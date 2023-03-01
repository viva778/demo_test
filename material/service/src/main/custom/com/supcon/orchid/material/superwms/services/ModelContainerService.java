package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.material.entities.MaterialContainerParts;
import com.supcon.orchid.material.entities.MaterialPurchInSingle;
import com.supcon.orchid.material.entities.MaterialPurchInSubDtl;

import java.util.List;

public interface ModelContainerService {

    /**
     * 通过条码台账Ids清空容器
     * @param itemIds 条码台账Ids
     */

    void clearQrItems(List<Long> itemIds);

    /**
     * 容器明细确认按触发
     * 保存明细数据
     * 更改到货单明细，卸货完毕字段
     * @param details
     * @return
     */
    void checkoutContainerDetails(List<MaterialContainerParts> details);

    void savePurchaseInContainerDetails(List<MaterialPurchInSubDtl> substanceDetails);

    List<MaterialPurchInSingle> queryDetailsInPurchaseInSingles(MaterialContainerParts details);

    void deletePurchaseInContainerDetails (List<Long> ids);

}
