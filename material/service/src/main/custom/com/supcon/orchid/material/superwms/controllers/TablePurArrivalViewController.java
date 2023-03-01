package com.supcon.orchid.material.superwms.controllers;


import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.material.entities.MaterialWarehouse;
import com.supcon.orchid.material.superwms.services.ModelWareModelService;
import com.supcon.orchid.material.superwms.services.TablePurArrivalViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class TablePurArrivalViewController {

    @Autowired
    TablePurArrivalViewService purArrivalViewService;

    @Autowired
    ModelWareModelService modelWareModelService;


    //通过客商查找对应物料
    @GetMapping(value = "/material/purArrival/purArrival/findWarewithVendor")
    public String findWarewithVendor(Long vendor) {
        List<String> ids = purArrivalViewService.findWareByVendor(vendor);
        return String.join(",", ids);
    }

    //查询采购到货单默认仓库
    @PostMapping(value = "/material/purArrival/purArrival/findDefaultWare")
    public Map<String, BaseSetWarehouse> findDefaultWare(@RequestBody List<Long> materialIds) {
        return modelWareModelService.findDefaultWare(materialIds);
    }


}