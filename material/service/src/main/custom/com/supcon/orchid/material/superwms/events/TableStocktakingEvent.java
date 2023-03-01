package com.supcon.orchid.material.superwms.events;

import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.fooramework.util.ArrayOperator;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.RequestCaches;
import com.supcon.orchid.material.services.MaterialBusinessDetailService;
import com.supcon.orchid.material.services.MaterialStjDetailService;
import com.supcon.orchid.material.services.MaterialStocktakingJobService;
import com.supcon.orchid.material.superwms.constants.systemcode.StockState;
import com.supcon.orchid.fooramework.services.PlatformWorkflowService;
import com.supcon.orchid.material.superwms.services.TableStocktakingService;
import com.supcon.orchid.material.superwms.util.MaterialExceptionThrower;
import com.supcon.orchid.material.superwms.util.StockOperator;
import com.supcon.orchid.material.superwms.util.factories.BusinessDetailFactory;
import com.supcon.orchid.orm.entities.IdEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TableStocktakingEvent {

    @Autowired
    private PlatformWorkflowService workflowService;
    @Autowired
    private MaterialStocktakingJobService stocktakingJobService;
    @Autowired
    private MaterialStjDetailService stjDetailService;
    @Autowired
    private TableStocktakingService stocktakingService;

    @Signal("StocktakingAfterSubmit")
    private void afterSubmit(MaterialStocktaking stocktaking){
        switch (String.valueOf(stocktaking.getWorkFlowVar().getOutcomeType())){
            case "reject":{
                //解锁现存量
                unlockStockState(stocktaking);
                //删除盘点任务
                Dbs.execute(
                        "UPDATE "+MaterialStocktakingJob.TABLE_NAME+" SET VALID=0,DELETE_TIME=? WHERE STOCKTAKING=?",
                        new Date(),stocktaking.getId()
                );
                break;
            }
            case "cancel":
                break;
            default:{
                if(workflowService.isFirstTransition(stocktaking.getDeploymentId(),stocktaking.getWorkFlowVar().getOutcome())){
                    //锁现存量状态
                    lockStockState(stocktaking);
                    //创建任务
                    generateStocktakingJob(stocktaking);
                }
                if(stocktaking.getStatus()==99){
                    //自动提交跳过校验
                    if(!Boolean.TRUE.equals(RequestCaches.get("__auto_submit__"))){
                        //生效前校验任务完成
                        ///查找盘点次数为0的任务
                        List<Long> remainDistributionIds = Dbs.stream(
                                "SELECT DISTRIBUTION FROM "+MaterialStjDetail.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING_JOB IN(SELECT ID FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING=?) AND TAKING_COUNT=0",
                                Long.class,
                                stocktaking.getId()
                        ).collect(Collectors.toList());
                        if(!remainDistributionIds.isEmpty()){
                            ///提示不允许提交并显示待办人
                            MaterialExceptionThrower.stocktaking_job_remain_submit_forbidden(
                                    Dbs.stream(
                                            "SELECT DISTINCT NAME FROM "+ Staff.TABLE_NAME+" WHERE ID IN(SELECT STAFF FROM "+ MaterialStMultiStaff.TABLE_NAME +" WHERE VALID=1 AND "+Dbs.inCondition("DISTRIBUTION",remainDistributionIds.size())+")",
                                            String.class,
                                            remainDistributionIds.toArray()
                                    ).collect(Collectors.joining(","))
                            );
                        } else {
                            ///查找保存过但未提交数据的处理人
                            List<Long> remainStaffIds = Dbs.stream(
                                    "SELECT DISTINCT CREATE_STAFF_ID FROM "+MaterialStjStockRecord.TABLE_NAME+" WHERE VALID=1 AND SUBMIT_RECORD IS NULL AND STOCKTAKING_JOB IN(SELECT ID FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING=?)",
                                    Long.class,
                                    stocktaking.getId()
                            ).collect(Collectors.toList());
                            if(!remainStaffIds.isEmpty()){
                                ///提示不允许提交并显示待办人
                                MaterialExceptionThrower.stocktaking_job_remain_submit_forbidden(
                                        Dbs.stream(
                                                "SELECT NAME FROM "+ Staff.TABLE_NAME+" WHERE "+Dbs.inCondition("ID",remainStaffIds.size()),
                                                String.class,
                                                remainStaffIds.toArray()
                                        ).collect(Collectors.joining(","))
                                );
                            }
                        }
                    }
                    //解锁现存量状态
                    unlockStockState(stocktaking);
                    //根据盘点任务更改现存量
                    reviseStock(stocktaking);
                }
            }
        }
    }

    @Autowired
    private MaterialBusinessDetailService businessDetailService;

    //校对现存量
    private void reviseStock(MaterialStocktaking stocktaking){
        //获取现存量盘点记录
        List<MaterialStjStockRecord> records = stocktakingService.findFinalStockRecordByStocktakingId(stocktaking.getId());
        //根据每条记录，查询当前现存量并更新偏移值
        records.forEach(record->{
            //不直接使用QuantityOffset，因为这条字段是前台计算的存在误差
            BigDecimal offset = record.getQuantityByCount().subtract(record.getQuantityOnBook());
            if(!BigDecimal.ZERO.equals(offset)){
                StockOperator.of(
                        record.getMaterial().getId(),
                        Dbs.getProp(record.getBatchInfo(),MaterialBatchInfo::getBatchNum),
                        stocktaking.getWarehouse().getId(),
                        Optional.ofNullable(record.getPlace()).map(IdEntity::getId).orElse(null)
                ).setSourceEntity(record).offset(offset);
                //生成流水
                MaterialBusinessDetail bizDetail = BusinessDetailFactory.getStocktakingBizDetail(stocktaking,record);
                if(null!=bizDetail) {
                    businessDetailService.saveBusinessDetail(bizDetail, null);
                }
            }

        });


    }


    //生成任务
    private void generateStocktakingJob(MaterialStocktaking stocktaking){
        List<MaterialStDistribution> distributions = Dbs.findByCondition(
                MaterialStDistribution.class,
                "VALID=1 AND STOCKTAKING=?",
                stocktaking.getId()
        );
        MaterialStocktakingJob stocktakingJob = new MaterialStocktakingJob();
        stocktakingJob.setStocktaking(stocktaking);
        List<MaterialStjDetail> stjDetails = distributions.stream().map(distribution->{
            MaterialStjDetail stjDetail = new MaterialStjDetail();
            stjDetail.setDistribution(distribution);
            stjDetail.setTakingCount(0);
            return stjDetail;
        }).collect(Collectors.toList());
        RequestCaches.set("__auto_save__",true);
        stocktakingJobService.saveStocktakingJob(stocktakingJob,()-> stjDetails.forEach(stjDetail->{
            stjDetail.setStocktakingJob(stocktakingJob);
            stjDetailService.saveStjDetail(stjDetail,null);
        }));
        RequestCaches.set("__auto_save__",false);
    }

    //锁现存量状态
    private void lockStockState(MaterialStocktaking stocktaking){
        //查出对应现存量
        List<MaterialStandingcrop> stocks = stocktakingService.findStocksByStocktakingIdAndStaffId(stocktaking.getId(), null,null,
                "id," +
                        "good.id,materBatchInfo.id,placeSet.id,ware.id," +
                        "batchText,availiQuantity,frozenQuantity,onhand,stockState");
        stocks.forEach(stock->{
            //更新盘点状态
            StockOperator.of(stock).setSourceEntity(stock).update(s->{
                if(s.getStockState()!=null){
                    if(s.getStockState().contains(StockState.STOCKTAKING)){
                        //状态已为盘点中，检索与之冲突的盘点单并提示
                        Long stocktakingId = stocktakingService.findStocktakingIdByStock(stock);
                        if(stocktakingId!=null){
                            String tableNo = Dbs.first(
                                    "SELECT TABLE_NO FROM "+MaterialStocktaking.TABLE_NAME+" WHERE ID=?",
                                    String.class,stocktakingId
                            );
                            MaterialExceptionThrower.stocktaking_range_conflict(tableNo);
                        }
                    } else {
                        s.setStockState(s.getStockState()+","+ StockState.STOCKTAKING);
                    }
                } else {
                    s.setStockState(StockState.STOCKTAKING);
                }
            });
        });
    }

    //解锁现存量状态
    private void unlockStockState(MaterialStocktaking stocktaking){
        //查出对应现存量
        List<MaterialStandingcrop> stocks = stocktakingService.findStocksByStocktakingIdAndStaffId(stocktaking.getId(), null, null,
                "id," +
                        "good.id,materBatchInfo.id,placeSet.id,ware.id," +
                        "batchText,availiQuantity,frozenQuantity,onhand,stockState");
        stocks.forEach(stock->{
            //更新盘点状态
            StockOperator.of(stock).setSourceEntity(stock).update(s->{
                if(s.getStockState()!=null){
                    if(s.getStockState().equals(StockState.STOCKTAKING)){
                        s.setStockState(null);
                    } else {
                        //删除掉最后一个盘点状态
                        s.setStockState(String.join(",",
                                ArrayOperator.of(s.getStockState().split(",")).removeLast(StockState.STOCKTAKING::equals).get()
                        ));
                    }
                }
            });
        });
    }


    //现存量处于盘点状态时，不允许操作
    @Signal({"StockFroze","StockRestored","StockDecreased"})
    private void checkStocktakingState(MaterialStandingcrop stock){
        if(stock.getStockState()!=null&&stock.getStockState().contains(StockState.STOCKTAKING)){
            MaterialExceptionThrower.operate_after_stocktaking_completed();
        }
    }

    @Signal("StockIncreased")
    private void checkStockIncrease(MaterialStandingcrop stock){
        if(stock.getId()!=null){
            //如果现存量历史存在
            checkStocktakingState(stock);
        } else {
            //如果现存量不存在，则没有打盘点状态标记，需要手动检查盘点范围
            Long stocktakingId = stocktakingService.findStocktakingIdByStock(stock);
            if(stocktakingId!=null){
                //现存量处于盘点状态时，不允许操作
                MaterialExceptionThrower.operate_after_stocktaking_completed();
            }
        }
    }
}
