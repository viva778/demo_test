package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity;
import com.supcon.orchid.material.entities.MaterialBatchInfo;
import com.supcon.orchid.fooramework.support.Pair;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public interface ModelBatchInfoService {

    @Transactional
    MaterialBatchInfo getBatchInfo(BaseSetMaterial material, String batchNum);

    @Transactional
    MaterialBatchInfo getBatchInfo(Long materialId, String batchNum);

    @Transactional
    MaterialBatchInfo getBatchInfo(String originBatchNum);

    @Transactional
    boolean batchExist(BaseSetMaterial material, String batchNum);

    @Transactional
    Set<String> getMaterialCodes(String batchNum);

    @Transactional
    boolean batchExist(String originBatchNum);

    @Transactional
    Long getBatchInfoId(BaseSetMaterial material, String batchNum);

    void deleteBatchInfo(BaseSetMaterial material, String batchNum);

    @Transactional
    Long getBatchInfoId(Long materialId, String batchNum);

    @Transactional
    Long getBatchInfoId(String batchNum);

    String getInConditionOfMaterials(List<Pair<String, Long>> batch$material);

    /**
     * 校验：
     * 按件批次不能重复入库
     * @param batchByPiece 按件批次$物料ID 列表
     */
    void checkBatchConflictByPiece(List<Pair<String,Long>> batchByPiece);

    /**
     * 校验批次规则，并过滤
     */
    <T extends AbstractEcPartEntity> List<T> checkBatchRuleAndFilterExist(List<T> detailsWithBatch, Function<T,BaseSetMaterial> materialGetter, Function<T,String> batchGetter, boolean checkConflict);
}
