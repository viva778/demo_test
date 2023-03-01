package com.supcon.orchid.material.superwms.events;

import com.supcon.orchid.ec.services.MsModuleRelationService;
import com.supcon.orchid.material.client.MaterialSpareClient;
import com.supcon.orchid.material.entities.MaterialOtherOutSingle;
import com.supcon.orchid.material.entities.MaterialOutSingleDetai;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.material.superwms.entities.dto.SpareManageOtherOutDTO;
import com.supcon.orchid.material.superwms.entities.dto.SpareManageOtherOutDetailDTO;
import com.supcon.orchid.material.superwms.util.MaterialExceptionThrower;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Jacksons;
import com.supcon.orchid.services.BAPException;
import com.supcon.orchid.support.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.data.util.CastUtils.cast;

/**
 * 备件管理事件
 */
@Component
public class ExternSpareManageEvent {

    @Autowired
    private MsModuleRelationService msModuleRelationService;
    @Autowired
    private MaterialSpareClient materialSpareClient;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * 其他出库取消回填备件
     */
    @Signal("OtherOutCanceling")
    private void otherCanceling(MaterialOtherOutSingle table){
        //获取表体详细
        List<MaterialOutSingleDetai> details = Dbs.findByCondition(
                MaterialOutSingleDetai.class,
                "VALID=1 AND OUT_SINGLE=? ",
                table.getId()
        );
        callback(table,details);
    }

    /**
     * 其他出库生效回填备件
     */
    @Signal("OtherOutEffecting")
    private void callback(MaterialOtherOutSingle table, List<MaterialOutSingleDetai> details) {
        if(Boolean.TRUE.equals(table.getIsCallBack())) {
            if (!msModuleRelationService.checkModuleStatus("SpareManage")) {
                MaterialExceptionThrower.spare_mgmt_disable();
            }
            SpareManageOtherOutDTO dto = new SpareManageOtherOutDTO();
            dto.setStatus(table.getStatus());
            if(table.getStatus().equals(0)){
                dto.setDetailList(details.stream().map(detail->{
                    SpareManageOtherOutDetailDTO detailDTO = new SpareManageOtherOutDetailDTO();
                    detailDTO.setRecordSpareDetailId(detail.getRecordSpareDetail());
                    return detailDTO;
                }).collect(Collectors.toList()));
            } else if(table.getStatus().equals(99)){
                dto.setDetailList(details.stream().map(detail->{
                    SpareManageOtherOutDetailDTO detailDTO = new SpareManageOtherOutDetailDTO();
                    detailDTO.setRecordSpareDetailId(detail.getRecordSpareDetail());
                    detailDTO.setOutQuantity(detail.getOutQuantity());
                    return detailDTO;
                }).collect(Collectors.toList()));
            }
            Result<?> result = materialSpareClient.generateOtherOutSingleCallBack(Jacksons.writeValue(dto));
            if(result.getCode()==200){
                Map<String,Object> data = cast(result.getData());
                if(Integer.valueOf(0).equals(data.get("code"))){
                    throw new BAPException(data.get("msg").toString());
                }
            } else {
                throw new BAPException(result.getMessage());
            }
        }
    }
}
