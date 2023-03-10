package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity;
import com.supcon.orchid.material.entities.MaterialBatchInfo;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Elements;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.material.superwms.config.MaterialSystemConfig;
import com.supcon.orchid.material.superwms.constants.BatchUniqueRule;
import com.supcon.orchid.material.superwms.services.ModelBatchInfoService;
import com.supcon.orchid.material.superwms.util.MaterialExceptionThrower;
import com.supcon.orchid.orm.entities.IdEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ModelBatchInfoServiceImpl implements ModelBatchInfoService {

    @Override
    @Transactional
    public MaterialBatchInfo getBatchInfo(BaseSetMaterial material, String batchNum){
        return Dbs.load(
                MaterialBatchInfo.class,
                "VALID=1 AND MATERIAL_ID=? AND BATCH_NUM=?",
                material.getId(),batchNum
        );
    }


    @Override
    @Transactional
    public MaterialBatchInfo getBatchInfo(Long materialId, String batchNum){
        return Dbs.load(
                MaterialBatchInfo.class,
                "VALID=1 AND MATERIAL_ID=? AND BATCH_NUM=?",
                materialId,batchNum
        );
    }

    @Override
    @Transactional
    public MaterialBatchInfo getBatchInfo(String originBatchNum){
        return Dbs.load(
                MaterialBatchInfo.class,
                "VALID=1 AND ORIGIN_BATCH_NUM=?",
                originBatchNum
        );
    }

    @Override
    @Transactional
    public boolean batchExist(BaseSetMaterial material, String batchNum){
        return Dbs.exist(
                MaterialBatchInfo.TABLE_NAME,
                "VALID=1 AND MATERIAL_ID=? AND BATCH_NUM=?",
                material.getId(),batchNum
        );
    }

    @Override
    @Transactional
    public Set<String> getMaterialCodes(String batchNum){
        return Dbs.stream(
                "SELECT m.CODE FROM "+MaterialBatchInfo.TABLE_NAME+" b LEFT JOIN "+BaseSetMaterial.TABLE_NAME+" m ON b.MATERIAL_ID=m.ID WHERE VALID=1 AND BATCH_NUM=?",
                String.class,
                batchNum
        ).collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public boolean batchExist(String originBatchNum){
        return Dbs.exist(
                MaterialBatchInfo.TABLE_NAME,
                "VALID=1 AND ORIGIN_BATCH_NUM=?",
                originBatchNum
        );
    }

    @Override
    @Transactional
    public Long getBatchInfoId(BaseSetMaterial material, String batchNum){
        return Dbs.first(
                "SELECT ID FROM "+MaterialBatchInfo.TABLE_NAME+" WHERE VALID=1 AND MATERIAL_ID=? AND BATCH_NUM=?",
                Long.class,
                material.getId(),batchNum
        );
    }

    @Override
    @Transactional
    public void deleteBatchInfo(BaseSetMaterial material, String batchNum){
        Dbs.execute(
                "UPDATE "+MaterialBatchInfo.TABLE_NAME+" SET VALID=0,DELETE_TIME=? WHERE VALID=1 AND MATERIAL_ID=? AND BATCH_NUM=?",
                material.getId(),batchNum
        );
    }

    @Override
    @Transactional
    public Long getBatchInfoId(Long materialId, String batchNum){
        return Dbs.first(
                "SELECT ID FROM "+MaterialBatchInfo.TABLE_NAME+" WHERE VALID=1 AND MATERIAL_ID=? AND BATCH_NUM=?",
                Long.class,
                materialId,batchNum
        );
    }

    @Override
    @Transactional
    public Long getBatchInfoId(String originBatchNum){
        return Dbs.first(
                "SELECT ID FROM "+MaterialBatchInfo.TABLE_NAME+" WHERE VALID=1 AND ORIGIN_BATCH_NUM=?",
                Long.class,
                originBatchNum
        );
    }

    @Override
    public String getInConditionOfMaterials(List<Pair<String, Long>> batch$material){
        return batch$material.isEmpty()?"1=0":"("+batch$material.stream().map(pair->"(BATCH_NUM='"+pair.getFirst()+"' AND MATERIAL_ID="+pair.getSecond()+")").collect(Collectors.joining(" OR "))+")";
    }

    @Override
    public void checkBatchConflictByPiece(List<Pair<String, Long>> batchByPiece){
        String conflictBatch = Strings.join(",",
                //???????????????????????????
                Elements.repeat(batchByPiece).stream().map(Pair::getFirst),
                //???????????????????????????
                Dbs.stream(
                        "SELECT BATCH_NUM FROM "+MaterialBatchInfo.TABLE_NAME+" WHERE VALID=1 AND ID IN(SELECT MATER_BATCH_INFO FROM "+ MaterialStandingcrop.TABLE_NAME +" WHERE VALID=1 AND ONHAND>0) AND "+getInConditionOfMaterials(batchByPiece),
                        String.class
                )
        );
        if(Strings.valid(conflictBatch)){
            MaterialExceptionThrower.piece_batch_cannot_repeat(conflictBatch);
        }
    }

    @Autowired
    private MaterialSystemConfig materialSystemConfig;

    @Override
    public <T extends AbstractEcPartEntity> List<T> checkBatchRuleAndFilterExist(List<T> detailsWithBatch, Function<T,BaseSetMaterial> materialGetter,Function<T,String> batchGetter, boolean checkConflict) {
        if(BatchUniqueRule.BATCH.equals(materialSystemConfig.getBatchUniqueRule())){
            //??????????????????????????????????????????
            //1.??????????????????????????????????????????
            List<String> batchNumList = detailsWithBatch.stream().map(batchGetter).distinct().collect(Collectors.toList());
            Map<String,Set<Long>> batch$materials = Dbs.pairList(
                            "SELECT BATCH_NUM,MATERIAL_ID FROM "+MaterialBatchInfo.TABLE_NAME+" WHERE VALID=1 AND "+Dbs.inCondition("BATCH_NUM",batchNumList.size()),
                            String.class,Long.class,
                            batchNumList.toArray()
                    ).stream()
                    .collect(Collectors.groupingBy(Pair::getFirst))
                    .entrySet()
                    .stream().collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry->entry.getValue().stream().map(Pair::getSecond).collect(Collectors.toSet())
                    ));
            //????????????????????????????????????????????????????????????????????????
            detailsWithBatch.stream().collect(Collectors.groupingBy(batchGetter)).forEach((batch, groupDetails)->{
                //??????????????????????????????????????????
                Set<Long> existMaterialIds = batch$materials.get(batch);
                List<Long> currentMaterialIds = groupDetails.stream().map(materialGetter).filter(Objects::nonNull).map(IdEntity::getId).distinct().collect(Collectors.toList());
                if(currentMaterialIds.size()>1||(existMaterialIds!=null&&!existMaterialIds.isEmpty()&&!existMaterialIds.contains(currentMaterialIds.get(0)))){
                    //??????????????????????????????1????????? ????????????????????????????????????????????????????????????
                    MaterialExceptionThrower.cannot_refer_by_diff_material(batch);
                }
            });
            //???????????????
            if(batch$materials.keySet().size()>0){
                if(checkConflict){
                    //??????????????????????????????????????????
                    MaterialExceptionThrower.batch_existed(String.join(",",batch$materials.keySet()));
                } else {
                    return detailsWithBatch.stream().filter(detail->!batch$materials.containsKey(batchGetter.apply(detail))).collect(Collectors.toList());
                }
            }
        } else {
            //???????????????????????????????????????????????????????????????
            List<String> batchNumList = detailsWithBatch.stream().map(batchGetter).distinct().collect(Collectors.toList());
            //????????????????????????$????????????
            Map<String,Set<Long>> batch$materials =  Dbs.pairList(
                    "SELECT BATCH_NUM,MATERIAL_ID FROM "+MaterialBatchInfo.TABLE_NAME+" WHERE VALID=1 AND "+Dbs.inCondition("BATCH_NUM",batchNumList.size()),
                    String.class,Long.class,
                    batchNumList.toArray()
            ).stream().collect(Collectors.groupingBy(Pair::getFirst)).entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry->entry.getValue().stream().map(Pair::getSecond).collect(Collectors.toSet())
            ));
            List<T> detailWithBatchExist = detailsWithBatch.stream().filter(detail->{
                //???????????????????????????????????????????????????
                return Optional.ofNullable(batch$materials.get(batchGetter.apply(detail))).filter(set->set.contains(materialGetter.apply(detail).getId())).isPresent();
            }).collect(Collectors.toList());

            if(detailWithBatchExist.size()>0){
                if(checkConflict){
                    //??????????????????????????????????????????
                    MaterialExceptionThrower.batch_existed(detailWithBatchExist.stream().map(batchGetter).collect(Collectors.joining(",")));
                } else {
                    //????????????????????????
                    return detailsWithBatch.stream().filter(detail->!detailWithBatchExist.contains(detail)).collect(Collectors.toList());
                }
            }
        }
        return detailsWithBatch;
    }
}
