package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.material.entities.MaterialUnpackDetail;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 条码台账
 */
public interface ModelBarCodeService {

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
    void generatePrintList(Long id, String tableNo, BaseSetWarehouse warehouse, BaseSetMaterial material, String batchNum, BigDecimal quantity, Date arrivalDate, String qrTypeCode);

    void splitBatchAndSetIdBack(Long stockId, List<MaterialUnpackDetail> unpackDetails);

    /**
     * 生成拆包条码，并回填条码id至明细
     * @param stockId 现存量ID
     * @param unpackDetails 拆包明细
     */
    void unpackStockAndSetIdBack(Long stockId, List<MaterialUnpackDetail> unpackDetails);
}
