package com.supcon.orchid.fooramework.services.impl;

import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Entities;
import com.supcon.orchid.fooramework.util.RequestCaches;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.fooramework.services.PlatformSystemCodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformSystemCodeServiceImpl implements PlatformSystemCodeService {

    @Override
    @Transactional
    public SystemCode get(String id){
        return RequestCaches.computeIfAbsent(id, k-> {
            SystemCode systemCode = Dbs.load(SystemCode.class,id);
            Entities.translateSystemCode(systemCode);
            return systemCode;
        });
    }
}
