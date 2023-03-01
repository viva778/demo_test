package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.material.entities.MaterialServiceType;
import com.supcon.orchid.material.entities.MaterialStorageType;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.material.superwms.services.ModelBizTypeService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ModelBizTypeServiceImpl implements ModelBizTypeService {

    private static final Map<String,MaterialServiceType> BIZ_TYPE_MAP = new HashMap<>();

    private static final Map<String, MaterialStorageType> STORAGE_TYPE_MAP = new HashMap<>();

    /**
     * 根据ID获取业务类型编码
     * @param id 业务类型ID
     * @return 业务类型编码
     */
    @Override
    public String getBizType(Long id){
        return BIZ_TYPE_MAP.values().stream().filter(val->val.getId().equals(id)).findAny().map(MaterialServiceType::getServiceTypeCode).orElseGet(()->{
            String code = Dbs.first(
                    "SELECT SERVICE_TYPE_CODE FROM "+ MaterialServiceType.TABLE_NAME+" WHERE ID=? ",
                    String.class,
                    id
            );
            MaterialServiceType bizType = new MaterialServiceType();
            bizType.setId(id);
            bizType.setServiceTypeCode(code);
            BIZ_TYPE_MAP.put(code,bizType);
            return code;
        });
    }

    /**
     * 根据ID获取业务类型
     * @param bizType 业务类型编码
     * @return 业务类型
     */
    @Override
    public MaterialServiceType getBizType(String bizType){
        //获取业务类型
        return BIZ_TYPE_MAP.computeIfAbsent(bizType,k->Dbs.load(
                MaterialServiceType.class,
                "VALID=1 AND SERVICE_TYPE_CODE=?",
                bizType
        ));
    }


    @Override
    public MaterialStorageType getStorageType(String storageTypeCode) {
        return STORAGE_TYPE_MAP.computeIfAbsent(storageTypeCode,k->Dbs.load(
                MaterialStorageType.class,
                "VALID=1 AND REASON_CODE=?",
                storageTypeCode
        ));
    }


}
