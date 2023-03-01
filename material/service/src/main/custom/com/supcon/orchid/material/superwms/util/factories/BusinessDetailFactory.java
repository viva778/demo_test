package com.supcon.orchid.material.superwms.util.factories;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetUnit;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity;
import com.supcon.orchid.foundation.entities.Department;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.staticautowired.StaticAutowired;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.*;
import com.supcon.orchid.material.superwms.constants.BizTypeCode;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseBatchType;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseRedBlue;
import com.supcon.orchid.material.superwms.constants.systemcode.OperateDirection;
import com.supcon.orchid.material.superwms.services.ModelBatchInfoService;
import com.supcon.orchid.material.superwms.services.ModelBizTypeService;
import com.supcon.orchid.material.superwms.util.adptor.AdaptorCenter;
import com.supcon.orchid.material.superwms.util.adptor.InboundTypeAdaptor;
import com.supcon.orchid.material.superwms.util.adptor.OutboundTypeAdaptor;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class BusinessDetailFactory {

    @StaticAutowired
    private static ModelBizTypeService bizTypeService;
    @StaticAutowired
    private static ModelBatchInfoService batchInfoService;


    public static List<MaterialBusinessDetail> getInboundBizDetailList(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizTypeCode) {
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizTypeCode);
        MaterialServiceType bizType = bizTypeService.getBizType(bizTypeCode);
        MaterialStorageType storageType = adaptor.getStorageType(table);

        MaterialBusinessDetail sample = new MaterialBusinessDetail();
        sample.setTrasactionDate(new Date());
        sample.setWare(adaptor.getWarehouse(table));
        sample.setBillCode(table.getTableNo());
        sample.setBillInfoId(table.getTableInfoId());
        sample.setTableHeadID(table.getId().toString());
        sample.setCreatePerson(table.getCreateStaff());
        if (table.getCreateStaff() != null) {
            sample.setCreatePersonID(table.getCreateStaff().getId().toString());
            sample.setEffectPersonName(table.getCreateStaff().getName());
            sample.setEffectStaffId(table.getCreateStaff().getId());
        }
        sample.setEffectPerson(table.getCreateStaff());
        sample.setRedBlue(Optional.ofNullable(adaptor.getRedBlue(table)).map(SystemCode::new).orElse(null));
        sample.setCreateDate(table.getCreateTime());
        sample.setEffectDate(table.getEffectTime());
        sample.setServiceTypeID(bizType);
        sample.setServiceTypeCode(bizType.getServiceTypeCode());
        sample.setServiceTypeExplain(bizType.getServiceTypeExplain());
        sample.setDirection(new SystemCode(OperateDirection.RECEIVE));
        if (storageType != null) {
            sample.setStorageTypeID(storageType);
            sample.setStorageTypeCode(Dbs.getProp(storageType, MaterialStorageType::getReasonCode));
            sample.setStorageTypeExplain(Dbs.getProp(storageType, MaterialStorageType::getReasonExplain));
        }
        sample.setCreateDate(table.getCreateTime());
        sample.setDept(Optional.ofNullable(table.getCreateDepartment()).orElse(Organazations.getCurrentDepartment()));
        sample.setDeptCode(Dbs.getProp(table.getCreateDepartment(), Department::getCode));
        sample.setDeptName(Dbs.getProp(table.getCreateDepartment(), Department::getName));

        return details.stream().map(detail -> {
            BaseSetMaterial good = adaptor.$getMaterial(detail);
            MaterialBusinessDetail businessDetail = Beans.getCopy(sample);
            if (BaseBatchType.isEnable(Dbs.getProp(good, BaseSetMaterial::getIsBatch))) {
                businessDetail.setBatchText(adaptor.$getBatchNum(detail));
                Long batchId = batchInfoService.getBatchInfoId(adaptor.$getMaterial(detail), adaptor.$getBatchNum(detail));
                if (batchId != null) {
                    businessDetail.setMaterBatchInfo(Entities.ofId(MaterialBatchInfo.class, batchId));
                }
            }
            businessDetail.setTableBodyID(detail.getId().toString());
            businessDetail.setGood(good);
            businessDetail.setUnit(Dbs.getProp(good, BaseSetMaterial::getMainUnit, BaseSetUnit::getName));
            businessDetail.setQuantity(adaptor.$getInQuantity(detail));
            businessDetail.setBusinessMemo(adaptor.$getMemoField(detail));
            businessDetail.setMoneyTotal(adaptor.$getBill(detail));
            businessDetail.setPlaceSet(adaptor.$getPlace(detail));
            return businessDetail;
        }).collect(Collectors.toList());
    }

    //期初导入专用
    public static MaterialBusinessDetail getInitBizDetailList(MaterialMaterInitmian initData) {
        MaterialServiceType bizType = bizTypeService.getBizType(BizTypeCode.OTHER_STORAGE);
        MaterialStorageType storageType = bizTypeService.getStorageType("materInitial");

        MaterialBusinessDetail sample = new MaterialBusinessDetail();
        sample.setTrasactionDate(new Date());
        sample.setWare(initData.getWare());
        sample.setBillCode(initData.getTableNo());
        sample.setBillInfoId(initData.getTableInfoId());
        sample.setTableHeadID(initData.getId().toString());
        sample.setCreatePerson(initData.getCreateStaff());
        sample.setCreatePersonID(initData.getCreateStaff().getId().toString());
        sample.setCreateDate(initData.getCreateTime());
        sample.setEffectDate(initData.getEffectTime());
        sample.setServiceTypeID(bizType);
        sample.setServiceTypeCode(bizType.getServiceTypeCode());
        sample.setServiceTypeExplain(bizType.getServiceTypeExplain());
        sample.setDirection(bizType.getDirection());
        sample.setRedBlue(new SystemCode(BaseRedBlue.BLUE));
        if (storageType != null) {
            sample.setStorageTypeID(storageType);
            sample.setStorageTypeCode(Dbs.getProp(storageType, MaterialStorageType::getReasonCode));
            sample.setStorageTypeExplain(Dbs.getProp(storageType, MaterialStorageType::getReasonExplain));
        }
        sample.setCreateDate(initData.getCreateTime());
        sample.setDept(Optional.ofNullable(initData.getDept()).orElse(Organazations.getCurrentDepartment()));
        sample.setDeptCode(Dbs.getProp(initData.getDept(), Department::getCode));
        sample.setDeptName(Dbs.getProp(initData.getDept(), Department::getName));
        sample.setEffectPersonName(initData.getCreateStaff().getName());
        sample.setEffectStaffId(initData.getCreateStaff().getId());
        sample.setEffectPerson(initData.getCreateStaff());
        sample.setEffectDate(new Date());
        BaseSetMaterial good = initData.getGood();
        MaterialBusinessDetail businessDetail = Beans.getCopy(sample);
        if (BaseBatchType.isEnable(Dbs.getProp(good, BaseSetMaterial::getIsBatch))) {
            businessDetail.setBatchText(initData.getBatchNum());
            Long batchId = Dbs.first(
                    "SELECT ID FROM " + MaterialBatchInfo.TABLE_NAME + " WHERE VALID=1 AND BATCH_NUM=? ",
                    Long.class,
                    sample.getBatchText()
            );
            if (batchId != null) {
                businessDetail.setMaterBatchInfo(Entities.ofId(MaterialBatchInfo.class, batchId));
            }
        }
        businessDetail.setTableBodyID(initData.getId().toString());
        businessDetail.setGood(good);
        businessDetail.setUnit(Dbs.getProp(good, BaseSetMaterial::getMainUnit, BaseSetUnit::getName));
        businessDetail.setQuantity(initData.getOnhand());
        businessDetail.setPlaceSet(initData.getPlaceSet());
        return businessDetail;
    }

    public static MaterialBusinessDetail getInboundBizDetail(AbstractEcFullEntity table, AbstractEcPartEntity detail, MaterialBatchInfo batchInfo, BigDecimal quantity, String bizTypeCode) {
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizTypeCode);
        MaterialServiceType bizType = bizTypeService.getBizType(bizTypeCode);
        MaterialStorageType storageType = adaptor.getStorageType(table);

        MaterialBusinessDetail businessDetail = new MaterialBusinessDetail();
        businessDetail.setTrasactionDate(new Date());
        businessDetail.setWare(adaptor.getWarehouse(table));
        businessDetail.setBillCode(table.getTableNo());
        businessDetail.setBillInfoId(table.getTableInfoId());
        businessDetail.setTableHeadID(table.getId().toString());
        businessDetail.setTableBodyID(detail.getId().toString());
        businessDetail.setCreatePerson(table.getCreateStaff());
        businessDetail.setCreatePersonID(table.getCreateStaff().getId().toString());
        businessDetail.setRedBlue(Optional.ofNullable(adaptor.getRedBlue(table)).map(SystemCode::new).orElse(null));
        businessDetail.setCreateDate(table.getCreateTime());
        businessDetail.setEffectDate(table.getEffectTime());
        businessDetail.setServiceTypeID(bizType);
        businessDetail.setServiceTypeCode(bizType.getServiceTypeCode());
        businessDetail.setServiceTypeExplain(bizType.getServiceTypeExplain());
        businessDetail.setDirection(new SystemCode(OperateDirection.RECEIVE));
        if (storageType != null) {
            businessDetail.setStorageTypeID(storageType);
            businessDetail.setStorageTypeCode(Dbs.getProp(storageType, MaterialStorageType::getReasonCode));
            businessDetail.setStorageTypeExplain(Dbs.getProp(storageType, MaterialStorageType::getReasonExplain));
        }
        businessDetail.setCreateDate(table.getCreateTime());
        businessDetail.setDept(Optional.ofNullable(table.getCreateDepartment()).orElse(Organazations.getCurrentDepartment()));
        businessDetail.setDeptCode(Dbs.getProp(table.getCreateDepartment(), Department::getCode));
        businessDetail.setDeptName(Dbs.getProp(table.getCreateDepartment(), Department::getName));
        businessDetail.setEffectPersonName(table.getCreateStaff().getName());
        businessDetail.setEffectStaffId(table.getCreateStaff().getId());
        businessDetail.setEffectPerson(table.getCreateStaff());
        BaseSetMaterial good = adaptor.$getMaterial(detail);
        businessDetail.setMaterBatchInfo(batchInfo);
        businessDetail.setBatchText(Dbs.getProp(batchInfo, MaterialBatchInfo::getBatchNum));
        businessDetail.setGood(good);
        businessDetail.setUnit(Dbs.getProp(good, BaseSetMaterial::getMainUnit, BaseSetUnit::getName));
        businessDetail.setQuantity(quantity);
        businessDetail.setBusinessMemo(adaptor.$getMemoField(detail));
        businessDetail.setMoneyTotal(adaptor.$getBill(detail));
        businessDetail.setPlaceSet(adaptor.$getPlace(detail));
        return businessDetail;
    }

    public static List<MaterialBusinessDetail> getOutboundBizDetailList(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizTypeCode) {
        OutboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getOutboundTypeAdaptor(bizTypeCode);
        //表头数据
        MaterialServiceType bizType = bizTypeService.getBizType(bizTypeCode);
        MaterialStorageType storageType = adaptor.getStorageType(table);

        MaterialBusinessDetail sample = new MaterialBusinessDetail();

        sample.setTrasactionDate(new Date());

        sample.setCreatePerson(table.getCreateStaff());
        if (table.getCreateStaff() != null) {
            sample.setCreatePersonID(table.getCreateStaff().getId().toString());
            sample.setEffectPersonName(table.getCreateStaff().getName());
            sample.setEffectStaffId(table.getCreateStaff().getId());
        }
        sample.setEffectPerson(table.getCreateStaff());
        sample.setBillCode(table.getTableNo());
        sample.setBillInfoId(table.getTableInfoId());
        sample.setTableHeadID(table.getId().toString());
        sample.setRedBlue(Optional.ofNullable(adaptor.getRedBlue(table)).map(SystemCode::new).orElse(null));
        sample.setWare(adaptor.getWarehouse(table));
        sample.setCreateDate(table.getCreateTime());
        sample.setEffectDate(table.getEffectTime());
        sample.setServiceTypeID(bizType);
        sample.setServiceTypeCode(bizType.getServiceTypeCode());
        sample.setServiceTypeExplain(bizType.getServiceTypeExplain());
        sample.setDirection(new SystemCode(OperateDirection.SEND));
        if (storageType != null) {
            sample.setStorageTypeID(storageType);
            sample.setStorageTypeCode(Dbs.getProp(storageType, MaterialStorageType::getReasonCode));
            sample.setStorageTypeExplain(Dbs.getProp(storageType, MaterialStorageType::getReasonExplain));
        }
        sample.setDept(Optional.ofNullable(table.getCreateDepartment()).orElse(Organazations.getCurrentDepartment()));
        sample.setDeptCode(Dbs.getProp(table.getCreateDepartment(), Department::getCode));
        sample.setDeptName(Dbs.getProp(table.getCreateDepartment(), Department::getName));


        return details.stream().map(detail -> {
            BaseSetMaterial good = adaptor.$getMaterial(detail);
            MaterialBusinessDetail businessDetail = Beans.getCopy(sample);
            businessDetail.setQuantity(adaptor.$getOutQuantity(detail));
            businessDetail.setBusinessMemo(adaptor.$getMemoField(detail));
            businessDetail.setMoneyTotal(adaptor.$getBill(detail));
            businessDetail.setTableBodyID(detail.getId().toString());

            businessDetail.setGood(good);
            businessDetail.setUnit(Dbs.getProp(good, BaseSetMaterial::getMainUnit, BaseSetUnit::getName));
            MaterialStandingcrop stock = adaptor.$getStock(detail);
            businessDetail.setMaterBatchInfo(Dbs.getProp(stock, MaterialStandingcrop::getMaterBatchInfo));
            businessDetail.setBatchText(Dbs.getProp(stock, MaterialStandingcrop::getBatchText));
            businessDetail.setPlaceSet(Dbs.getProp(stock, MaterialStandingcrop::getPlaceSet));
            return businessDetail;
        }).collect(Collectors.toList());
    }

    public static MaterialBusinessDetail getOutboundBizDetail(AbstractEcFullEntity table, AbstractEcPartEntity detail, MaterialStandingcrop stock, String bizTypeCode) {
        OutboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getOutboundTypeAdaptor(bizTypeCode);
        //表头数据
        MaterialServiceType bizType = bizTypeService.getBizType(bizTypeCode);
        MaterialStorageType storageType = adaptor.getStorageType(table);

        MaterialBusinessDetail businessDetail = new MaterialBusinessDetail();

        businessDetail.setTrasactionDate(new Date());

        businessDetail.setCreatePerson(table.getCreateStaff());
        businessDetail.setCreatePersonID(table.getCreateStaff().getId().toString());
        businessDetail.setBillCode(table.getTableNo());
        businessDetail.setBillInfoId(table.getTableInfoId());
        businessDetail.setTableHeadID(table.getId().toString());
        businessDetail.setTableBodyID(detail.getId().toString());
        businessDetail.setRedBlue(Optional.ofNullable(adaptor.getRedBlue(table)).map(SystemCode::new).orElse(null));
        businessDetail.setWare(adaptor.getWarehouse(table));
        businessDetail.setCreateDate(table.getCreateTime());
        businessDetail.setEffectDate(table.getEffectTime());
        businessDetail.setServiceTypeID(bizType);
        businessDetail.setServiceTypeCode(bizType.getServiceTypeCode());
        businessDetail.setServiceTypeExplain(bizType.getServiceTypeExplain());
        businessDetail.setDirection(new SystemCode(OperateDirection.SEND));
        if (storageType != null) {
            businessDetail.setStorageTypeID(storageType);
            businessDetail.setStorageTypeCode(Dbs.getProp(storageType, MaterialStorageType::getReasonCode));
            businessDetail.setStorageTypeExplain(Dbs.getProp(storageType, MaterialStorageType::getReasonExplain));
        }
        businessDetail.setDept(Optional.ofNullable(table.getCreateDepartment()).orElse(Organazations.getCurrentDepartment()));
        businessDetail.setDeptCode(Dbs.getProp(table.getCreateDepartment(), Department::getCode));
        businessDetail.setDeptName(Dbs.getProp(table.getCreateDepartment(), Department::getName));
        businessDetail.setEffectPersonName(table.getCreateStaff().getName());
        businessDetail.setEffectStaffId(table.getCreateStaff().getId());
        businessDetail.setEffectPerson(table.getCreateStaff());
        BaseSetMaterial good = adaptor.$getMaterial(detail);
        businessDetail.setQuantity(adaptor.$getOutQuantity(detail));
        businessDetail.setBusinessMemo(adaptor.$getMemoField(detail));
        businessDetail.setMoneyTotal(adaptor.$getBill(detail));

        businessDetail.setGood(good);
        businessDetail.setUnit(Dbs.getProp(good, BaseSetMaterial::getMainUnit, BaseSetUnit::getName));
        businessDetail.setMaterBatchInfo(Dbs.getProp(stock, MaterialStandingcrop::getMaterBatchInfo));
        businessDetail.setBatchText(Dbs.getProp(stock, MaterialStandingcrop::getBatchText));
        businessDetail.setPlaceSet(Dbs.getProp(stock, MaterialStandingcrop::getPlaceSet));
        return businessDetail;
    }


    //盘点
    public static MaterialBusinessDetail getStocktakingBizDetail(MaterialStocktaking stocktaking, MaterialStjStockRecord record) {
        //表头数据
        BigDecimal offset = record.getQuantityByCount().subtract(record.getQuantityOnBook());
        MaterialStorageType storageType;
        SystemCode direction;
        if (offset.compareTo(BigDecimal.ZERO) > 0) {
            //盘盈入库
            storageType = bizTypeService.getStorageType("inventoryProfitIn");
            direction = new SystemCode(OperateDirection.RECEIVE);
        } else if (offset.compareTo(BigDecimal.ZERO) < 0) {
            //盘亏出库
            storageType = bizTypeService.getStorageType("inventoryLossOut");
            direction = new SystemCode(OperateDirection.SEND);
        } else {
            return null;
        }
        MaterialServiceType bizType = bizTypeService.getBizType(BizTypeCode.INVENTORY);
        MaterialBusinessDetail businessDetail = new MaterialBusinessDetail();
        businessDetail.setTrasactionDate(new Date());
        businessDetail.setCreatePerson(Organazations.getCurrentStaff());
        businessDetail.setCreatePersonID(Organazations.getCurrentStaffId().toString());
        businessDetail.setBillCode(stocktaking.getTableNo());
        businessDetail.setBillInfoId(stocktaking.getTableInfoId());
        businessDetail.setTableHeadID(stocktaking.getId().toString());
        businessDetail.setTableBodyID(record.getId().toString());
        businessDetail.setRedBlue(new SystemCode(BaseRedBlue.BLUE));
        businessDetail.setWare(stocktaking.getWarehouse());
        businessDetail.setCreateDate(stocktaking.getCreateTime());
        businessDetail.setEffectDate(new Date());
        businessDetail.setServiceTypeID(bizType);
        businessDetail.setServiceTypeCode(bizType.getServiceTypeCode());
        businessDetail.setServiceTypeExplain(bizType.getServiceTypeExplain());
        businessDetail.setDirection(direction);
        if (storageType != null) {
            businessDetail.setStorageTypeID(storageType);
            businessDetail.setStorageTypeCode(Dbs.getProp(storageType, MaterialStorageType::getReasonCode));
            businessDetail.setStorageTypeExplain(Dbs.getProp(storageType, MaterialStorageType::getReasonExplain));
        }
        businessDetail.setDept(Optional.ofNullable(stocktaking.getCreateDepartment()).orElse(Organazations.getCurrentDepartment()));
        businessDetail.setDeptCode(Dbs.getProp(stocktaking.getCreateDepartment(), Department::getCode));
        businessDetail.setDeptName(Dbs.getProp(stocktaking.getCreateDepartment(), Department::getName));
        businessDetail.setEffectPersonName(stocktaking.getCreateStaff().getName());
        businessDetail.setEffectStaffId(stocktaking.getCreateStaff().getId());
        businessDetail.setEffectPerson(stocktaking.getCreateStaff());
        BaseSetMaterial good = Dbs.reload(record.getMaterial());
        businessDetail.setQuantity(offset.abs());
        businessDetail.setBusinessMemo(stocktaking.getPs());

        businessDetail.setGood(good);
        businessDetail.setUnit(Dbs.getProp(good, BaseSetMaterial::getMainUnit, BaseSetUnit::getName));
        businessDetail.setMaterBatchInfo(record.getBatchInfo());
        businessDetail.setBatchText(Dbs.getProp(record.getBatchInfo(), MaterialBatchInfo::getBatchNum));
        businessDetail.setPlaceSet(record.getPlace());
        return businessDetail;
    }

    //拆批
    public static List<MaterialBusinessDetail> getSplitBatchBizDetails(MaterialUnpackInfo unpackInfo, List<MaterialUnpackDetail> details) {
        MaterialStandingcrop stock = Dbs.reload(unpackInfo.getStock());
        //生成一个扣减流水和n个新增流水

        MaterialBusinessDetail sampleDetail = new MaterialBusinessDetail();
        sampleDetail.setTrasactionDate(new Date());
        sampleDetail.setCreatePerson(Organazations.getCurrentStaff());
        sampleDetail.setCreatePersonID(Organazations.getCurrentStaffId().toString());
        sampleDetail.setBillCode(unpackInfo.getRecordCode());
        sampleDetail.setTableHeadID(unpackInfo.getId().toString());
        sampleDetail.setRedBlue(new SystemCode(BaseRedBlue.BLUE));
        sampleDetail.setWare(stock.getWare());
        sampleDetail.setCreateDate(unpackInfo.getCreateTime());
        sampleDetail.setEffectDate(new Date());
        sampleDetail.setDept(Optional.ofNullable(unpackInfo.getCreateDepartment()).orElse(Organazations.getCurrentDepartment()));
        sampleDetail.setDeptCode(Dbs.getProp(unpackInfo.getCreateDepartment(), Department::getCode));
        sampleDetail.setDeptName(Dbs.getProp(unpackInfo.getCreateDepartment(), Department::getName));
        sampleDetail.setEffectPersonName(unpackInfo.getCreateStaff().getName());
        sampleDetail.setEffectStaffId(unpackInfo.getCreateStaff().getId());
        sampleDetail.setEffectPerson(unpackInfo.getCreateStaff());
        BaseSetMaterial good = Dbs.reload(stock.getGood());
        sampleDetail.setGood(good);
        sampleDetail.setUnit(Dbs.getProp(good, BaseSetMaterial::getMainUnit, BaseSetUnit::getName));
        sampleDetail.setPlaceSet(stock.getPlaceSet());
        List<MaterialBusinessDetail> bizDetails = new ArrayList<>(details.size() + 1);

        //创建扣减流水
        MaterialServiceType outBizType = bizTypeService.getBizType(BizTypeCode.BATCH_SPLIT_OUT);
        MaterialStorageType outStorageType = bizTypeService.getStorageType("splitOut");
        MaterialBusinessDetail outDetail = Beans.getCopy(sampleDetail);
        outDetail.setMaterBatchInfo(stock.getMaterBatchInfo());
        outDetail.setBatchText(stock.getBatchText());
        outDetail.setServiceTypeID(outBizType);
        outDetail.setServiceTypeCode(outBizType.getServiceTypeCode());
        outDetail.setServiceTypeExplain(outBizType.getServiceTypeExplain());
        outDetail.setDirection(new SystemCode(OperateDirection.SEND));
        outDetail.setStorageTypeID(outStorageType);
        outDetail.setStorageTypeCode(Dbs.getProp(outStorageType, MaterialStorageType::getReasonCode));
        outDetail.setStorageTypeExplain(Dbs.getProp(outStorageType, MaterialStorageType::getReasonExplain));
        outDetail.setQuantity(details.stream().map(MaterialUnpackDetail::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add));
        //查询对应批次信息
        List<String> batchNumList = details.stream().map(MaterialUnpackDetail::getBatchNum).distinct().collect(Collectors.toList());
        Map<String, Long> batchNum$info = Dbs.pairList(
                "SELECT ID,BATCH_NUM FROM " + MaterialBatchInfo.TABLE_NAME + " WHERE VALID=1 AND MATERIAL_ID=? AND " + Dbs.inCondition("BATCH_NUM", batchNumList.size()),
                Long.class, String.class,
                Elements.toArray(stock.getGood().getId(), batchNumList)
        ).stream().collect(Collectors.toMap(
                Pair::getSecond, Pair::getFirst
        ));
        bizDetails.add(outDetail);

        //对明细创建流水
        MaterialServiceType inBizType = bizTypeService.getBizType(BizTypeCode.BATCH_SPLIT_IN);
        MaterialStorageType inStorageType = bizTypeService.getStorageType("splitIn");
        details.forEach(detail -> {
            MaterialBusinessDetail inDetail = Beans.getCopy(sampleDetail);
            inDetail.setTableBodyID(detail.getId().toString());
            inDetail.setBusinessMemo(detail.getPs());
            inDetail.setMaterBatchInfo(Entities.ofId(
                    MaterialBatchInfo.class,
                    batchNum$info.get(detail.getBatchNum())
            ));
            inDetail.setBatchText(detail.getBatchNum());
            inDetail.setQuantity(detail.getQuantity());
            inDetail.setServiceTypeID(inBizType);
            inDetail.setServiceTypeCode(inBizType.getServiceTypeCode());
            inDetail.setServiceTypeExplain(inBizType.getServiceTypeExplain());
            inDetail.setDirection(new SystemCode(OperateDirection.RECEIVE));
            inDetail.setStorageTypeID(inStorageType);
            inDetail.setStorageTypeCode(Dbs.getProp(inStorageType, MaterialStorageType::getReasonCode));
            inDetail.setStorageTypeExplain(Dbs.getProp(inStorageType, MaterialStorageType::getReasonExplain));
            bizDetails.add(inDetail);
        });
        return bizDetails;
    }

}
