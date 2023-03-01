package com.supcon.orchid.material.superwms.events;


import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.material.superwms.config.MaterialSystemConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TableMaterialInboundViewEvent {

    @Autowired
    private MaterialSystemConfig materialSystemConfig;

    //质检采购入库生成
    @Signal("PurchaseInCustomCondition")
    private String qualityCheck(){
        if(Boolean.FALSE.equals(materialSystemConfig.getArrivalCheck())){
            return "( is_check_over =1 )";
        }
        return null;
    }
}
