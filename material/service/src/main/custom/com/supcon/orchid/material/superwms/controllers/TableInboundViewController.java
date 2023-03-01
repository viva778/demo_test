package com.supcon.orchid.material.superwms.controllers;

import com.supcon.orchid.material.entities.MaterialWasteInDetail;
import com.supcon.orchid.fooramework.util.Jacksons;
import com.supcon.orchid.material.superwms.services.TableInboundViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class TableInboundViewController {

    @Autowired
    private TableInboundViewService tableInboundViewService;

    @GetMapping(value = "/material/waste/detail/wasteCreateDetailsByWarehouseAndMaterials")
    public String wasteCreateDetailsByWarehouseAndMaterials(Long warehouseId, @RequestParam Long[] materialIds){
        List<MaterialWasteInDetail> details = tableInboundViewService.wasteCreateDetailsByWarehouseAndMaterials(warehouseId, Arrays.asList(materialIds));
        return Jacksons.writeValueWithIncludes(
                details,"good.id,eiaNumber.id,eiaNumber.eiaNumber,nowQuanlity"
        );
    }

    @GetMapping(value = "/material/waste/detail/wasteRefreshStockByWarehouse")
    public String wasteRefreshStockByWarehouse(@RequestParam Long warehouseId, @RequestParam Long[] materialIds){
        List<MaterialWasteInDetail> details = tableInboundViewService.wasteRefreshStockByWarehouse(warehouseId, Arrays.asList(materialIds));
        return Jacksons.writeValueWithIncludes(
                details,"good.id,nowQuanlity"
        );
    }
}
