package com.supcon.orchid.material.superwms.events;

import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.fooramework.util.Https;
import org.springframework.stereotype.Component;

@Component
public class ModelReportViewEvent {
    @Signal("DaySettlementCustomCondition")
    private String conditionByScale(){
        String groupType = Https.getParameter("groupType");
        if("place".equals(groupType)){
            return "( \"daySettlement\".PLACE_SET IS NOT NULL)";
        } else {
            return "( \"daySettlement\".PLACE_SET IS NULL)";
        }
    }
}
