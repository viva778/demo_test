package com.supcon.orchid.material.superwms.controllers;


import com.supcon.orchid.material.superwms.services.ModelEntityViewService;
import com.supcon.orchid.material.superwms.services.ModelScreenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModelScreenViewController {

    @Autowired
    private ModelScreenService screenService;

    @GetMapping(value = "/public/material/foreign/stockStatistics", produces = "application/json")
    public String stockStatistics(){
        return screenService.stockStatistics();
    }
}
