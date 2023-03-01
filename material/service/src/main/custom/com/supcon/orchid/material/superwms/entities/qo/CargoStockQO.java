package com.supcon.orchid.material.superwms.entities.qo;

import lombok.Data;

@Data
public class CargoStockQO {

    private Long warehouseId;

    private Long placeId;

    private Long materialId;

    private String batchNum;
}
