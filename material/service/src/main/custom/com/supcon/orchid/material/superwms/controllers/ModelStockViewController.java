package com.supcon.orchid.material.superwms.controllers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.fooramework.util.Jacksons;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.material.superwms.entities.qo.CargoStockQO;
import com.supcon.orchid.material.superwms.services.ModelStockViewService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ModelStockViewController {

    @Autowired
    private ModelStockViewService viewService;


    /**
     * 根据ids查询现存量
     * @param ids 现存量id列表
     * @param includes 包含列
     * @return 现存量列表json
     */
    @SneakyThrows
    @GetMapping(value = "/material/cargoStock/cargoStock/getCargoStockByIds", produces = "application/json")
    public String getCargoStockByIds(Long[] ids, String includes){
        List<MaterialStandingcrop> list = viewService.getStockByIds(Arrays.asList(ids));
        return Strings.valid(includes)?
                Jacksons.writeValueWithIncludes(list,includes):
                Jacksons.writeValue(includes);
    }

    @RequestMapping(value = "/material/cargoStock/cargoStock/getCargoStockList", produces = "application/json")
    public String getCargoStockList(@RequestBody List<CargoStockQO> qoList){
        List<MaterialStandingcrop> list = qoList.stream().map(viewService::getCargoStock).collect(Collectors.toList());
        //去除空属性
        return Jacksons.config()
                .include(JsonInclude.Include.NON_NULL)
                .writeValue(list);
    }

}
