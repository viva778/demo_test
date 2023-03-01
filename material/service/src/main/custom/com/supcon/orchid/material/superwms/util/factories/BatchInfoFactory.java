package com.supcon.orchid.material.superwms.util.factories;

import com.supcon.orchid.BaseSet.entities.BaseSetCooperate;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.Beans;
import com.supcon.orchid.fooramework.util.Dates;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.material.superwms.constants.systemcode.*;
import com.supcon.orchid.material.superwms.util.BatchNumberConvertor;
import com.supcon.orchid.material.superwms.util.adptor.AdaptorCenter;
import com.supcon.orchid.material.superwms.util.adptor.BizTypeContext;
import com.supcon.orchid.material.superwms.util.adptor.InboundTypeAdaptor;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class BatchInfoFactory {

    public static List<MaterialBatchInfo> purchaseArrivalBatchInfoList(MaterialPurArrivalInfo table, List<MaterialPurArrivalPart> details) {
        //首次提交，生成批次信息（同样的批次信息只生成一次
        return details.stream().filter(detail -> BaseBatchType.isEnable(Dbs.getProp(detail.getGood(), BaseSetMaterial::getIsBatch)) && Strings.valid(detail.getBatch())).collect(Collectors.groupingBy(MaterialPurArrivalPart::getBatch)).entrySet().stream().map(entry -> {
            String batch = entry.getKey();
            MaterialPurArrivalPart sampleDetail = entry.getValue().get(0);
            // 采购订单明细
            // MaterialPurchasePart orderDetail = Dbs.load(MaterialPurchasePart.class,Long.parseLong(sampleDetail.getPurchaseId()));

            MaterialBatchInfo batchInfo = new MaterialBatchInfo();
            // 批号
            batchInfo.setBatchNum(batch);
            //原批号
            batchInfo.setOriginBatchNum(BatchNumberConvertor.getOriginBatchNum(sampleDetail.getGood(), batch));
            // 批次数据来源为采购到货
            batchInfo.setSourceType(new SystemCode(BaseSourceType.PURCHASE_IN));
            // 供应商
            batchInfo.setVendorId(table.getVendor());
            // 到货日期
            batchInfo.setArriveDate(table.getArrivalDate());
            // 生产厂商名称
            batchInfo.setManufacture(Dbs.getProp(table.getVendor(), BaseSetCooperate::getName));
            // 采购日期
            batchInfo.setPurchDate(Optional.ofNullable(sampleDetail.getPurchaseId())
                    .map(Long::parseLong)
                    .map(id -> Dbs.load(MaterialPurchasePart.class, id))
                    .map(orderDetail -> Dbs.getProp(orderDetail.getPurchaseInfo(), MaterialPurchaseInfo::getPurchDate))
                    .orElse(new Date())
            );
            // 物品
            batchInfo.setMaterialId(sampleDetail.getGood());
            // 生产日期
            batchInfo.setProductionDate(sampleDetail.getProduceDate());
            // 有效期
            batchInfo.setValidDate(sampleDetail.getValidTime());
            // 近效期
            batchInfo.setApprochTime(sampleDetail.getApproachTime());
            // 供应商批号信息
            batchInfo.setSupplierBatch(sampleDetail.getSupplierBatch());
            //设置质检状态
            if (Boolean.TRUE.equals(Dbs.getProp(sampleDetail.getGood(), BaseSetMaterial::getIsCheck))) {
                // 需质检, 检验状态为待检
                batchInfo.setCheckState(new SystemCode(BaseCheckState.TO_CHECK));
                batchInfo.setIsAvailable(false);
            } else {
                // 无需质检或"免检"物品, 检验状态为无需检验
                batchInfo.setCheckState(new SystemCode(BaseCheckState.UNNECESSARY));
                batchInfo.setIsAvailable(true);
                batchInfo.setAvailableDate(table.getArrivalDate());
            }
            return batchInfo;
        }).collect(Collectors.toList());
    }

    public static List<MaterialBatchInfo> inboundBatchInfoList(String bizType, AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details) {
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
        BizTypeContext context = AdaptorCenter.getBizTypeContext(bizType);
        return details.stream()
                .filter(detail -> Strings.valid(adaptor.$getBatchNum(detail)))
                .collect(Collectors.groupingBy(detail -> Pair.of(adaptor.$getBatchNum(detail), adaptor.$getMaterial(detail).getId())))
                .entrySet().stream().map(entry -> {
                    String batchNum = entry.getKey().getFirst();
                    AbstractEcPartEntity detail = entry.getValue().get(0);
                    BaseSetMaterial material = Dbs.reload(adaptor.$getMaterial(detail));
                    if (BaseBatchType.isEnable(material.getIsBatch())) {
                        MaterialBatchInfo batchInfo = new MaterialBatchInfo();
                        batchInfo.setCheckState(new SystemCode(BaseCheckState.UNNECESSARY));
                        batchInfo.setIsAvailable(true);
                        batchInfo.setAvailableDate(new Date());
                        batchInfo.setBatchNum(batchNum);
                        batchInfo.setOriginBatchNum(adaptor.$getOriginBatchNum(detail));
                        batchInfo.setSourceType(Optional.ofNullable(context.getBaseSourceType()).map(SystemCode::new).orElse(null));
                        batchInfo.setMaterialId(material);
                        batchInfo.setIsCycleRetest(material.getIsRecheck());
                        batchInfo.setInStoreDate(new Date());
                        //设置检验结论
                        if (context.getInContext().isNeedQualityCheck() && Boolean.TRUE.equals(material.getIsCheck())) {
                            batchInfo.setCheckResult(adaptor.$getCheckResult(detail));
                            if (BaseCheckResult.isQualified(adaptor.$getCheckResult(detail))) {
                                //合格 设置为已检 ，可用
                                batchInfo.setCheckState(new SystemCode(BaseCheckState.CHECKED));
                                batchInfo.setIsAvailable(true);
                                batchInfo.setAvailableDate(adaptor.getStorageDate(table));
                            } else {
                                //否则设置为待检
                                batchInfo.setCheckState(new SystemCode(BaseCheckState.TO_CHECK));
                                batchInfo.setIsAvailable(false);
                                batchInfo.setAvailableDate(null);
                            }
                        } else {
                            batchInfo.setCheckState(new SystemCode(BaseCheckState.UNNECESSARY));
                            batchInfo.setIsAvailable(true);
                            batchInfo.setAvailableDate(adaptor.getStorageDate(table));
                        }
                        //近效期有效期设置
                        Date produceDate = adaptor.$getProduceDate(detail);
                        if (produceDate != null) {
                            batchInfo.setProductionDate(produceDate);
                            if (material.getApproachDate() != null && material.getApproachUnit() != null) {
                                //近效期
                                batchInfo.setApprochTime(Dates.offset(
                                        produceDate,
                                        material.getApproachDate(),
                                        BaseTimeUnit.getTemporalUnit(material.getApproachUnit().getId())
                                ));
                            }
                            if (material.getValidPeriod() != null && material.getValidUnit() != null) {
                                //有效期
                                batchInfo.setValidDate(Dates.offset(
                                        produceDate,
                                        material.getValidPeriod(),
                                        BaseTimeUnit.getTemporalUnit(material.getValidUnit().getId())
                                ));
                            }
                        }
                        return batchInfo;
                    }
                    return null;
                }).filter(Objects::nonNull).collect(Collectors.toList());
    }


    public static List<MaterialBatchInfo> batchSplitBatchInfoList(MaterialBatchInfo headerBatchInfo, List<MaterialUnpackDetail> details) {
        return details.stream().map(MaterialUnpackDetail::getBatchNum).distinct().map(batchNum -> {
            //拷贝主批号，去除创建信息
            MaterialBatchInfo batchInfo = Beans.getCopy(headerBatchInfo);
            batchInfo.setId(null);
            batchInfo.setCreateTime(null);
            batchInfo.setModifyTime(null);
            //批号
            batchInfo.setBatchNum(batchNum);
            //原批号
            batchInfo.setOriginBatchNum(BatchNumberConvertor.getOriginBatchNum(headerBatchInfo.getMaterialId(), batchNum));
            //主批号
            batchInfo.setHeadBatchNum(headerBatchInfo.getBatchNum());
            return batchInfo;
        }).collect(Collectors.toList());
    }


    /**
     * 接口更新生成的批次信息现存量无需检验
     * @param good
     * @param batch
     * @return
     */
    public static MaterialBatchInfo standingcropBatchInfo(BaseSetMaterial good, String batch) {
        MaterialBatchInfo batchInfo = new MaterialBatchInfo();
        // 批号
        batchInfo.setBatchNum(batch);
        //原批号
        batchInfo.setOriginBatchNum(BatchNumberConvertor.getOriginBatchNum(good, batch));
        // 批次数据来源为采购到货
        batchInfo.setSourceType(new SystemCode(BaseSourceType.PURCHASE_IN));
        // 物品
        batchInfo.setMaterialId(good);
        //设置质检状态
        // 无需质检或"免检"物品, 检验状态为无需检验
        batchInfo.setCheckState(new SystemCode(BaseCheckState.UNNECESSARY));
        batchInfo.setIsAvailable(true);
        //设置日期为当天
        batchInfo.setAvailableDate(Dates.getDateWithoutTime(new Date()));
        return batchInfo;
    }
}
