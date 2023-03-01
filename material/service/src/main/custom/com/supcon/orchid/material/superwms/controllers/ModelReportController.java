package com.supcon.orchid.material.superwms.controllers;

import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.Dates;
import com.supcon.orchid.material.superwms.services.ModelReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

//报表相关
@RestController
public class ModelReportController {

    @Autowired
    private ModelReportService reportService;

    //生成库存日结算
    @GetMapping(value = "/public/material/daySettlement/daySettlement/updateDailySettlement", produces = "application/json")
    public void doStockReport() {
        Date today = new Date();
        Pair<Date, Date> dayRange = Dates.getDayRange(today);
        reportService.doStockReport(dayRange, today);
    }

    //生成库存日结算
    @PostMapping(value = "/public/material/daySettlement/daySettlement/updateDailySettlement/test", produces = "application/json")
    public void doStockReportTest(@RequestBody Map<String, String> params) throws ParseException {
        String beginTime = params.get("beginTime");
        String endTime = params.get("endTime");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Pair<Date, Date> dayRange = Pair.of(sdf.parse(beginTime), sdf.parse(endTime));
        reportService.doStockReport(dayRange, sdf.parse(beginTime));
    }
}
