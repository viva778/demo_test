package com.supcon.orchid.material.superwms.events;

import com.supcon.orchid.ec.services.MsModuleRelationService;
import com.supcon.orchid.material.DTO.EAMOutSingleDTO;
import com.supcon.orchid.material.client.MaterialInstallClient;
import com.supcon.orchid.material.entities.MaterialOtherOutSingle;
import com.supcon.orchid.material.entities.MaterialOutSingleDetai;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.material.superwms.util.MaterialExceptionThrower;
import com.supcon.orchid.support.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 设备事件
 */
@Component
public class ExternEAMEvent {

    @Autowired
    private MsModuleRelationService msModuleRelationService;
    @Autowired
    private MaterialInstallClient installClient;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * 设备领用发起安装验收
     * @author wangkai
     * create by wk 2021-09-18
     */
    @Signal("OtherOutEffecting")
    private void installNotify(MaterialOtherOutSingle table, List<MaterialOutSingleDetai> details){
        if("equipmentOut".equals(table.getOutCome().getReasonCode())){
            if (!msModuleRelationService.checkModuleStatus("EAM")) {
                //设备模块未启动！
                MaterialExceptionThrower.eam_disable();
            }
            details.forEach(detail->{
                EAMOutSingleDTO dto = new EAMOutSingleDTO();
                dto.setOutPerson(table.getOutPerson().getId());
                dto.setWareId(table.getWare().getId());
                dto.setOutStorageDate(table.getOutStorageDate());

                dto.setGoodCode(detail.getGood().getCode());
                dto.setOutQuantity(detail.getOutQuantity());
                Result<?> result = installClient.dealOutSingle(dto);
                if(result.getCode()!=200){
                    MaterialExceptionThrower.eam_error(result.getMessage());
                }
            });
        }
    }
}
