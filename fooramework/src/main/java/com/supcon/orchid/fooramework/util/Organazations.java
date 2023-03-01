package com.supcon.orchid.fooramework.util;

import com.supcon.orchid.foundation.entities.*;
import com.supcon.orchid.foundation.internal.services.CompanyServiceImpl;
import com.supcon.orchid.orm.entities.IEntity;

import java.util.Optional;

public class Organazations {


    private static CompanyServiceImpl serviceInstance;

    private static CompanyServiceImpl getService(){
        if(serviceInstance ==null){
            serviceInstance = Springs.getBean(CompanyServiceImpl.class);
        }
        return serviceInstance;
    }

    public static User getCurrentUser(){
        return (User) getService().getCurrentUser();
    }

    public static Long getCurrentUserId(){
        User user = (User) getService().getCurrentUser();
        return user!=null?user.getId():null;
    }

    public static Company getCurrentCompany(){
        return (Company) getService().getCurrentCompany();
    }

    public static String getCurrentLanguage(){
        return getService().getCurrentLanguage();
    }

    public static Staff getCurrentStaff(){
        return (Staff) getService().getCurrentStaff();
    }

    public static Long getCurrentStaffId(){
        return Optional.ofNullable(getService().getCurrentStaff()).map(IEntity::getId).orElse(-1L);
    }

    public static Department getCurrentDepartment(){
        return (Department) getService().getCurrentDepartment();
    }

    public static Position getCurrentPosition(){
        return (Position) getService().getCurrentCompanyPosition();
    }

    public static Long getCurrentCompanyId(){
        return getService().getCurrentCompanyId();
    }

}
