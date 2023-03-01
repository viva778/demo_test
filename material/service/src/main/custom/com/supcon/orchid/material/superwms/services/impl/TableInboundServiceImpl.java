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
     * 标准入库事件
     *
     * @param bizType     业务类型
     * @param table       单据
     * @param workFlowVar 工作流对象
     * @modify 1. 创建
     * 2. 修改: 生效时才考虑生成入库任务,驳回时不做任何处理 modify by yaoyao 2022-05-30
     */
    @Override
    public void standardInboundEvent(String bizType, AbstractEcFullEntity table, WorkFlowVar workFlowVar) {
        BizTypeContext context = AdaptorCenter.getBizTypeContext(bizType);
        String prefix = context.getPrefix();
        //获取表体详细
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
                    //---------------------------------首次迁移------------------------------
                    SignalManager.propagate("InboundFirstCommit", table, details);
                    SignalManager.propagate(prefix + "FirstCommit", table, details);
                    _onFirstCommit(table, details, bizType);
                }
                if (table.getStatus() == 99) {
                    //---------------------------------生效------------------------------
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
        //获取表体详细
        List<? extends AbstractEcPartEntity> details = Dbs.findByCondition(
                context.getDetailClass(),
                "VALID=1 AND " + context.getAssociatedColumn() + "=? ",
                table.getId()
        ).stream().map(PostSqlUpdater::getAutoWritableEntity).collect(Collectors.toList());
        //---------------------------------首次迁移------------------------------
        SignalManager.propagate("InboundFirstCommit", table, details);
        SignalManager.propagate(prefix + "FirstCommit", table, details);
        _onFirstCommit(table, details, bizType);
        //---------------------------------生效------------------------------
        SignalManager.propagate("InboundEffecting", table, details);
        SignalManager.propagate(prefix + "Effecting", table, details);
        _onEffect(table, details, bizType);
    }

    /**
     * 判断物品是否能被放入仓库
     *
     * @param warehouseId 仓库id
     * @param goodId      物品id
     * @return 是否能被放入仓库
     */
    @Override
    public boolean allowToStore(Long warehouseId, Long goodId) {
        switch (String.valueOf(materialSystemConfig.getStorageCheckType())) {
            case "ware": {
                //--------------------------仓库下设置物品存放-------------------------
                //查找仓库下允许存放的物品集合
                Set<Long> containMaterials = Dbs.stream(
                        "SELECT MATERIAL FROM " + BaseSetWareMater.TABLE_NAME + " WHERE VALID=1 AND WAREHOUSE=? ",
                        Long.class,
                        warehouseId
                ).collect(Collectors.toSet());
                //仓库不限制物料或仓库允许存放物料
                if (containMaterials.size() == 0 || containMaterials.contains(goodId)) {
                    return true;
                }
                //再查找仓库是否允许存放该物品类别
                return Dbs.exist(
                        BaseSetWareMaterClz.TABLE_NAME,
                        "VALID=1 AND WAREHOUSE=? AND MATERIAL_CLASS=(" +
                                "SELECT MATERIAL_CLASS FROM " + BaseSetMaterial.TABLE_NAME + " WHERE VALID = 1 AND ID=?" +
                                ")",
                        warehouseId, goodId
                );
            }
            case "good": {
                //--------------------------物品下设置仓库存放-------------------------
                Map<Long, Boolean> ware$allow = Dbs.binaryMap(
                        "SELECT l.WARE_HOUSE,s.SET_ALLOW FROM " + BaseSetMaterialWareList.TABLE_NAME + " l LEFT JOIN " + BaseSetMaterialWareSet.TABLE_NAME + " s ON l.MATERIAL_WARE_SET=s.ID WHERE l.VALID=1 AND s.VALID=1 AND s.MATERIAL=?",
                        Long.class, Boolean.class,
                        goodId
                );
                //未设置或设置了可存放
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
            //将申请量填到实际量
            details.forEach(detail -> adaptor.$setInQuantity(detail, adaptor.$getApplyQuantity(detail)));
            //生成现存量
            _updateStock(table, details, bizType);
            //创建流水并保存
            List<MaterialBusinessDetail> businessDetails = BusinessDetailFactory.getInboundBizDetailList(table, details, bizType);
            businessDetails.forEach(businessDetail -> businessDetailService.saveBusinessDetail(businessDetail, null));
            //创建批次处理信息并保存
            List<MaterialBatchDeal> batchDeals = BatchDealInfoFactory.getInboundDealInfoList(table, details, BaseDealState.EFFECT, bizType);
            batchDeals.forEach(batchDeal -> batchDealService.saveBatchDeal(batchDeal, null));
            //发出已入库信号
            details.forEach(detail -> SignalManager.propagate("InboundDetail", bizType, table, detail, adaptor.$getInQuantity(detail)));
        }
    }

    @Override
    public void createInboundTask(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        if (!details.isEmpty()) {
            //生成入库任务
            SignalManager.propagate("GenerateInboundTask", bizType, table, details);
        }
    }


    @Override
    @Transactional
    public void deleteInboundTable(String tableNo, String bizType) {
        if (StringUtils.isEmpty(tableNo)) {
            throw new BAPException("tableNo为空");
        }
        BizTypeContext context = AdaptorCenter.getBizTypeContext(bizType);
        AbstractEcFullEntity table = Dbs.load(
                context.getTableClass(),
                "VALID =1 AND STATUS <> 0 AND TABLE_NO=?",
                tableNo
        );
        if (table == null || table.getStatus() == null) {
            throw new BAPException("找不到单据,tableNo:" + tableNo);
        }
        if (table.getStatus() == 99) {
            //生效时，撤销数据
            //通知下游模块，数据被删除
            List<? extends AbstractEcPartEntity> details = Dbs.findByCondition(
                    context.getDetailClass(),
                    "VALID=1 AND " + context.getAssociatedColumn() + "=?",
                    table.getId()
            );
            SignalManager.propagate("DeleteInboundTable", bizType, table, details);
            //找到对应流水
            List<String> sDetailIds = details.stream().map(IdEntity::getId).map(Objects::toString).collect(Collectors.toList());
            List<MaterialBusinessDetail> bds = Dbs.findByCondition(
                    MaterialBusinessDetail.class,
                    "VALID=1 AND " + Dbs.inCondition("TABLE_BODYID", sDetailIds.size()),
                    sDetailIds.toArray()
            );
            if (!bds.isEmpty()) {
                bds.forEach(bd -> {
                    Long placeId = Optional.ofNullable(bd.getPlaceSet()).map(IdEntity::getId).orElse(null);
                    //删除流水
                    bd.setValid(false);
                    Dbs.save(bd);
                    //冲销现存量
                    StockOperator.of(
                            bd.getGood().getId(),
                            bd.getBatchText(),
                            bd.getWare().getId(),
                            placeId
                    ).setSourceEntity(bd).increaseWriteOff(bd.getQuantity());
                    //扣减流水后的日结算
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
                    //删除条码台账
                    Dbs.execute(
                            "UPDATE " + MaterialQrDetailInfo.TABLE_NAME + " SET VALID = 0 WHERE SRC_ID = ? ", bd.getId()
                    );
                });
            }
            Set<String> batchNumSet = bds.stream().map(MaterialBusinessDetail::getBatchText).filter(Strings::valid).collect(Collectors.toSet());
            if (!batchNumSet.isEmpty()) {
                InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
                //删除批次处理信息
                Dbs.execute(
                        "UPDATE " + MaterialBatchDeal.TABLE_NAME + " SET VALID = 0 WHERE DEAL_TABLE_NO =?", tableNo
                );
                //增加作废批次处理信息
//                List<MaterialBatchDeal> batchDeals = BatchDealInfoFactory.getInboundDealInfoList(table, details.stream().filter(detail -> batchNumSet.contains(adaptor.$getBatchNum(detail))).collect(Collectors.toList()), BaseDealState.CANCEL, bizType);
//                batchDeals.forEach(batchDeal -> batchDealService.saveBatchDeal(batchDeal, null));
            }
            //处理待打印列表
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
                                        log.error("明细id:{},无对应申请检验id", dto.getId());
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
                                    log.info("非质检相关单据。不删除相关质检单");
                                }
                            }
                    );
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                    log.info("非质检相关单据。不删除相关质检单");
                }
            });
        }

        //super edit进行作废
        _cancelBySuperEdit(table, context.getProcessKey(), null);
    }


    private void _cancelBySuperEdit(AbstractEcFullEntity table, String processKey, DataGridService dataGridService) {
        table.setStatus(0);
        Long deploymentId = workflowService.getCurrentDeploymentId(processKey);
        Assert.notNull(deploymentId, "找不到已发布工作流信息！");

        Object service = Entities.getEntityService(table.getClass());
        String mtdNameOfSuperEdit = "saveSuperEdit" + Entities.getEntityName(table.getClass());
        //调用superEdit
        Reflects.call(
                service, mtdNameOfSuperEdit,
                table, null, dataGridService, null, new boolean[]{false}
        );
    }

    /**
     * 处理提交“生效”时代码逻辑
     *
     * @param table   单据
     * @param details 单据明细
     * @param bizType 单据类型
     * @author zhuzhenbo
     * @date 2022-xx-xx
     * @modify 1.2022-xx-xx 新增 modify by zhuzhenbo
     */
    private void _onEffect(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
        //根据配置项和表头仓库货位状态判断是否需要生成任务
        boolean generateTask = Boolean.TRUE.equals(materialSystemConfig.getEnableTask()) && Boolean.TRUE.equals(Dbs.getProp(adaptor.getWarehouse(table), BaseSetWarehouse::getStoresetState));
        if (generateTask) {
            //再根据表体设置的是否需要生成任务字段分流
            Elements.shunt(details, adaptor::$isGenTask, Collectors.toList()).consume((genDetails, othDetails) -> {
                //生成下游出库任务
                createInboundTask(table, genDetails, bizType);
                //处理入库
                solveInbound(table, othDetails, bizType);
            });
        } else {
            //处理入库
            solveInbound(table, details, bizType);
        }
    }


    private void _onRejecting(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        BizTypeContext context = AdaptorCenter.getBizTypeContext(bizType);
        //如果带冲突检查，则驳回时需要删除批次信息
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
     * 处理首次迁移
     *
     * @param table   入库单据
     * @param details 入库明细
     * @param bizType 业务类型
     */
    private void _onFirstCommit(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
        //判断物料仓库是否匹配可存放
        details.stream().map(adaptor::$getMaterial).distinct().forEach(good -> {
            if (!allowToStore(adaptor.getWarehouse(table).getId(), good.getId())) {
                MaterialExceptionThrower.storage_not_allowed(Dbs.getProp(good, BaseSetMaterial::getName));
            }
        });
        List<? extends AbstractEcPartEntity> detailsWithBatch = details.stream()
                .filter(detail -> Strings.valid(adaptor.$getBatchNum(detail)))
                .collect(Collectors.toList());
        if (!detailsWithBatch.isEmpty()) {
            //校验按件数量
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
            //校验按件和当前现存量是否冲突，存在则报错
            modelBatchInfoService.checkBatchConflictByPiece(detailsByPiece.stream()
                    .map(detail -> Pair.of(adaptor.$getBatchNum(detail), adaptor.$getMaterial(detail).getId()))
                    .collect(Collectors.toList()));
            //创建批次信息，并过滤掉已有的批次，不进行重复创建
            List<? extends AbstractEcPartEntity> detailsWithBatchAndNotCreated = modelBatchInfoService.checkBatchRuleAndFilterExist(detailsWithBatch, adaptor::$getMaterial, adaptor::$getBatchNum, AdaptorCenter.getBizTypeContext(bizType).getInContext().isBatchConflictCheck());
            if (!detailsWithBatchAndNotCreated.isEmpty()) {
                List<MaterialBatchInfo> batchInfoList = BatchInfoFactory.inboundBatchInfoList(bizType, table, detailsWithBatchAndNotCreated);
                batchInfoList.forEach(batchInfo -> batchInfoService.saveBatchInfo(batchInfo, null));
                //标记批次id
                detailsWithBatchAndNotCreated.forEach(detail -> batchInfoList
                        .stream()
                        .filter(batchInfo -> adaptor.$getBatchNum(detail).equals(batchInfo.getBatchNum()) && adaptor.$getMaterial(detail).equals(batchInfo.getMaterialId()))
                        .findAny()
                        .ifPresent(batchInfo -> adaptor.$setBatchInfoId(detail, batchInfo.getId()))
                );
                Dbs.flush();
            }
            //校验并关联批次ID
            _associateBatchInfo(table, detailsWithBatch, bizType);
            //回填批次信息
            _pushbackBatchInfo(table, detailsWithBatch, bizType);
        }
        //生成待打印信息
        _generatePrintInfoList(table, details, bizType);
    }

    /**
     * 处理单据取消
     *
     * @param details 表体列表
     * @modify 取消不生成批次处理，只有带冲突检查的单据取消时才删除批次 zzb 22/06/30
     */
    private void _onCancel(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        BizTypeContext context = AdaptorCenter.getBizTypeContext(bizType);
        //如果带冲突检查，则驳回时需要删除批次信息
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
     * 校验批次被多物料关联，并关联批次id到详细表
     *
     * @param table            表头
     * @param detailsWithBatch 表体列表
     * @param bizType          业务类型
     */
    private void _associateBatchInfo(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> detailsWithBatch, String bizType) {
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
        //查找未关联批次信息的列表
        List<? extends AbstractEcPartEntity> detailsWithBatchAndNotAssociated = detailsWithBatch
                .stream()
                .filter(detail -> adaptor.$getBatchInfoId(detail) == null)
                .collect(Collectors.toList());
        if (!detailsWithBatchAndNotAssociated.isEmpty()) {
            //批次唯一模式
            if (BatchUniqueRule.BATCH.equals(materialSystemConfig.getBatchUniqueRule())) {
                Set<String> batchSet = detailsWithBatchAndNotAssociated.stream().map(adaptor::$getBatchNum).collect(Collectors.toSet());
                //查询原来这些批号对应的物料号
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
                //进行校验
                detailsWithBatchAndNotAssociated.stream().collect(Collectors.groupingBy(adaptor::$getBatchNum)).forEach((batchNum, groupDetails) -> {
                    Set<Long> existMaterialIds = batch$materials.get(batchNum);
                    List<Long> currentMaterialIds = groupDetails.stream().map(adaptor::$getMaterial).filter(Objects::nonNull).map(IdEntity::getId).distinct().collect(Collectors.toList());
                    if (currentMaterialIds.size() > 1 || (existMaterialIds != null && !existMaterialIds.isEmpty() && !existMaterialIds.contains(currentMaterialIds.get(0)))) {
                        //当前批号对应物料超过1，或者 原来此批号已存在，但非此物料时，进行报错
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
            //寻找未创建批次信息的批号
            Set<Pair<String, Long>> batchMaterialSetWithoutInfo = Elements.difference(batch$material, batch_material$id.keySet());
            if (!batchMaterialSetWithoutInfo.isEmpty()) {
                //进行报错
                MaterialExceptionThrower.batch_not_existed(batchMaterialSetWithoutInfo.stream().map(Pair::getFirst).collect(Collectors.joining(",")));
            }
            //进行关联并保存
            detailsWithBatchAndNotAssociated.forEach(detail -> adaptor.$setBatchInfoId(detail, batch_material$id.get(Pair.of(adaptor.$getBatchNum(detail), adaptor.$getMaterial(detail).getId()))));
        }
    }


    /**
     * 回填批次信息
     *
     * @param table            单据
     * @param detailsWithBatch 表体
     * @param bizType          业务类型
     */
    private void _pushbackBatchInfo(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> detailsWithBatch, String bizType) {
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
        if (adaptor != null) {
            detailsWithBatch.forEach(detail -> {
                Long batchInfoId = adaptor.$getBatchInfoId(detail);
                if (batchInfoId != null) {
                    //回填批次信息的生产日期、可用日期、采购日期
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
