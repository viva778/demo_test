package com.supcon.orchid.material.superwms.events;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.fooramework.annotation.signal.SignalManager;
import com.supcon.orchid.fooramework.util.ArrayOperator;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.*;
import com.supcon.orchid.material.superwms.constants.systemcode.StocktakingJobState;
import com.supcon.orchid.fooramework.services.PlatformEcAccessService;
import com.supcon.orchid.fooramework.services.PlatformSystemCodeService;
import com.supcon.orchid.material.superwms.services.TableStocktakingService;
import com.supcon.orchid.orm.entities.IdEntity;
import com.supcon.orchid.services.Page;
import com.supcon.orchid.services.QueryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.util.CastUtils.cast;

@Component
public class ModelStocktakingJobViewEvent {

    @Autowired
    private PlatformEcAccessService ecAccessService;

    @Signal("StocktakingJobBeforeQuery")
    public void jobBQ(String code, String methodName, QueryEntity queryEntity){
        //记录qe
        RequestCaches.set("__queryEntity__",queryEntity);
        //记录模型信息
        String modelCode;
        if(methodName!=null&&methodName.startsWith("dataGridData")){
            String dgName = methodName.substring(methodName.lastIndexOf("dg"));
            if(code.endsWith(dgName)){
                modelCode = ecAccessService.getModelCodeByDgCode(code);
            } else {
                modelCode = ecAccessService.getModelCodeByDgCode(code+dgName);
            }
        } else {
            modelCode = ecAccessService.getModelCodeByViewCode(code);
        }
        RequestCaches.set("__model_code__",modelCode);
    }

    @Signal("StocktakingJobAfterQuery")
    @Transactional
    public void jobAQ(String code, String methodName, Page<?> page) {
        String modelCode = RequestCaches.get("__model_code__");
        //根据模型细分信号
        String signal;
        switch (String.valueOf(modelCode)){
            case MaterialStjDetail.MODEL_CODE:{
                signal = "StjDetailPage";
                break;
            }
            case MaterialStocktakingJob.MODEL_CODE:{
                signal = "StocktakingJobPage";
                break;
            }
            case MaterialStjStockRecord.MODEL_CODE:{
                signal = "StjStockRecordPage";
                break;
            }
            default:
                return;
        }
        SignalManager.propagate(signal,Maps.immutable(
                "code",code,
                "methodName",methodName,
                "page",page
        ));
    }

