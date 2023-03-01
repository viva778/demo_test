package com.supcon.orchid.material.superwms.controllers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.MaterialWareModel;
import com.supcon.orchid.fooramework.support.Converter;
import com.supcon.orchid.fooramework.util.Entities;
import com.supcon.orchid.fooramework.util.Jacksons;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.material.superwms.entities.dto.StockAlarmDTO;
import com.supcon.orchid.material.superwms.services.ModelWareModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class ModelWareModelViewController {

    @Autowired
    private ModelWareModelService wareModelService;

    @GetMapping(value = "/material/wareModel/getBulkEnabledPlaceModels", produces = "application/json")
    public String getBulkEnabledPlaceModels(@RequestParam Long[] wareModelIds, String[] includes) {
        String str_includes = (includes != null && includes.length > 0) ? String.join(",", includes) : null;
        List<MaterialWareModel> wareModelList = wareModelService.getBulkEnabledPlaces(Stream.of(wareModelIds).map(id -> {
            MaterialWareModel wareModel = new MaterialWareModel();
            wareModel.setId(id);
            wareModel.setWareType(new SystemCode("fill"));
            return wareModel;
        }).collect(Collectors.toList()));
        wareModelList.forEach(Entities::translateSystemCode);
        return Strings.valid(str_includes)
                ? Jacksons.writeValueWithIncludes(wareModelList, str_includes)
                : Jacksons.config().include(JsonInclude.Include.NON_NULL).writeValue(wareModelList);
    }

    @GetMapping(value = "/material/wareModel/getPlaceModelsByWarehouse", produces = "application/json")
    public String getPlaceModelsByWarehouse(@RequestParam Long warehouseId, String movedCheckBeginDate, String[] includes) {
        String str_includes = (includes != null && includes.length > 0) ? String.join(",", includes) : null;
        List<MaterialWareModel> wareModelList = wareModelService.getPlaceModelsByWarehouse(warehouseId, Strings.valid(movedCheckBeginDate) ? Converter.dateConverter(movedCheckBeginDate) : null);
        wareModelList.forEach(Entities::translateSystemCode);
        return Strings.valid(str_includes)
                ? Jacksons.writeValueWithIncludes(wareModelList, str_includes)
                : Jacksons.config().include(JsonInclude.Include.NON_NULL).writeValue(wareModelList);
    }

    /**
     * 查询出入库情况是否触发库存预警
     *
     * @param goodId    物品Id
     * @param wareId    仓库ID
     * @param direction 收发方向
     * @param num       数量
     * @return isNo :true /false
     * @author wangkai
     * @date 2019/12/23 15:44
     */
    @GetMapping(value = "/material/socketSet/socketSetInfo/findSocketSet")
    public StockAlarmDTO findSocketSet(@RequestParam(value = "wareId") Long wareId, @RequestParam(value = "goodId") Long goodId, @RequestParam(value = "direction") String direction, @RequestParam(value = "num") String num) {
        return wareModelService.checkStockAlarm(wareId, goodId, direction, new BigDecimal(num));
    }

    /**
     * 查询仓库关联的物料id
     * 若返回为null，则返回后需当作查询全部物料处理
     *
     * @return null 查询全部物料处理
     * ids 对应物料的id集合
     */
    @GetMapping(value = "/material/wareModel/findGoodIdByWare")
    public String findGoodIds(@RequestParam(value = "wareId") Long wareId) {
        String strings = wareModelService.findGoodIds(wareId);
        return strings == null ? "" : strings;
    }


}
