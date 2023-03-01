package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetQrCodeType;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Elements;
import com.supcon.orchid.fooramework.util.Entities;
import com.supcon.orchid.fooramework.util.PostSqlUpdater;
import com.supcon.orchid.material.services.MaterialPrintInfoService;
import com.supcon.orchid.material.services.MaterialQrDetailInfoService;
import com.supcon.orchid.material.superwms.services.ModelBarCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ModelBarCodeServiceImpl implements ModelBarCodeService {

    @Autowired
    private MaterialPrintInfoService printInfoService;

    @Autowired
    private MaterialQrDetailInfoService qrDetailInfoService;

    /**
     * 生成待打印列表
     * @param id 单据id
     * @param tableNo 单据编号
     * @param warehouse 仓库
     * @param material 物料
     * @param batchNum 批号
     * @param quantity 数量
     * @param arrivalDate 到货日期
     * @param qrTypeCode 二维码类型
     */
    @Override
    @Transactional
    public void generatePrintList(Long id, String tableNo, BaseSetWarehouse warehouse, BaseSetMaterial material, String batchNum, BigDecimal quantity, Date arrivalDate, String qrTypeCode){
        if(!Dbs.exist(
                MaterialPrintInfo.TABLE_NAME,
                "VALID=1 AND SRC_ID=?",
                id
        )){
            MaterialPrintInfo printInfo = new MaterialPrintInfo();
            printInfo.setSrcId(id);
            printInfo.setSrcTableNo(tableNo);
            printInfo.setBatchCode(batchNum);
            printInfo.setIsRequest(true);
            printInfo.setMaterial(material);
            printInfo.setPrintNum(quantity);
            printInfo.setRemainNum(quantity);
            printInfo.setArrivalDate(arrivalDate);
            printInfo.setWare(warehouse);
            BigDecimal singleQuan = Dbs.getProp(material,BaseSetMaterial::getItemQty);
            printInfo.setItemAmount(singleQuan);
            printInfo.setRealHeavy(singleQuan);
            if(qrTypeCode!=null){
                printInfo.setDataSource(Dbs.load(BaseSetQrCodeType.class,"VALID=1 AND CODE=?",qrTypeCode));
            }
            printInfoService.savePrintInfo(printInfo,null);
        }
    }

    @Transactional
    @Override
    public void splitBatchAndSetIdBack(Long stockId, List<MaterialUnpackDetail> unpackDetails){
        MaterialStandingcrop stock = Dbs.load(MaterialStandingcrop.class,stockId);
        BaseSetQrCodeType source = Dbs.load(
                BaseSetQrCodeType.class,
                "VALID=1 AND CODE=?",
                "onHand"
        );
        //查询批次信息
        List<String> batchNumList = unpackDetails.stream().map(MaterialUnpackDetail::getBatchNum).distinct().collect(Collectors.toList());
        Map<String,Long> batchNum$info = Dbs.binaryMap(
                "SELECT BATCH_NUM,ID FROM "+ MaterialBatchInfo.TABLE_NAME+" WHERE VALID=1 AND MATERIAL_ID=? AND "+Dbs.inCondition("BATCH_NUM",batchNumList.size()),
                String.class,Long.class,
                Elements.toArray(stock.getGood().getId(),batchNumList)
        );
        //创建条码
        unpackDetails.forEach(unpackDetail->{
            MaterialQrDetailInfo qrDetailInfo = new MaterialQrDetailInfo();
            qrDetailInfo.setBatchText(unpackDetail.getBatchNum());
            qrDetailInfo.setBatchCode(Optional.ofNullable(batchNum$info.get(unpackDetail.getBatchNum())).map(info->Entities.ofId(MaterialBatchInfo.class,info)).orElse(null));
            qrDetailInfo.setMaterial(stock.getGood());
            qrDetailInfo.setAvailableQty(unpackDetail.getQuantity());
            qrDetailInfo.setItemQty(unpackDetail.getQuantity());
            qrDetailInfo.setPlaceSet(stock.getPlaceSet());
            qrDetailInfo.setWarehouse(stock.getWare());
            qrDetailInfo.setSrcId(stockId);
            qrDetailInfo.setDataSource(source);
            qrDetailInfoService.saveQrDetailInfo(qrDetailInfo,null);
            if(unpackDetail.getId()!=null){
                MaterialUnpackDetail detailForUpdate = PostSqlUpdater.getAutoWritableEntity(unpackDetail);
                //将对应待打印列表数量设置空
                Dbs.execute(
                        "UPDATE "+MaterialPrintInfo.TABLE_NAME+" SET REMAIN_NUM=? WHERE SRC_ID=?",
                        BigDecimal.ZERO,unpackDetail.getId()
                );
                //取消待打印
                detailForUpdate.setToBePrinted(false);
                //回填条码
                detailForUpdate.setBarCode(qrDetailInfo);
            }
            unpackDetail.setBarCode(qrDetailInfo);
        });
    }


    @Transactional
    @Override
    public void unpackStockAndSetIdBack(Long stockId, List<MaterialUnpackDetail> unpackDetails){
        MaterialStandingcrop stock = Dbs.load(MaterialStandingcrop.class,stockId);
        BaseSetQrCodeType source = Dbs.load(
                BaseSetQrCodeType.class,
                "VALID=1 AND CODE=?",
                "onHand"
        );
        //创建条码
        unpackDetails.forEach(unpackDetail->{
            MaterialQrDetailInfo qrDetailInfo = new MaterialQrDetailInfo();
            qrDetailInfo.setBatchCode(stock.getMaterBatchInfo());
            qrDetailInfo.setBatchText(stock.getBatchText());
            qrDetailInfo.setMaterial(stock.getGood());
            qrDetailInfo.setAvailableQty(unpackDetail.getQuantity());
            qrDetailInfo.setItemQty(unpackDetail.getQuantity());
            qrDetailInfo.setPlaceSet(stock.getPlaceSet());
            qrDetailInfo.setWarehouse(stock.getWare());
            qrDetailInfo.setSrcId(stockId);
            qrDetailInfo.setDataSource(source);
            qrDetailInfoService.saveQrDetailInfo(qrDetailInfo,null);
            if(unpackDetail.getId()!=null){
                MaterialUnpackDetail detailForUpdate = PostSqlUpdater.getAutoWritableEntity(unpackDetail);
                //将对应待打印列表数量设置空
                Dbs.execute(
                        "UPDATE "+MaterialPrintInfo.TABLE_NAME+" SET REMAIN_NUM=? WHERE SRC_ID=?",
                        BigDecimal.ZERO,unpackDetail.getId()
                );
                //取消待打印
                detailForUpdate.setToBePrinted(false);
                //回填条码
                detailForUpdate.setBarCode(qrDetailInfo);
            }
            unpackDetail.setBarCode(qrDetailInfo);
        });
    }
}
