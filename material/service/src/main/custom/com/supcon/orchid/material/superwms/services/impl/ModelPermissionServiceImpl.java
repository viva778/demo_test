package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.BaseSet.entities.BaseSetWareRole;
import com.supcon.orchid.BaseSet.entities.BaseSetWareUser;
import com.supcon.orchid.material.entities.MaterialStandingcrop;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Organazations;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.material.superwms.services.ModelPermissionService;
import com.supcon.orchid.fooramework.services.PlatformEcAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModelPermissionServiceImpl implements ModelPermissionService {

    @Autowired
    private PlatformEcAccessService ecAccessService;

    @Override
    public boolean isViewCodeFromModel(String viewCode, String modelCode){
        return modelCode.equals(ecAccessService.getModelCodeByViewCode(viewCode));
    }

    @Override
    public String getFilterSqlWarehouseCondition(String modelCode, String warehouseColumn){
        Long userId = Organazations.getCurrentUserId();
        Assert.notNull(userId,"登陆人信息缺失");
        List<Long> warehouseIds = findPermittedWarehouse(userId);
        if(warehouseIds.size()>0){
            String tableAlias = Strings.lowerCaseFirst(modelCode.substring(modelCode.lastIndexOf("_")+1));
            String columnAlias = "\""+tableAlias+"\"."+warehouseColumn;
            return "("+columnAlias+" IN("+ Strings.join(",",warehouseIds)+"))";
        }
        return null;
    }

    @Override
    public String getFilterSqlStockCondition(String modelCode, String stockColumn){
        Long userId = Organazations.getCurrentUserId();
        Assert.notNull(userId,"登陆人信息缺失");
        List<Long> warehouseIds = findPermittedWarehouse(userId);
        if(warehouseIds.size()>0){
            String tableAlias = Strings.lowerCaseFirst(modelCode.substring(modelCode.lastIndexOf("_")+1));
            String columnAlias = "\""+tableAlias+"\"."+stockColumn;
            String sqlSelectStock = "SELECT ID FROM "+ MaterialStandingcrop.TABLE_NAME+" WHERE WARE IN("+ Strings.join(",",warehouseIds)+")";
            return "("+columnAlias+" IN("+ sqlSelectStock+"))";
        }
        return null;
    }

    public List<Long> findPermittedWarehouse(Long userId){
        String sqlSelectWarehouseByRole = "SELECT WAREHOUSE FROM "+ BaseSetWareRole.TABLE_NAME +" WHERE VALID=1 AND WARE_ROLE IN (SELECT ROLE_ID FROM BASE_ROLEUSER WHERE USER_ID=? AND VALID=1)";
        String sqlSelectWarehouseByUser = "SELECT WAREHOUSE FROM "+ BaseSetWareUser.TABLE_NAME +" WHERE VALID=1 AND  WARE_USER=?";
        String sqlSelectWarehouseByUserAndRole = "("+sqlSelectWarehouseByRole+") UNION ("+sqlSelectWarehouseByUser+")";
        return Dbs.stream(
                sqlSelectWarehouseByUserAndRole,
                Long.class,
                userId,userId
        ).distinct().collect(Collectors.toList());
    }
}
