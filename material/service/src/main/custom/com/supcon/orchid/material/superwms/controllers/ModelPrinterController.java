package com.supcon.orchid.material.superwms.controllers;

import com.supcon.orchid.material.superwms.entities.dto.UnpackDetailDTO;
import com.supcon.orchid.material.superwms.entities.dto.UnpackPrintDTO;
import com.supcon.orchid.material.superwms.services.ModelPrintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 打印相关接口
 *  由于之前一部分都是在前端调用的，事务处理会比较麻烦
 *  这一版是在6.2.0.8之后增加的，可以把之前的打印都转成后端调用
 */
@RestController
public class ModelPrinterController {

    @Autowired
    private ModelPrintService printService;

    /**
     * 拆包并打印
     */
    @PostMapping(value = "/material/printer/unpackStockAndPrint")
    public List<UnpackDetailDTO> unpackStockAndPrint(@RequestBody UnpackPrintDTO dto){
        return printService.unpackStockAndPrint(dto);
    }

    /**
     * 拆包并打印
     */
    @PostMapping(value = "/material/printer/splitBatchAndPrint")
    public List<UnpackDetailDTO> splitBatchAndPrint(@RequestBody UnpackPrintDTO dto){
        return printService.splitBatchAndPrint(dto);
    }
}
