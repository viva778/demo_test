package com.supcon.orchid.material.superwms.util;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetUnit;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcEntity;
import com.supcon.orchid.material.entities.MaterialCropGather;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.fooramework.annotation.signal.SignalManager;
import com.supcon.orchid.fooramework.annotation.staticautowired.StaticAutowired;
import com.supcon.orchid.fooramework.util.*;
import com.supcon.orchid.material.services.MaterialCropGatherService;
import com.supcon.orchid.material.services.MaterialStandingcropService;
import com.supcon.orchid.material.superwms.services.ModelBatchInfoService;
import com.supcon.orchid.material.superwms.services.ModelStockService;
import com.supcon.orchid.material.superwms.services.ModelStockSummaryService;
import com.supcon.orchid.material.superwms.util.factories.StockFactory;
import com.supcon.orchid.material.superwms.util.factories.StockSummaryFactory;
import com.supcon.orchid.orm.entities.IdEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 库存操作
 *   --包含数据库访问需要事务
 */
public class StockOperator {

    @StaticAutowired
    private static ModelStockService stockService;
    @StaticAutowired
    private static ModelStockSummaryService stockSummaryService;
    @StaticAutowired
    private static MaterialStandingcropService standingcropService;
    @StaticAutowired
    private static MaterialCropGatherService cropGatherService;
    @StaticAutowired
    private static ModelBatchInfoService modelBatchInfoService;

    private static final Logger log = LoggerFactory.getLogger(StockOperator.class);


    /**
     * 指定现存量操作器
     * @param materialId 物品ID
     * @param batchNum 批次号
     * @param warehouseId 仓库ID
     * @param placeId 货位ID
     * @return 现存量操作器
     */
    public static StockOperator of(Long materialId, String batchNum, Long warehouseId, Long placeId){
        return TransactionCaches.computeIfAbsent(_GetHandle(materialId,batchNum,warehouseId,placeId),k->new StockOperator(materialId, batchNum, warehouseId, placeId));
    }

    /**
     * 指定现存量操作器
     * @param stock 现存量
     * @return 现存量操作器
     */
    public static StockOperator of(MaterialStandingcrop stock){
        //先从缓存中查找，如果不存在，则新建
        Assert.isTrue(stock!=null&&stock.getId()!=null,"现存量参数异常");

        return TransactionCaches.computeIfAbsent(_GetHandle(
                stock.getGood().getId(),
                stock.getBatchText(),
                stock.getWare().getId(),
                Optional.ofNullable(stock.getPlaceSet()).map(IdEntity::getId).orElse(null)
        ),k->new StockOperator(stock));
    }

    private final Long materialId;

    private final String batchNum;

    private final Long warehouseId;

    private final Long placeId;

    private MaterialStandingcrop stock;

    private MaterialCropGather stockSummary;

    private boolean stockUpdateFlag;

    private boolean summaryUpdateFlag;

    private String goodUnitName;

    private AbstractEcEntity sourceEntity;


    /**
     * 获取现存量对象
     * @return 现存量对象
     */
    public MaterialStandingcrop getStock() {
        get();
        return stock;
    }

    /**
     * 设置来源实体（如出入库单表体、期初导入数据等等
     */
    public StockOperator setSourceEntity(AbstractEcEntity sourceEntity) {
        this.sourceEntity = sourceEntity;
        return this;
    }


    /**
     * 冻结现存量
     * @param quantity 冻结量
     * @return 操作器
     */
    public StockOperator freeze(BigDecimal quantity){
        get();
        assert_not_null(stock,"找不到冻结现存量！");
        assert_is_true(stock.getAvailiQuantity().compareTo(quantity)>=0,"脏数据：现存量不足以冻结！");
        setStockUpdateFlag();
        stock.setAvailiQuantity(stock.getAvailiQuantity().subtract(quantity));
        stock.setFrozenQuantity(
                stock.getFrozenQuantity()!=null? stock.getFrozenQuantity().add(quantity):quantity
        );
        SignalManager.propagate("StockFroze",stock,quantity,sourceEntity);
        return this;
    }

