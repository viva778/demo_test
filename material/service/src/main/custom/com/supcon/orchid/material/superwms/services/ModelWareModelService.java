package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.material.entities.MaterialWareModel;
import com.supcon.orchid.material.entities.MaterialWarehouse;
import com.supcon.orchid.material.superwms.entities.dto.StockAlarmDTO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ModelWareModelService {

    List<MaterialWareModel> getPlaceModelsByWarehouse(Long warehouseId, Date movedCheckBeginDate);

    Set<Long> getPlaceModelIdsByWarehouse(Long warehouseId, Date movedCheckBeginDate);

    List<MaterialWareModel> getBulkEnabledPlaces(List<MaterialWareModel> wareModelList);

    /**
     * 获取建模下启用的货位
     * @param wareModel 仓库建模
     * @return 货位列表
     */
    List<MaterialWareModel> getEnabledPlaces(MaterialWareModel wareModel);

    /**
     * 获取建模下所有的货位
     * @param wareModel 仓库建模
     * @return 货位列表
     */
    List<MaterialWareModel> getAllPlaces(MaterialWareModel wareModel);


    StockAlarmDTO checkStockAlarm(Long warehouseId, Long materialId, String direction, BigDecimal quantity);


    /**
     * 获取默认货位
     */
    Map<String, BaseSetWarehouse> findDefaultWare(List<Long> materialIds);

    String findGoodIds(Long wareId);
}
