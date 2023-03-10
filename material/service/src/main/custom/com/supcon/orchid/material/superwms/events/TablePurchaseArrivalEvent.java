package com.supcon.orchid.material.superwms.events;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.fooramework.annotation.signal.SignalManager;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.PostSqlUpdater;
import com.supcon.orchid.fooramework.util.RequestCaches;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.material.services.MaterialBatchInfoService;
import com.supcon.orchid.material.services.MaterialPurchInPartService;
import com.supcon.orchid.material.superwms.config.MaterialSystemConfig;
import com.supcon.orchid.material.superwms.constants.BizTypeCode;
import com.supcon.orchid.material.superwms.constants.QrTypeCode;
import com.supcon.orchid.material.superwms.constants.systemcode.*;
import com.supcon.orchid.material.superwms.services.ModelBarCodeService;
import com.supcon.orchid.material.superwms.services.ModelBatchInfoService;
import com.supcon.orchid.material.superwms.services.ModelBizTypeService;
import com.supcon.orchid.fooramework.services.PlatformWorkflowService;
import com.supcon.orchid.material.superwms.util.factories.BatchDealInfoFactory;
import com.supcon.orchid.material.superwms.util.factories.BatchInfoFactory;
import com.supcon.orchid.orm.entities.CodeEntity;
import com.supcon.orchid.orm.entities.IdEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TablePurchaseArrivalEvent {

    @Autowired
    private PlatformWorkflowService workflowService;
    @Autowired
    private MaterialPurchInPartService purchaseInDetailService;

    @Signal("PurchaseArrivalSuperEdit")
    private void superEdit(MaterialPurArrivalInfo table) {
        if (table.getStatus() != null && table.getStatus() == 99) {
            List<MaterialPurArrivalPart> details = Dbs.findByCondition(
                    MaterialPurArrivalPart.class,
                    "VALID=1 AND ARRIVAL_INFO=?",
                    table.getId()
            );
            SignalManager.propagate("PurchaseArrivalEffecting", table, details);
        }
    }

    @Signal("PurchaseArrivalAfterSubmit")
    private void afterSubmit(MaterialPurArrivalInfo table) {
        List<MaterialPurArrivalPart> details = Dbs.findByCondition(
                MaterialPurArrivalPart.class,
                "VALID=1 AND ARRIVAL_INFO=?",
                table.getId()
        );
        switch (table.getWorkFlowVar().getOutcomeType()) {
            case "reject": {
                SignalManager.propagate("PurchaseArrivalRejecting", table, details);
                break;
            }
            case "cancel": {
                SignalManager.propagate("PurchaseArrivalCanceling", table, details);
                break;
            }
            default: {
                if (workflowService.isFirstTransition(table.getDeploymentId(), table.getWorkFlowVar().getOutcome())) {
                    SignalManager.propagate("PurchaseArrivalFirstCommit", table, details);
                }
                // ?????????????????????????????????????????????????????????????????????
                if (table.getStatus() == 99) {
                    SignalManager.propagate("PurchaseArrivalEffecting", table, details);
                }
                break;
            }
        }
    }


    //??????????????????????????????????????????????????????
    @Signal("BeforeSavePurchaseArrivalDetail")
    private void beforeSaveDetail(MaterialPurArrivalPart detail) {
        //????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        boolean auto_batch_mgmt = !Strings.valid(detail.getBatch()) && BaseBatchType.BY_BATCH.equals(Dbs.getProp(detail.getGood(), BaseSetMaterial::getIsBatch, CodeEntity::getId));
        //??????????????????????????????????????????,???afterSubmit????????????
        boolean remark_auto_batch = false;
        if (auto_batch_mgmt && null != detail.getPurchaseId()) {
            //???????????? ????????????ID$?????? ??????
            Map<String, String> order_detail$batch = RequestCaches.computeIfAbsent("purchase_batch_" + detail.getArrivalInfo().getId(), k -> new HashMap<>());
            if (order_detail$batch.get(detail.getPurchaseId()) == null) {
                //??????????????????
                //??????????????????????????????????????????????????????
                String batch = Dbs.first(
                        "SELECT BATCH FROM " + MaterialPurArrivalPart.TABLE_NAME + " WHERE VALID=1 AND PURCHASE_NO=? AND ARRIVAL_INFO=? AND BATCH IS NOT NULL AND BATCH<>''",
                        String.class,
                        detail.getPurchaseId(), detail.getArrivalInfo().getId()
                );
                if (Strings.valid(batch)) {
                    //????????????????????????????????????????????????
                    detail.setBatch(batch);
                    order_detail$batch.put(detail.getPurchaseId(), batch);
                } else {
                    remark_auto_batch = true;
                }
            } else {
                //????????????????????????????????????????????????
                detail.setBatch(order_detail$batch.get(detail.getPurchaseId()));
            }
        }
        RequestCaches.set("remark_auto_batch", remark_auto_batch);
    }

    //??????????????????????????????????????????????????????
    @Signal("AfterSavePurchaseArrivalDetail")
    private void afterSaveDetail(MaterialPurArrivalPart detail) {
        if (!BaseBatchType.isEnable(Dbs.getProp(detail.getGood(), BaseSetMaterial::getIsBatch))) {
            //??????????????????????????????(???????????????????????????????????????????????????)
            PostSqlUpdater.getAutoWritableEntity(Dbs.reload(detail)).setBatch(null);
        } else {
            boolean remark_auto_batch = Boolean.TRUE.equals(RequestCaches.get("remark_auto_batch"));
            if (remark_auto_batch) {
                //????????????????????????????????????
                Map<String, String> order_detail$batch = RequestCaches.computeIfAbsent("purchase_batch_" + detail.getArrivalInfo().getId(), k -> new HashMap<>());
                if (null != detail.getPurchaseId()) {
                    order_detail$batch.put(detail.getPurchaseId(), Dbs.reload(detail).getBatch());
                }
            }
        }
    }

    @Autowired
    private MaterialBatchInfoService batchInfoService;
    @Autowired
    private ModelBatchInfoService modelBatchInfoService;
    @Autowired
    private MaterialSystemConfig materialSystemConfig;

    @Signal("PurchaseArrivalFirstCommit")
    private void solveBatch(MaterialPurArrivalInfo table, List<MaterialPurArrivalPart> details) {
        List<MaterialPurArrivalPart> detailsWithBatch = details.stream().filter(detail -> BaseBatchType.isEnable(Dbs.getProp(detail.getGood(), BaseSetMaterial::getIsBatch)) && Strings.valid(detail.getBatch())).collect(Collectors.toList());
        if (!detailsWithBatch.isEmpty()) {
            modelBatchInfoService.checkBatchConflictByPiece(detailsWithBatch.stream()
                    .filter(detail -> BaseBatchType.BY_PIECE.equals(Dbs.getProp(detail.getGood(), BaseSetMaterial::getIsBatch, SystemCode::getId)))
                    .map(detail -> Pair.of(detail.getBatch(), detail.getGood().getId()))
                    .collect(Collectors.toList())
            );
            modelBatchInfoService.checkBatchRuleAndFilterExist(detailsWithBatch, MaterialPurArrivalPart::getGood,MaterialPurArrivalPart::getBatch, true);
            //????????????????????????????????????????????????
            List<MaterialBatchInfo> batchInfoList = BatchInfoFactory.purchaseArrivalBatchInfoList(table, details);
            batchInfoList.forEach(batchInfo -> batchInfoService.saveBatchInfo(batchInfo, null));
        }
    }


    //???????????????????????????
    @Signal({"PurchaseArrivalRejecting", "PurchaseArrivalCanceling"})
    private void deleteBatchInfo(List<MaterialPurArrivalPart> details) {
        Object[] batch_array = details.stream().map(MaterialPurArrivalPart::getBatch).filter(Objects::nonNull).toArray();
        if (batch_array.length > 0) {
            Dbs.execute(
                    "UPDATE " + MaterialBatchInfo.TABLE_NAME + " SET VALID=0 WHERE VALID=1 AND " + Dbs.inCondition("BATCH_NUM", batch_array.length),
                    batch_array
            );
        }
    }

    //???????????????????????????
    @Signal("PurchaseArrivalRejecting")
    private void clearBatch(List<MaterialPurArrivalPart> details) {
        Dbs.execute(
                "UPDATE " + MaterialPurArrivalPart.TABLE_NAME + " SET BATCH=NULL WHERE " + Dbs.inCondition("ID", details.size()),
                details.stream().map(IdEntity::getId).toArray()
        );
    }

    @Autowired
    private ModelBarCodeService barCodeService;
    @Autowired
    private ModelBizTypeService bizTypeService;

    //??????????????????
    @Signal("PurchaseArrivalEffecting")
    private void effect(MaterialPurArrivalInfo table, List<MaterialPurArrivalPart> details) {
        //1.????????????????????????
        List<MaterialBatchDeal> dealInfoList = BatchDealInfoFactory.purchaseArrivalDealList(table, details, BaseDealState.EFFECT);
        dealInfoList.forEach(Dbs::save);
        //2.???????????????????????????????????????????????????????????????
        Map<MaterialPurchasePart, List<MaterialPurArrivalPart>> orderDetail$details = details.stream().filter(detail -> null != detail.getPurchaseId())
                .collect(Collectors.groupingBy(detail -> Dbs.load(MaterialPurchasePart.class, Long.parseLong(detail.getPurchaseId()))));
        orderDetail$details.forEach((orderDetail, groupDetails) -> {
            MaterialPurchasePart wOrderDetail = PostSqlUpdater.getAutoWritableEntity(orderDetail);
            wOrderDetail.setArrivalNum(wOrderDetail.getArrivalNum().add(groupDetails.stream().map(MaterialPurArrivalPart::getArrivalQuan).reduce(BigDecimal.ZERO, BigDecimal::add)));
            BigDecimal remainQuan = wOrderDetail.getPurchQuantity().subtract(wOrderDetail.getArrivalNum());
            if (remainQuan.compareTo(BigDecimal.ZERO) > 0) {
                wOrderDetail.setNoArrivalNum(remainQuan);
                wOrderDetail.setArrivalState(new SystemCode(ArrivalState.MIDDLE));
            } else {
                wOrderDetail.setNoArrivalNum(BigDecimal.ZERO);
                wOrderDetail.setArrivalState(new SystemCode(ArrivalState.COMPLETED));
            }
        });

        //3.?????????????????????
        details.forEach(detail -> {
            if (Boolean.TRUE.equals(detail.getGenPrintInfo())) {
                barCodeService.generatePrintList(
                        detail.getId(),
                        table.getTableNo(),
                        detail.getWare(),
                        detail.getGood(),
                        detail.getBatch(),
                        detail.getArrivalQuan(),
                        table.getArrivalDate(),
                        QrTypeCode.PURCHASE
                );
            }
        });

        //4.???????????????(????????????????????????
        MaterialServiceType bizType = bizTypeService.getBizType(BizTypeCode.PURCHASE_STORAGE);
        MaterialStorageType storageType = bizTypeService.getStorageType("purchaseIn");
        details.stream().collect(Collectors.groupingBy(MaterialPurArrivalPart::getWare)).forEach((warehouse, groupDetails) -> {
            MaterialPurchInSingle inTable = new MaterialPurchInSingle();
            inTable.setWareId(warehouse);
            inTable.setSrcId(table.getId());
            inTable.setPurArrivalNo(table.getTableNo());
            inTable.setVendor(table.getVendor());
            inTable.setInPerson(table.getPurePerson());
            inTable.setInDepart(table.getPurchDept());
            inTable.setInStorageDate(table.getArrivalDate());
            inTable.setRedBlue(table.getRedBlue());
            inTable.setServiceTypeId(bizType);
            inTable.setInCome(storageType);
            //??????????????????????????????????????????isovercheck???false
            if (BaseRedBlue.BLUE.equals(table.getRedBlue().getId()) && Boolean.TRUE.equals(materialSystemConfig.getGenerateCheckRequest())) {
                List<MaterialPurArrivalPart> detailsNeedCheck = groupDetails
                        .stream()
                        .filter(detail -> Boolean.TRUE.equals(Dbs.getProp(detail.getGood(), BaseSetMaterial::getIsCheck)))
                        .collect(Collectors.toList());
                inTable.setIsCheckOver(detailsNeedCheck.isEmpty());
            }
            List<MaterialPurchInPart> inDetails = groupDetails.stream().map(detail -> {
                BaseSetMaterial material = Dbs.reload(detail.getGood());
                MaterialPurchInPart inDetail = new MaterialPurchInPart();
                inDetail.setSrcPartId(detail.getId());
                inDetail.setGood(material);
                if (BaseBatchType.isEnable(material.getIsBatch())) {
                    inDetail.setBatch(detail.getBatch());
                    if (Boolean.TRUE.equals(material.getIsCheck())) {
                        inDetail.setCheckState(new SystemCode(BaseCheckState.TO_CHECK));
                    } else {
                        inDetail.setCheckState(new SystemCode(BaseCheckState.UNNECESSARY));
                    }
                }
                inDetail.setProductionDate(detail.getProduceDate());
                inDetail.setPlaceSet(detail.getStoreSet());
                inDetail.setApplyQuantity(detail.getArrivalQuan());
                inDetail.setPurchaseId(Optional.ofNullable(detail.getPurchaseId()).map(Long::parseLong).orElse(null));
                inDetail.setPurOrderNo(detail.getPurchaseNo());
                inDetail.setArrivalQuantity(detail.getArrivalQuan());
                inDetail.setScparama(detail.getScparama());
                inDetail.setScparamb(detail.getScparamb());
                inDetail.setBigintparama(detail.getBigintparama());

                return inDetail;
            }).collect(Collectors.toList());
            workflowService.saveTable(inTable, "purchaseInsingleFlw", () -> inDetails.forEach(inDetail -> {
                inDetail.setInSingle(inTable);
                purchaseInDetailService.savePurchInPart(inDetail, null);
            }));
        });
    }
}
