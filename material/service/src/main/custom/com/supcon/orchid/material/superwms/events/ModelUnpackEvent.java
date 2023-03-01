package com.supcon.orchid.material.superwms.events;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetQrCodeType;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.PostSqlUpdater;
import com.supcon.orchid.material.services.MaterialBatchInfoService;
import com.supcon.orchid.material.services.MaterialBusinessDetailService;
import com.supcon.orchid.material.services.MaterialPrintInfoService;
import com.supcon.orchid.material.superwms.constants.systemcode.SplitType;
import com.supcon.orchid.material.superwms.services.ModelBatchInfoService;
import com.supcon.orchid.material.superwms.util.StockOperator;
import com.supcon.orchid.material.superwms.util.factories.BatchInfoFactory;
import com.supcon.orchid.material.superwms.util.factories.BusinessDetailFactory;
import com.supcon.orchid.orm.entities.CodeEntity;
import com.supcon.orchid.orm.entities.IdEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ModelUnpackEvent {

    @Autowired
    private MaterialPrintInfoService printInfoService;
    @Autowired
    private ModelBatchInfoService modelBatchInfoService;
    @Autowired
    private MaterialBatchInfoService batchInfoService;
    @Autowired
    private MaterialBusinessDetailService businessDetailService;

    @Signal("UnpackInfoAfterSave")
    public void afterSave(MaterialUnpackInfo unpackInfo){
        List<MaterialUnpackDetail> details = Dbs.findByCondition(
                MaterialUnpackDetail.class,
                "VALID=1 AND UNPACK_INFO=?",
                unpackInfo.getId()
        ).stream().map(PostSqlUpdater::getAutoWritableEntity).collect(Collectors.toList());
        //将字符串转为big decimal
        details.forEach(detail-> detail.setQuantity(new BigDecimal(detail.getDecimalQuantity())));
        //根据待打印情况，生成待打印列表和打印完成列表
        MaterialStandingcrop stock = Dbs.reload(unpackInfo.getStock());
        BaseSetQrCodeType source = Dbs.load(
                BaseSetQrCodeType.class,
                "VALID=1 AND CODE=?",
                "onHand"
        );
        //将可用量快照进行冗余
        MaterialUnpackInfo wUnpackInfo = PostSqlUpdater.getAutoWritableEntity(unpackInfo);
        wUnpackInfo.setQuantitySnapshot(Dbs.getProp(unpackInfo.getStock(),MaterialStandingcrop::getAvailiQuantity));
        wUnpackInfo.setSplitTime(new Date());
        String splitType = Optional.ofNullable(unpackInfo.getSplitType()).map(CodeEntity::getId).orElse(null);
        boolean splitBatch = SplitType.BATCH.equals(splitType);
        if(splitBatch){
            //拆批时需要消耗原批号现存量，产生新的批次信息、现存量、流水信息
            List<MaterialUnpackDetail> detailsWithoutBatchInfo = modelBatchInfoService.checkBatchRuleAndFilterExist(details, val->stock.getGood(),MaterialUnpackDetail::getBatchNum,false);
            if(!detailsWithoutBatchInfo.isEmpty()){
                //没有创建批次信息的先进行创建
                List<MaterialBatchInfo> batchInfoList = BatchInfoFactory.batchSplitBatchInfoList(Dbs.reload(stock.getMaterBatchInfo()),detailsWithoutBatchInfo);
                batchInfoList.forEach(batchInfo->batchInfoService.saveBatchInfo(batchInfo,null));
            }
            //对拆分明细进行新增现存量
            details.forEach(detail-> StockOperator.of(
                    stock.getGood().getId(),
                    detail.getBatchNum(),
                    stock.getWare().getId(),
                    Optional.ofNullable(stock.getPlaceSet()).map(IdEntity::getId).orElse(null)
            ).setSourceEntity(detail).increase(detail.getQuantity()));
            //扣减原现存量
            StockOperator.of(stock).setSourceEntity(unpackInfo).decrease(details.stream().map(MaterialUnpackDetail::getQuantity).reduce(BigDecimal.ZERO,BigDecimal::add));
            //创建流水信息
            List<MaterialBusinessDetail> bizDetails = BusinessDetailFactory.getSplitBatchBizDetails(unpackInfo,details);
            bizDetails.forEach(bizDetail->businessDetailService.saveBusinessDetail(bizDetail,null));
        }
        details.forEach(detail->{
            MaterialPrintInfo printInfo = new MaterialPrintInfo();
            printInfo.setSrcId(detail.getId());
            printInfo.setSrcTableNo(unpackInfo.getRecordCode());
            if(splitBatch){
                //拆批时批号为手动输入的新批号
                printInfo.setBatchCode(detail.getBatchNum());
            } else {
                //拆包时批号不变
                printInfo.setBatchCode(stock.getBatchText());
            }
            printInfo.setIsRequest(true);
            printInfo.setMaterial(stock.getGood());
            printInfo.setWare(stock.getWare());
            printInfo.setPlaceSet(stock.getPlaceSet());
            printInfo.setArrivalDate(new Date());
            BigDecimal singleQuan = Dbs.getProp(stock.getGood(), BaseSetMaterial::getItemQty);
            printInfo.setItemAmount(singleQuan);
            printInfo.setRealHeavy(singleQuan);
            printInfo.setPrintNum(detail.getQuantity());
            printInfo.setDataSource(source);
            if(Boolean.TRUE.equals(detail.getToBePrinted())){
                //剩余量非0生成到待打印列表
                printInfo.setRemainNum(detail.getQuantity());
            } else {
                //剩余量0生成到打印完成列表
                printInfo.setRemainNum(BigDecimal.ZERO);
            }
            printInfoService.savePrintInfo(printInfo,null);
        });
    }
}
