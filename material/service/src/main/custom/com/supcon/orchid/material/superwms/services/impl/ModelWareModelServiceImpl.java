package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.BaseSet.entities.*;
import com.supcon.orchid.material.entities.MaterialBusinessDetail;
import com.supcon.orchid.material.entities.MaterialSocketSetPartt;
import com.supcon.orchid.material.entities.MaterialWareModel;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Entities;
import com.supcon.orchid.fooramework.util.Hbs;
import com.supcon.orchid.material.superwms.config.MaterialSystemConfig;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseWareState;
import com.supcon.orchid.material.superwms.constants.systemcode.OperateDirection;
import com.supcon.orchid.material.superwms.constants.systemcode.WareType;
import com.supcon.orchid.material.superwms.entities.dto.StockAlarmDTO;
import com.supcon.orchid.material.superwms.services.ModelStockService;
import com.supcon.orchid.material.superwms.services.ModelWareModelService;
import com.supcon.orchid.orm.entities.IdEntity;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ModelWareModelServiceImpl implements ModelWareModelService {


    @Autowired
    MaterialSystemConfig materialSystemConfig;


    @Transactional
    @Override
    public List<MaterialWareModel> getPlaceModelsByWarehouse(Long warehouseId, Date movedCheckBeginDate) {
        List<String> conditions = new LinkedList<>();
        List<Object> params = new LinkedList<>();
        conditions.add("VALID=1");
        conditions.add("USING_STATE=?");
        params.add(BaseWareState.ENABLE);
        conditions.add("WARE_TYPE=?");
        params.add(WareType.CARGO_PLACE);
        conditions.add("LAY_REC LIKE CONCAT((SELECT LAY_REC FROM " + MaterialWareModel.TABLE_NAME + " WHERE VALID=1 AND ONLY_CODE=(SELECT CODE FROM " + BaseSetWarehouse.TABLE_NAME + " WHERE VALID=1 AND ID=?)),'%')");
        params.add(warehouseId);
        if (movedCheckBeginDate != null) {
            conditions.add("ONLY_CODE IN(SELECT ONLY_CODE FROM " + BaseSetStoreSet.TABLE_NAME + " WHERE VALID=1 AND ID IN(SELECT PLACE_SET FROM " + MaterialBusinessDetail.TABLE_NAME + " WHERE TRASACTION_DATE>?))");
            params.add(movedCheckBeginDate);
        }
        return Dbs.findByCondition(
                MaterialWareModel.class,
                String.join(" AND ", conditions),
                params.toArray()
        );
    }

    @Transactional
    @Override
    public Set<Long> getPlaceModelIdsByWarehouse(Long warehouseId, Date movedCheckBeginDate) {
        List<String> conditions = new LinkedList<>();
        List<Object> params = new LinkedList<>();
        conditions.add("VALID=1");
        conditions.add("USING_STATE=?");
        params.add(BaseWareState.ENABLE);
        conditions.add("WARE_TYPE=?");
        params.add(WareType.CARGO_PLACE);
        conditions.add("LAY_REC LIKE CONCAT((SELECT LAY_REC FROM " + MaterialWareModel.TABLE_NAME + " WHERE VALID=1 AND ONLY_CODE=(SELECT CODE FROM " + BaseSetWarehouse.TABLE_NAME + " WHERE VALID=1 AND ID=?)),'%')");
        params.add(warehouseId);
        if (movedCheckBeginDate != null) {
            conditions.add("ONLY_CODE IN(SELECT ONLY_CODE FROM " + BaseSetStoreSet.TABLE_NAME + " WHERE VALID=1 AND ID IN(SELECT PLACE_SET FROM " + MaterialBusinessDetail.TABLE_NAME + " WHERE TRASACTION_DATE>?))");
            params.add(movedCheckBeginDate);
        }
        return Dbs.stream(
                "SELECT ID FROM " + MaterialWareModel.TABLE_NAME + " WHERE " + String.join(" AND ", conditions),
                Long.class,
                params.toArray()
        ).collect(Collectors.toSet());
    }


    /**
     * 获取建模下启用的货位
     *
     * @param wareModelList 仓库建模列表
     * @return 货位列表
     */
    @Transactional
    @Override
    public List<MaterialWareModel> getBulkEnabledPlaces(List<MaterialWareModel> wareModelList) {
        List<MaterialWareModel> enabledPlaces = new LinkedList<>();
        List<MaterialWareModel> nodeModelList = wareModelList.stream().filter(wareModel -> {
            if (WareType.CARGO_PLACE.equals(wareModel.getWareType().getId())) {
                enabledPlaces.add(wareModel);
                return false;
            } else {
                return true;
            }
        }).collect(Collectors.toList());
        if (!nodeModelList.isEmpty()) {
            //查询非叶子节点下的货位，尝试拼接层级条件
            enabledPlaces.addAll(Hbs.findByCriteriaWithIncludes(
                    MaterialWareModel.class,
                    "id,code,name,onlyCode,wareType.id,wareType.value",
                    Restrictions.eq("wareType.id", WareType.CARGO_PLACE),
                    Restrictions.or(nodeModelList.stream().map(IdEntity::getId).map(id -> Restrictions.like("layRec", "%" + id + "%")).toArray(Criterion[]::new)),
                    Restrictions.eq("usingState.id", BaseWareState.ENABLE)
            ));
        }
        enabledPlaces.forEach(Entities::translateSystemCode);
        return enabledPlaces;
    }

    /**
     * 获取建模下启用的货位
     *
     * @param wareModel 仓库建模
     * @return 货位列表
     */
    @Transactional
    @Override
    public List<MaterialWareModel> getEnabledPlaces(MaterialWareModel wareModel) {
        if (WareType.CARGO_PLACE.equals(wareModel.getWareType().getId())) {
            return Collections.singletonList(wareModel);
        } else {
            return Hbs.findByCriteriaWithProjections(
                    MaterialWareModel.class,
                    "id,code,name,onlyCode",
                    Restrictions.eq("wareType.id", WareType.CARGO_PLACE),
                    Restrictions.like("layRec", "%" + wareModel.getId() + "%"),
                    Restrictions.eq("usingState.id", BaseWareState.ENABLE)
            );
        }
    }

    /**
     * 获取建模下所有的货位
     *
     * @param wareModel 仓库建模
     * @return 货位列表
     */
    @Transactional
    @Override
    public List<MaterialWareModel> getAllPlaces(MaterialWareModel wareModel) {
        if (WareType.CARGO_PLACE.equals(wareModel.getWareType().getId())) {
            return Collections.singletonList(wareModel);
        } else {
            return Hbs.findByCriteriaWithProjections(
                    MaterialWareModel.class,
                    "id,code,name,onlyCode",
                    Restrictions.eq("wareType.id", WareType.CARGO_PLACE),
                    Restrictions.like("layRec", "%" + wareModel.getId() + "%")
            );
        }
    }

    @Autowired
    private ModelStockService stockService;


    //遗留方法
    @Transactional
    @Override
    public StockAlarmDTO checkStockAlarm(Long warehouseId, Long materialId, String direction, BigDecimal quantity) {
        StockAlarmDTO alarmDTO = new StockAlarmDTO();
        //查找对应仓库建模
        Long wareModelId = Dbs.first(
                "SELECT ID FROM " + MaterialWareModel.TABLE_NAME + " WHERE VALID=1 AND CODE=(SELECT CODE FROM " + BaseSetWarehouse.TABLE_NAME + " WHERE ID=?)",
                Long.class,
                warehouseId
        );
        if (wareModelId == null) {
            return alarmDTO;
        }
        //查找上下限设置
        MaterialSocketSetPartt setting = Dbs.load(
                MaterialSocketSetPartt.class,
                "VALID=1 AND WARE_MODEL=? AND GOOD=?",
                wareModelId, materialId
        );
        if (setting == null) {
            return alarmDTO;
        }
        Pair<BigDecimal, BigDecimal> quantityTotal = stockService.getQuantityTotal(warehouseId, materialId);
        BigDecimal stockQuan = quantityTotal.getFirst();
        if (OperateDirection.RECEIVE.endsWith(direction)) {
            //收，比较上限
            if (stockQuan.add(quantity).compareTo(setting.getUpAlarm()) > 0) {
                alarmDTO.setActivate(true);
            }
        } else {
            //发，比较下限
            if (stockQuan.subtract(quantity).compareTo(setting.getDownAlarm()) < 0) {
                alarmDTO.setActivate(true);
            }
        }
        alarmDTO.setUpAlarm(setting.getUpAlarm());
        alarmDTO.setDownAlarm(setting.getDownAlarm());
        alarmDTO.setStockQuan(stockQuan);
        return alarmDTO;
    }

    @Override
    @Transactional
    public Map<String, BaseSetWarehouse> findDefaultWare(List<Long> materialIds) {
        switch (String.valueOf(materialSystemConfig.getStorageCheckType())) {
            case "ware": {
                //--------------------------仓库下设置物品存放-------------------------
                //查询对应物料
                List<BaseSetMaterial> materials = Dbs.findByCondition(BaseSetMaterial.class, " VALID = 1 AND " +
                        Dbs.inCondition("ID", materialIds.size()), materialIds.toArray()
                );
                //查找仓库
                List<BaseSetWareMater> wares = Dbs.findByCondition(
                        BaseSetWareMater.class, " VALID=1 AND " +
                                Dbs.inCondition("MATERIAL", materialIds.size()), materialIds.toArray()
                );
                //物料id -- 物料仓库映射
                Map<Long, List<BaseSetWareMater>> goodIdMaterMap = wares.stream().collect(Collectors.groupingBy(e -> e.getMaterial().getId()));
                //物料分类对应关系
                Set<Long> clazzs = materials.stream().map(e -> e.getMaterialClass().getId()).collect(Collectors.toSet());
                List<BaseSetWareMaterClz> material_class = Dbs.findByCondition(
                        BaseSetWareMaterClz.class, " VALID=1 AND " +
                                Dbs.inCondition("MATERIAL_CLASS", clazzs.size()), clazzs.toArray()
                );
                //物料类型 -- 类型仓库映射
                Map<Long, List<BaseSetWareMaterClz>> goodClassIdMaterMap = material_class.stream().collect(Collectors.groupingBy(e -> e.getMaterialClass().getId()));
                Map<String, BaseSetWarehouse> map = new HashMap<>();
                for (BaseSetMaterial material : materials) {
                    List<BaseSetWarehouse> warehouses = new ArrayList<>();
                    Long id = material.getId();
                    //加入物料映射
                    List<BaseSetWareMater> baseSetWareMaters = goodIdMaterMap.get(id);
                    if (baseSetWareMaters != null && !baseSetWareMaters.isEmpty()) {
                        warehouses.addAll(baseSetWareMaters.stream().map(BaseSetWareMater::getWarehouse).collect(Collectors.toList()));
                    }
                    //加入物料类型映射
                    List<BaseSetWareMaterClz> baseSetWareMaterClzs = goodClassIdMaterMap.get(material.getMaterialClass().getId());
                    if (baseSetWareMaterClzs != null && !baseSetWareMaterClzs.isEmpty()) {
                        warehouses.addAll(baseSetWareMaterClzs.stream().map(BaseSetWareMaterClz::getWarehouse).collect(Collectors.toList()));
                    }
                    if (!warehouses.isEmpty()) {
                        warehouses.sort(Comparator.comparing(BaseSetWarehouse::getCode));
                        map.put(String.valueOf(id), warehouses.get(0));
                    }
                }
                return map;
            }
            case "good": {
                //--------------------------物品下设置仓库存放-------------------------
                List<BaseSetMaterialWareSet> material = Dbs.findByCondition(
                        BaseSetMaterialWareSet.class, " VALID=1 AND SET_ALLOW = 1 AND " +
                                Dbs.inCondition("MATERIAL", materialIds.size()), materialIds.toArray()
                );
                List<Long> setIds = material.stream().map(BaseSetMaterialWareSet::getId).collect(Collectors.toList());
                List<BaseSetMaterialWareList> material_ware_set = Dbs.findByCondition(BaseSetMaterialWareList.class, "VALID = 1 AND " +
                        Dbs.inCondition("MATERIAL_WARE_SET", setIds.size()), setIds.toArray()
                );
                // 物料id -- 物料映射
                Map<Long, List<BaseSetMaterialWareList>> collect = material_ware_set.stream().collect(Collectors.groupingBy(e -> Dbs.getProp(e.getMaterialWareSet(), f -> f.getMaterial().getId())));
                Map<String, BaseSetWarehouse> map = new HashMap<>();
                for (Long materialId : materialIds) {
                    List<BaseSetMaterialWareList> list = collect.get(materialId);
                    if (list != null && !list.isEmpty()) {
                        List<BaseSetWarehouse> warehouses = list.stream().map(BaseSetMaterialWareList::getWareHouse).sorted(Comparator.comparing(BaseSetWarehouse::getCode)).collect(Collectors.toList());
                        map.put(String.valueOf(materialId), warehouses.get(0));
                    }
                }
                return map;
            }
            default: {
                return null;
            }
        }
    }

    @Override
    @Transactional
    public String findGoodIds(Long wareId) {
        switch (String.valueOf(materialSystemConfig.getStorageCheckType())) {
            case "ware": {
                Set<String> goodIds = new HashSet<>();
                //--------------------------仓库下设置物品存放-------------------------
                //查找仓库实体对应下的所有物料id和物料分类id
                List<BaseSetWareMaterClz> material_class = Dbs.findByCondition(
                        BaseSetWareMaterClz.class, " VALID=1 AND WAREHOUSE= ?", wareId
                );
                //查找仓库物料映射
                List<BaseSetWareMater> baseSetWareMaters = Dbs.findByCondition(
                        BaseSetWareMater.class, " VALID=1 AND WAREHOUSE= ?", wareId
                );
                //若所选仓库未设置存储物料/物料分类，则保持现有逻辑，展示所有物料明细。
                if (material_class.isEmpty() && baseSetWareMaters.isEmpty()) {
                    return null;
                }
                List<String> goodClassIds = material_class.stream().map(e -> e.getMaterialClass().getId().toString()).collect(Collectors.toList());
                if (!goodClassIds.isEmpty()) {
                    //和获取所有类型的物料
                    List<BaseSetMaterial> materials = Dbs.findByCondition(BaseSetMaterial.class, "VALID=1 AND " +
                            Dbs.inCondition("MATERIAL_CLASS", goodClassIds.size()), goodClassIds.toArray()
                    );
                    goodIds.addAll(materials.stream().map(e -> e.getId().toString()).collect(Collectors.toList()));
                }
                if (!baseSetWareMaters.isEmpty()) {
                    goodIds.addAll(baseSetWareMaters.stream().map(e -> e.getMaterial().getId().toString()).collect(Collectors.toList()));
                }
                if (goodIds.isEmpty()) {
                    return null;
                }
                return String.join(",", goodIds);

            }
            case "good": {
                //--------------------------物品下设置仓库存放-------------------------
                List<BaseSetMaterialWareList> materialWareLists = Dbs.findByCondition(
                        BaseSetMaterialWareList.class, " VALID=1 AND WARE_HOUSE = ?", wareId
                );
                if(materialWareLists.isEmpty()){
                    return null;
                }
                Set<String> goodIds = materialWareLists.stream().filter(e -> Dbs.getProp(e.getMaterialWareSet(), BaseSetMaterialWareSet::getSetAllow))
                        .map(e -> Dbs.getProp(e.getMaterialWareSet(), BaseSetMaterialWareSet::getMaterial, f -> f.getId().toString())).collect(Collectors.toSet());
                if (goodIds.isEmpty()) {
                    return null;
                }
                return String.join(",", goodIds);
            }
            default: {
                return null;
            }
        }
    }


}
