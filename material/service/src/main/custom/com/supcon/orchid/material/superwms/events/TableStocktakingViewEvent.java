package com.supcon.orchid.material.superwms.events;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.material.controllers.MaterialStandingcropController;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.fooramework.util.*;
import com.supcon.orchid.fooramework.services.PlatformEcAccessService;
import com.supcon.orchid.orm.entities.IdEntity;
import com.supcon.orchid.services.Page;
import com.supcon.orchid.services.QueryEntity;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.data.util.CastUtils.cast;

@Component
public class TableStocktakingViewEvent {

    @Autowired
    private MaterialStandingcropController stockController;
    @Autowired
    private PlatformEcAccessService ecAccessService;


    //-------------------------------------------------------------盘点申请页面展示-------------------------------------------------------------

    @SneakyThrows
    @Signal("StStockViewAfterQuery")
    private void dispatchToStockQuery(String code, String methodName, Page<MaterialStStockView> page){
        int dgIdx = methodName!=null?methodName.lastIndexOf("dg"):-1;
        if(dgIdx>0&&MaterialStStockView.MODEL_CODE.equals(ecAccessService.getModelCodeByDgCode(code+methodName.substring(dgIdx)))){
            HttpServletRequest request = Https.getRequest();
            Assert.notNull(request,"request is missing");
            QueryEntity queryEntity = RequestCaches.get("queryEntity");
            Assert.notNull(queryEntity,"queryEntity is missing");

            //转发查询到现存量
            RequestCaches.set("__request_dispatch__",true);
            String viewCode = (String) request.getAttribute("ViewCode");
            request.setAttribute("ViewCode","material_1.0.0_standingcrop_onhandRef");
            stockController.onhandRefListQuery_backup(queryEntity, request, Https.getResponse());
            request.setAttribute("ViewCode",viewCode);
            RequestCaches.get("__page_result__");

            //转换现存量查询结果到正确对象
            Page<MaterialStandingcrop> stockPage = RequestCaches.get("__page_result__");
            if(stockPage!=null&&!stockPage.getResult().isEmpty()){
                page.setResult(stockPage.getResult().stream().map(stock->{
                    MaterialStStockView stockView = new MaterialStStockView();
                    stockView.setStock(stock);
                    return stockView;
                }).collect(Collectors.toList()));
                page.setPageNo(stockPage.getPageNo());
                page.setPageSize(stockPage.getPageSize());
                page.setTotalCount(stockPage.getTotalCount());
                page.setMaxPageSize(stockPage.getMaxPageSize());
            }
        }
    }


    @Signal("StockAfterQuery")
    private void setStockResult(Page<?> page){
        if(page!=null&&Boolean.TRUE.equals(RequestCaches.get("__request_dispatch__"))){
            RequestCaches.set("__page_result__",page);
        }
    }

    @Signal("StockBeforeQuery")
    private void switchQueryCondition(QueryEntity queryEntity){
        if(queryEntity!=null&&Boolean.TRUE.equals(RequestCaches.get("__request_dispatch__"))){
            //删除排序列的前缀
            String sortKey = queryEntity.getDataTableSortColKey();
            if(Strings.valid(sortKey)){
                if(sortKey.startsWith("stock.")){
                    queryEntity.setDataTableSortColKey(sortKey.substring("stock.".length()));
                }
            } else {
                //默认货位升序
                queryEntity.setDataTableSortColKey("placeSet.name");
                queryEntity.setDataTableSortColName("NAME");
                queryEntity.setDataTableSortColOrder("ASC");
            }
        }
    }


    //-------------------------------------------------------------盘点作业页面展示-------------------------------------------------------------


    //-------------------------------盘点记录统计-------------------------------

    @Signal("StRecordStatusBeforeQuery")
    @Transactional
    public void stRecordStatusBQ(String code, String methodName, QueryEntity queryEntity){
        int dgIdx = methodName!=null?methodName.lastIndexOf("dg"):-1;
        if(queryEntity!=null&&dgIdx>0&&MaterialStRecordStatus.MODEL_CODE.equals(ecAccessService.getModelCodeByDgCode(code+methodName.substring(dgIdx)))){
            RequestCaches.set("__queryEntity__",queryEntity);
        }
    }

