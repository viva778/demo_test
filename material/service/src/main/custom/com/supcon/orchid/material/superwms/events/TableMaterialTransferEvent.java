package com.supcon.orchid.material.superwms.events;

import com.alibaba.fastjson.JSON;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.fooramework.annotation.signal.SignalManager;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.PostSqlUpdater;
import com.supcon.orchid.material.services.MaterialBatchDealService;
import com.supcon.orchid.material.services.MaterialBusinessDetailService;
import com.supcon.orchid.material.services.MaterialInSingleDetailService;
import com.supcon.orchid.material.superwms.config.MaterialSystemConfig;
import com.supcon.orchid.material.superwms.constants.BizTypeCode;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseDealState;
import com.supcon.orchid.fooramework.services.PlatformWorkflowService;
import com.supcon.orchid.material.superwms.services.TableInboundService;
import com.supcon.orchid.material.superwms.services.TableOutboundService;
import com.supcon.orchid.material.superwms.util.StockOperator;
import com.supcon.orchid.material.superwms.util.factories.BatchDealInfoFactory;
import com.supcon.orchid.material.superwms.util.factories.BusinessDetailFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TableMaterialTransferEvent {

    @Autowired
    private PlatformWorkflowService workflowService;
    @Autowired
    private TableOutboundService outboundService;
    @Autowired
    private TableInboundService inboundService;

    //----------------------------------------------------------调拨单----------------------------------------------START
    @Signal("AllocationAfterSubmit")
    private void afterSubmitAllocation(MaterialAppropriation table) {
        List<MaterialAppDetail> details = Dbs.findByCondition(
                MaterialAppDetail.class,
                "VALID=1 AND APP_HEADER=?",
                table.getId()
        ).stream().map(PostSqlUpdater::getAutoWritableEntity).collect(Collectors.toList());
        switch (table.getWorkFlowVar().getOutcomeType()) {
            case "cancel": {
                SignalManager.propagate("AllocationCanceling", table, details);
                break;
            }
            case "reject": {
                SignalManager.propagate("AllocationRejecting", table, details);
                break;
            }
            default: {
                if (workflowService.isFirstTransition(table.getDeploymentId(), table.getWorkFlowVar().getOutcome())) {
                    //---------------------------------首次迁移------------------------------
                    SignalManager.propagate("AllocationFirstCommit", table, details);
                }
                if (table.getStatus() == 99) {
                    //---------------------------------生效------------------------------
                    SignalManager.propagate("AllocationEffecting", table, details);
                    this.onAllocationEffect(table, details);
                }
            }
        }
    }

    private void onAllocationEffect(MaterialAppropriation table, List<MaterialAppDetail> details) {
        //根据配置项和表头仓库货位状态判断是否需要生成任务
        boolean generateOutTask = Boolean.TRUE.equals(materialSystemConfig.getEnableTask()) && Dbs.getProp(table.getFromWare(), BaseSetWarehouse::getStoresetState);
        if (generateOutTask) {
            outboundService.createOutboundTask(table, details, BizTypeCode.ALLOCATION);
        } else {
            outboundService.solveOutbound(table, details, BizTypeCode.ALLOCATION);
        }
    }

    @Autowired
    private MaterialSystemConfig materialSystemConfig;

    @Signal("OutboundDetail")
    private void allocateOutboundOver(MaterialAppropriation table, MaterialAppDetail detail, List<?> outboundTaskDetails) {
        if (table != null) {
            if (outboundTaskDetails != null) {
                //如果由下架任务传递，说明上架物品需要根据下架任务生成，交给扩展包处理
                SignalManager.propagate("AllocationInByAllocationOutTask", table, detail, outboundTaskDetails);
            } else {
                //否则根据出库回填的明细决定如何上架
                boolean generateInTask = Boolean.TRUE.equals(materialSystemConfig.getEnableTask()) && Boolean.TRUE.equals(Dbs.getProp(table.getToWare(), BaseSetWarehouse::getStoresetState));
                if (generateInTask) {
                    inboundService.createInboundTask(table, Collections.singletonList(detail), BizTypeCode.ALLOCATION);
                } else {
                    inboundService.solveInbound(table, Collections.singletonList(detail), BizTypeCode.ALLOCATION);
                }
            }
        }
    }

    @Signal("AllocationSuperEdit")
    private void allocationSuperEdit(MaterialAppropriation table) {
        if (table.getStatus() != null && table.getStatus() == 99) {
            //获取表体详细
            List<MaterialAppDetail> details = Dbs.findByCondition(
                    MaterialAppDetail.class,
                    "VALID=1 AND APP_HEADER=?",
                    table.getId()
            ).stream().map(PostSqlUpdater::getAutoWritableEntity).collect(Collectors.toList());
            //---------------------------------首次迁移------------------------------
            SignalManager.propagate("AllocationFirstCommit", table, details);
            //---------------------------------生效------------------------------
            SignalManager.propagate("AllocationEffecting", table, details);
            this.onAllocationEffect(table, details);
        }
    }

    @Signal("StockIncreased")
    private void setStockNotAvailable(MaterialStandingcrop stock, MaterialAppDetail source) {
        if (source != null) {
            // 生产批次写入现存量
            String productionBatch = source.getProductionBatch();
            stock.setProductionBatch(productionBatch);
        }
    }


    //----------------------------------------------------------调拨单----------------------------------------------END


    //----------------------------------------------------------货位调整单----------------------------------------------START

    @Autowired
    private MaterialBusinessDetailService businessDetailService;
    @Autowired
    private MaterialBatchDealService batchDealService;

    @Signal("PlaceAdjustAfterSubmit")
    private void afterSubmitPlaceAdjust(MaterialPlaceAjustInfo table) {
        if (table.getStatus() == 99) {
            List<MaterialPlaceAjustPart> details = Dbs.findByCondition(
                    MaterialPlaceAjustPart.class,
                    "VALID=1 AND PLACE_AJUST_INFO=?",
                    table.getId()
            );
            onPlaceAdjustEffect(table, details);
        }
    }

    @Signal("PlaceAdjustSuperEdit")
    private void placeAdjustSuperEdit(MaterialPlaceAjustInfo table) {
        if (table.getStatus() != null && table.getStatus() == 99) {
            //获取表体详细
            List<MaterialPlaceAjustPart> details = Dbs.findByCondition(
                    MaterialPlaceAjustPart.class,
                    "VALID=1 AND PLACE_AJUST_INFO=?",
                    table.getId()
            );
            onPlaceAdjustEffect(table, details);
        }
    }

    private void onPlaceAdjustEffect(MaterialPlaceAjustInfo table, List<MaterialPlaceAjustPart> details) {
        //1.转移现存量
        details.forEach(detail -> {
            StockOperator fromOperator = StockOperator.of(Dbs.reload(detail.getOnhand()));
            StockOperator toOperator = StockOperator.of(
                    detail.getGood().getId(),
                    Dbs.getProp(detail.getOnhand(), MaterialStandingcrop::getBatchText),
                    table.getWare().getId(),
                    detail.getToPlaceSet().getId()
            );
            fromOperator.setSourceEntity(detail).decrease(detail.getAjustAmount());
            toOperator.setSourceEntity(detail).increase(detail.getAjustAmount());
        });
        //2.生成流水
        List<MaterialBusinessDetail> outBusinessDetails = BusinessDetailFactory.getOutboundBizDetailList(table, details, BizTypeCode.PLACE_ADJUST);
        List<MaterialBusinessDetail> inBusinessDetails = BusinessDetailFactory.getInboundBizDetailList(table, details, BizTypeCode.PLACE_ADJUST);
        outBusinessDetails.forEach(businessDetail -> businessDetailService.saveBusinessDetail(businessDetail, null));
        inBusinessDetails.forEach(businessDetail -> businessDetailService.saveBusinessDetail(businessDetail, null));
        //3.生成批次处理信息
        List<MaterialBatchDeal> outBatchDeals = BatchDealInfoFactory.getOutboundDealInfoList(table, details, BaseDealState.EFFECT, BizTypeCode.PLACE_ADJUST);
        List<MaterialBatchDeal> inBatchDeals = BatchDealInfoFactory.getInboundDealInfoList(table, details, BaseDealState.EFFECT, BizTypeCode.PLACE_ADJUST);
        outBatchDeals.forEach(batchDeal -> batchDealService.saveBatchDeal(batchDeal, null));
        inBatchDeals.forEach(batchDeal -> batchDealService.saveBatchDeal(batchDeal, null));
    }

    //----------------------------------------------------------货位调整单----------------------------------------------END


    //---------------------------------------------------------其他入库单----------------------------------------------START

    @Autowired
    private MaterialInSingleDetailService materialInSingleDetailService;


    @Signal("StockIncreased")
    private void setStockNotAvailableOther(MaterialStandingcrop stock, MaterialInSingleDetail source) {
        if (source != null) {
            // 生产批次写入现存量
            stock.setProductionBatch(source.getCharparama());
        }
    }
    //----------------------------------------------------------其他入库单----------------------------------------------END


}
