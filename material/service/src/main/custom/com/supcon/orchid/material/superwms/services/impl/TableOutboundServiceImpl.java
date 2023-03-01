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
        //获取表体详细
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
                    //---------------------------------首次迁移------------------------------
                    SignalManager.propagate("OutboundFirstCommit", table, details);
                    SignalManager.propagate(prefix + "FirstCommit", table, details);
                    _onFirstCommit(table, details, bizType);
                }

                if (table.getStatus() == 99) {
                    //---------------------------------生效------------------------------
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
        //获取表体详细
        List<? extends AbstractEcPartEntity> details = Dbs.findByCondition(
                context.getDetailClass(),
                "VALID=1 AND " + context.getAssociatedColumn() + "=? ",
                table.getId()
        ).stream().map(PostSqlUpdater::getAutoWritableEntity).collect(Collectors.toList());
        //---------------------------------首次迁移------------------------------
        SignalManager.propagate("OutboundFirstCommit", table, details);
        SignalManager.propagate(prefix + "FirstCommit", table, details);
        _onFirstCommit(table, details, bizType);
        //---------------------------------生效------------------------------
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
            //将申请量填到实际量
            details.forEach(detail -> adaptor.$setOutQuantity(detail, adaptor.$getApplyQuantity(detail)));
            //更新库存
            _updateStock(table, details, bizType);
            //创建流水并保存
            List<MaterialBusinessDetail> businessDetails = BusinessDetailFactory.getOutboundBizDetailList(table, details, bizType);
            businessDetails.forEach(businessDetail -> businessDetailService.saveBusinessDetail(businessDetail, null));
            //创建批次处理信息并保存
            List<MaterialBatchDeal> batchDeals = BatchDealInfoFactory.getOutboundDealInfoList(table, details, BaseDealState.EFFECT, bizType);
            batchDeals.forEach(batchDeal -> batchDealService.saveBatchDeal(batchDeal, null));
            //发出已出库信号
            details.forEach(detail -> SignalManager.propagate("OutboundDetail", bizType, table, detail, adaptor.$getOutQuantity(detail)));
        }
    }

    @Override
    @Transactional
    public void deleteOutboundTable(String tableNo, String bizType) {
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
            SignalManager.propagate("DeleteOutboundTable", bizType, table, details);
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
                    ).setSourceEntity(bd).decreaseWriteOff(bd.getQuantity());
                    //增加流水后的日结算
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
//                        //还原条码台账
//                        Dbs.execute(
//                                "UPDATE " + MaterialQrDetailInfo.TABLE_NAME + " SET VALID = 1 SRC_ID = ? ", bd.getId()
//                        );
//                    }
                });
            }
            Set<String> batchNumSet = bds.stream().map(MaterialBusinessDetail::getBatchText).filter(Strings::valid).collect(Collectors.toSet());
            InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
            if (!batchNumSet.isEmpty()) {
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
            //设置事务结束后调用QCS，避免死锁

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

    private void _onFirstCommit(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        OutboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getOutboundTypeAdaptor(bizType);
        //校验按件数量
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
                //物品批号{0}已开启按件管理，数量只能为1。
                MaterialExceptionThrower.by_piece_quantity_check(quanErrorBatch);
            }
        }
    }


    private void _onEffect(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        OutboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getOutboundTypeAdaptor(bizType);
        //根据配置项和表头仓库货位状态判断是否需要生成任务
        boolean generateTask = Boolean.TRUE.equals(materialSystemConfig.getEnableTask()) && Boolean.TRUE.equals(Dbs.getProp(adaptor.getWarehouse(table), BaseSetWarehouse::getStoresetState));
        if (generateTask) {
            //再根据表体设置的是否需要生成任务字段分流
            Elements.shunt(details, adaptor::$isGenTask, Collectors.toList()).consume((genDetails, othDetails) -> {
                //生成下游出库任务
                createOutboundTask(table, genDetails, bizType);
                //处理出库
                solveOutbound(table, othDetails, bizType);
            });
        } else {
            //处理出库
            solveOutbound(table, details, bizType);
        }
    }

    /**
     * 更新现存量
     *
     * @param table   表头
     * @param details 表体
     */
    private void _updateStock(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType) {
        OutboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getOutboundTypeAdaptor(bizType);
        details.forEach(detail -> {
            MaterialStandingcrop stock = Dbs.reload(adaptor.$getStock(detail));
            Assert.notNull(stock, "找不到对应现存量");
            StockOperator
                    .of(stock)
                    .setSourceEntity(detail)
                    .decrease(adaptor.$getApplyQuantity(detail));
        });
    }

}
