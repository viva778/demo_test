package com.supcon.orchid.material.superwms.services.impl;

import com.google.common.collect.ImmutableMap;
import com.supcon.orchid.id.SnowFlakeIdWorker;
import com.supcon.orchid.material.entities.MaterialBatchInfo;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.entities.MaterialUnpackDetail;
import com.supcon.orchid.fooramework.support.ModuleRequestContext;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.ModuleHttpClient;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.material.services.MaterialBatchInfoService;
import com.supcon.orchid.material.superwms.config.MaterialSystemConfig;
import com.supcon.orchid.material.superwms.entities.dto.PrinterDTO;
import com.supcon.orchid.material.superwms.entities.dto.PrinterPrintDTO;
import com.supcon.orchid.material.superwms.entities.dto.UnpackDetailDTO;
import com.supcon.orchid.material.superwms.entities.dto.UnpackPrintDTO;
import com.supcon.orchid.material.superwms.services.ModelBarCodeService;
import com.supcon.orchid.material.superwms.services.ModelBatchInfoService;
import com.supcon.orchid.material.superwms.services.ModelPrintService;
import com.supcon.orchid.material.superwms.util.factories.BatchInfoFactory;
import com.supcon.orchid.services.BAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModelPrintServiceImpl implements ModelPrintService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ModelBarCodeService barCodeService;
    @Autowired
    private MaterialSystemConfig materialSystemConfig;
    @Autowired
    private ModelBatchInfoService modelBatchInfoService;
    @Autowired
    private MaterialBatchInfoService batchInfoService;

    @Transactional
    @Override
    public List<UnpackDetailDTO> splitBatchAndPrint(UnpackPrintDTO unpackPrintDTO) {
        MaterialStandingcrop stock = Dbs.load(MaterialStandingcrop.class, unpackPrintDTO.getStockId());
        //1.?????????????????????dto??????
        Map<UnpackDetailDTO, MaterialUnpackDetail> detailDto$unpackDetail = new LinkedHashMap<>();
        unpackPrintDTO.getDetails().forEach(dto -> {
            MaterialUnpackDetail detail = new MaterialUnpackDetail();
            detail.setId(dto.getUnpackDetailId());
            detail.setBatchNum(dto.getBatchNum());
            detail.setQuantity(dto.getQuantity());
            detailDto$unpackDetail.put(dto, detail);
        });

        List<MaterialUnpackDetail> unpackDetails = new ArrayList<>(detailDto$unpackDetail.values());
        //??????????????????????????????
        List<MaterialUnpackDetail> detailsWithoutBatchInfo = modelBatchInfoService.checkBatchRuleAndFilterExist(unpackDetails, val -> stock.getGood(), MaterialUnpackDetail::getBatchNum, false);
        if (!detailsWithoutBatchInfo.isEmpty()) {
            //??????????????????????????????????????????
            List<MaterialBatchInfo> batchInfoList = BatchInfoFactory.batchSplitBatchInfoList(Dbs.reload(stock.getMaterBatchInfo()), detailsWithoutBatchInfo);
            batchInfoList.forEach(batchInfo -> batchInfoService.saveBatchInfo(batchInfo, null));
        }
        //2.????????????????????????????????????
        barCodeService.splitBatchAndSetIdBack(unpackPrintDTO.getStockId(), unpackDetails);
        detailDto$unpackDetail.forEach((dto, detail) -> {
            //??????dto?????????
            dto.setBarCode(detail.getBarCode());
        });
        //3.????????????????????????
        if (Boolean.TRUE.equals(materialSystemConfig.getEnablePrinter())) {
            PrinterDTO printerDTO = unpackPrintDTO.getPrinter();
            detailDto$unpackDetail.forEach((dto, detail) -> {
                //????????????
                PrinterPrintDTO printerPrintDTO = new PrinterPrintDTO();
                //???????????????
                printerPrintDTO.setPrinterName(printerDTO.getPrinterName());
                printerPrintDTO.setClientIp(printerDTO.getClientIp());
                printerPrintDTO.setTemplateName(printerDTO.getTemplateName());
                //?????????????????????
                String filename = "unpack" + SnowFlakeIdWorker.getInstance().nextId();
                printerPrintDTO.setFileName(filename);
                //???????????????pk?????????id
                printerPrintDTO.setParameterMap(ImmutableMap.of(
                        "pk", detail.getBarCode().getId()
                ));
                //??????????????????
                String result = ModuleHttpClient.exchange(ModuleRequestContext.builder()
                        .moduleName("pdf-generator")
                        .path("/pdf-generator/generateAndPrint")
                        .method(HttpMethod.POST)
                        .body(printerPrintDTO)
                        .connectRequestTimeout(5 * 1000)
                        .connectTimeout(2 * 60 * 1000)
                        .readTimeout(10 * 60 * 1000)
                        .log(true).build(), String.class);
                if (!Strings.valid(result) || !result.contains("SUCCESS")) {
                    throw new BAPException("?????????????????????????????????????????????" + result);
                }
            });
        }
        return unpackPrintDTO.getDetails();
    }

    @Transactional
    @Override
    public List<UnpackDetailDTO> unpackStockAndPrint(UnpackPrintDTO unpackPrintDTO) {
        //1.?????????????????????dto??????
        Map<UnpackDetailDTO, MaterialUnpackDetail> detailDto$unpackDetail = new LinkedHashMap<>();
        unpackPrintDTO.getDetails().forEach(dto -> {
            MaterialUnpackDetail detail = new MaterialUnpackDetail();
            detail.setId(dto.getUnpackDetailId());
            detail.setQuantity(dto.getQuantity());
            detailDto$unpackDetail.put(dto, detail);
        });
        //2.????????????????????????????????????
        barCodeService.unpackStockAndSetIdBack(unpackPrintDTO.getStockId(), new ArrayList<>(detailDto$unpackDetail.values()));
        detailDto$unpackDetail.forEach((dto, detail) -> {
            //??????dto?????????
            dto.setBarCode(detail.getBarCode());
        });
        //3.????????????????????????
        if (Boolean.TRUE.equals(materialSystemConfig.getEnablePrinter())) {
            PrinterDTO printerDTO = unpackPrintDTO.getPrinter();
            detailDto$unpackDetail.forEach((dto, detail) -> {
                //????????????
                PrinterPrintDTO printerPrintDTO = new PrinterPrintDTO();
                //???????????????
                printerPrintDTO.setPrinterName(printerDTO.getPrinterName());
                printerPrintDTO.setClientIp(printerDTO.getClientIp());
                printerPrintDTO.setTemplateName(printerDTO.getTemplateName());
                //?????????????????????
                String filename = "unpack" + SnowFlakeIdWorker.getInstance().nextId();
                printerPrintDTO.setFileName(filename);
                //???????????????pk?????????id
                printerPrintDTO.setParameterMap(ImmutableMap.of(
                        "pk", detail.getBarCode().getId()
                ));
                //??????????????????
                String result = null;
                try {
                    result = ModuleHttpClient.exchange(ModuleRequestContext.builder()
                            .moduleName("pdf-generator")
                            .path("/pdf-generator/generateAndPrint")
                            .method(HttpMethod.POST)
                            .body(printerPrintDTO)
                            .connectRequestTimeout(5 * 1000)
                            .connectTimeout(2 * 60 * 1000)
                            .readTimeout(10 * 60 * 1000)
                            .log(true).build(), String.class);
                } catch (Exception e) {
                    log.error(e.toString());
                    throw new BAPException("????????????????????????????????????????????????:\n" + e);
                }
                if (!Strings.valid(result) || !result.contains("SUCCESS")) {
                    throw new BAPException("?????????????????????????????????????????????" + result);
                }
            });
        }
        return unpackPrintDTO.getDetails();
    }

}
