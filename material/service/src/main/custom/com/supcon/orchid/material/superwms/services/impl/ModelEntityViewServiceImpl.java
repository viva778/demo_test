package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.ec.entities.abstracts.AbstractAuditCidEntity;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Hbs;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.material.superwms.services.ModelEntityViewService;
import com.supcon.orchid.material.superwms.util.CriterionParser;
import lombok.SneakyThrows;
import org.hibernate.criterion.Criterion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.data.util.CastUtils.cast;

@Service
public class ModelEntityViewServiceImpl implements ModelEntityViewService {

    @SneakyThrows
    @Override
    @Transactional
    public List<? extends AbstractAuditCidEntity> findEntityListByIds(String moduleName, String entityName, String includes, List<Long> ids){
        Class<? extends AbstractAuditCidEntity> clazz = cast(Class.forName("com.supcon.orchid."+moduleName+".entities."+entityName));
        return Strings.valid(includes)
                ? Hbs.findByIdsWithIncludes(clazz,includes,ids)
                : Dbs.findByCondition(clazz,Dbs.inCondition("ID",ids.size()),ids);
    }

    @SneakyThrows
    @Transactional
    @Override
    public List<? extends AbstractAuditCidEntity> findEntityListByConditions(String moduleName, String entityName, String includes, List<String> conditions){
        Class<? extends AbstractAuditCidEntity> clazz = cast(Class.forName("com.supcon.orchid."+moduleName+".entities."+entityName));
        Criterion[] criteria = conditions.stream().map(condition-> CriterionParser.getCriterion(condition,clazz)).toArray(Criterion[]::new);
        return Strings.valid(includes)
                ? Hbs.findByCriteriaWithIncludes(clazz,includes,criteria)
                : Hbs.findByCriteria(clazz,criteria);
    }
}
