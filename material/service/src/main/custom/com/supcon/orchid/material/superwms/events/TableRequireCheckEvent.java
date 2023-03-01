package com.supcon.orchid.material.superwms.events;

import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.material.entities.MaterialInSingleDetail;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseCheckState;
import org.springframework.stereotype.Component;

@Component
public class TableRequireCheckEvent {

    @Signal("StockIncreased")
    private void setStockNotAvailable(MaterialStandingcrop stock, MaterialInSingleDetail source) {
        if (source != null) {
            Boolean inspectRequired = source.getInSingle().getInspectRequired();
            Boolean isCheck = stock.getGood().getIsCheck();
            if (Boolean.TRUE.equals(inspectRequired) && Boolean.TRUE.equals(isCheck)) {
                // 如果需要发起请检，设置现存量为不可用
                stock.setIsAvailable(false);
            } else {
                stock.setIsAvailable(true);
                stock.setCheckState(new SystemCode(BaseCheckState.UNNECESSARY));
            }
        }
    }

}