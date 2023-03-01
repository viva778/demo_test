package com.supcon.orchid.material.superwms.util.factories;

import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Entities;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.material.superwms.util.adptor.AdaptorCenter;
import com.supcon.orchid.material.superwms.util.adptor.InboundTypeAdaptor;
import com.supcon.orchid.material.superwms.util.adptor.OutboundTypeAdaptor;
import com.supcon.orchid.utils.OrchidUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.supcon.orchid.material.superwms.constants.systemcode.BaseDealState.CANCEL;

public class BatchDealInfoFactory {


    public static MaterialBatchDeal getOutboundDealInfo(AbstractEcFullEntity table, AbstractEcPartEntity detail, String dealState, MaterialStandingcrop stock, String bizType){
        OutboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getOutboundTypeAdaptor(bizType);
        String batchNum = Dbs.getProp(stock,MaterialStandingcrop::getBatchText);
        if(Strings.valid(batchNum)){
            MaterialBatchDeal info = new MaterialBatchDeal();
            info.setBatchNum(batchNum);
            info.setDealDate(new Date());
            info.setDealDeptId(table.getCreateDepartment());
            info.setDealStaffId(table.getCreateStaff());
            info.setDealTableId(table.getId());
            info.setDealTableNo(table.getTableNo());
            info.setMaterialId(adaptor.$getMaterial(detail));
            info.setDealTableUrl(getUrl(bizType,table));

            info.setTableState(new SystemCode(dealState));
            if(CANCEL.equals(dealState)){
                info.setAmount(BigDecimal.ZERO);
            } else {
                info.setAmount(adaptor.$getOutQuantity(detail));
            }
            String remark = AdaptorCenter.getBizTypeContext(bizType).getOutContext().getDescription();
            info.setRemark(remark);
            return info;
        } else {
            return  null;
        }
    }

    public static List<MaterialBatchDeal> getOutboundDealInfoList(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String dealState, String bizType){
        OutboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getOutboundTypeAdaptor(bizType);
        //对批次信息相同的进行合并
        return details.stream()
                .filter(detail-> Strings.valid(adaptor.$getBatchNum(detail)))
                .collect(Collectors.groupingBy(detail-> Pair.of(adaptor.$getBatchNum(detail),adaptor.$getMaterial(detail).getId())))
                .entrySet()
                .stream()
                .map(entry->{
                    //每条批次生成一条处理信息
                    String batch = entry.getKey().getFirst();
                    List<? extends AbstractEcPartEntity> batchDetails = entry.getValue();
                    MaterialBatchDeal info = new MaterialBatchDeal();
                    info.setBatchNum(batch);
                    info.setDealDate(new Date());
                    info.setDealDeptId(table.getCreateDepartment());
                    info.setDealStaffId(table.getCreateStaff());
                    info.setDealTableId(table.getId());
                    info.setDealTableNo(table.getTableNo());
                    info.setTableState(new SystemCode(dealState));
                    info.setMaterialId(adaptor.$getMaterial(batchDetails.get(0)));
                    info.setDealTableUrl(getUrl(bizType,table));
                    info.setAmount(batchDetails
                            .stream()
                            .map(adaptor::$getOutQuantity)
                            .reduce(BigDecimal.ZERO,BigDecimal::add)
                    );
                    String remark = AdaptorCenter.getBizTypeContext(bizType).getOutContext().getDescription();
                    info.setRemark(remark);
                    return info;
                }).collect(Collectors.toList());
    }

    public static MaterialBatchDeal getInboundDealInfo(AbstractEcFullEntity table, AbstractEcPartEntity detail, String dealState, MaterialBatchInfo batchInfo, BigDecimal quantity, String bizType){
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
        if(batchInfo!=null){
            MaterialBatchDeal info = new MaterialBatchDeal();
            info.setBatchNum(batchInfo.getBatchNum());
            info.setDealDate(new Date());
            info.setDealDeptId(table.getCreateDepartment());
            info.setDealStaffId(table.getCreateStaff());
            info.setDealTableId(table.getId());
            info.setDealTableNo(table.getTableNo());
            info.setTableState(new SystemCode(dealState));
            info.setMaterialId(adaptor.$getMaterial(detail));
            info.setDealTableUrl(getUrl(bizType,table));
            info.setAmount(quantity);
            String remark = AdaptorCenter.getBizTypeContext(bizType).getInContext().getDescription();
            info.setRemark(remark);
            return info;
        } else {
            return null;
        }
    }