    //-------------------------------------------------------------列表页面查询-------------------------------------------------------------
    @Signal("StocktakingJobBeforeQuery")
    public void setupMultiQuery(QueryEntity queryEntity){
        if(queryEntity!=null&&queryEntity.getFastQueryCond()!=null){
            Map<String,Object> cond = Jacksons.readValue(queryEntity.getFastQueryCond(),Map.class);
            if(cond.get("subconds")!=null){
                List<Map<String,String>> subconds = cast(cond.get("subconds"));
                //如果存在一对多查询，则标记后删除
                List<Map<String,String>> filterSubconds = subconds.stream().filter(condMap->{
                    String type = condMap.get("type");
                    String joinInfo = condMap.get("joinInfo");
                    List<Map<String,String>> subconds2 = cast(condMap.get("subconds"));
                    if("2".equals(type)&&joinInfo!=null&&joinInfo.toLowerCase().contains(MaterialStjOneToMany.TABLE_NAME)){
                        subconds2.forEach(condMap2->{
                            String type2 = condMap2.get("type");
                            String joinInfo2 = condMap2.get("joinInfo");
                            if("2".equals(type2)&&joinInfo2!=null){
                                //找到ID关联项和值
                                List<Map<String,String>> subconds3 = cast(condMap2.get("subconds"));
                                subconds3.stream().filter(condMap3-> "ID".equalsIgnoreCase(condMap3.get("columnName"))).findAny().ifPresent(condMap3->{
                                    if(joinInfo2.toLowerCase().contains(BaseSetMaterial.TABLE_NAME)){
                                        //关联查询物料时
                                        RequestCaches.set("__one_to_many_material__",condMap3.get("value"));
                                    } else if(joinInfo2.toLowerCase().contains(Staff.TABLE_NAME)){
                                        //关联查询负责人时
                                        RequestCaches.set("__one_to_many_staff__",condMap3.get("value"));
                                    }
                                });
                            }
                        });
                        return false;
                    } else if("JOB_STATE".equalsIgnoreCase(condMap.get("columnName"))){
                        //记录任务状态
                        RequestCaches.set("__job_state__",condMap.get("value"));
                        return false;
                    } else {
                        return true;
                    }
                }).collect(Collectors.toList());
                //删除自定义查询条件，不走平台过滤
                if(filterSubconds.size()!=subconds.size()){
                    cond.put("subconds",filterSubconds);
                    queryEntity.setFastQueryCond(Jacksons.writeValue(cond));
                }
            }
        }
    }
    @Signal("StocktakingJobCustomCondition")
    public String stjCustomCondition(){
        String materialId = RequestCaches.get("__one_to_many_material__");
        String staffId = Optional.ofNullable(RequestCaches.get("__one_to_many_staff__")).map(String::valueOf).orElse(Organazations.getCurrentStaffId().toString());
        String jobState = RequestCaches.get("__job_state__");
        List<String> conditions = new LinkedList<>();
        //增加人员查询条件
        String sqlSelectDist = "SELECT DISTRIBUTION FROM "+ MaterialStMultiStaff.TABLE_NAME+" WHERE VALID=1 AND STAFF="+staffId;
        String sqlSelectJob = "SELECT STOCKTAKING_JOB FROM "+ MaterialStjDetail.TABLE_NAME +" WHERE DISTRIBUTION IN("+sqlSelectDist+")";
        conditions.add("\"stocktakingJob\".ID IN("+sqlSelectJob+")");
        if(Strings.valid(materialId)){
            //增加物料查询条件
            conditions.add("\"stocktakingJob\".STOCKTAKING IN(SELECT STOCKTAKING FROM "+ MaterialStMultiMat.TABLE_NAME +" WHERE VALID=1 AND MATERIAL="+materialId+")");
        }
        if(Strings.valid(jobState)){
            //增加任务状态过滤
            switch (jobState){
                case StocktakingJobState.EDIT:{
                    //不存在当前人提交记录，且单据处于编辑状态
                    conditions.add("\"stocktakingJob\".ID NOT IN(SELECT STOCKTAKING_JOB FROM "+MaterialStjSubmitRec.TABLE_NAME+" WHERE VALID=1 AND SUBMIT_PERSON="+Organazations.getCurrentStaffId()+") AND \"stocktakingJob\".STOCKTAKING IN(SELECT ID FROM "+MaterialStocktaking.TABLE_NAME+" WHERE STATUS=88)");
                    break;
                }
                case StocktakingJobState.SUBMITTED:{
                    //存在提交记录，且单据处于编辑状态
                    conditions.add("\"stocktakingJob\".ID IN(SELECT STOCKTAKING_JOB FROM "+MaterialStjSubmitRec.TABLE_NAME+" WHERE VALID=1 AND SUBMIT_PERSON="+Organazations.getCurrentStaffId()+") AND \"stocktakingJob\".STOCKTAKING IN(SELECT ID FROM "+MaterialStocktaking.TABLE_NAME+" WHERE STATUS=88)");
                    break;
                }
                case StocktakingJobState.FINISHED:{
                    //单据处于非编辑状态
                    conditions.add("\"stocktakingJob\".STOCKTAKING IN(SELECT ID FROM "+MaterialStocktaking.TABLE_NAME+" WHERE STATUS<>88)");
                    break;
                }
            }
        }
        return String.join(" and ",conditions);
    }







    //-------------------------------------------------------------列表页面展示-------------------------------------------------------------
    @Autowired
    private PlatformSystemCodeService systemCodeService;

    private static final String ONE_TO_MANY_HEADER = "stjOneToMany.";

