package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.ec.entities.abstracts.AbstractAuditCidEntity;
import lombok.SneakyThrows;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ModelEntityViewService {

    List<? extends AbstractAuditCidEntity> findEntityListByIds(String moduleName, String entityName, String includes, List<Long> ids);

    @SneakyThrows
    @Transactional
    List<? extends AbstractAuditCidEntity> findEntityListByConditions(String moduleName, String entityName, String includes, List<String> conditions);
}