    /* 解冻现存量
     * @param quantity 解冻量
     * @return 操作器
     */
    public StockOperator restore(BigDecimal quantity){
        get();
        assert_not_null(stock,"找不到解冻现存量！");
        assert_is_true(stock.getFrozenQuantity()!=null&&stock.getFrozenQuantity().compareTo(quantity)>=0,"脏数据：解冻量大于冻结量！");
        setStockUpdateFlag();
        stock.setAvailiQuantity(stock.getAvailiQuantity().add(quantity));
        stock.setFrozenQuantity(stock.getFrozenQuantity().subtract(quantity));
        SignalManager.propagate("StockRestored",stock,quantity,sourceEntity);
        return this;
    }

    /**
     * 现存量偏移
     * @param offset 偏移量
     * @return 操作器
     */
    public StockOperator offset(BigDecimal offset){
        int compareValue = offset.compareTo(BigDecimal.ZERO);
        if(compareValue>0){
            increase(offset);
        } else if(compareValue<0){
            decrease(offset.negate());
        }
        return this;
    }

    /**
     * 增加现存量
     * @param quantity 增量
     * @return 操作器
     */
    public StockOperator increase(BigDecimal quantity){
        get();
        incStock(quantity);
        incSummary(quantity);
        SignalManager.propagate("StockIncreased",stock,quantity,sourceEntity);
        return this;
    }

    /**
     * 更新现存量属性
     * @param consumer 更新操作
     * @return 操作器
     */
    public StockOperator update(Consumer<MaterialStandingcrop> consumer){
        get();
        assert_not_null(stock,"找不到需操作现存量！");
        setStockUpdateFlag();
        consumer.accept(stock);
        return this;
    }

    /**
     * 减少现存量(冲销
     * @param quantity 减量
     * @return 操作器
     */
    public StockOperator increaseWriteOff(BigDecimal quantity){
        get();
        decStock(quantity);
        decSummary(quantity);
        return this;
    }

    /**
     * 减少现存量
     * @param quantity 减量
     * @return 操作器
     */
    public StockOperator decrease(BigDecimal quantity){
        get();
        decStock(quantity);
        decSummary(quantity);
        SignalManager.propagate("StockDecreased",stock,quantity,sourceEntity);
        return this;
    }

    /**
     * 增加现存量(冲销
     * @param quantity 增量
     * @return 操作器
     */
    public StockOperator decreaseWriteOff(BigDecimal quantity){
        get();
        incStock(quantity);
        incSummary(quantity);
        return this;
    }

    //----------------------------------------------------------私有方法----------------------------------------------------------


    private StockOperator(Long materialId, String batchNum, Long warehouseId, Long placeId) {
        this.materialId = materialId;
        this.batchNum = batchNum;
        this.warehouseId = warehouseId;
        this.placeId = placeId;
    }

    private StockOperator(MaterialStandingcrop stock) {
        this.stock = stock;
        this.warehouseId = stock.getWare().getId();
        this.materialId = stock.getGood().getId();
        this.batchNum = stock.getBatchText();
        this.placeId = Optional.ofNullable(stock.getPlaceSet()).map(IdEntity::getId).orElse(null);
    }


    private void incStock(BigDecimal quantity){
        //--------------------------------增加现存量--------------------------------
        setStockUpdateFlag();
        if(stock==null){
            //创建现存量
            stock = StockFactory.of(
                    warehouseId,
                    placeId,
                    Dbs.load(BaseSetMaterial.class, materialId),
                    Strings.valid(batchNum)?modelBatchInfoService.getBatchInfo(materialId,batchNum):null,
                    quantity,
                    goodUnitName
            );
        } else {
            //增量原有现存量
            stockService.quantityOffset(stock,quantity);
        }
    }

    private void incSummary(BigDecimal quantity){
        //--------------------------------增加汇总--------------------------------
        setStockSummaryUpdateFlag();
        if(stockSummary==null){
            //创建汇总
            stockSummary = StockSummaryFactory.of(
                    warehouseId,
                    materialId,
                    quantity,
                    goodUnitName
            );
        } else {
            //增量原有汇总
            stockSummaryService.quantityOffset(stockSummary,quantity);
        }
    }

