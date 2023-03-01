package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.material.superwms.entities.dto.UnpackDetailDTO;
import com.supcon.orchid.material.superwms.entities.dto.UnpackPrintDTO;

import java.util.List;

public interface ModelPrintService {

    List<UnpackDetailDTO> splitBatchAndPrint(UnpackPrintDTO unpackPrintDTO);

    List<UnpackDetailDTO> unpackStockAndPrint(UnpackPrintDTO unpackPrintDTO);
}