    @Signal("StRecordStatusAfterQuery")
    @Transactional
    public void stRecordStatusAQ(String code, String methodName, Page<MaterialStRecordStatus> page){
        QueryEntity queryEntity = RequestCaches.get("__queryEntity__");
        if(queryEntity!=null){
            String gt = Https.getParameter("groupType");
            if("material".equals(gt)){
                Long stocktakingId = Long.parseLong(Https.getParameter("id"));
                MaterialStocktaking stocktaking = Dbs.load(MaterialStocktaking.class,stocktakingId);
                Assert.notNull(stocktaking,"stocktaking is missing");
                //查询物料范围
                List<Long> materialIds = Dbs.stream(
                        "SELECT MATERIAL FROM "+MaterialStMultiMat.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING=?",
                        Long.class,
                        stocktakingId
                ).collect(Collectors.toList());
                List<Object> params = new LinkedList<>();
                params.add(stocktaking.getWarehouse().getId());
                //不存在物料则不指定范围
                String materialCondition = materialIds.isEmpty()?"":"AND "+Dbs.inCondition("GOOD",materialIds.size());
                if(!materialIds.isEmpty()){
                    params.addAll(materialIds);
                }
                //查找仓库下存在现存量的物料汇总
                Map<Long, BigDecimal> materialId$quan = Dbs.binaryMap(
                        "SELECT GOOD,SUM(AVAILI_QUANTITY) FROM "+MaterialStandingcrop.TABLE_NAME+" WHERE VALID=1 AND WARE=? AND ONHAND>0 "+materialCondition+" GROUP BY GOOD",
                        Long.class, BigDecimal.class,
                        params.toArray()
                );
                //物料ID对应实体
                Map<Long, BaseSetMaterial> id$material = Dbs.findByCondition(
                        BaseSetMaterial.class,
                        Dbs.inCondition("ID",materialId$quan.size()),
                        materialId$quan.keySet().toArray()
                ).stream().collect(Collectors.toMap(IdEntity::getId, val->val));
                //物料ID对应统计
                Map<Long,MaterialStRecordStatus> materialId$status = materialId$quan.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry->{
                            MaterialStRecordStatus recordStatus = new MaterialStRecordStatus();
                            recordStatus.setMaterial(id$material.get(entry.getKey()));
                            recordStatus.setQuantityOnBookTotal(entry.getValue());
                            return recordStatus;
                        }
                ));
                //查找所有存在提交记录的现存量盘点（排除抽盘未填写数量的
                List<MaterialStjStockRecord> stockRecords = Dbs.findByCondition(
                        MaterialStjStockRecord.class,
                        "VALID=1 AND STOCKTAKING_JOB IN(SELECT ID FROM "+MaterialStocktakingJob.TABLE_NAME+" WHERE VALID=1 AND STOCKTAKING=?) AND SUBMIT_RECORD IS NOT NULL AND QUANTITY_BY_COUNT IS NOT NULL",
                        stocktakingId
                );
                //根据stockKey分组
                Map<String,List<MaterialStjStockRecord>> stockKey$records = stockRecords.stream().collect(Collectors.groupingBy(MaterialStjStockRecord::getStockKey));
                //选出每个分组中的有效记录
                stockKey$records.values().forEach(records->{
                    BaseSetMaterial material = records.get(0).getMaterial();
                    MaterialStRecordStatus status = materialId$status.computeIfAbsent(material.getId(),k->{
                        MaterialStRecordStatus recordStatus = new MaterialStRecordStatus();
                        recordStatus.setMaterial(material);
                        recordStatus.setQuantityOnBookTotal(BigDecimal.ZERO);
                        recordStatus.setDispute(false);
                        return recordStatus;
                    });
                    MaterialStjStockRecord record;
                    if(records.size()==1){
                        //只有一条
                        record = records.get(0);
                    } else {
                        //如果盘点数量数量不同则设置为有差异
                        status.setDispute(status.getDispute()||records.stream().map(MaterialStjStockRecord::getQuantityByCount).distinct().count()>1);
                        //如果有被确认数据，则选择被确认的
                        record = records.stream().filter(rec->Boolean.TRUE.equals(rec.getChecked())).findAny().orElseGet(()->{
                            //否则找到提交时间最近一条 --实际上后面应该不会跑到这里，因为所有最新提交的数据都会被列为结算项
                            return records.stream().min(Comparator.comparingLong(r->Dbs.getProp(r.getSubmitRecord(),MaterialStjSubmitRec::getSubmitTime).getTime())).orElse(null);
                        });
                    }
                    if(record!=null){
                        //增加盘点量
                        status.setQuantityByCount(Optional.ofNullable(status.getQuantityByCount()).map(record.getQuantityByCount()::add).orElse(record.getQuantityByCount()));
                        status.setQuantityOnBook(Optional.ofNullable(status.getQuantityOnBook()).map(record.getQuantityOnBook()::add).orElse(record.getQuantityOnBook()));
                    }
                });
                List<MaterialStRecordStatus> statuses = materialId$status.values().stream().peek(val->{
                    if(val.getQuantityOnBook()==null){
                        val.setQuantityOnBook(BigDecimal.ZERO);
                    }
                    if(val.getQuantityByCount()==null){
                        val.setQuantityByCount(BigDecimal.ZERO);
                    }
                    val.setQuantityOffset(val.getQuantityByCount().subtract(val.getQuantityOnBook()));
                }).collect(Collectors.toList());
                //进行排序
                if(Strings.valid(queryEntity.getDataTableSortColKey())){
                    Function<MaterialStRecordStatus,Object> valGetter = Reflects.getFieldGetterByFullPath(
                            MaterialStRecordStatus.class,queryEntity.getDataTableSortColKey()
                    );
                    Comparator<MaterialStRecordStatus> comparator = Comparator.comparing(s->cast(valGetter.apply(s)));
                    if("DESC".equals(queryEntity.getDataTableSortColOrder())){
                        statuses.sort(comparator.reversed());
                    } else {
                        statuses.sort(comparator);
                    }
                }
                if(statuses.size()>0){
                    //设置分页信息
                    int begin = Math.min((page.getPageNo()-1)*page.getPageSize(),statuses.size()-1);
                    int end = Math.min(page.getPageNo()*page.getPageSize(),statuses.size());
                    page.setTotalCount(statuses.size());
                    page.setResult(statuses.subList(begin,end));
                }
            }
        }
    }
}
