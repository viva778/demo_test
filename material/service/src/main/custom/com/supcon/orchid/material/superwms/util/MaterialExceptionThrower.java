package com.supcon.orchid.material.superwms.util;

import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.services.BAPException;

public class MaterialExceptionThrower {

    /**
     * 现存量不足
     * @param batchText 批次号
     */
    public static void stock_insufficient(String batchText) throws BAPException {
        if(Strings.valid(batchText)){
            //批号：XXX 现存量不足
            throw new BAPException(
                    InternationalResource.get("material.businessDetail.BusinessDetail.batchText")
                            +":"+batchText+" "
                            + InternationalResource.get("material.custom.BatchNumInsufficientStock")
            );
        } else {
            //现存量不足
            throw new BAPException(InternationalResource.get("material.custom.BatchNumInsufficientStock"));
        }
    }

    /**
     * 现存量不足
     */
    public static void stock_insufficient() throws BAPException {
        throw new BAPException("material.custom.BatchNumInsufficientStock");
    }

    /**
     * 可用量不足，无合适批次
     */
    public static void available_quantity_insufficient() throws BAPException {
        throw new BAPException("material.custom.random1628228493739");
    }

    /**
     * 批次已存在
     * @param batchText 批次号
     */
    public static void batch_existed(String batchText) throws BAPException {
        throw new BAPException(InternationalResource.get("material.custom.batchInfo.batchExist", (Object) batchText));
    }

    /**
     * 出库任务未完成
     */
    public static void outbound_task_incomplete() throws BAPException {
        throw new BAPException("material.custom.OutboundTaskNotCompleted");
    }

    /**
     * 入库任务未完成
     */
    public static void inbound_task_incomplete() throws BAPException {
        throw new BAPException("material.custom.NotDirectlyEffective");
    }


    /**
     * {0}物料未被允许存放在当前仓库！
     */
    public static void storage_not_allowed(String goodName) throws BAPException {
        throw new BAPException(InternationalResource.get("material.custom.warehouseProhibitToDeposit",(Object)goodName));
    }
    /**
     * 物品已启用批次，批号不能为空!
     */
    public static void batch_text_required() throws BAPException {
        throw new BAPException(InternationalResource.get("material.custom.randon1573003779313"));
    }

    /**
     * 设备模块未启动
     */
    public static void eam_disable() throws BAPException {
        throw new BAPException("material.custom.random1631952863746");
    }


    /**
     * 设备模块发生异常
     */
    public static void eam_error(String message) throws BAPException {
        throw new BAPException("调用设备模块发生异常："+message);
    }

    /**
     * 备件管理未启动
     */
    public static void spare_mgmt_disable() throws BAPException {
        throw new BAPException(InternationalResource.get("material.custom.moduleIsNotPublished", (Object) "SpareManage"));
    }

    /**
     * {0}违反唯一性约束！
     */
    public static void uniqueness_constraint(String identifier) throws BAPException {
        throw new BAPException(InternationalResource.get("material.custom.violationOfUniquenessConstraint", (Object) identifier));
    }

    /**
     * 「{0} - {1}」下存在有效现存量，不允许删除！
     */
    public static void ware_model_cannot_delete(String modelType,String modelName) throws BAPException {
        throw new BAPException(InternationalResource.get("material.custom.random1646099155194", (Object) modelType, modelName));
    }

    /**
     * 批次不存在
     * @param batchText 批次号
     */
    public static void batch_not_existed(String batchText) throws BAPException {
        throw new BAPException(InternationalResource.get("material.custom.batchInfo.batchNotExist", (Object) batchText));
    }

    /**
     * 同批次不能被不同物料引用
     * @param batchText 批次号
     */
    public static void cannot_refer_by_diff_material(String batchText) throws BAPException {
        throw new BAPException(InternationalResource.get("material.batch.cannot_refered_by_diff_material", (Object) batchText));
    }

    /**
     * 按件管理下的物料{0}不允许重复相同批次
     * @param batchText 批次号
     */
    public static void piece_batch_cannot_repeat(String batchText) throws BAPException {
        throw new BAPException(InternationalResource.get("material.custom.random1658991103082", (Object) batchText));
    }


    /**
     * 物料批号{0}已开启按件管理，现存量只能为1
     */
    public static void piece_batch_stock_only_can_be_one(String batchText) throws BAPException {
        throw new BAPException(InternationalResource.get("material.custom.random1658992085891", (Object) ("["+batchText+"]")));
    }

    /**
     * 物品批号{0}已开启按件管理，数量只能为1。
     */
    public static void by_piece_quantity_check(String batchText) throws BAPException {
        throw new BAPException(InternationalResource.get("material.validator.by_piece_quantity_check", (Object) batchText));
    }

    /**
     * 存在盘点任务未完成，不允许提交，待办人:
     */
    public static void stocktaking_job_remain_submit_forbidden(String pendingPersonList) throws BAPException {
        throw new BAPException(InternationalResource.get("material.stocktaking.stocktaking_job_remain_submit_forbidden", (Object) pendingPersonList));
    }

    /**
     * 盘点任务已结束
     */
    public static void stocktaking_job_finished_already() throws BAPException {
        throw new BAPException(InternationalResource.get("material.stocktaking.stocktaking_job_finished_already"));
    }

    /**
     * 盘点任务未开始
     */
    public static void stocktaking_job_not_start_yet() throws BAPException {
        throw new BAPException(InternationalResource.get("material.stocktaking.stocktaking_job_not_start_yet"));
    }

    /**
     * 现存量盘点中，请等待盘点结束后操作
     */
    public static void operate_after_stocktaking_completed() throws BAPException {
        throw new BAPException(InternationalResource.get("material.stocktaking.operate_after_completed"));
    }

    /**
     * 盘点范围与单据{0}冲突，请重新选择范围
     */
    public static void stocktaking_range_conflict(String tableNo) throws BAPException {
        throw new BAPException(InternationalResource.get("material.stocktaking.stocktaking_range_conflict", (Object) tableNo));
    }

    /**
     * 盘点对象{0}正在盘点中，不需要下达复盘
     */
    public static void stocktaking_recheck_unnecessary(String targetNames) throws BAPException {
        throw new BAPException(InternationalResource.get("material.stocktaking.stocktaking_recheck_unnecessary", (Object) targetNames));
    }

}
