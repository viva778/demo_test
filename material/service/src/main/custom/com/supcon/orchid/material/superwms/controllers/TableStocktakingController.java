package com.supcon.orchid.material.superwms.controllers;

import com.supcon.orchid.material.entities.MaterialStDistribution;
import com.supcon.orchid.material.entities.MaterialStjStockRecord;
import com.supcon.orchid.fooramework.services.PlatformLoginService;
import com.supcon.orchid.material.superwms.services.TableStocktakingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TableStocktakingController {

    @Autowired
    private TableStocktakingService stocktakingService;

    @PostMapping(value = "/material/stocktakingJob/confirmResult")
    public String confirmResult(@RequestParam Long recordId){
        stocktakingService.confirmResult(recordId);
        return "success";
    }

    @PostMapping(value = "/material/stocktaking/quickEnd")
    public String quickEnd(@RequestParam Long stocktakingId){
        stocktakingService.quickEnd(stocktakingId);
        return "success";
    }

    @PostMapping(value = "/material/stocktaking/saveStockRecord")
    public String saveStockRecord(@RequestBody List<MaterialStjStockRecord> stockRecords){
        stocktakingService.saveStockRecord(stockRecords);
        return "success";
    }

    @GetMapping(value = "/material/stocktaking/getUndoneTargetCodesAndSubmitIfDone")
    public List<String> getUndoneTargetCodesAndSubmitIfDone(@RequestParam Long stocktakingJobId, @RequestParam Long staffId){
        return stocktakingService.getUndoneTargetCodesAndSubmitIfDone(stocktakingJobId, staffId);
    }

    @PostMapping(value = "/material/stocktaking/generateRecheckTask")
    public String generateRecheckTask(@RequestBody List<MaterialStDistribution> distributions,@RequestParam Long stocktakingId){
        stocktakingService.generateRecheckTask(distributions, stocktakingId);
        return "success";
    }

    @Autowired
    private PlatformLoginService loginService;
    @GetMapping(value = "/public/material/stocktaking/generateStocktakingByStrategy")
    public String generateStocktakingByStrategy(String staffCode){
        loginService.login(staffCode);
        stocktakingService.generateStocktakingByStrategy();
        return "success";
    }
}