    public static List<MaterialBatchDeal> getInboundDealInfoList(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String dealState, String bizType){
        InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity> adaptor = AdaptorCenter.getInboundTypeAdaptor(bizType);
        //对批次信息相同的进行合并
        return details.stream()
                .filter(detail-> Strings.valid(adaptor.$getBatchNum(detail)))
                .collect(Collectors.groupingBy(detail-> Pair.of(adaptor.$getBatchNum(detail),adaptor.$getMaterial(detail).getId())))
                .entrySet()
                .stream()
                .map(entry->{
                    //每条批次生成一条处理信息
                    String batch = entry.getKey().getFirst();
                    List<? extends AbstractEcPartEntity> batchDetails = entry.getValue();
                    MaterialBatchDeal info = new MaterialBatchDeal();
                    info.setBatchNum(batch);
                    info.setDealDate(new Date());
                    info.setDealDeptId(table.getCreateDepartment());
                    info.setDealStaffId(table.getCreateStaff());
                    info.setDealTableId(table.getId());
                    info.setDealTableNo(table.getTableNo());
                    info.setTableState(new SystemCode(dealState));
                    info.setMaterialId(adaptor.$getMaterial(batchDetails.get(0)));
                    info.setDealTableUrl(getUrl(bizType,table));
                    info.setAmount(batchDetails
                            .stream()
                            .map(adaptor::$getInQuantity)
                            .reduce(BigDecimal.ZERO,BigDecimal::add)
                    );
                    String remark = AdaptorCenter.getBizTypeContext(bizType).getInContext().getDescription();
                    info.setRemark(remark);
                    return info;
                }).collect(Collectors.toList());
    }


    public static List<MaterialBatchDeal> purchaseArrivalDealList(MaterialPurArrivalInfo table, List<MaterialPurArrivalPart> details, String dealState){
        return details.stream()
                .filter(detail->Strings.valid(detail.getBatch())).collect(Collectors.groupingBy(MaterialPurArrivalPart::getBatch))
                .entrySet()
                .stream()
                .map(entry->{
                    String batchNum = entry.getKey();
                    List<MaterialPurArrivalPart> groupDetails = entry.getValue();
                    MaterialBatchDeal dealInfo = new MaterialBatchDeal();
                    dealInfo.setBatchNum(batchNum);
                    dealInfo.setAmount(groupDetails.stream().map(MaterialPurArrivalPart::getArrivalQuan).reduce(BigDecimal.ZERO,BigDecimal::add));
                    dealInfo.setMaterialId(groupDetails.get(0).getGood());
                    dealInfo.setDealDate(new Date());
                    dealInfo.setDealDeptId(table.getPurchDept());
                    dealInfo.setDealStaffId(table.getPurePerson());
                    dealInfo.setDealTableId(table.getId());
                    dealInfo.setDealTableNo(table.getTableNo());
                    dealInfo.setSourceBatchNum(batchNum);
                    dealInfo.setTableState(new SystemCode(dealState));
                    dealInfo.setRemark(InternationalResource.get("material.description.purchaseArrival"));
                    dealInfo.setDealTableUrl(getUrl(
                            "material_1.0.0_purArrivalInfos_purArrivalInfoView",
                            "material_1.0.0_purArrivalInfos_purArrivalInfoList",
                            table
                    ));
                    return dealInfo;
                }).collect(Collectors.toList());
    }


    private static String getUrl(String bizType, AbstractEcFullEntity entity){
        String viewCode = AdaptorCenter.getBizTypeContext(bizType).getViewCode();
        String listCode = AdaptorCenter.getBizTypeContext(bizType).getListCode();
        return getUrl(viewCode, listCode, entity);
    }

    private static String getUrl(String viewCode, String listCode, AbstractEcFullEntity entity){
        String url = Dbs.first(
                "SELECT URL FROM EC_VIEW WHERE VALID=1 AND CODE=? ",
                String.class,
                viewCode
        );
        return url+"?"+ String.join("&",
                "tableInfoId=" + entity.getTableInfoId(),
                "entityCode=" + Entities.getEntityCode(entity.getClass()),
                "id=" + entity.getId(),
                "__pc__=" + new String(OrchidUtils.encode((listCode + "_self|").getBytes()))
        );
    }
}
