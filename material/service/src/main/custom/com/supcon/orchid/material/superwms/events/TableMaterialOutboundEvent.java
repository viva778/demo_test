package com.supcon.orchid.material.superwms.events;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Elements;
import com.supcon.orchid.fooramework.util.PostSqlUpdater;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.material.superwms.constants.BizTypeCode;
import com.supcon.orchid.material.superwms.services.TableOutboundService;
import com.supcon.orchid.material.superwms.util.adptor.AdaptorCenter;
import com.supcon.orchid.orm.entities.IdEntity;
import com.supcon.orchid.services.BAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TableMaterialOutboundEvent {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private TableOutboundService outboundTableService;

    //----------------------------------------------其他出库----------------------------------------------START
    @Signal("OtherOutAfterSubmit")
    private void otherOutAfterSubmit(MaterialOtherOutSingle table){
        outboundTableService.standardOutboundEvent(BizTypeCode.OTHER_SHIPMENT, table,table.getWorkFlowVar());
    }
    //----------------------------------------------其他出库----------------------------------------------END



    //----------------------------------------------生产出库----------------------------------------------START
    @Signal("ProduceOutAfterSubmit")
    private void ProduceOutAfterSubmit(MaterialProduceOutSing table){
        outboundTableService.standardOutboundEvent(BizTypeCode.PRODUCE_SHIPMENT, table,table.getWorkFlowVar());
    }
    //----------------------------------------------生产出库----------------------------------------------END




    //----------------------------------------------销售出库----------------------------------------------START
    @Signal("SaleOutAfterSubmit")
    private void saleOutAfterSubmit(MaterialSaleOutSingle table){
        outboundTableService.standardOutboundEvent(BizTypeCode.SALE_SHIPMENT, table,table.getWorkFlowVar());
    }

    //回填销售发货单
    @Signal("OutboundDetail")
    private void afterSubmit(String bizType,MaterialSaleOutDetail detail, BigDecimal outQuan) {
        if(detail!=null&&detail.getSaleDeliveryDetail()!=null) {
            Dbs.execute("update "+MaterialSalDeliDetail.TABLE_NAME+" set valid_quan= valid_quan + ?  where id=? AND VALID = 1 ",outQuan,detail.getSaleDeliveryDetail().getId());
        }
    }

    //----------------------------------------------销售出库----------------------------------------------END

    //----------------------------------------------采购退货----------------------------------------------START
    @Signal("PurchaseReturnOutAfterSubmit")
    private void purchaseReturnAfterSubmit(MaterialPurReturn table){
        outboundTableService.standardOutboundEvent(BizTypeCode.PURCHASE_RETURN, table,table.getWorkFlowVar());
    }

    //采购退货单出库完成时，回填上游单据退货量
    @Signal("OutboundDetail")
    private void pushbackPurchaseIn(MaterialPurReturnPart detail, BigDecimal outQuan){
        if(detail!=null&&detail.getPurchaseInSingleDetail()!=null){
            MaterialPurchInPart wSrcDetail = PostSqlUpdater.getAutoWritableEntity(Dbs.reload(detail.getPurchaseInSingleDetail()));
            //累加退货量
            BigDecimal rtnQuan = Optional.ofNullable(wSrcDetail.getReturnNum()).map(outQuan::add).orElse(outQuan);
            wSrcDetail.setReturnNum(rtnQuan);
        }
    }

    //----------------------------------------------采购退货----------------------------------------------END


    //----------------------------------------------废料出库----------------------------------------------START
    @Signal("WasteOutAfterSubmit")
    private void wasteOutAfterSubmit(MaterialWasteOutSingle table){
        outboundTableService.standardOutboundEvent(BizTypeCode.WASTE_SHIPMENT, table,table.getWorkFlowVar());
    }

    @Signal("WasteOutFirstCommit")
    private void checkDisposalUnit(MaterialWasteOutSingle table, List<MaterialWasteOutDetail> details){
        //校验剩余处置量是否足够
        String insufficientWastes = details.stream().filter(detail->{
            BigDecimal restNumber = Dbs.getProp(detail.getResidualNumber(), MaterialUnitDetails::getResidualNumber);
            return restNumber!=null&&restNumber.compareTo(detail.getApplyNumber())<0;
        }).map(e->Dbs.getProp(e.getWaste(), BaseSetMaterial::getName)).collect(Collectors.joining(","));
        if(Strings.valid(insufficientWastes)){
            //剩余处置量不足！
            throw new BAPException(InternationalResource.get("material.custom.residualNumber.is.Insufficient", (Object) insufficientWastes));
        }
    }

    @Signal("WasteOutEffecting")
    private void updateDisposalUnit(MaterialWasteOutSingle table, List<MaterialWasteOutDetail> details){
        //更新处置单元信息
        List<Long> wasteMaterialIds = details.stream().map(MaterialWasteOutDetail::getWaste).filter(Objects::nonNull).map(IdEntity::getId).collect(Collectors.toList());
        Map<BaseSetMaterial,MaterialUnitDetails> material$unitDetail = Dbs.findByCondition(
                MaterialUnitDetails.class,
                "VALID=1 AND DISPOSAL_UNIT=? AND "+Dbs.inCondition("WASTE",wasteMaterialIds.size()),
                Elements.toArray(table.getDisposalUnit().getId(),wasteMaterialIds)
        ).stream().collect(Collectors.toMap(
                MaterialUnitDetails::getWaste,
                detail->detail
        ));
        details.forEach(detail->{
            MaterialUnitDetails unitDetail = material$unitDetail.get(detail.getWaste());
            if(unitDetail!=null){
                Optional.ofNullable(unitDetail.getResidualNumber()).ifPresent(restNumber->{
                    BigDecimal outNumber = Optional.ofNullable(detail.getApplyNumber()).orElse(BigDecimal.ZERO);
                    //扣减剩余处置量
                    if(restNumber.compareTo(outNumber)>=0){
                        PostSqlUpdater.getAutoWritableEntity(unitDetail).setResidualNumber(restNumber.subtract(outNumber));
                    } else {
                        //处置单位下物料剩余处置量不足
                        throw new BAPException(InternationalResource.get("material.custom.random1632813207897", (Object) detail.getWaste().getName()));
                    }
                });
            } else {
                //处置单位下未找到
                throw new BAPException(InternationalResource.get("material.custom.random1632811948633", (Object) detail.getWaste().getName()));
            }
        });
    }
    //----------------------------------------------废料出库----------------------------------------------END

    //----------------------------------------------出库SE----------------------------------------------START
    @Signal(value = ".*OutSuperEdit",signal_as_param = true)
    private void outboundSuperEdit(AbstractEcFullEntity table, String signal){
        if(table.getStatus()!=null&&table.getStatus()==99){
            String prefix = signal.substring(0,signal.length()-"SuperEdit".length());
            outboundTableService.quickEffect(AdaptorCenter.getBizTypeContext(prefix).getBizType(),table);
        }
    }
    //----------------------------------------------出库SE----------------------------------------------END


}
