package com.supcon.orchid.material.superwms.services;

public interface ModelPermissionService {
    boolean isViewCodeFromModel(String viewCode, String modelCode);

    String getFilterSqlWarehouseCondition(String modelCode, String warehouseColumn);

    String getFilterSqlStockCondition(String modelCode, String stockColumn);
}
