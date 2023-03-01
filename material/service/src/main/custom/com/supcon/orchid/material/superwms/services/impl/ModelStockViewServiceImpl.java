package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.material.superwms.entities.qo.CargoStockQO;
import com.supcon.orchid.material.superwms.services.ModelStockViewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;

@Service
public class ModelStockViewServiceImpl implements ModelStockViewService {

    @Transactional
    @Override
    public List<MaterialStandingcrop> getStockByIds(List<Long> ids){
        return Dbs.findByCondition(
                MaterialStandingcrop.class,
                "VALID=1 AND "+Dbs.inCondition("ID",ids.size()),
                ids.toArray()
        );
    }

    @Transactional
    @Override
    public MaterialStandingcrop getCargoStock(CargoStockQO qo){
        List<String> conditions = new LinkedList<>();
        List<Object> params = new LinkedList<>();
        if(qo.getWarehouseId()!=null){
            conditions.add("WARE=?");
            params.add(qo.getWarehouseId());
        }
        if(qo.getMaterialId()!=null){
            conditions.add("GOOD=?");
            params.add(qo.getMaterialId());
        }
        if(Strings.valid(qo.getBatchNum())){
            conditions.add("BATCH_TEXT=?");
            params.add(qo.getBatchNum());
        } else {
            conditions.add("(BATCH_TEXT='' OR BATCH_TEXT IS NULL)");
        }
        if(qo.getPlaceId()!=null) {
            conditions.add("PLACE_SET=?");
            params.add(qo.getPlaceId());
        } else {
            conditions.add("PLACE_SET IS NULL");
        }
        return Dbs.load(
                MaterialStandingcrop.class,
                "VALID=1 AND ONHAND>0 AND "+String.join(" AND ",conditions),
                params.toArray()
        );
    }
}
