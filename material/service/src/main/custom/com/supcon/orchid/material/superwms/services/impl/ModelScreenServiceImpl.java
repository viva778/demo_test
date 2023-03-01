package com.supcon.orchid.material.superwms.services.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supcon.orchid.material.entities.MaterialSocketSetPartt;
import com.supcon.orchid.material.entities.MaterialWareModel;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.material.superwms.services.ModelScreenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ModelScreenServiceImpl implements ModelScreenService {

    @Override
    @Transactional
    public String stockStatistics() {
        JSONObject resultJson = new JSONObject();
        //查询仓库建模所有仓库
        List<MaterialWareModel> materialWareModels = Dbs.findByCondition(MaterialWareModel.class, "VALID = 1 AND WARE_TYPE IN ('material_wareType/wareYL','material_wareType/wareCP')");
        materialWareModels.forEach(o -> {
            JSONArray materialJson = new JSONArray();
            //根据仓库建模查询所有物料上下限设置
            List<MaterialSocketSetPartt> materials = Dbs.findByCondition(MaterialSocketSetPartt.class, "VALID = 1 AND WARE_MODEL = ?", o.getId());
            materials.forEach(ele -> {
                if (ele.getUpAlarm() == null || ele.getUpAlarm().compareTo(BigDecimal.ZERO) == 0) {
                    return;
                }
                JSONObject materialDetail = new JSONObject();
                //根据仓库物料汇总现存量
                BigDecimal total = Dbs.first(
                        "SELECT SUM(onhand) AS total FROM MATER_STANDINGCROPS WHERE VALID = 1 AND GOOD = ? AND WARE = (SELECT ID FROM BASESET_WAREHOUSES WHERE CODE = ?)",
                        BigDecimal.class,ele.getGood().getId(),o.getCode());
                materialDetail.put("actual",total);
                materialDetail.put("safetyAlarm",ele.getSafetyAlarm());
                materialDetail.put("upAlarm",ele.getUpAlarm());
                int status = 0;
                //实际现存量/最高库存量 > 80% 状态为超限 0正常 1超限
                String pre="0.8";
                assert total != null;
                int totalPre=4;
                if (total.divide(ele.getUpAlarm(),totalPre, RoundingMode.HALF_UP).compareTo(new BigDecimal(pre)) > 0) {
                    status = 1;
                }
                materialDetail.put("status",status);
                materialDetail.put("good",ele.getGood().getName());
                materialJson.add(materialDetail);
            });
            if (materialJson.size() > 0) {
                resultJson.put(o.getName(),materialJson);
            }
        });
        return resultJson.toJSONString();
    }
}
