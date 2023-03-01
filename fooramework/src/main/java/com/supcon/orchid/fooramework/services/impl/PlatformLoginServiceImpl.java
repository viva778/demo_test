package com.supcon.orchid.fooramework.services.impl;

import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.foundation.entities.Company;
import com.supcon.orchid.foundation.internal.services.SimulatedLoginService;
import com.supcon.orchid.fooramework.services.PlatformLoginService;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Optional;

@Service
public class PlatformLoginServiceImpl implements PlatformLoginService {
    @Autowired
    private SimulatedLoginService simulatedLoginService;

    public boolean isLoggedIn(){
        return UserContext.getUserContext()!=null&&UserContext.getUserContext().getUserId()!=null;
    }

    @Transactional
    @Override
    public void login(String username, String companyCode){
        if(!isLoggedIn()){
            Assert.isTrue(Dbs.exist(
                    "AUTH_USER",
                    "USER_NAME=?",
                    username
            ),"找不到对应用户！");
            Long cid = Dbs.first("SELECT ID FROM "+ Company.TABLE_NAME+" WHERE VALID=1 AND CODE=?",Long.class,companyCode);
            Assert.notNull(cid,"找不到编码对应的公司！");
            simulatedLoginService.login(username,cid);
        }
    }

    @Transactional
    @Override
    public void login(String personCode){
        if(!isLoggedIn()){
            Pair<String,Long> pair = personCode!=null?Dbs.pair(
                    "SELECT USER_NAME,COMPANY_ID FROM AUTH_USER WHERE VALID=1 AND PERSON_CODE = ?",
                    String.class,Long.class,
                    personCode
            ):null;
            String username = Optional.ofNullable(pair).map(Pair::getFirst).orElse("admin");
            Long cid = Optional.ofNullable(pair).map(Pair::getSecond).orElse(1000L);
            simulatedLoginService.login(username,cid);
        }
    }
}
