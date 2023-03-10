package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity;
import com.supcon.orchid.ec.services.DataGridService;
import com.supcon.orchid.fooramework.support.ModuleRequestContext;
import com.supcon.orchid.fooramework.util.*;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.signal.SignalManager;
import com.supcon.orchid.material.services.MaterialBatchDealService;
import com.supcon.orchid.material.services.MaterialBusinessDetailService;
import com.supcon.orchid.material.services.MaterialQrDetailInfoService;
import com.supcon.orchid.material.superwms.config.MaterialSystemConfig;
import com.supcon.orchid.material.superwms.constants.QCSInspect;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseBatchType;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseDealState;
import com.supcon.orchid.fooramework.services.PlatformWorkflowService;
import com.supcon.orchid.material.superwms.services.TableOutboundService;
import com.supcon.orchid.material.superwms.util.MaterialExceptionThrower;
import com.supcon.orchid.material.superwms.util.StockOperator;
import com.supcon.orchid.material.superwms.util.adptor.AdaptorCenter;
import com.supcon.orchid.material.superwms.util.adptor.BizTypeContext;
import com.supcon.orchid.material.superwms.util.adptor.InboundTypeAdaptor;
import com.supcon.orchid.material.superwms.util.adptor.OutboundTypeAdaptor;
import com.supcon.orchid.material.superwms.util.factories.BatchDealInfoFactory;
import com.supcon.orchid.material.superwms.util.factories.BusinessDetailFactory;
import com.supcon.orchid.orm.entities.IdEntity;
import com.supcon.orchid.services.BAPException;
import com.supcon.orchid.support.Result;
import com.supcon.orchid.utils.StringUtils;
import com.supcon.orchid.workflow.engine.entities.WorkFlowVar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TableOutboundServiceImpl implements TableOutboundService {

    @Autowired
    private MaterialBusinessDetailService businessDetailService;
    @Autowired
    private MaterialBatchDealService batchDealService;
    @Autowired
    private MaterialSystemConfig materialSystemConfig;
    @Autowired
    private PlatformWorkflowService workflowService;


    @Override
    public void standardOutboundEvent(String bizType, AbstractEcFullEntity table, WorkFlowVar workFlowVar) {
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
                SignalManager.propagate("OutboundCanceling", table, details);
                SignalManager.propagate(prefix + "Canceling", table, details);
                break;
            }
            case "reject": {
                SignalManager.propagate("OutboundRejecting", table, details);
                SignalManager.propagate(prefix + "Rejecting", table, details);
                break;
            }
            default: {
                if (workflowService.isFirstTransition(table.getDeploymentId(), workFlowVar.getOutcome())) {
                    //---------------------------------????????????------------------------------
                    SignalManager.propagate("OutboundFirstCommit", table, details);
                    SignalManager.propagate(prefix + "FirstCommit", table, details);
                    _onFirstCommit(table, details, bizType);
                }

                if (table.getStatus() == 99) {
                    //---------------------------------??????------------------------------
                    SignalManager.propagate("OutboundEffecting", table, details);
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
        SignalManager.propagate("OutboundFirstCommit", table, details);
        SignalManager.propagate(prefix + "FirstCommit", table, details);
        _onFirstCommit(table, details, bizType);
        //---------------------------------??????------------------------------
        SignalManager.propagate("OutboundEffecting", table, details);
        SignalManager.propagate(prefix + "Effecting", table, details);
        _onEffect(table, details, bizType);
    }


    @Override
    public void createOutboundTask(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        if (!details.isEmpty()) {
            SignalManager.propagate("GenerateOutboundTask", bizType, table, details);
        }
    }

    @Override
    public void solveOutbound(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        if (!details.isEmpty()) {
            OutboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getOutboundTypeAdaptor(bizType);
            //???????????????????????????
            details.forEach(detail -> adaptor.$setOutQuantity(detail, adaptor.$getApplyQuantity(detail)));
            //????????????
            _updateStock(table, details, bizType);
            //?????????????????????
            List<MaterialBusinessDetail> businessDetails = BusinessDetailFactory.getOutboundBizDetailList(table, details, bizType);
            businessDetails.forEach(businessDetail -> businessDetailService.saveBusinessDetail(businessDetail, null));
            //?????????????????????????????????
            List<MaterialBatchDeal> batchDeals = BatchDealInfoFactory.getOutboundDealInfoList(table, details, BaseDealState.EFFECT, bizType);
            batchDeals.forEach(batchDeal -> batchDealService.saveBatchDeal(batchDeal, null));
            //?????????????????????
            details.forEach(detail -> SignalManager.propagate("OutboundDetail", bizType, table, detail, adaptor.$getOutQuantity(detail)));
        }
    }

    @Override
    @Transactional
    public void deleteOutboundTable(String tableNo, String bizType) {
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
            SignalManager.propagate("DeleteOutboundTable", bizType, table, details);
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
                    ).setSourceEntity(bd).decreaseWriteOff(bd.getQuantity());
                    //???????????????????????????
                    Date transactDate = Dates.getDateWithoutTime(bd.getTrasactionDate());
                    Dbs.execute(
                            "UPDATE " + MaterialDaySettlement.TABLE_NAME + " SET DAY_SETTLEMENT=DAY_SETTLEMENT+?, TOTAL_OUT=TOTAL_OUT-? WHERE WARE=? AND GOOD=? AND PLACE_SET IS NULL AND SETTLEMENT_DATE>=?",
                            bd.getQuantity(), bd.getQuantity(), bd.getWare().getId(), bd.getGood().getId(), transactDate
                    );
                    if (placeId != null) {
                        Dbs.execute(
                                "UPDATE " + MaterialDaySettlement.TABLE_NAME + " SET DAY_SETTLEMENT=DAY_SETTLEMENT+?, TOTAL_OUT=TOTAL_OUT-? WHERE WARE=? AND GOOD=? AND PLACE_SET=? AND SETTLEMENT_DATE>=?",
                                bd.getQuantity(), bd.getQuantity(), bd.getWare().getId(), bd.getGood().getId(), placeId, transactDate
                        );
                    }
//                    int count = Dbs.count(MaterialStandingcrop.TABLE_NAME, " VALID = 1 AND GOOD = ? AND BATCH_TEXT = ? ", bd.getGood(), bd.getBatchText());
//                    if (count > 0) {
//                        //??????????????????
//                        Dbs.execute(
//                                "UPDATE " + MaterialQrDetailInfo.TABLE_NAME + " SET VALID = 1 SRC_ID = ? ", bd.getId()
//                        );
//                    }
                });
            }
            Set<String> batchNumSet = bds.stream().map(MaterialBusinessDetail::getBatchText).filter(Strings::valid).collect(Collectors.toSet());
            InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
            if (!batchNumSet.isEmpty()) {
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
            //???????????????????????????QCS???????????????

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

    private void _onFirstCommit(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        OutboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getOutboundTypeAdaptor(bizType);
        //??????????????????
        List<? extends AbstractEcPartEntity> detailsByPiece = details.stream()
                .filter(detail -> BaseBatchType.BY_PIECE.equals(Dbs.getProp(adaptor.$getMaterial(detail), BaseSetMaterial::getIsBatch, SystemCode::getId)))
                .collect(Collectors.toList());
        if (!detailsByPiece.isEmpty()) {
            String quanErrorBatch = detailsByPiece.stream().filter(detail -> BigDecimal.ONE.compareTo(adaptor.$getApplyQuantity(detail)) != 0).map(detail -> {
                String materialName = Dbs.getProp(adaptor.$getMaterial(detail), BaseSetMaterial::getName);
                String batchNum = adaptor.$getBatchNum(detail);
                return Strings.valid(batchNum) ? materialName + "/" + batchNum : null;
            }).filter(Objects::nonNull).collect(Collectors.joining(","));
            if (Strings.valid(quanErrorBatch)) {
                //????????????{0}???????????????????????????????????????1???
                MaterialExceptionThrower.by_piece_quantity_check(quanErrorBatch);
            }
        }
    }


    private void _onEffect(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        OutboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getOutboundTypeAdaptor(bizType);
        //????????????????????????????????????????????????????????????????????????
        boolean generateTask = Boolean.TRUE.equals(materialSystemConfig.getEnableTask()) && Boolean.TRUE.equals(Dbs.getProp(adaptor.getWarehouse(table), BaseSetWarehouse::getStoresetState));
        if (generateTask) {
            //????????????????????????????????????????????????????????????
            Elements.shunt(details, adaptor::$isGenTask, Collectors.toList()).consume((genDetails, othDetails) -> {
                //????????????????????????
                createOutboundTask(table, genDetails, bizType);
                //????????????
                solveOutbound(table, othDetails, bizType);
            });
        } else {
            //????????????
            solveOutbound(table, details, bizType);
        }
    }

    /**
     * ???????????????
     *
     * @param table   ??????
     * @param details ??????
     */
    private void _updateStock(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        OutboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getOutboundTypeAdaptor(bizType);
        details.forEach(detail -> {
            MaterialStandingcrop stock = Dbs.reload(adaptor.$getStock(detail));
            Assert.notNull(stock, "????????????????????????");
            StockOperator
                    .of(stock)
                    .setSourceEntity(detail)
                    .decrease(adaptor.$getApplyQuantity(detail));
        });
    }

}