    private void decStock(BigDecimal quantity){
        assert_not_null(stock,"找不到扣减现存量！");
        assert_is_true(stock.getAvailiQuantity().compareTo(quantity)>=0,"现存量不足！");
        setStockUpdateFlag();
        stockService.quantityOffset(stock,quantity.negate());
    }

    private void decSummary(BigDecimal quantity){
        assert_not_null(stockSummary,"找不到对应现存量汇总！");
        setStockSummaryUpdateFlag();
        stockSummaryService.quantityOffset(stockSummary,quantity.negate());
    }

    private void get(){
        if(stock==null){
            stock = stockService.get(materialId, batchNum, warehouseId, placeId);
        }
        if(stockSummary==null){
            stockSummary = getStockSummary();
        }
        if(goodUnitName==null){
            goodUnitName = Dbs.getProp(Dbs.load(BaseSetMaterial.class, materialId),BaseSetMaterial::getMainUnit,BaseSetUnit::getName);
        }
    }

    /**
     * 将所有操作保存至数据库
     */
    private static void finishAll(){
        Set<StockOperator> operators = TransactionCaches.get("__stock_operators__");
        if(operators!=null&& !operators.isEmpty()){
            operators.forEach(StockOperator::finish);
        }
    }


    /**
     * 将操作保存至数据库
     */
    private void finish(){
        if(stockUpdateFlag && stock!=null){
            if(stock.getId()!=null){
                PostSqlUpdater.manualUpdate(stock);
            } else {
                standingcropService.saveStandingcrop(stock,null);
            }
        }
        if(summaryUpdateFlag && stockSummary!=null){
            if(stockSummary.getId()!=null){
                PostSqlUpdater.manualUpdate(stockSummary);
            } else {
                cropGatherService.saveCropGather(stockSummary,null);
            }
        }
        fbsFlag = summaryUpdateFlag = stockUpdateFlag = false;
    }

    private void setStockUpdateFlag(){
        if(!stockUpdateFlag){
            if(stock!=null){
                stock = PostSqlUpdater.getManualWritableEntity(stock);
            }
            finishBeforeSubmit();
        }
        stockUpdateFlag = true;
    }

    private void setStockSummaryUpdateFlag(){
        if(!summaryUpdateFlag){
            if(stockSummary!=null){
                stockSummary = PostSqlUpdater.getManualWritableEntity(stockSummary);
            }
            finishBeforeSubmit();
        }
        summaryUpdateFlag = true;
    }

    private boolean fbsFlag = false;
    private void finishBeforeSubmit(){
        if(!fbsFlag){
            fbsFlag = true;
            TransactionCaches.computeIfAbsent("__stock_operators__", k->{
                //在提交前设置结束操作
                Transactions.appendEventBeforeCommit(StockOperator::finishAll);
                return new HashSet<>();
            }).add(this);
        }
    }

    private MaterialCropGather getStockSummary(){
        return TransactionCaches.computeIfAbsent("__st_sm_"+ warehouseId + materialId, k->stockSummaryService.get(warehouseId, materialId));
    }

    private void assert_not_null(Object object, String message){
        Assert.notNull(object,()->{
            if(sourceEntity!=null){
                log.error(">> stock operator source entity : {}", sourceEntity);
            }
            return message;
        });
    }
    
    private void assert_is_true(boolean expression, String message){
        Assert.isTrue(expression,()->{
            if(sourceEntity!=null){
                log.error(">> stock operator source entity : {}", sourceEntity);
            }
            return message;
        });
    }

    /**
     * @return 现存量句柄
     */
    private String getHandle(){
        return _GetHandle(materialId,batchNum,warehouseId,placeId);
    }

    private static String _GetHandle(Long materialId, String batchNum, Long warehouseId, Long placeId){
        return Strings.join("_",
                "StockOperator", materialId,batchNum, warehouseId, placeId
        );
    }

    //----------------------------------------------------------重写方法----------------------------------------------------------

    @Override
    public boolean equals(Object that) {
        if(that instanceof StockOperator){
            return this.getHandle().equals(((StockOperator) that).getHandle());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getHandle().hashCode();
    }
}
