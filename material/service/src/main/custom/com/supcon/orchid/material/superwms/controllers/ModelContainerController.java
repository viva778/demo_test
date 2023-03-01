package com.supcon.orchid.material.superwms.controllers;

import com.supcon.orchid.material.entities.MaterialContainerParts;
import com.supcon.orchid.material.entities.MaterialPurchInSingle;
import com.supcon.orchid.material.entities.MaterialPurchInSubDtl;
import com.supcon.orchid.material.superwms.services.ModelContainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ModelContainerController {
    @Autowired
    private ModelContainerService containerService;

    /**
     * 容器明细确认按触发
     * 保存明细数据
     * 更改到货单明细，卸货完毕字段
     * @param details
     * @return
     */
    @PostMapping(value = "/material/container/checkoutContainerDetails", produces = "application/json")
    public String checkoutContainerDetails(@RequestBody List<MaterialContainerParts> details){
        containerService.checkoutContainerDetails(details);
        return "success";
    }

    @PostMapping(value = "/material/container/savePurchaseInContainerDetails", produces = "application/json")
    public String savePurchaseInContainerDetails(@RequestBody List<MaterialPurchInSubDtl> substanceDetails){
        containerService.savePurchaseInContainerDetails(substanceDetails);
        return "success";
    }


    @PostMapping(value = "/material/container/queryDetailsInPurchaseInSingles", produces = "application/json")
    public List<MaterialPurchInSingle> queryDetailsInPurchaseInSingles(@RequestBody MaterialContainerParts details){
        return containerService.queryDetailsInPurchaseInSingles(details);
    }

    @PostMapping(value = "/material/container/deletePurchaseInContainerDetails", produces = "application/json")
    public String deletePurchaseInContainerDetails(@RequestBody List<Long> ids){
        containerService.deletePurchaseInContainerDetails(ids);
        return "success";
    }

}
