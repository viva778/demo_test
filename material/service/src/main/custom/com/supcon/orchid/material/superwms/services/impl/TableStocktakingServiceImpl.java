package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.util.ArrayOperator;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.*;
import com.supcon.orchid.material.services.*;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseBatchType;
import com.supcon.orchid.material.superwms.constants.systemcode.FullOrSpot;
import com.supcon.orchid.material.superwms.constants.systemcode.StocktakingWay;
import com.supcon.orchid.material.superwms.constants.systemcode.WareType;
import com.supcon.orchid.material.superwms.entities.support.StocktakingRange;
import com.supcon.orchid.material.superwms.services.ModelWareModelService;
import com.supcon.orchid.fooramework.services.PlatformWorkflowService;
import com.supcon.orchid.material.superwms.services.TableStocktakingService;
import com.supcon.orchid.material.superwms.util.MaterialExceptionThrower;
import com.supcon.orchid.orm.entities.CodeEntity;
import com.supcon.orchid.orm.entities.IdEntity;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TableStocktakingServiceImpl implements TableStocktakingService {

    @Override
    @Transactional
    public List<MaterialStandingcrop> findStocksByStocktakingJobIdAndStaffId(Long stocktakingJobId, Long staffId, Long[] placeModelIds, String includes){
        Long stocktakingId = Dbs.first(
                "SELECT STOCKTAKING FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE VALID=1 AND ID=?",
                Long.class,
                stocktakingJobId
        );
        //???????????????findStocksByStocktakingIdAndStaffId???????????????????????????????????????stocktakingJobId??????
        List<Criterion> criteria = new LinkedList<>();
        //1.????????????
        List<Long> materialIds = Dbs.stream(
                "SELECT DISTINCT MATERIAL FROM "+ MaterialStMultiMat.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING=?",
                Long.class,
                stocktakingId
        ).collect(Collectors.toList());
        if(!materialIds.isEmpty()){
            criteria.add(Restrictions.in("good.id", materialIds));
        }
        //2.????????????
        List<Object> params = new LinkedList<>();

        String sqlSelectOnlyCodes;
        if(placeModelIds !=null&& placeModelIds.length!=0){
            params.add(WareType.CARGO_PLACE);
            params.addAll(Arrays.asList(placeModelIds));
            sqlSelectOnlyCodes = "SELECT ONLY_CODE FROM "+MaterialWareModel.TABLE_NAME+" WHERE WARE_TYPE=? AND "+Dbs.inCondition("ID", placeModelIds.length);
        } else {
            params.add(WareType.CARGO_PLACE);
            params.add(stocktakingId);
            String sqlSelectWareModels;
            //????????????????????????
            String sqlSelectDistributionByJob = "SELECT DISTRIBUTION FROM "+MaterialStjDetail.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING_JOB=?";
            params.add(stocktakingJobId);
            if(staffId!=null){
                //?????????????????????????????????????????????
                params.add(staffId);
                String sqlSelectDistributionByStaff = "SELECT DISTRIBUTION FROM "+MaterialStMultiStaff.TABLE_NAME+" WHERE STAFF=?";
                sqlSelectWareModels = "SELECT TARGET FROM "+ MaterialStDistribution.TABLE_NAME+" WHERE STOCKTAKING=? AND ID IN("+sqlSelectDistributionByJob+") AND ID IN("+sqlSelectDistributionByStaff+")";
            } else {
                //??????????????????
                sqlSelectWareModels = "SELECT TARGET FROM "+ MaterialStDistribution.TABLE_NAME+" WHERE STOCKTAKING=? AND ID IN("+sqlSelectDistributionByJob+")";
            }
            sqlSelectOnlyCodes = "SELECT ONLY_CODE FROM "+MaterialWareModel.TABLE_NAME+" WHERE WARE_TYPE=? AND ID IN("+sqlSelectWareModels+")";
        }
        String sqlSelectPlaceIds = "SELECT ID FROM "+ BaseSetStoreSet.TABLE_NAME+" WHERE VALID=1 AND ONLY_CODE IN("+sqlSelectOnlyCodes+")";
        List<Long> placeIds = Dbs.stream(sqlSelectPlaceIds,Long.class, params.toArray()).collect(Collectors.toList());

        if(!placeIds.isEmpty()){
            criteria.add(Restrictions.in("placeSet.id",placeIds));
        } else {
            //??????????????????????????????????????????
            Long warehouseId = Dbs.first(
                    "SELECT WAREHOUSE FROM "+MaterialStocktaking.TABLE_NAME+" WHERE ID=?",
                    Long.class,
                    stocktakingId
            );
            criteria.add(Restrictions.eq("ware.id",warehouseId));
        }
        criteria.add(Restrictions.eq("valid",true));
        criteria.add(Restrictions.gt("availiQuantity", BigDecimal.ZERO));
        //?????????????????????
        return Hbs.findByCriteriaWithIncludes(
                MaterialStandingcrop.class,
                includes,
                criteria.toArray(new Criterion[0])
        );
    }

    @Override
    @Transactional
    public List<MaterialStjStockRecord> findFinalStockRecordByStocktakingId(Long stocktakingId){
        return Dbs.findByCondition(
                MaterialStjStockRecord.class,
                "VALID=1 AND CHECKED=1 AND QUANTITY_BY_COUNT IS NOT NULL AND STOCKTAKING_JOB IN(SELECT ID FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING=?)",
                stocktakingId
        );
    }

    @Override
    @Transactional
    public List<MaterialStandingcrop> findStocksByStocktakingIdAndStaffId(Long stocktakingId, Long staffId, String[] onlyCodes, String includes){
        List<Criterion> criteria = new LinkedList<>();
        //1.????????????
        List<Long> materialIds = Dbs.stream(
                "SELECT DISTINCT MATERIAL FROM "+ MaterialStMultiMat.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING=?",
                Long.class,
                stocktakingId
        ).collect(Collectors.toList());
        if(!materialIds.isEmpty()){
            criteria.add(Restrictions.in("good.id", materialIds));
        }
        //2.????????????
        List<Object> params = new LinkedList<>();

        String sqlSelectPlaceIds;
        if(onlyCodes!=null&&onlyCodes.length!=0){
            sqlSelectPlaceIds = "SELECT ID FROM "+ BaseSetStoreSet.TABLE_NAME+" WHERE VALID=1 AND "+Dbs.inCondition("ONLY_CODE",onlyCodes.length);
            params.addAll(Arrays.asList(onlyCodes));
        } else {
            params.add(WareType.CARGO_PLACE);
            params.add(stocktakingId);
            String sqlSelectWareModels;
            if(staffId!=null){
                //?????????????????????????????????????????????
                params.add(staffId);
                String sqlSelectDistribution = "SELECT DISTRIBUTION FROM "+MaterialStMultiStaff.TABLE_NAME+" WHERE STAFF=?";
                sqlSelectWareModels = "SELECT TARGET FROM "+ MaterialStDistribution.TABLE_NAME+" WHERE STOCKTAKING=? AND ID IN("+sqlSelectDistribution+")";
            } else {
                //??????????????????
                sqlSelectWareModels = "SELECT TARGET FROM "+ MaterialStDistribution.TABLE_NAME+" WHERE STOCKTAKING=?";
            }
            String sqlSelectOnlyCodes = "SELECT ONLY_CODE FROM "+MaterialWareModel.TABLE_NAME+" WHERE WARE_TYPE=? AND ID IN("+sqlSelectWareModels+")";
            sqlSelectPlaceIds = "SELECT ID FROM "+ BaseSetStoreSet.TABLE_NAME+" WHERE VALID=1 AND ONLY_CODE IN("+sqlSelectOnlyCodes+")";
        }
        List<Long> placeIds = Dbs.stream(sqlSelectPlaceIds,Long.class, params.toArray()).collect(Collectors.toList());

        if(!placeIds.isEmpty()){
            criteria.add(Restrictions.in("placeSet.id",placeIds));
        } else {
            //??????????????????????????????????????????
            Long warehouseId = Dbs.first(
                    "SELECT WAREHOUSE FROM "+MaterialStocktaking.TABLE_NAME+" WHERE ID=?",
                    Long.class,
                    stocktakingId
            );
            criteria.add(Restrictions.eq("ware.id",warehouseId));
        }
        criteria.add(Restrictions.eq("valid",true));
        criteria.add(Restrictions.gt("availiQuantity", BigDecimal.ZERO));
        //?????????????????????
        return Hbs.findByCriteriaWithIncludes(
                MaterialStandingcrop.class,
                includes,
                criteria.toArray(new Criterion[0])
        );
    }


    @Override
    public Long findStocktakingIdByStock(MaterialStandingcrop stock){
        //??????????????????
        List<StocktakingRange> stocktakingRanges = getStocktakingRanges();
        //????????????
        return stocktakingRanges.stream().filter(range->{
            //??????
            if(!range.getWarehouseId().equals(stock.getWare().getId())){
                return false;
            }
            //??????
            if(stock.getPlaceSet()!=null&&range.getPlaceIds()!=null&&!range.getPlaceIds().contains(stock.getPlaceSet().getId())){
                return false;
            }
            //??????
            if(range.getMaterialIds()!=null&&range.getMaterialIds().size()>0&&!range.getMaterialIds().contains(stock.getGood().getId())){
                return false;
            }
            return true;
        }).map(StocktakingRange::getStocktakingId).findFirst().orElse(null);

    }


    /**
     * ??????????????????????????????
     */
    public List<StocktakingRange> getStocktakingRanges(){
        return RequestCaches.computeIfAbsent("__stocktaking_ranges__",k->{
            //1.?????????????????????????????????????????????????????????
            Map<Long,Long> stocktaking$warehouse = Dbs.binaryMap(
                    "SELECT ID,WAREHOUSE FROM "+MaterialStocktaking.TABLE_NAME+" WHERE VALID=1 AND STATUS=88 AND ID IN(SELECT STOCKTAKING FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE VALID=1)",
                    Long.class,Long.class
            );
            if(!stocktaking$warehouse.isEmpty()){
                //????????????????????????????????????
                //?????? ????????????
                Map<Long,Set<Long>> stocktaking$materials = Dbs.pairList(
                        "SELECT STOCKTAKING,MATERIAL FROM "+MaterialStMultiMat.TABLE_NAME+" WHERE VALID=1 AND "+Dbs.inCondition("STOCKTAKING",stocktaking$warehouse.size()),
                        Long.class,Long.class,
                        stocktaking$warehouse.keySet().toArray()
                ).stream().collect(Collectors.groupingBy(Pair::getFirst)).entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry->entry.getValue().stream().map(Pair::getSecond).collect(Collectors.toSet())
                ));
                //?????? ???????????????
                List<Pair<Long,String>> stocktaking$placeCodes = Dbs.pairList(
                        "SELECT d.STOCKTAKING,w.ONLY_CODE FROM "+MaterialStDistribution.TABLE_NAME+" d LEFT JOIN "+MaterialWareModel.TABLE_NAME+" w ON d.TARGET=w.ID WHERE d.VALID=1 AND w.WARE_TYPE=? AND "+Dbs.inCondition("d.STOCKTAKING",stocktaking$warehouse.size()),
                        Long.class,String.class,
                        Elements.toArray(WareType.CARGO_PLACE,stocktaking$warehouse.keySet())
                );
                List<String> onlyCodes = stocktaking$placeCodes.stream().map(Pair::getSecond).distinct().collect(Collectors.toList());

                //????????? ??????
                Map<String,Long> onlyCode$place = Dbs.binaryMap(
                        "SELECT ONLY_CODE,ID FROM "+BaseSetStoreSet.TABLE_NAME+" WHERE VALID=1 AND "+ Dbs.inCondition("ONLY_CODE",onlyCodes.size()),
                        String.class,Long.class,
                        onlyCodes.toArray()
                );
                //????????? ??????
                Map<Long,Set<Long>> stocktaking$places = stocktaking$placeCodes
                        .stream()
                        .collect(Collectors.groupingBy(Pair::getFirst))
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry->entry.getValue().stream().map(Pair::getSecond).map(onlyCode$place::get).collect(Collectors.toSet())));

                return stocktaking$warehouse.entrySet().stream().map(entry->{
                    StocktakingRange stocktakingRange = new StocktakingRange();
                    stocktakingRange.setStocktakingId(entry.getKey());
                    stocktakingRange.setWarehouseId(entry.getValue());
                    stocktakingRange.setMaterialIds(stocktaking$materials.get(entry.getKey()));
                    stocktakingRange.setPlaceIds(stocktaking$places.get(entry.getKey()));
                    return stocktakingRange;
                }).collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        });
    }


    @Override
    public String getStockKey(MaterialStandingcrop stock){
        return Strings.join(",",
                stock.getGood().getId(),
                Optional.ofNullable(stock.getMaterBatchInfo()).map(IdEntity::getId).map(String::valueOf).orElse(""),
                Optional.ofNullable(stock.getPlaceSet()).map(IdEntity::getId).map(String::valueOf).orElse("")
        );
    }

    @Override
    @Transactional
    public void confirmResult(Long recordId){
        MaterialStjStockRecord record = Hbs.loadWithIncludes(
                MaterialStjStockRecord.class,
                "id,stockKey,stocktakingJob.id,stocktakingJob.stocktaking.id",
                recordId
        );
        Assert.notNull(record,"record is missing");
        Long stocktakingId = record.getStocktakingJob().getStocktaking().getId();
        //????????????????????????????????????????????????stockKey??????????????????????????????
        Dbs.execute(
                "UPDATE "+MaterialStjStockRecord.TABLE_NAME+" SET CHECKED=0 WHERE STOCK_KEY=? AND ID<>? AND STOCKTAKING_JOB IN(SELECT ID FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING=?)",
                record.getStockKey(),recordId,stocktakingId
        );
        //????????????????????????
        Dbs.execute(
                "UPDATE "+MaterialStjStockRecord.TABLE_NAME+" SET CHECKED=1 WHERE ID=?",
                recordId
        );
    }

    @Autowired
    private PlatformWorkflowService workflowService;

    @Override
    @Transactional
    public void quickEnd(Long stocktakingId){
        MaterialStocktaking stocktaking = Dbs.load(
                MaterialStocktaking.class,
                stocktakingId
        );
        Assert.notNull(stocktaking,"stocktaking is missing");
        //?????????????????????????????????
        if(stocktaking.getStatus()!=88){
            //???????????????
            MaterialExceptionThrower.stocktaking_job_finished_already();
        }
        if(!Dbs.exist(
                MaterialStocktakingJob.TABLE_NAME,
                "VALID=1 AND STOCKTAKING=?",
                stocktakingId
        )){
            //???????????????
            MaterialExceptionThrower.stocktaking_job_not_start_yet();
        }
        //?????????????????????
        workflowService.submitTableToNext(stocktaking,null);
    }


    @Override
    @Transactional
    public void saveStockRecord(List<MaterialStjStockRecord> stockRecords){
        Elements.shunt(stockRecords,dto->dto.getId()!=null,Collectors.toList()).consume((listWithId,listWithoutId)->{
            //?????????ID?????????????????????
            listWithId.forEach(record->{
                record.setCreateStaffId(Organazations.getCurrentStaffId());
                PostSqlUpdater.updateByFields(record,"valid,material.id,batchInfo.id,place.id,stockKey,quantityByCount,quantityOnBook");
            });
            //????????????ID?????????????????????
            listWithoutId.forEach(record->{
                record.setCreateStaffId(Organazations.getCurrentStaffId());
                Dbs.save(record);
            });
        });
    }

    @Autowired
    private MaterialStjSubmitRecService submitRecService;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * ???????????????????????????????????????????????????????????????????????????
     * @param stocktakingJobId ????????????ID
     * @param staffId ??????ID
     * @return ?????????????????????????????????
     */
    @Override
    @Transactional
    public List<String> getUndoneTargetCodesAndSubmitIfDone(Long stocktakingJobId, Long staffId){
        //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????

        //???????????????????????????
        List<MaterialStjStockRecord> stockRecords = Dbs.findByCondition(
                MaterialStjStockRecord.class,
                "VALID=1 AND STOCKTAKING_JOB=? AND CREATE_STAFF_ID=?",
                stocktakingJobId,staffId
        );
        //????????????
        boolean isFull = FullOrSpot.FULL.equals(Dbs.first(
                "SELECT FULL_OR_SPOT FROM "+MaterialStocktaking.TABLE_NAME+" WHERE ID=(SELECT STOCKTAKING FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE ID=?)",
                String.class,
                stocktakingJobId
        ));
        //???????????????????????????????????????????????????????????????????????????
        if(!isFull&&stockRecords.stream().noneMatch(record -> record.getQuantityByCount()!=null)){
            return Dbs.stream(
                    "SELECT ONLY_CODE FROM "+MaterialWareModel.TABLE_NAME+" WHERE WARE_TYPE=? AND ID IN(SELECT TARGET FROM "+MaterialStDistribution.TABLE_NAME+" WHERE STOCKTAKING=(SELECT STOCKTAKING FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE ID=?) AND ID IN(SELECT DISTRIBUTION FROM "+MaterialStMultiStaff.TABLE_NAME+" WHERE VALID=1 AND STAFF=?))",
                    String.class,
                    WareType.CARGO_PLACE,stocktakingJobId,staffId
            ).collect(Collectors.toList());
        }
        //?????????????????????
        List<MaterialStandingcrop> stocks = findStocksByStocktakingJobIdAndStaffId(stocktakingJobId,staffId,null,"id,placeSet.id,placeSet.onlyCode,ware.id,materBatchInfo.id,good.id");
        //??????????????????
        String warehouseCode = Dbs.first(
                "SELECT CODE FROM "+BaseSetWarehouse.TABLE_NAME+" WHERE ID=(SELECT WAREHOUSE FROM "+MaterialStocktaking.TABLE_NAME+" WHERE ID=(SELECT STOCKTAKING FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE ID=?))",
                String.class,
                stocktakingJobId
        );

        //1).????????????????????????????????????(??????) 2).???????????????????????????????????????????????????????????????????????????????????????
        //3).??????????????????????????????????????? 4).???????????????????????????Key????????????
        Set<String> savedStockKeys = stockRecords.stream().map(MaterialStjStockRecord::getStockKey).collect(Collectors.toSet());
        Set<String> repeatKeys = Elements.repeat(stockRecords.stream().map(MaterialStjStockRecord::getStockKey));
        //????????????????????????????????????????????????
        String[] recordNotExist = isFull
                ?stocks.stream()
                .filter(stock->!savedStockKeys.contains(getStockKey(stock)))
                .map(stock->Optional.ofNullable(stock.getPlaceSet()).map(place->Dbs.getProp(place,BaseSetStoreSet::getOnlyCode)).orElse(warehouseCode))
                .toArray(String[]::new)
                :new String[0];
        //??????????????????
        String[] singleCheckFail = stockRecords.stream().filter(record ->{
            if(record.getMaterial()==null||record.getMaterial().getId()==null){
                //????????????
                return true;
            }
            if(record.getQuantityByCount()==null){
                //??????????????????
                if(isFull){
                    return true;
                }
            } else {
                //?????????<0
                if(record.getQuantityByCount().compareTo(BigDecimal.ZERO)<0){
                    return true;
                }
            }
            if(record.getBatchInfo()==null&&BaseBatchType.isEnable(Dbs.getProp(record.getMaterial(), BaseSetMaterial::getIsBatch))){
                //???????????????????????????
                return true;
            }
            if(BaseBatchType.BY_PIECE.equals(Dbs.getProp(record.getMaterial(), BaseSetMaterial::getIsBatch, CodeEntity::getId))&&record.getQuantityByCount().compareTo(BigDecimal.ZERO)!=0&&record.getQuantityByCount().compareTo(BigDecimal.ONE)!=0){
                //?????????????????????0???1
                return true;
            }
            return false;
        }).map(record -> Optional.ofNullable(record.getPlace()).map(place->Dbs.getProp(place,BaseSetStoreSet::getOnlyCode)).orElse(warehouseCode)).toArray(String[]::new);
        //???????????????????????????Key?????????
        String[] repeatPlaces = stockRecords.stream().filter(record->repeatKeys.contains(record.getStockKey())).map(record->Optional.ofNullable(record.getPlace()).map(place->Dbs.getProp(place,BaseSetStoreSet::getOnlyCode)).orElse(warehouseCode)).toArray(String[]::new);
        log.info("recordNotExist.len:{},singleCheckFail.len:{},repeatPlaces.len:{}",new Object[]{
                recordNotExist.length,singleCheckFail.length,repeatPlaces.length
        });
        //????????????????????????????????????
        List<String> undoneOnlyCodes = ArrayOperator.of(Elements.toArray(
                String[]::new,
                recordNotExist,singleCheckFail,repeatPlaces
        )).stream().distinct().filter(Objects::nonNull).collect(Collectors.toList());
        if(undoneOnlyCodes.isEmpty()){
            //??????????????????
            MaterialStjSubmitRec submitRec = new MaterialStjSubmitRec();
            submitRec.setStocktakingJob(Entities.ofId(MaterialStocktakingJob.class,stocktakingJobId));
            submitRec.setSubmitPerson(Organazations.getCurrentStaff());
            submitRec.setSubmitTime(new Date());
            submitRecService.saveStjSubmitRec(submitRec,null);
            //????????????????????????
            List<MaterialStjStockRecord> records = Hbs.findByCriteriaWithProjections(
                    MaterialStjStockRecord.class,
                    "id,stockKey",
                    Restrictions.eq("stocktakingJob.id",stocktakingJobId),
                    Restrictions.eq("createStaffId",Organazations.getCurrentStaffId())
            );
            //????????????????????? ??????????????????????????????????????????
            Dbs.execute(
                    "UPDATE "+MaterialStjStockRecord.TABLE_NAME+" SET SUBMIT_RECORD=?,CHECKED=1 WHERE VALID=1 AND "+Dbs.inCondition("ID",records.size()),
                    Elements.toArray(submitRec.getId(),records.stream().map(IdEntity::getId))
            );
            //??????????????????????????????stockKey????????????????????????
            List<Long> stocktakingJobIdsWithSameStocktaking = Dbs.stream(
                    "SELECT ID FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING=(SELECT STOCKTAKING FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE ID=?)",
                    Long.class,
                    stocktakingJobId
            ).collect(Collectors.toList());
            Dbs.execute(
                    "UPDATE "+MaterialStjStockRecord.TABLE_NAME+" SET CHECKED=0 WHERE VALID=1 AND "+Dbs.inCondition("STOCKTAKING_JOB",stocktakingJobIdsWithSameStocktaking.size())+" AND "+Dbs.inCondition("STOCK_KEY",records.size())+" AND "+Dbs.notInCondition("ID",records.size()),
                    Elements.toArray(stocktakingJobIdsWithSameStocktaking,records.stream().map(MaterialStjStockRecord::getStockKey),records.stream().map(IdEntity::getId))
            );
            //????????????????????????+1
            Dbs.execute(
                    "UPDATE "+ MaterialStjDetail.TABLE_NAME+" SET TAKING_COUNT=TAKING_COUNT+1 WHERE VALID=1 AND STOCKTAKING_JOB=? AND DISTRIBUTION IN(SELECT DISTRIBUTION FROM "+ MaterialStMultiStaff.TABLE_NAME +" WHERE STAFF=?)",
                    stocktakingJobId,Organazations.getCurrentStaffId()
            );
        }
        return undoneOnlyCodes;
    }

    @Autowired
    private MaterialStDistributionService distributionService;
    @Autowired
    private MaterialStMultiStaffService multiStaffService;
    @Autowired
    private MaterialStocktakingJobService stocktakingJobService;
    @Autowired
    private MaterialStjDetailService stjDetailService;

    @Transactional
    @Override
    public void generateRecheckTask(List<MaterialStDistribution> distributions, Long stocktakingId){
        //1.??????????????????????????????
        ////1.????????????????????????????????????????????????
        List<Pair<Long,Long>> job$sub_staff = Dbs.pairList(
                "SELECT STOCKTAKING_JOB,SUBMIT_PERSON FROM "+MaterialStjSubmitRec.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING_JOB IN(SELECT ID FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING=?)",
                Long.class,Long.class,
                stocktakingId
        );
        ////2.job??????dist??????
        Map<Long,List<Long>> job$dist_list = Dbs.binaryMap(
                "SELECT DISTRIBUTION,STOCKTAKING_JOB FROM "+MaterialStjDetail.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING_JOB IN(SELECT ID FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING=?)",
                Long.class,Long.class,
                stocktakingId
        ).entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue)).entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry->entry.getValue().stream().map(Map.Entry::getKey).collect(Collectors.toList())
        ));
        ////3.?????????????????????????????????
        List<Pair<Long,Long>> dist$sub_staff = job$sub_staff
                .stream()
                .flatMap(pair-> job$dist_list.get(pair.getFirst()).stream().map(dist->Pair.of(dist,pair.getSecond())))
                .collect(Collectors.toList());

        ////4.?????????????????????????????????
        List<Pair<Long,Long>> dist$staff = Dbs.pairList(
                "SELECT DISTRIBUTION,STAFF FROM "+MaterialStMultiStaff.TABLE_NAME+" WHERE VALID=1 AND DISTRIBUTION IN(SELECT ID FROM "+MaterialStDistribution.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING=?)",
                Long.class,Long.class,
                stocktakingId
        );

        ////5.???????????????????????? ??????????????????(?????????)
        Map<Long,Set<Long>> dist$no_sub_staff_list = Elements.difference(dist$staff,dist$sub_staff).stream().collect(Collectors.groupingBy(Pair::getFirst)).entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry->entry.getValue().stream().map(Pair::getSecond).collect(Collectors.toSet())
        ));

        ////6.??????????????????????????????
        Map<Long,Long> dist$target = Dbs.binaryMap(
                "SELECT ID,TARGET FROM "+MaterialStDistribution.TABLE_NAME+" WHERE "+Dbs.inCondition("ID",dist$no_sub_staff_list.size()),
                Long.class,Long.class,
                dist$no_sub_staff_list.keySet().toArray()
        );

        ////7.?????????????????????????????????????????????
        Map<Long,List<Long>> target$staff_list = distributions.stream().collect(Collectors.toMap(
                dist->dist.getTarget().getId(),
                dist-> Stream.of(dist.getDistributionStaffmultiselectIDs().split(",")).map(Long::parseLong).collect(Collectors.toList())
        ));

        ////8.????????????????????????????????????????????????
        List<Long> conflictDist = dist$no_sub_staff_list.entrySet().stream().map(entry->{
            Long dist = entry.getKey();
            Set<Long> no_sub_staff_list = entry.getValue();
            List<Long> staff_list_sub_this_time = Optional.ofNullable(dist).map(dist$target::get).map(target$staff_list::get).orElse(null);
            if(staff_list_sub_this_time!=null){
                if(staff_list_sub_this_time.stream().anyMatch(no_sub_staff_list::contains)){
                    return dist;
                }
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        ////9.????????????????????????
        if(!conflictDist.isEmpty()){
            List<String> targetNames = Dbs.stream(
                    "SELECT NAME FROM "+MaterialWareModel.TABLE_NAME+" WHERE VALID=1 AND "+Dbs.inCondition("ID",conflictDist.size()),
                    String.class,
                    conflictDist.stream().map(dist$target::get).toArray()
            ).collect(Collectors.toList());
            MaterialExceptionThrower.stocktaking_recheck_unnecessary(String.join(",",targetNames));
        }

        //2.?????????????????????????????????
        distributions.forEach(distribution->{
            distribution.setStocktaking(Entities.ofId(MaterialStocktaking.class,stocktakingId));
            distributionService.saveStDistribution(distribution,()-> ArrayOperator.of(distribution.getDistributionStaffmultiselectIDs().split(",")).map_to(Long.class).forEach(staff->{
                MaterialStMultiStaff multiStaff = new MaterialStMultiStaff();
                multiStaff.setStaff(Entities.ofId(Staff.class,staff));
                multiStaff.setDistribution(distribution);
                multiStaffService.saveStMultiStaff(multiStaff,null);
            }));
        });

        //3.??????????????????
        MaterialStocktakingJob stocktakingJob = new MaterialStocktakingJob();
        stocktakingJob.setStocktaking(Entities.ofId(MaterialStocktaking.class,stocktakingId));
        List<MaterialStjDetail> stjDetails = distributions.stream().map(distribution->{
            MaterialStjDetail stjDetail = new MaterialStjDetail();
            stjDetail.setDistribution(distribution);
            stjDetail.setTakingCount(0);
            return stjDetail;
        }).collect(Collectors.toList());
        RequestCaches.set("__auto_save__",true);
        stocktakingJobService.saveStocktakingJob(stocktakingJob,()-> stjDetails.forEach(stjDetail->{
            stjDetail.setStocktakingJob(stocktakingJob);
            stjDetailService.saveStjDetail(stjDetail,null);
        }));
        RequestCaches.set("__auto_save__",false);
    }

    @Autowired
    private ModelWareModelService wareModelService;
    @Autowired
    private MaterialStMultiMatService stMultiMatService;
    @Autowired
    private MaterialStDistributionService stDistributionService;
    @Autowired
    private MaterialStMultiStaffService stMultiStaffService;

    @Override
    @Transactional
    public void generateStocktakingByStrategy(){
        //????????????????????????????????????????????????
        List<MaterialStStrategy> strategies = Dbs.findByCondition(
                MaterialStStrategy.class,
                "VALID=1"
        );
        List<MaterialStsDist> stsDistList = Dbs.findByCondition(
                MaterialStsDist.class,
                "VALID=1 AND "+Dbs.inCondition("STOCKTAKING_STRATEGY",strategies.size()),
                strategies.stream().map(IdEntity::getId).toArray()
        );
        List<MaterialStsMultiStaff> stsMultiStaffList = Dbs.findByCondition(
                MaterialStsMultiStaff.class,
                "VALID=1 AND "+Dbs.inCondition("DISTRIBUTION",stsDistList.size()),
                stsDistList.stream().map(IdEntity::getId).toArray()
        );
        List<MaterialStsMultiMat> stsMultiMats = Dbs.findByCondition(
                MaterialStsMultiMat.class,
                "VALID=1 AND "+Dbs.inCondition("STOCKTAKING_STRATEGY",strategies.size()),
                strategies.stream().map(IdEntity::getId).toArray()
        );
        Date today = Dates.getDateWithoutTime(new Date());
        strategies.stream().filter(strategy->{
            //??????????????????????????????????????????
            Date startDate = strategy.getBeginDate();
            int period = strategy.getPeriod();
            String periodUnit = strategy.getPeriodUnit().getId();
            if(periodUnit.endsWith("month")){
                //????????????
                long offset = Dates.getOffset(startDate,new Date(),ChronoUnit.MONTHS);
                if(offset%period==0){
                    //???????????????
                    if(Dates.getDay(today) == Dates.getDay(startDate)){
                        return true;
                    }
                }
            } else {
                //????????????
                long offset = Dates.getOffset(startDate,new Date(),ChronoUnit.DAYS);
                return offset%period == 0;
            }
            return false;
        }).forEach(strategy->{
            //???????????????
            MaterialStocktaking stocktaking = new MaterialStocktaking();
            stocktaking.setWarehouse(strategy.getWarehouse());

            stocktaking.setFullOrSpot(strategy.getFullOrSpot());
            stocktaking.setShowStockOrNot(strategy.getShowStockOrNot());
            stocktaking.setTakingWay(strategy.getTakingWay());
            stocktaking.setTargetMaterial(strategy.getTargetMaterial());
            stocktaking.setTakingDate(today);
            stocktaking.setApplyStaff(Organazations.getCurrentStaff());
            //????????????????????????????????????????????????
            List<MaterialStsDist> thisDistList;

            if(StocktakingWay.MOVE.equals(strategy.getTakingWay().getId())){
                //????????????????????????????????????????????????????????????
                Date lastDate = strategy.getPeriodUnit().getId().endsWith("month")
                        ? Dates.offset(today,-strategy.getPeriod(),ChronoUnit.MONTHS)
                        : Dates.offset(today,-strategy.getPeriod(), ChronoUnit.DAYS);
                stocktaking.setMovedCheckBeginDate(lastDate);
                //???????????????????????????
                Set<Long> allWareIds = wareModelService
                        .getPlaceModelIdsByWarehouse(stocktaking.getWarehouse().getId(),lastDate);
                thisDistList = stsDistList
                        .stream()
                        .filter(dist-> strategy.equals(dist.getStocktakingStrategy()))
                        .filter(dist->allWareIds.contains(dist.getTarget().getId()))
                        .collect(Collectors.toList());
            } else {
                thisDistList = stsDistList
                        .stream()
                        .filter(dist-> strategy.equals(dist.getStocktakingStrategy()))
                        .collect(Collectors.toList());
            }
            if(!thisDistList.isEmpty()){
                //???????????????
                workflowService.saveTable(stocktaking,"stocktakingFlw",()->{
                    //??????????????????
                    if(Boolean.TRUE.equals(strategy.getTargetMaterial())){
                        stsMultiMats.stream().filter(mm-> strategy.equals(mm.getStocktakingStrategy())).forEach(stsMM->{
                            MaterialStMultiMat stMM = new MaterialStMultiMat();
                            stMM.setMaterial(stsMM.getMaterial());
                            stMM.setStocktaking(stocktaking);
                            stMultiMatService.saveStMultiMat(stMM,null);
                        });
                    }
                    //??????????????????
                    thisDistList.forEach(stsDist->{
                        MaterialStDistribution stDist = new MaterialStDistribution();
                        stDist.setStocktaking(stocktaking);
                        stDist.setTarget(stsDist.getTarget());
                        stDistributionService.saveStDistribution(stDist,()->{
                            //??????????????????
                            stsMultiStaffList.stream().filter(stsMS-> stsDist.equals(stsMS.getDistribution())).forEach(stsMS->{
                                MaterialStMultiStaff stMS = new MaterialStMultiStaff();
                                stMS.setDistribution(stDist);
                                stMS.setStaff(stsMS.getStaff());
                                stMultiStaffService.saveStMultiStaff(stMS,null);
                            });
                        });
                    });
                });
            }
        });
    }
}
