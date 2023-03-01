package com.supcon.orchid.material.superwms.controllers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.orchid.ec.entities.abstracts.AbstractAuditCidEntity;
import com.supcon.orchid.fooramework.util.Entities;
import com.supcon.orchid.fooramework.util.Jacksons;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.material.superwms.services.ModelEntityViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class ModelEntityViewController {

    @Autowired
    private ModelEntityViewService entityViewService;


    @GetMapping(value = "/material/entity/getEntityList", produces = "application/json")
    public String getEntityList(@RequestParam String moduleName,@RequestParam String entityName,Long[] ids, String[] conditions, String[] includes){
        String str_includes = (includes!=null&&includes.length>0)?String.join(",",includes):null;
        List<? extends AbstractAuditCidEntity> list = ids!=null
                ? entityViewService.findEntityListByIds(moduleName, entityName, str_includes, Arrays.asList(ids))
                : entityViewService.findEntityListByConditions(moduleName, entityName, str_includes, Arrays.asList(conditions));
        list.forEach(Entities::translateSystemCode);
        return Strings.valid(str_includes)
                ? Jacksons.writeValueWithIncludes(list,str_includes)
                : Jacksons.config().include(JsonInclude.Include.NON_NULL).writeValue(list);
    }
}
