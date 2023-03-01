package com.supcon.orchid.material.superwms.entities.dto;

import lombok.Data;

import java.util.List;

/**
 * 拆包打印
 */
@Data
public class UnpackPrintDTO {
    private PrinterDTO printer;

    private Long stockId;

    private List<UnpackDetailDTO> details;
}
