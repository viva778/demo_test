package com.supcon.orchid.material.superwms.services.impl;


import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetSupplierMater;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.material.superwms.services.TablePurArrivalViewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TablePurArrivalViewServiceImpl implements TablePurArrivalViewService {

    @Override
    @Transactional
   public List<String> findWareByVendor(Long vendor){
        List<BaseSetSupplierMater> supMaterList=Dbs.findByCondition(
                BaseSetSupplierMater.class,
                "VALID=1 AND COOPERATOR=? AND SUPPLIER_STATE=?",
                vendor,"BaseSet_supplierState/valid"
        );
        return supMaterList.stream().map(BaseSetSupplierMater::getMaterial)
                .map(BaseSetMaterial::getId).map(String::valueOf).collect(Collectors.toList());
    }

}
