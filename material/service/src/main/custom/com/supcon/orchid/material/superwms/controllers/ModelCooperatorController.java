package com.supcon.orchid.material.superwms.controllers;

import com.supcon.orchid.BaseSet.entities.BaseSetSupplierMater;
import com.supcon.orchid.fooramework.util.Dbs;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ModelCooperatorController {

    @GetMapping(value = "/public/material/otherInSingle/cooperator")
    public List<BaseSetSupplierMater> getMaterialsUnderCooperator(@RequestParam("id") Long id){
        return Dbs.findByCondition(
                BaseSetSupplierMater.class
                ,"VALID=1 AND COOPERATOR=?"
                ,id
        );
    }
}