    @Signal("StocktakingJobPage")
    @Transactional
    public void setMultiDataInfo(String code, String methodName, Page<MaterialStocktakingJob> page){
        //列表查询时设置多选内容
        Map<String,String> multiFieldsInfo = ecAccessService.findMultiFieldsInfo(code, methodName);
        if(!multiFieldsInfo.isEmpty()){
            //根据关联模型分组
            multiFieldsInfo.entrySet().stream().filter(entry->entry.getKey().startsWith(ONE_TO_MANY_HEADER)).map(entry->{
                String propertyName = entry.getKey();
                String propertyName1 = propertyName.substring(ONE_TO_MANY_HEADER.length());
                String asModelPropName = propertyName1.substring(0,propertyName1.indexOf("."));
                String innerPropName = propertyName1.substring(asModelPropName.length()+1);
                return new String[]{asModelPropName, innerPropName, entry.getValue()};
            }).collect(Collectors.groupingBy(sss->sss[0])).forEach((asModelPropName,arrList)->{
                if("material".equals(asModelPropName)){
                    //查询任务$物料ID
                    Map<Long,List<Long>> job$materials = Dbs.pairList(
                            "SELECT stj.ID,mm.MATERIAL FROM "+MaterialStMultiMat.TABLE_NAME+" mm LEFT JOIN "+MaterialStocktakingJob.TABLE_NAME+" stj ON mm.STOCKTAKING=stj.STOCKTAKING WHERE mm.VALID=1 AND "+ Dbs.inCondition("stj.ID",page.getResult().size()),
                            Long.class,Long.class,
                            page.getResult().stream().map(IdEntity::getId).toArray()
                    ).stream().collect(Collectors.groupingBy(Pair::getFirst)).entrySet().stream().collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry->entry.getValue().stream().map(Pair::getSecond).collect(Collectors.toList())
                    ));
                    if(!job$materials.isEmpty()){
                        List<Long> materialIds = job$materials.values().stream().flatMap(Collection::stream).distinct().collect(Collectors.toList());
                        Map<Long,BaseSetMaterial> id$material = Dbs.findByCondition(
                                BaseSetMaterial.class,
                                "VALID=1 AND "+Dbs.inCondition("ID",materialIds.size()),
                                materialIds.toArray()
                        ).stream().collect(Collectors.toMap(
                                IdEntity::getId,
                                val->val
                        ));
                        page.getResult().forEach(stocktakingJob -> {
                            List<Long> materials = job$materials.get(stocktakingJob.getId());
                            if(materials!=null&&!materials.isEmpty()){
                                arrList.forEach(arr->{
                                    String innerPropName = arr[1];
                                    String fieldCode = arr[2];
                                    String propValues = materials.stream().map(id$material::get).map(material -> Beans.getPropertyValue(material,innerPropName)).filter(Objects::nonNull).map(String::valueOf).distinct().collect(Collectors.joining(","));
                                    Map<String,Object> attrMap = Optional.ofNullable(stocktakingJob.getAttrMap()).orElse(new LinkedHashMap<>());
                                    attrMap.put(fieldCode.replace(".","_"),propValues);
                                    stocktakingJob.setAttrMap(attrMap);
                                });
                            }
                        });
                    }
                } else if("staff".equals(asModelPropName)){
                    //查询任务$人员ID
                    Map<Long,List<Long>> job$staffs = Dbs.pairList(
                            "SELECT std.STOCKTAKING_JOB,ms.STAFF FROM "+MaterialStMultiStaff.TABLE_NAME+" ms LEFT JOIN "+MaterialStjDetail.TABLE_NAME+" std ON ms.DISTRIBUTION=std.DISTRIBUTION WHERE ms.VALID=1 AND "+ Dbs.inCondition("std.STOCKTAKING_JOB",page.getResult().size()),
                            Long.class,Long.class,
                            page.getResult().stream().map(IdEntity::getId).toArray()
                    ).stream().collect(Collectors.groupingBy(Pair::getFirst)).entrySet().stream().collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry->entry.getValue().stream().map(Pair::getSecond).collect(Collectors.toList())
                    ));
                    if(!job$staffs.isEmpty()){
                        List<Long> staffIds = job$staffs.values().stream().flatMap(Collection::stream).distinct().collect(Collectors.toList());
                        Map<Long,Staff> id$staff = Dbs.findByCondition(
                                Staff.class,
                                "VALID=1 AND "+Dbs.inCondition("ID",staffIds.size()),
                                staffIds.toArray()
                        ).stream().collect(Collectors.toMap(
                                IdEntity::getId,
                                val->val
                        ));
                        page.getResult().forEach(stocktakingJob -> {
                            List<Long> staffs = job$staffs.get(stocktakingJob.getId());
                            if(staffs!=null&&!staffs.isEmpty()){
                                arrList.forEach(arr->{
                                    String innerPropName = arr[1];
                                    String fieldCode = arr[2];
                                    String propValues = staffs.stream().map(id$staff::get).map(staff -> Beans.getPropertyValue(staff,innerPropName)).filter(Objects::nonNull).map(String::valueOf).distinct().collect(Collectors.joining(","));
                                    Map<String,Object> attrMap = Optional.ofNullable(stocktakingJob.getAttrMap()).orElse(new LinkedHashMap<>());
                                    attrMap.put(fieldCode.replace(".","_"),propValues);
                                    stocktakingJob.setAttrMap(attrMap);
                                });
                            }
                        });
                    }
                }
            });
        }
        //查询任务状态
        //1).不存在提交记录为编辑 2).盘点单未生效为已提交 3).生效则结束
        ////查询当前人提交记录
        Set<Long> jobSubmitted = Dbs.stream(
                "SELECT STOCKTAKING_JOB FROM "+MaterialStjSubmitRec.TABLE_NAME+" WHERE SUBMIT_PERSON=? AND "+Dbs.inCondition("STOCKTAKING_JOB",page.getResult().size()),
                Long.class,
                Elements.toArray(Organazations.getCurrentStaffId(),page.getResult().stream().map(IdEntity::getId))
        ).collect(Collectors.toSet());
        ////查询盘单点非编辑的任务
        Set<Long> jobFinished = Dbs.stream(
                "SELECT ID FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE STOCKTAKING IN(SELECT ID FROM "+MaterialStocktaking.TABLE_NAME+" WHERE STATUS<>88) AND "+Dbs.inCondition("ID",page.getResult().size()),
                Long.class,
                page.getResult().stream().map(IdEntity::getId).toArray()
        ).collect(Collectors.toSet());
        page.getResult().forEach(job->{
            if(jobFinished.contains(job.getId())){
                job.setJobState(systemCodeService.get(StocktakingJobState.FINISHED));
            } else if (jobSubmitted.contains(job.getId())) {
                job.setJobState(systemCodeService.get(StocktakingJobState.SUBMITTED));
            } else {
                job.setJobState(systemCodeService.get(StocktakingJobState.EDIT));
            }
        });
    }


    //-------------------------------------------------------------明细展示-------------------------------------------------------------
    @Signal("StjDetailPage")
    private void stjDetailPage(Page<MaterialStjDetail> page){
        //查询负责人名称
        //查询明细$人员ID
        Map<Long,List<Long>> dist$staffs = Dbs.pairList(
                "SELECT std.ID,ms.STAFF FROM "+MaterialStMultiStaff.TABLE_NAME+" ms LEFT JOIN "+MaterialStjDetail.TABLE_NAME+" std ON ms.DISTRIBUTION=std.DISTRIBUTION WHERE ms.VALID=1 AND "+ Dbs.inCondition("std.ID",page.getResult().size()),
                Long.class,Long.class,
                page.getResult().stream().map(IdEntity::getId).toArray()
        ).stream().collect(Collectors.groupingBy(Pair::getFirst)).entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry->entry.getValue().stream().map(Pair::getSecond).collect(Collectors.toList())
        ));
        if(!dist$staffs.isEmpty()){
            //将人员设置到对应表体
            List<Long> staffIds = dist$staffs.values().stream().flatMap(Collection::stream).distinct().collect(Collectors.toList());
            Map<Long,String> id$staff = Dbs.binaryMap(
                    "SELECT ID,NAME FROM "+Staff.TABLE_NAME+" WHERE VALID=1 AND "+Dbs.inCondition("ID",staffIds.size()),
                    Long.class,String.class,
                    staffIds.toArray()
            );
            page.getResult().forEach(stjDetail -> {
                String names = dist$staffs.get(stjDetail.getId()).stream().map(id$staff::get).collect(Collectors.joining(","));
                stjDetail.setStaffNames(names);
            });
        }
    }


    //-------------------------------------------------------------现存量展示-------------------------------------------------------------
    @Autowired
    private TableStocktakingService stocktakingService;

    @Signal("StjStockRecordPage")
    private void stockRecordPage(String code, Page<MaterialStjStockRecord> page){
        if(page!=null){
            //如果不是查看视图，才需要拼接未保存现存量
            if(!"true".equals(Https.getParameter("isView"))){
                //查询需要盘点的现存量，并拼接
                long id = Long.parseLong(Https.getParameter("id"));
                if(id>0){
                    //获取页面includes对应的现存量字段
                    String includes = ecAccessService.getDgIncludeWithFieldMapper(code,Maps.immutable(
                            "material","good",
                            "place","placeSet",
                            "batchInfo","materBatchInfo",
                            "quantityOnBook","availiQuantity"
                    ));
                    //如果customCondition中对货位进行了过滤，就只查询对应货位的现存量
                    QueryEntity queryEntity = RequestCaches.get("__queryEntity__");
                    Long[] placeModelIds = Optional.ofNullable(queryEntity)
                            .map(QueryEntity::getCustomCondition)
                            .map(map->map.get("placeModelIds"))
                            .filter(Strings::valid)
                            .map(String::valueOf)
                            .map(val-> ArrayOperator.of(val.split(",")).map_to(Long.class).get())
                            .orElse(null);
                    //获取现存量且计算key
                    Map<String,MaterialStandingcrop> key$stock = stocktakingService.findStocksByStocktakingJobIdAndStaffId(id, Organazations.getCurrentStaffId(),placeModelIds, includes).stream().collect(Collectors.toMap(
                            stocktakingService::getStockKey,
                            val->val
                    ));
                    //取差集
                    Set<String> diff = Elements.difference(key$stock.keySet(),page.getResult().stream().map(MaterialStjStockRecord::getStockKey).collect(Collectors.toSet()));
                    //增加差集部分到page
                    page.setResult(Arrays.asList(Elements.toArray(
                            MaterialStjStockRecord[]::new,
                            diff.stream().map(key->{
                                MaterialStjStockRecord stockRecord = new MaterialStjStockRecord();
                                MaterialStandingcrop stock = key$stock.get(key);
                                stockRecord.setStockKey(key);
                                stockRecord.setMaterial(stock.getGood());
                                stockRecord.setBatchInfo(stock.getMaterBatchInfo());
                                stockRecord.setPlace(stock.getPlaceSet());
                                stockRecord.setQuantityOnBook(stock.getAvailiQuantity());
                                return stockRecord;
                            }),
                            page.getResult()
                    )));
                    page.getResult().sort(Comparator.comparingLong(r -> Optional.ofNullable(r.getPlace()).map(IdEntity::getId).orElse(0L)));
                    page.setTotalCount(page.getResult().size());
                }
            }
        }
    }






    //-------------------------------------------------------------现存量盘点清单-------------------------------------------------------------

    @Signal("StjStockRecordBeforeQuery")
    private void stockRecordListBQ(QueryEntity queryEntity){
        if(queryEntity!=null){
            RequestCaches.set("__queryEntity__",queryEntity);
        }
    }

    @Signal("StjStockRecordAfterQuery")
    @Transactional
    public void stockRecordListAQ(String code, String methodName, Page<MaterialStjStockRecord> page){
        int dgIdx = methodName!=null?methodName.lastIndexOf("dg"):-1;
        if(page!=null&&dgIdx>0&&MaterialStjStockRecord.MODEL_CODE.equals(ecAccessService.getModelCodeByDgCode(code+methodName.substring(dgIdx)))){
            QueryEntity queryEntity = RequestCaches.get("__queryEntity__");
            Assert.notNull(queryEntity,"queryEntity is missing");
            if(queryEntity.getCustomCondition()!=null&&!queryEntity.getCustomCondition().containsKey("stockKey")){
                if(!page.getResult().isEmpty()){
                    Long stocktakingId = Long.parseLong(String.valueOf(queryEntity.getCustomCondition().get("stocktakingId")));
                    //获取相同stockKey下对应的数据差异数量
                    Map<String,Long> key$count = Dbs.pairList(
                            "SELECT STOCK_KEY,QUANTITY_BY_COUNT FROM "+MaterialStjStockRecord.TABLE_NAME+" WHERE VALID=1 AND QUANTITY_BY_COUNT IS NOT NULL AND STOCKTAKING_JOB IN(SELECT ID FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING=?) AND "+Dbs.inCondition("STOCK_KEY",page.getResult().size()),
                            String.class, BigDecimal.class,
                            Elements.toArray(stocktakingId,page.getResult().stream().map(MaterialStjStockRecord::getStockKey))
                    ).stream().collect(Collectors.groupingBy(Pair::getFirst)).entrySet().stream().collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry->entry.getValue().stream().map(Pair::getSecond).distinct().count()
                    ));
                    page.getResult().forEach(record-> record.setDispute(
                            key$count.getOrDefault(record.getStockKey(),0L)>1
                    ));
                }
            }
        }
    }
}
