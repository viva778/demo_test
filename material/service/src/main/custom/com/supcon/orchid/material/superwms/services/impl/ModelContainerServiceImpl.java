package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Elements;
import com.supcon.orchid.material.services.MaterialContainerPartsService;
import com.supcon.orchid.material.services.MaterialPurchInSubDtlService;
import com.supcon.orchid.material.superwms.services.ModelContainerService;
import org.apache.tools.ant.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ModelContainerServiceImpl implements ModelContainerService {

    /**
     * 通过条码台账Ids清空容器
     *
     * @param itemIds 条码台账Ids
     */
    @Transactional
    public void clearQrItems(List<Long> itemIds) {
        //1.删除容器明细
        Dbs.execute(
                "UPDATE " + MaterialContainerParts.TABLE_NAME + " SET VALID=0 WHERE " + Dbs.inCondition("QR_DETAIL_INFO", itemIds.size()),
                itemIds.toArray()
        );
        //2.更新容器状态
        //查询明细被全部删除的容器，设置状态为空
        List<Long> invalidContainerIds = Dbs.stream(
                "SELECT DISTINCT CONTAINER FROM " + MaterialContainerParts.TABLE_NAME + " WHERE CONTAINER NOT IN (SELECT CONTAINER FROM " + MaterialContainerParts.TABLE_NAME + " WHERE VALID=1) AND " + Dbs.inCondition("QR_DETAIL_INFO", itemIds.size()),
                Long.class,
                itemIds.toArray()
        ).collect(Collectors.toList());
        if (invalidContainerIds.size() > 0) {
            Dbs.execute(
                    "UPDATE " + MaterialContainerFile.TABLE_NAME + " SET STORAGE_STATE=? WHERE " + Dbs.inCondition("ID", invalidContainerIds.size()),
                    Elements.toArray("material_ctState/vacancy", invalidContainerIds)
            );
        }
    }

    @Autowired
    private MaterialContainerPartsService detailService;

    @Override
    @Transactional
    public void checkoutContainerDetails(List<MaterialContainerParts> details) {
        //若存在基础仓库，则转换为仓库建模id
        details.forEach(detail -> {
            if (detail.getStoreset() != null && detail.getStoreset().getCode() != null) {
                MaterialWareModel load = Dbs.load(MaterialWareModel.class, "VALID =1 AND CODE = ?", detail.getStoreset().getCode());
                if (load != null) {
                    detail.setStoreset(load);
                }
                if (detail.getArrivalPart() != null && detail.getArrivalPart().getId() != null) {
                    MaterialPurArrivalPart load1 = Dbs.load(MaterialPurArrivalPart.class, "VALID =1 AND ID = ?", detail.getArrivalPart().getId());
                    if (load1 != null) {
                        detail.setArrivalPart(load1);
                    }
                }
            }
            detailService.saveContainerParts(detail, null);
            //同步更新到货明细状态
            Long purArrivalPartId = Dbs.getProp(detail.getArrivalPart(), MaterialPurArrivalPart::getId);
            if (purArrivalPartId != null) {
                int unloadOverInt = detail.getUnloadOver() != null && detail.getUnloadOver() ? 1 : 0;
                Dbs.execute("UPDATE " + MaterialPurArrivalPart.TABLE_NAME + " SET UNLOAD_OVER = ? WHERE ID = ? AND (UNLOAD_OVER IS NULL OR UNLOAD_OVER = 0) "
                        , unloadOverInt, purArrivalPartId);
            }
        });
    }

    @Autowired
    private MaterialPurchInSubDtlService subDtlService;

    @Override
    @Transactional
    public void savePurchaseInContainerDetails(List<MaterialPurchInSubDtl> substanceDetails) {
        substanceDetails.forEach(detail -> subDtlService.savePurchInSubDtl(detail, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MaterialPurchInSingle> queryDetailsInPurchaseInSingles(MaterialContainerParts details) {


        MaterialPurArrivalInfo prop = Dbs.getProp(details.getArrivalPart(), MaterialPurArrivalPart::getArrivalInfo);
        if (prop == null) {
            return null;
        }
        List<MaterialPurchInSingle> load = Dbs.findByCondition(MaterialPurchInSingle.class, "VALID = 1 AND STATUS <> 99 AND  PUR_ARRIVAL_NO = ? ", prop.getTableNo());
        if (load == null) {
            return null;
        }
//        Long containerId = Dbs.getProp(details.getContainer(), MaterialContainerFile::getId);
//        List<MaterialPurchInSubDtl> byCondition = Dbs.findByCondition(MaterialPurchInSubDtl.class, "VALID =1 AND CONTAINER = ?", containerId);
        Set<Long> purchInSingleIds = load.stream().map(MaterialPurchInSingle::getId).collect(Collectors.toSet());
        List<MaterialPurchInSubDtl> byCondition = Dbs.findByCondition(MaterialPurchInSubDtl.class, "VALID = 1 AND "
                        + Dbs.inCondition("PURCHASE_IN", purchInSingleIds.size()),
                purchInSingleIds.toArray());
        if (byCondition == null || byCondition.size() == 0) {
            return null;
        }
        Set<Long> collect = byCondition.stream().map(e -> Dbs.getProp(e.getPurchaseIn(), MaterialPurchInSingle::getId)).filter(Objects::nonNull).collect(Collectors.toSet());
        return load.stream().filter(e -> collect.contains(e.getId())).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePurchaseInContainerDetails(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Long> collect = ids.stream().filter(Objects::nonNull).collect(Collectors.toList());
        Dbs.execute("UPDATE " + MaterialContainerParts.TABLE_NAME + " SET VALID = 0 WHERE "
                + Dbs.inCondition("ID", collect.size()), collect.toArray());
    }

}
