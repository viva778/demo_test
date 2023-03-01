package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.material.entities.MaterialStDistribution;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.entities.MaterialStjStockRecord;

import java.util.List;

public interface TableStocktakingService {

    /**
     * 查询盘点任务下影响的现存量
     * @param stocktakingJobId 盘点任务ID
     * @param staffId 人员ID，不填则不指定
     * @param placeModelIds 货位建模IDS
     * @param includes 现存量包含属性
     * @return 现存量对象
     */
    List<MaterialStandingcrop> findStocksByStocktakingJobIdAndStaffId(Long stocktakingJobId, Long staffId, Long[] placeModelIds, String includes);

    /**
     * 查询盘点单下的现存量盘点记录
     * @param stocktakingId 盘点单ID
     */
    List<MaterialStjStockRecord> findFinalStockRecordByStocktakingId(Long stocktakingId);

    /**
     * 查询盘点单下影响的现存量
     * @param stocktakingId 盘点单ID
     * @param staffId 人员ID，不填则不指定
     * @param onlyCodes 货位唯一码
     * @param includes 现存量包含属性
     * @return 现存量对象
     */
    List<MaterialStandingcrop> findStocksByStocktakingIdAndStaffId(Long stocktakingId, Long staffId, String[] onlyCodes, String includes);

    /**
     * 查找正在盘点现存量的盘点单
     * @param stock 现存量
     * @return 盘点单ID
     */
    Long findStocktakingIdByStock(MaterialStandingcrop stock);

    String getStockKey(MaterialStandingcrop stock);

    void confirmResult(Long recordId);

    void quickEnd(Long stocktakingId);

    void saveStockRecord(List<MaterialStjStockRecord> stockRecords);

    List<String> getUndoneTargetCodesAndSubmitIfDone(Long stocktakingJobId, Long staffId);

    void generateRecheckTask(List<MaterialStDistribution> distributions, Long stocktakingId);

    void generateStocktakingByStrategy();
}
