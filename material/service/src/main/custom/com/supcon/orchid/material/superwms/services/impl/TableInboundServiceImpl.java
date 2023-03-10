package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.BaseSet.entities.*;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity;
import com.supcon.orchid.ec.services.DataGridService;
import com.supcon.orchid.fooramework.services.PlatformWorkflowService;
import com.supcon.orchid.fooramework.support.ModuleRequestContext;
import com.supcon.orchid.fooramework.util.*;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.signal.SignalManager;
import com.supcon.orchid.fooramework.support.Converter;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.material.services.MaterialBatchDealService;
import com.supcon.orchid.material.services.MaterialBatchInfoService;
import com.supcon.orchid.material.services.MaterialBusinessDetailService;
import com.supcon.orchid.material.superwms.config.MaterialSystemConfig;
import com.supcon.orchid.material.superwms.constants.BatchUniqueRule;
import com.supcon.orchid.material.superwms.constants.QCSInspect;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseBatchType;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseDealState;
import com.supcon.orchid.material.superwms.services.ModelBarCodeService;
import com.supcon.orchid.material.superwms.services.ModelBatchInfoService;
import com.supcon.orchid.material.superwms.services.TableInboundService;
import com.supcon.orchid.material.superwms.util.MaterialExceptionThrower;
import com.supcon.orchid.material.superwms.util.StockOperator;
import com.supcon.orchid.material.superwms.util.adptor.AdaptorCenter;
import com.supcon.orchid.material.superwms.util.adptor.BizTypeContext;
import com.supcon.orchid.material.superwms.util.adptor.InboundTypeAdaptor;
import com.supcon.orchid.material.superwms.util.factories.BatchDealInfoFactory;
import com.supcon.orchid.material.superwms.util.factories.BatchInfoFactory;
import com.supcon.orchid.material.superwms.util.factories.BusinessDetailFactory;
import com.supcon.orchid.orm.entities.IdEntity;
import com.supcon.orchid.services.BAPException;
import com.supcon.orchid.support.Result;
import com.supcon.orchid.utils.StringUtils;
import com.supcon.orchid.workflow.engine.entities.WorkFlowVar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TableInboundServiceImpl implements TableInboundService {
    @Autowired
    private MaterialBatchDealService batchDealService;
    @Autowired
    private MaterialBusinessDetailService businessDetailService;
    @Autowired
    private MaterialSystemConfig materialSystemConfig;
    @Autowired
    private MaterialBatchInfoService batchInfoService;
    @Autowired
    private PlatformWorkflowService workflowService;
    @Autowired
    private ModelBatchInfoService modelBatchInfoService;
    @Autowired
    private ModelBarCodeService barCodeService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * ??????????????????
     *
     * @param bizType     ????????????
     * @param table       ??????
     * @param workFlowVar ???????????????
     * @modify 1. ??????
     * 2. ??????: ????????????????????????????????????,??????????????????????????? modify by yaoyao 2022-05-30
     */
    @Override
    public void standardInboundEvent(String bizType, AbstractEcFullEntity table, WorkFlowVar workFlowVar) {
        BizTypeContext context = AdaptorCenter.getBizTypeContext(bizType);
        String prefix = context.getPrefix();
        //??????????????????
        List<? extends AbstractEcPartEntity> details = Dbs.findByCondition(
                context.getDetailClass(),
                "VALID=1 AND " + context.getAssociatedColumn() + "=? ",
                table.getId()
        ).stream().map(PostSqlUpdater::getAutoWritableEntity).collect(Collectors.toList());
        switch (workFlowVar.getOutcomeType()) {
            case "cancel": {
                SignalManager.propagate("InboundCanceling", table, details);
                SignalManager.propagate(prefix + "Canceling", table, details);
                _onCancel(table, details, bizType);
                break;
            }
            case "reject": {
                SignalManager.propagate("InboundRejecting", table, details);
                SignalManager.propagate(prefix + "Rejecting", table, details);
                _onRejecting(table, details, bizType);
                break;
            }
            default: {
                if (workflowService.isFirstTransition(table.getDeploymentId(), workFlowVar.getOutcome())) {
                    //---------------------------------????????????------------------------------
                    SignalManager.propagate("InboundFirstCommit", table, details);
                    SignalManager.propagate(prefix + "FirstCommit", table, details);
                    _onFirstCommit(table, details, bizType);
                }
                if (table.getStatus() == 99) {
                    //---------------------------------??????------------------------------
                    SignalManager.propagate("InboundEffecting", table, details);
                    SignalManager.propagate(prefix + "Effecting", table, details);
                    _onEffect(table, details, bizType);
                }
            }
        }
    }

    @Override
    public void quickEffect(String bizType, AbstractEcFullEntity table) {
        BizTypeContext context = AdaptorCenter.getBizTypeContext(bizType);
        String prefix = context.getPrefix();
        //??????????????????
        List<? extends AbstractEcPartEntity> details = Dbs.findByCondition(
                context.getDetailClass(),
                "VALID=1 AND " + context.getAssociatedColumn() + "=? ",
                table.getId()
        ).stream().map(PostSqlUpdater::getAutoWritableEntity).collect(Collectors.toList());
        //---------------------------------????????????------------------------------
        SignalManager.propagate("InboundFirstCommit", table, details);
        SignalManager.propagate(prefix + "FirstCommit", table, details);
        _onFirstCommit(table, details, bizType);
        //---------------------------------??????------------------------------
        SignalManager.propagate("InboundEffecting", table, details);
        SignalManager.propagate(prefix + "Effecting", table, details);
        _onEffect(table, details, bizType);
    }

    /**
     * ????????????????????????????????????
     *
     * @param warehouseId ??????id
     * @param goodId      ??????id
     * @return ????????????????????????
     */
    @Override
    public boolean allowToStore(Long warehouseId, Long goodId) {
        switch (String.valueOf(materialSystemConfig.getStorageCheckType())) {
            case "ware": {
                //--------------------------???????????????????????????-------------------------
                //??????????????????????????????????????????
                Set<Long> containMaterials = Dbs.stream(
                        "SELECT MATERIAL FROM " + BaseSetWareMater.TABLE_NAME + " WHERE VALID=1 AND WAREHOUSE=? ",
                        Long.class,
                        warehouseId
                ).collect(Collectors.toSet());
                //????????????????????????????????????????????????
                if (containMaterials.size() == 0 || containMaterials.contains(goodId)) {
                    return true;
                }
                //????????????????????????????????????????????????
                return Dbs.exist(
                        BaseSetWareMaterClz.TABLE_NAME,
                        "VALID=1 AND WAREHOUSE=? AND MATERIAL_CLASS=(" +
                                "SELECT MATERIAL_CLASS FROM " + BaseSetMaterial.TABLE_NAME + " WHERE VALID = 1 AND ID=?" +
                                ")",
                        warehouseId, goodId
                );
            }
            case "good": {
                //--------------------------???????????????????????????-------------------------
                Map<Long, Boolean> ware$allow = Dbs.binaryMap(
                        "SELECT l.WARE_HOUSE,s.SET_ALLOW FROM " + BaseSetMaterialWareList.TABLE_NAME + " l LEFT JOIN " + BaseSetMaterialWareSet.TABLE_NAME + " s ON l.MATERIAL_WARE_SET=s.ID WHERE l.VALID=1 AND s.VALID=1 AND s.MATERIAL=?",
                        Long.class, Boolean.class,
                        goodId
                );
                //??????????????????????????????
                return ware$allow.size() == 0 || Boolean.TRUE.equals(ware$allow.get(warehouseId));
            }
            default: {
                return true;
            }
        }
    }


    @Override
    public void solveInbound(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        if (!details.isEmpty()) {
            InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
            //???????????????????????????
            details.forEach(detail -> adaptor.$setInQuantity(detail, adaptor.$getApplyQuantity(detail)));
            //???????????????
            _updateStock(table, details, bizType);
            //?????????????????????
            List<MaterialBusinessDetail> businessDetails = BusinessDetailFactory.getInboundBizDetailList(table, details, bizType);
            businessDetails.forEach(businessDetail -> businessDetailService.saveBusinessDetail(businessDetail, null));
            //?????????????????????????????????
            List<MaterialBatchDeal> batchDeals = BatchDealInfoFactory.getInboundDealInfoList(table, details, BaseDealState.EFFECT, bizType);
            batchDeals.forEach(batchDeal -> batchDealService.saveBatchDeal(batchDeal, null));
            //?????????????????????
            details.forEach(detail -> SignalManager.propagate("InboundDetail", bizType, table, detail, adaptor.$getInQuantity(detail)));
        }
    }

    @Override
    public void createInboundTask(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        if (!details.isEmpty()) {
            //??????????????????
            SignalManager.propagate("GenerateInboundTask", bizType, table, details);
        }
    }


    @Override
    @Transactional
    public void deleteInboundTable(String tableNo, String bizType) {
        if (StringUtils.isEmpty(tableNo)) {
            throw new BAPException("tableNo??????");
        }
        BizTypeContext context = AdaptorCenter.getBizTypeContext(bizType);
        AbstractEcFullEntity table = Dbs.load(
                context.getTableClass(),
                "VALID =1 AND STATUS <> 0 AND TABLE_NO=?",
                tableNo
        );
        if (table == null || table.getStatus() == null) {
            throw new BAPException("???????????????,tableNo:" + tableNo);
        }
        if (table.getStatus() == 99) {
            //????????????????????????
            //????????????????????????????????????
            List<? extends AbstractEcPartEntity> details = Dbs.findByCondition(
                    context.getDetailClass(),
                    "VALID=1 AND " + context.getAssociatedColumn() + "=?",
                    table.getId()
            );
            SignalManager.propagate("DeleteInboundTable", bizType, table, details);
            //??????????????????
            List<String> sDetailIds = details.stream().map(IdEntity::getId).map(Objects::toString).collect(Collectors.toList());
            List<MaterialBusinessDetail> bds = Dbs.findByCondition(
                    MaterialBusinessDetail.class,
                    "VALID=1 AND " + Dbs.inCondition("TABLE_BODYID", sDetailIds.size()),
                    sDetailIds.toArray()
            );
            if (!bds.isEmpty()) {
                bds.forEach(bd -> {
                    Long placeId = Optional.ofNullable(bd.getPlaceSet()).map(IdEntity::getId).orElse(null);
                    //????????????
                    bd.setValid(false);
                    Dbs.save(bd);
                    //???????????????
                    StockOperator.of(
                            bd.getGood().getId(),
                            bd.getBatchText(),
                            bd.getWare().getId(),
                            placeId
                    ).setSourceEntity(bd).increaseWriteOff(bd.getQuantity());
                    //???????????????????????????
                    Date transactDate = Dates.getDateWithoutTime(bd.getTrasactionDate());
                    Dbs.execute(
                            "UPDATE " + MaterialDaySettlement.TABLE_NAME + " SET DAY_SETTLEMENT=DAY_SETTLEMENT-?, TOTAL_IN=TOTAL_IN-? WHERE WARE=? AND GOOD=? AND PLACE_SET IS NULL AND SETTLEMENT_DATE>=?",
                            bd.getQuantity(), bd.getQuantity(), bd.getWare().getId(), bd.getGood().getId(), transactDate
                    );
                    if (placeId != null) {
                        Dbs.execute(
                                "UPDATE " + MaterialDaySettlement.TABLE_NAME + " SET DAY_SETTLEMENT=DAY_SETTLEMENT-?, TOTAL_IN=TOTAL_IN-? WHERE WARE=? AND GOOD=? AND PLACE_SET=? AND SETTLEMENT_DATE>=?",
                                bd.getQuantity(), bd.getQuantity(), bd.getWare().getId(), bd.getGood().getId(), placeId, transactDate
                        );
                    }
                    //??????????????????
                    Dbs.execute(
                            "UPDATE " + MaterialQrDetailInfo.TABLE_NAME + " SET VALID = 0 WHERE SRC_ID = ? ", bd.getId()
                    );
                });
            }
            Set<String> batchNumSet = bds.stream().map(MaterialBusinessDetail::getBatchText).filter(Strings::valid).collect(Collectors.toSet());
            if (!batchNumSet.isEmpty()) {
                InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
                //????????????????????????
                Dbs.execute(
                        "UPDATE " + MaterialBatchDeal.TABLE_NAME + " SET VALID = 0 WHERE DEAL_TABLE_NO =?", tableNo
                );
                //??????????????????????????????
//                List<MaterialBatchDeal> batchDeals = BatchDealInfoFactory.getInboundDealInfoList(table, details.stream().filter(detail -> batchNumSet.contains(adaptor.$getBatchNum(detail))).collect(Collectors.toList()), BaseDealState.CANCEL, bizType);
//                batchDeals.forEach(batchDeal -> batchDealService.saveBatchDeal(batchDeal, null));
            }
            //?????????????????????
            Dbs.execute(
                    "UPDATE " + MaterialPrintInfo.TABLE_NAME + " SET VALID = 0 WHERE SRC_TABLE_NO =?", tableNo
            );
            Transactions.appendEventAfterCommit(() -> {
                try {
                    if (details.isEmpty()) {
                        return;
                    }
                    Class<? extends AbstractEcPartEntity> aClass = details.get(0).getClass();
                    Field check_apply_id = aClass.getDeclaredField("checkApplyId");
                    check_apply_id.setAccessible(true);
                    details.forEach(dto ->
                            {
                                try {
                                    if (check_apply_id.get(dto) == null) {
                                        log.error("??????id:{},?????????????????????id", dto.getId());
                                        return;
                                    }
                                    String body = check_apply_id.get(dto).toString();
                                    ModuleHttpClient.exchange(
                                            ModuleRequestContext.builder()
                                                    .moduleName(QCSInspect.QCS_MODULE_NAME)
                                                    .path(QCSInspect.DELETE_INSPECT_URL + "?id=" + body)
                                                    .method(HttpMethod.POST)
                                                    .body(null)
                                                    .log(true)
                                                    .build(),
                                            Result.class);
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                    log.info("????????????????????????????????????????????????");
                                }
                            }
                    );
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                    log.info("????????????????????????????????????????????????");
                }
            });
        }

        //super edit????????????
        _cancelBySuperEdit(table, context.getProcessKey(), null);
    }


    private void _cancelBySuperEdit(AbstractEcFullEntity table, String processKey, DataGridService dataGridService) {
        table.setStatus(0);
        Long deploymentId = workflowService.getCurrentDeploymentId(processKey);
        Assert.notNull(deploymentId, "????????????????????????????????????");

        Object service = Entities.getEntityService(table.getClass());
        String mtdNameOfSuperEdit = "saveSuperEdit" + Entities.getEntityName(table.getClass());
        //??????superEdit
        Reflects.call(
                service, mtdNameOfSuperEdit,
                table, null, dataGridService, null, new boolean[]{false}
        );
    }

    /**
     * ???????????????????????????????????????
     *
     * @param table   ??????
     * @param details ????????????
     * @param bizType ????????????
     * @author zhuzhenbo
     * @date 2022-xx-xx
     * @modify 1.2022-xx-xx ?????? modify by zhuzhenbo
     */
    private void _onEffect(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
        //????????????????????????????????????????????????????????????????????????
        boolean generateTask = Boolean.TRUE.equals(materialSystemConfig.getEnableTask()) && Boolean.TRUE.equals(Dbs.getProp(adaptor.getWarehouse(table), BaseSetWarehouse::getStoresetState));
        if (generateTask) {
            //????????????????????????????????????????????????????????????
            Elements.shunt(details, adaptor::$isGenTask, Collectors.toList()).consume((genDetails, othDetails) -> {
                //????????????????????????
                createInboundTask(table, genDetails, bizType);
                //????????????
                solveInbound(table, othDetails, bizType);
            });
        } else {
            //????????????
            solveInbound(table, details, bizType);
        }
    }


    private void _onRejecting(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        BizTypeContext context = AdaptorCenter.getBizTypeContext(bizType);
        //????????????????????????????????????????????????????????????
        if (context.getInContext().isBatchConflictCheck()) {
            InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
            List<String> batchNumList = details.stream().map(adaptor::$getBatchNum).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!batchNumList.isEmpty()) {
                Dbs.execute(
                        "UPDATE " + MaterialBatchInfo.TABLE_NAME + " SET VALID=0, DELETE_TIME=? WHERE VALID=1 AND " + Dbs.inCondition("BATCH_NUM", batchNumList.size()),
                        Elements.toArray(new Date(), batchNumList)
                );
            }
        }
    }

    /**
     * ??????????????????
     *
     * @param table   ????????????
     * @param details ????????????
     * @param bizType ????????????
     */
    private void _onFirstCommit(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
        //???????????????????????????????????????
        details.stream().map(adaptor::$getMaterial).distinct().forEach(good -> {
            if (!allowToStore(adaptor.getWarehouse(table).getId(), good.getId())) {
                MaterialExceptionThrower.storage_not_allowed(Dbs.getProp(good, BaseSetMaterial::getName));
            }
        });
        List<? extends AbstractEcPartEntity> detailsWithBatch = details.stream()
                .filter(detail -> Strings.valid(adaptor.$getBatchNum(detail)))
                .collect(Collectors.toList());
        if (!detailsWithBatch.isEmpty()) {
            //??????????????????
            List<? extends AbstractEcPartEntity> detailsByPiece = detailsWithBatch.stream()
                    .filter(detail -> BaseBatchType.BY_PIECE.equals(Dbs.getProp(adaptor.$getMaterial(detail), BaseSetMaterial::getIsBatch, SystemCode::getId)))
                    .collect(Collectors.toList());
            String errorBatchList = detailsByPiece.stream().filter(detail -> BigDecimal.ONE.compareTo(adaptor.$getApplyQuantity(detail)) != 0).map(detail -> {
                String materialName = Dbs.getProp(adaptor.$getMaterial(detail), BaseSetMaterial::getName);
                String batchNum = adaptor.$getBatchNum(detail);
                return materialName + "/" + batchNum;
            }).collect(Collectors.joining(","));
            if (Strings.valid(errorBatchList)) {
                MaterialExceptionThrower.piece_batch_stock_only_can_be_one(errorBatchList);
            }
            //????????????????????????????????????????????????????????????
            modelBatchInfoService.checkBatchConflictByPiece(detailsByPiece.stream()
                    .map(detail -> Pair.of(adaptor.$getBatchNum(detail), adaptor.$getMaterial(detail).getId()))
                    .collect(Collectors.toList()));
            //????????????????????????????????????????????????????????????????????????
            List<? extends AbstractEcPartEntity> detailsWithBatchAndNotCreated = modelBatchInfoService.checkBatchRuleAndFilterExist(detailsWithBatch, adaptor::$getMaterial, adaptor::$getBatchNum, AdaptorCenter.getBizTypeContext(bizType).getInContext().isBatchConflictCheck());
            if (!detailsWithBatchAndNotCreated.isEmpty()) {
                List<MaterialBatchInfo> batchInfoList = BatchInfoFactory.inboundBatchInfoList(bizType, table, detailsWithBatchAndNotCreated);
                batchInfoList.forEach(batchInfo -> batchInfoService.saveBatchInfo(batchInfo, null));
                //????????????id
                detailsWithBatchAndNotCreated.forEach(detail -> batchInfoList
                        .stream()
                        .filter(batchInfo -> adaptor.$getBatchNum(detail).equals(batchInfo.getBatchNum()) && adaptor.$getMaterial(detail).equals(batchInfo.getMaterialId()))
                        .findAny()
                        .ifPresent(batchInfo -> adaptor.$setBatchInfoId(detail, batchInfo.getId()))
                );
                Dbs.flush();
            }
            //?????????????????????ID
            _associateBatchInfo(table, detailsWithBatch, bizType);
            //??????????????????
            _pushbackBatchInfo(table, detailsWithBatch, bizType);
        }
        //?????????????????????
        _generatePrintInfoList(table, details, bizType);
    }

    /**
     * ??????????????????
     *
     * @param details ????????????
     * @modify ???????????????????????????????????????????????????????????????????????????????????? zzb 22/06/30
     */
    private void _onCancel(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        BizTypeContext context = AdaptorCenter.getBizTypeContext(bizType);
        //????????????????????????????????????????????????????????????
        if (context.getInContext().isBatchConflictCheck()) {
            InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
            List<Pair<String, Long>> batch$material = details
                    .stream()
                    .filter(detail -> Strings.valid(adaptor.$getBatchNum(detail)) && adaptor.$getMaterial(detail) != null)
                    .map(detail -> Pair.of(adaptor.$getBatchNum(detail), adaptor.$getMaterial(detail).getId()))
                    .distinct()
                    .collect(Collectors.toList());
            if (!batch$material.isEmpty()) {
                Dbs.execute(
                        "UPDATE " + MaterialBatchInfo.TABLE_NAME + " SET VALID=0, DELETE_TIME=? WHERE VALID=1 AND " + modelBatchInfoService.getInConditionOfMaterials(batch$material),
                        new Date()
                );
            }
        }
    }


    private void _updateStock(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
        BaseSetWarehouse warehouse = adaptor.getWarehouse(table);
        details.forEach(detail -> {
            BaseSetMaterial good = adaptor.$getMaterial(detail);
            Long placeId = Optional.ofNullable(adaptor.$getPlace(detail)).map(IdEntity::getId).orElse(null);
            StockOperator.of(
                    good.getId(),
                    BaseBatchType.isEnable(Dbs.getProp(good, BaseSetMaterial::getIsBatch)) && Strings.valid(adaptor.$getBatchNum(detail))
                            ? adaptor.$getBatchNum(detail) : null,
                    warehouse.getId(),
                    placeId
            ).setSourceEntity(detail).increase(adaptor.$getInQuantity(detail));
        });
    }


    private void _generatePrintInfoList(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
        BizTypeContext context = AdaptorCenter.getBizTypeContext(bizType);
        details.forEach(detail -> {
            if (adaptor.$isGenPrintInfo(detail)) {
                barCodeService.generatePrintList(
                        detail.getId(),
                        table.getTableNo(),
                        adaptor.getWarehouse(table),
                        adaptor.$getMaterial(detail),
                        adaptor.$getBatchNum(detail),
                        adaptor.$getApplyQuantity(detail),
                        new Date(),
                        context.getQrTypeCode()
                );
            }
        });
    }


    /**
     * ????????????????????????????????????????????????id????????????
     *
     * @param table            ??????
     * @param detailsWithBatch ????????????
     * @param bizType          ????????????
     */
    private void _associateBatchInfo(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> detailsWithBatch, String bizType) {
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
        //????????????????????????????????????
        List<? extends AbstractEcPartEntity> detailsWithBatchAndNotAssociated = detailsWithBatch
                .stream()
                .filter(detail -> adaptor.$getBatchInfoId(detail) == null)
                .collect(Collectors.toList());
        if (!detailsWithBatchAndNotAssociated.isEmpty()) {
            //??????????????????
            if (BatchUniqueRule.BATCH.equals(materialSystemConfig.getBatchUniqueRule())) {
                Set<String> batchSet = detailsWithBatchAndNotAssociated.stream().map(adaptor::$getBatchNum).collect(Collectors.toSet());
                //??????????????????????????????????????????
                Map<String, Set<Long>> batch$materials = Dbs.pairList(
                        "SELECT BATCH_NUM,MATERIAL_ID FROM " + MaterialBatchInfo.TABLE_NAME + " WHERE VALID=1 AND " + Dbs.inCondition("BATCH_NUM", batchSet.size()),
                        String.class, Long.class,
                        batchSet.toArray()
                ).stream()
                        .collect(Collectors.groupingBy(Pair::getFirst))
                        .entrySet()
                        .stream().collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().stream().map(Pair::getSecond).collect(Collectors.toSet())
                        ));
                //????????????
                detailsWithBatchAndNotAssociated.stream().collect(Collectors.groupingBy(adaptor::$getBatchNum)).forEach((batchNum, groupDetails) -> {
                    Set<Long> existMaterialIds = batch$materials.get(batchNum);
                    List<Long> currentMaterialIds = groupDetails.stream().map(adaptor::$getMaterial).filter(Objects::nonNull).map(IdEntity::getId).distinct().collect(Collectors.toList());
                    if (currentMaterialIds.size() > 1 || (existMaterialIds != null && !existMaterialIds.isEmpty() && !existMaterialIds.contains(currentMaterialIds.get(0)))) {
                        //??????????????????????????????1????????? ????????????????????????????????????????????????????????????
                        MaterialExceptionThrower.cannot_refer_by_diff_material(batchNum);
                    }
                });
            }
            List<Pair<String, Long>> batch$material = detailsWithBatchAndNotAssociated.stream().map(detail -> Pair.of(adaptor.$getBatchNum(detail), adaptor.$getMaterial(detail).getId())).collect(Collectors.toList());
            Map<Pair<String, Long>, Long> batch_material$id = Dbs.stream(
                    "SELECT BATCH_NUM,MATERIAL_ID,ID FROM " + MaterialBatchInfo.TABLE_NAME + " WHERE VALID=1 AND " + modelBatchInfoService.getInConditionOfMaterials(batch$material),
                    Object[].class
            ).collect(Collectors.toMap(
                    arr -> Pair.of(Converter.stringConverter(arr[0]), Converter.longConverter(arr[1])),
                    arr -> Optional.ofNullable(Converter.longConverter(arr[2])).orElse(-1L)
            ));
            //????????????????????????????????????
            Set<Pair<String, Long>> batchMaterialSetWithoutInfo = Elements.difference(batch$material, batch_material$id.keySet());
            if (!batchMaterialSetWithoutInfo.isEmpty()) {
                //????????????
                MaterialExceptionThrower.batch_not_existed(batchMaterialSetWithoutInfo.stream().map(Pair::getFirst).collect(Collectors.joining(",")));
            }
            //?????????????????????
            detailsWithBatchAndNotAssociated.forEach(detail -> adaptor.$setBatchInfoId(detail, batch_material$id.get(Pair.of(adaptor.$getBatchNum(detail), adaptor.$getMaterial(detail).getId()))));
        }
    }


    /**
     * ??????????????????
     *
     * @param table            ??????
     * @param detailsWithBatch ??????
     * @param bizType          ????????????
     */
    private void _pushbackBatchInfo(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> detailsWithBatch, String bizType) {
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
        if (adaptor != null) {
            detailsWithBatch.forEach(detail -> {
                Long batchInfoId = adaptor.$getBatchInfoId(detail);
                if (batchInfoId != null) {
                    //???????????????????????????????????????????????????????????????
                    Date storageDate = adaptor.getStorageDate(table);
                    Date produceDate = adaptor.$getProduceDate(detail);
                    Date purchaseDate = adaptor.$getPurchaseDate(detail);
                    if (storageDate != null || produceDate != null || purchaseDate != null) {
                        List<String> conditions = new LinkedList<>();
                        List<Object> params = new LinkedList<>();
                        if (storageDate != null) {
                            conditions.add("AVAILABLE_DATE=?");
                            params.add(storageDate);
                            conditions.add("IN_STORE_DATE=?");
                            params.add(storageDate);
                        }
                        if (produceDate != null) {
                            conditions.add("PRODUCTION_DATE=?");
                            params.add(produceDate);
                        }
                        if (purchaseDate != null) {
                            conditions.add("PURCH_DATE=?");
                            params.add(purchaseDate);
                        }
                        params.add(batchInfoId);
                        Dbs.execute(
                                "UPDATE " + MaterialBatchInfo.TABLE_NAME + " SET " + String.join(",", conditions) + " WHERE ID=?",
                                params.toArray()
                        );
                    }
                }
            });
        }
    }

}
