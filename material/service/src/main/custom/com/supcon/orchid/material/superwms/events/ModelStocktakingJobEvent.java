package com.supcon.orchid.material.superwms.events;

import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.fooramework.util.*;
import com.supcon.orchid.material.services.MaterialStjSubmitRecService;
import com.supcon.orchid.material.superwms.util.MaterialExceptionThrower;
import com.supcon.orchid.orm.entities.IdEntity;
import lombok.Data;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class ModelStocktakingJobEvent {

    @Data
    public static class $operate_type_holder {
        private String operateType;
    }

    @Autowired
    private MaterialStjSubmitRecService submitRecService;

    @Signal("StocktakingJobBeforeSave")
    private void beforeSave(MaterialStocktakingJob stocktakingJob){
        //校验上游单据是否还处于编辑
        Integer status = Dbs.getProp(stocktakingJob.getStocktaking(), AbstractEcFullEntity::getStatus);
        if(status!=null&&status!=88){
            //盘点任务已结束
            MaterialExceptionThrower.stocktaking_job_finished_already();
        }
    }

    @Signal("StocktakingJobAfterSave")
    private void afterSave(MaterialStocktakingJob stocktakingJob){
        if(!Boolean.TRUE.equals(RequestCaches.get("__auto_save__"))){
            //获取操作类型
            $operate_type_holder holder = Jacksons.readValue(
                    RequestCaches.get("body"),
                    $operate_type_holder.class
            );
            if("submit".equals(holder.operateType)){
                //生成盘点记录
                MaterialStjSubmitRec submitRec = new MaterialStjSubmitRec();
                submitRec.setStocktakingJob(stocktakingJob);
                submitRec.setSubmitPerson(Organazations.getCurrentStaff());
                submitRec.setSubmitTime(new Date());
                submitRecService.saveStjSubmitRec(submitRec,null);
                //得到本次提交数据
                List<MaterialStjStockRecord> records = Hbs.findByCriteriaWithProjections(
                        MaterialStjStockRecord.class,
                        "id,stockKey",
                        Restrictions.eq("stocktakingJob",stocktakingJob),
                        Restrictions.eq("createStaffId",Organazations.getCurrentStaffId())
                );
                //涉及现存量明细 关联提交记录，且更新为结算项
                Dbs.execute(
                        "UPDATE "+MaterialStjStockRecord.TABLE_NAME+" SET SUBMIT_RECORD=?,CHECKED=1 WHERE VALID=1 AND "+Dbs.inCondition("ID",records.size()),
                        Elements.toArray(submitRec.getId(),records.stream().map(IdEntity::getId))
                );
                //将其他和本次提交相同stockKey的记录取消结算项
                Dbs.execute(
                        "UPDATE "+MaterialStjStockRecord.TABLE_NAME+" SET CHECKED=0 WHERE VALID=1 AND STOCKTAKING_JOB=? AND "+Dbs.inCondition("STOCK_KEY",records.size())+" AND "+Dbs.notInCondition("ID",records.size()),
                        Elements.toArray(stocktakingJob.getId(),records.stream().map(MaterialStjStockRecord::getStockKey),records.stream().map(IdEntity::getId))
                );
                //涉及明细盘点次数+1
                Dbs.execute(
                        "UPDATE "+ MaterialStjDetail.TABLE_NAME+" SET TAKING_COUNT=TAKING_COUNT+1 WHERE VALID=1 AND STOCKTAKING_JOB=? AND DISTRIBUTION IN(SELECT DISTRIBUTION FROM "+ MaterialStMultiStaff.TABLE_NAME +" WHERE STAFF=?)",
                        stocktakingJob.getId(),Organazations.getCurrentStaffId()
                );
            }
        }
    }
}
