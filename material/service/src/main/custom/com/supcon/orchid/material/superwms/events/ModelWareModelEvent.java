package com.supcon.orchid.material.superwms.events;

import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.material.entities.MaterialQuicklyCreate;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.entities.MaterialWareModel;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.fooramework.annotation.signal.SignalManager;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.material.services.MaterialWareModelService;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseWareAttribute;
import com.supcon.orchid.material.superwms.constants.systemcode.WareType;
import com.supcon.orchid.material.superwms.services.ModelWareModelService;
import com.supcon.orchid.material.superwms.util.MaterialExceptionThrower;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;

@Component
public class ModelWareModelEvent {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MaterialWareModelService materialWareModelService;

    @Autowired
    private ModelWareModelService modelWareModelService;

    @Signal("BeforeSaveWareModel")
    private void beforeSaveWareModel(MaterialWareModel wareModel){
        if(wareModel.getOnlyCode()==null){
            //计算only-code
            String onlyCode;
            if(wareModel.getParentId()!=null && wareModel.getParentId() != -1){
                String parentCode = Dbs.first(
                        "SELECT ONLY_CODE FROM "+MaterialWareModel.TABLE_NAME+" WHERE ID=?",
                        String.class,
                        wareModel.getParentId()
                );
                Assert.notNull(parentCode,"找不到编码「"+wareModel.getCode()+"」对应父节点");
                onlyCode = parentCode+"-"+wareModel.getCode();
            } else {
                onlyCode = wareModel.getCode();
            }
            //校验唯一性
            if(Dbs.exist(
                    MaterialWareModel.TABLE_NAME,
                    "VALID=1 AND ONLY_CODE=?",
                    onlyCode
            )){
                MaterialExceptionThrower.uniqueness_constraint("编码["+wareModel.getCode()+"]");
            }
            //设置onlyCode
            wareModel.setOnlyCode(onlyCode);
        }
    }

    @Signal("AfterSaveWareModel")
    private void afterSaveWareModel(MaterialWareModel wareModel){
        //同步到基础
        if(WareType.isPlace(wareModel.getWareType().getId())){
            //----------------------------货位类型--------------------------------
            //根据onlyCode查找已存在货位
            BaseSetStoreSet existPlace = Dbs.load(BaseSetStoreSet.class,"VALID=1 AND ONLY_CODE=?",wareModel.getOnlyCode());
            if(existPlace!=null){
                //------------------修改---------------------
                existPlace.setName(wareModel.getName());
                existPlace.setKeeper(wareModel.getWareKeeper());
                existPlace.setMemoField(wareModel.getMemoField());
                SignalManager.propagate("SyncCargoPlace",existPlace,wareModel);
                Dbs.saveTreeNode(existPlace);
            } else {
                //------------------新增---------------------
                //获取所属仓库
                BaseSetWarehouse warehouse =  Dbs.load(
                        BaseSetWarehouse.class,
                        "VALID=1 AND CODE=(SELECT ONLY_CODE FROM "+MaterialWareModel.TABLE_NAME+" WHERE VALID=1 AND LAY_REC=?)",
                        wareModel.getLayRec().split("-")[0]
                );
                BaseSetStoreSet place = new BaseSetStoreSet();
                place.setCode(wareModel.getCode());
                place.setOnlyCode(wareModel.getOnlyCode());
                place.setName(wareModel.getName());
                place.setKeeper(wareModel.getWareKeeper());
                place.setMemoField(wareModel.getMemoField());
                place.setCid(wareModel.getCid());
                place.setWarehouse(warehouse);
                SignalManager.propagate("SyncCargoPlace",place,wareModel);
                Dbs.saveTreeNode(place);
                //更新仓库状态为使用
                if(warehouse!=null&&!Boolean.TRUE.equals(warehouse.getStoresetState())){
                    warehouse.setStoresetState(true);
                    Dbs.merge(warehouse);
                }
            }
        } else if(WareType.isWarehouse(wareModel.getWareType().getId())){
            //----------------------------仓库类型--------------------------------
            BaseSetWarehouse existWarehouse = Dbs.load(BaseSetWarehouse.class,"VALID=1 AND CODE=?",wareModel.getCode());
            if(existWarehouse!=null) {
                //------------------修改---------------------
                existWarehouse.setName(wareModel.getName());
                existWarehouse.setWarehouseState(wareModel.getUsingState());
                existWarehouse.setBelongDept(wareModel.getBelongDept());
                existWarehouse.setKeeper(wareModel.getWareKeeper());
                existWarehouse.setMemoField(wareModel.getMemoField());
                existWarehouse.setWarehouseClass(wareModel.getWarehouseClass());
                existWarehouse.setWarehouseAttribute(new SystemCode(
                        WareType.WARE_PRODUCT.equals(wareModel.getWareType().getId())?
                                BaseWareAttribute.LOGISTICS:BaseWareAttribute.WORKSHOP
                ));
                Dbs.merge(existWarehouse);
            } else {
                //------------------新增---------------------
                BaseSetWarehouse warehouse = new BaseSetWarehouse();
                warehouse.setCode(wareModel.getCode());
                warehouse.setName(wareModel.getName());
                warehouse.setWarehouseState(wareModel.getUsingState());
                warehouse.setBelongDept(wareModel.getBelongDept());
                warehouse.setKeeper(wareModel.getWareKeeper());
                warehouse.setMemoField(wareModel.getMemoField());
                warehouse.setWarehouseClass(wareModel.getWarehouseClass());
                warehouse.setWarehouseAttribute(new SystemCode(
                        WareType.WARE_PRODUCT.equals(wareModel.getWareType().getId())?
                                BaseWareAttribute.LOGISTICS:BaseWareAttribute.WORKSHOP
                ));
                warehouse.setStoresetState(false);
                warehouse.setCid(wareModel.getCid());
                Dbs.save(warehouse);
            }
        }
    }


