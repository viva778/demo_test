package com.supcon.orchid.material.superwms.entities.dto;

import lombok.Data;

import java.util.Map;

@Data
public class PrinterPrintDTO {
    private String clientIp;

    private String printerName;

    private String templateName;

    private int count = 1;

    private String fileName;

    private Map<String,Object> parameterMap;

}
