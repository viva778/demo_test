package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.material.superwms.entities.qo.CargoStockQO;

import java.util.List;

public interface ModelStockViewService {

    List<MaterialStandingcrop> getStockByIds(List<Long> ids);


    MaterialStandingcrop getCargoStock(CargoStockQO qo);

}