    @Signal("BeforeDeleteWareModel")
    private void beforeDeleteWareModel(MaterialWareModel wareModel){
        boolean cannot_delete;
        if(WareType.isWarehouse(wareModel.getWareType().getId())){
            //如果该仓库下存在有效现存量，则不允许删除
            cannot_delete = Dbs.exist(
                    MaterialStandingcrop.TABLE_NAME,
                    "VALID=1 AND ONHAND>0 AND WARE=(SELECT ID FROM "+BaseSetWarehouse.TABLE_NAME+" WHERE VALID=1 AND CODE=?)",
                    wareModel.getCode()
            );
        } else {
            //查找建模下的货位
            List<MaterialWareModel> places = modelWareModelService.getAllPlaces(wareModel);
            //如果任意货位下存在有效现存量，则不允许删除
            cannot_delete = places.size()>0&&Dbs.exist(
                    MaterialStandingcrop.TABLE_NAME,
                    "VALID=1 AND ONHAND>0 AND PLACE_SET IN (SELECT ID FROM "+BaseSetStoreSet.TABLE_NAME+" WHERE VALID=1 AND "+Dbs.inCondition("ONLY_CODE",places.size())+")",
                    places.stream().map(MaterialWareModel::getOnlyCode).toArray()
            );
        }
        if(cannot_delete){
            //进行提示
            MaterialExceptionThrower.ware_model_cannot_delete(
                    InternationalResource.get(Dbs.getProp(wareModel.getWareType(),SystemCode::getValue)),
                    wareModel.getName()
            );
        }
    }

    @Signal("AfterDeleteWareModel")
    private void afterDeleteWareModel(MaterialWareModel wareModel){
        //同步到基础
        if(WareType.isPlace(wareModel.getWareType().getId())) {
            //----------------------------货位类型--------------------------------
            Dbs.execute(
                    "UPDATE "+BaseSetStoreSet.TABLE_NAME+" SET VALID=0 WHERE VALID=1 AND ONLY_CODE=?",
                    wareModel.getOnlyCode()
            );
        } else if(WareType.isWarehouse(wareModel.getWareType().getId())){
            //----------------------------仓库类型--------------------------------
            Dbs.execute(
                    "UPDATE "+BaseSetWarehouse.TABLE_NAME+" SET VALID=0 WHERE VALID=1 AND CODE=?",
                    wareModel.getCode()
            );
        }
    }

    /**
     * 批量新增
     */
    @Signal("afterSaveQuicklyCreate")
    private void batchAdd(MaterialQuicklyCreate quicklyCreate) {
        int toyal = quicklyCreate.getAddTotal();
        int begin = quicklyCreate.getInitialNumber();
        String codePrefix = "";
        if (null != quicklyCreate.getCodePrefix() && !quicklyCreate.getCodePrefix().isEmpty()) {
            codePrefix = quicklyCreate.getCodePrefix();
        }
        for (int i = begin; i < toyal+begin; i++) {
            MaterialWareModel wareModel = new MaterialWareModel();
            if("" != codePrefix){
                wareModel.setCode(codePrefix+String.valueOf(i));
                wareModel.setWareType(quicklyCreate.getWareType());
                wareModel.setWareKeeper(quicklyCreate.getWareKeeper());
                if(null != quicklyCreate.getShelvesType()){
                    wareModel.setShelvesType(quicklyCreate.getShelvesType());
                }
                if(null != quicklyCreate.getStoreSetType()){
                    wareModel.setStoreSetType(quicklyCreate.getStoreSetType());
                }
                if(null != quicklyCreate.getCapacity()){
                    wareModel.setCapacity(quicklyCreate.getCapacity());
                }
                if(null != quicklyCreate.getVolume()){
                    wareModel.setVolume(quicklyCreate.getVolume());
                }
            }
            MaterialWareModel parentWareModel = new MaterialWareModel();
            parentWareModel = materialWareModelService.getWareModel(quicklyCreate.getNodeId());
            Long id = wareModel.getId();
            wareModel.setParentId(quicklyCreate.getNodeId());
            wareModel.setFullPathName(parentWareModel.getFullPathName()+"/"+id);
            wareModel.setLayRec(parentWareModel.getLayRec()+"-"+id);
            if("material_wareType/cargoArea".equals(quicklyCreate.getWareType().getId())){
                wareModel.setLayNo(3);
                wareModel.setName(codePrefix+String.valueOf(i)+"货架");
            }else if("material_wareType/storeSet".equals(quicklyCreate.getWareType().getId())){
                wareModel.setLayNo(4);
                wareModel.setName(codePrefix+String.valueOf(i)+"货位");
            }
            wareModel.setLeaf(true);
            parentWareModel.setLeaf(false);
            materialWareModelService.saveWareModel(wareModel,null);
            materialWareModelService.saveWareModel(parentWareModel,null);
        }
    }
}
