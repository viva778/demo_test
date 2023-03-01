package com.supcon.orchid.material.superwms.controllers;

import com.alibaba.fastjson.JSON;
import com.supcon.orchid.material.superwms.services.ForeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ForeignController {

    @Autowired
    ForeignService foreignService;

    @PostMapping(value = "/public/material/standingCrop/standingCrop/syncStandingCrop")
    public Map<String, String> syncStandingCrop(@RequestBody String jsonParams) {
        Map<String, Object> params = JSON.parseObject(jsonParams);
        return foreignService.syncStandingCrop(params);
    }


}
