package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.material.entities.MaterialServiceType;
import com.supcon.orchid.material.entities.MaterialStorageType;

public interface ModelBizTypeService {

    /**
     * 根据ID获取业务类型编码
     * @param id 业务类型ID
     * @return 业务类型编码
     */
    String getBizType(Long id);

    /**
     * 根据ID获取业务类型
     * @param bizType 业务类型编码
     * @return 业务类型
     */
    MaterialServiceType getBizType(String bizType);


    MaterialStorageType getStorageType(String storageTypeCode);
}
