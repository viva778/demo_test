package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.material.entities.MaterialWasteInDetail;

import java.util.List;

public interface TableInboundViewService {
    List<MaterialWasteInDetail> wasteCreateDetailsByWarehouseAndMaterials(Long warehouseId, List<Long> materialIds);

    List<MaterialWasteInDetail> wasteRefreshStockByWarehouse(Long warehouseId, List<Long> materialIds);
}
