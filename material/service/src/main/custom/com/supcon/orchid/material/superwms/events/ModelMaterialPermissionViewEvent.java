package com.supcon.orchid.material.superwms.events;

import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.material.superwms.services.ModelPermissionService;
import com.supcon.orchid.services.QueryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class ModelMaterialPermissionViewEvent {

    @Autowired
    private ModelPermissionService permissionService;

    @Signal("OtherInCustomCondition")
    private String otherInCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialOtherInSingle.MODEL_CODE, "WARE");
    }

    @Signal("ProduceInCustomCondition")
    private String produceInCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialProduceInSingl.MODEL_CODE, "WARE");
    }

    @Signal("WasteInCustomCondition")
    private String wasteInCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialWasteInSingle.MODEL_CODE, "WAREHOURSE");
    }

    @Signal("PurchaseInCustomCondition")
    private String purchaseInCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialPurchInSingle.MODEL_CODE, "WARE_ID");
    }

    @Signal("SaleReturnInCustomCondition")
    private String saleReturnCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialSaleReturn.MODEL_CODE, "WARE");
    }

    @Signal("ProduceReturnCustomCondition")
    private String produceReturnCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialProdReturn.MODEL_CODE, "WAREHOUSE");
    }

    @Signal("OtherOutCustomCondition")
    private String otherOutCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialOtherOutSingle.MODEL_CODE, "WARE");
    }

    @Signal("SaleOutCustomCondition")
    private String saleOutCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialSaleOutSingle.MODEL_CODE, "WARE");
    }

    @Signal("WasteOutCustomCondition")
    private String wasteOutCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialWasteOutSingle.MODEL_CODE, "WARE");
    }

    @Signal("ProduceOutCustomCondition")
    private String produceOutCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialProduceOutSing.MODEL_CODE, "WARE");
    }

    @Signal("AllocationCustomCondition")
    private String allocationCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialAppropriation.MODEL_CODE, "FROM_WARE");
    }

    @Signal("PlaceAdjustCustomCondition")
    private String placeAdjustCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialPlaceAjustInfo.MODEL_CODE, "WARE");
    }

    @Signal("MixBatchCustomCondition")
    private String mixBatchCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialMixBatchSingle.MODEL_CODE, "WARE");
    }

    @Signal("StocktakingCustomCondition")
    private String stocktakingCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialStocktaking.MODEL_CODE, "WAREHOUSE");
    }

    @Signal("BusinessDetailCustomCondition")
    private String bizDetailCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialBusinessDetail.MODEL_CODE, "WARE");
    }

    @Signal("StockCustomCondition")
    private String stockCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialStandingcrop.MODEL_CODE, "WARE");
    }

    @Signal("StockSummaryCustomCondition")
    private String stockSummaryCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialCropGather.MODEL_CODE, "WARE");
    }

    @Signal("DaySettlementCustomCondition")
    private String daySettlementCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialDaySettlement.MODEL_CODE, "WARE");
    }

    @Signal("StockTurnoversCustomCondition")
    private String stockTurnoversCustomCondition(String viewCode) {
        return _getWarehouseCondition(viewCode, MaterialStockTurnovers.MODEL_CODE, "WARE");
    }

    @Signal("FreezeDetailCustomCondition")
    private String freezeDetailCustomCondition(String viewCode) {
        return _getStockCondition(viewCode, MaterialFrozenPart.MODEL_CODE, "ONHAND");
    }

    private String _getStockCondition(String viewCode, String modelCode, String stockColumn) {
        if (permissionService.isViewCodeFromModel(viewCode, modelCode)) {
            return permissionService.getFilterSqlStockCondition(modelCode, stockColumn);
        }
        return null;
    }

    private String _getWarehouseCondition(String viewCode, String modelCode, String warehouseColumn) {
        if (permissionService.isViewCodeFromModel(viewCode, modelCode)) {
            return permissionService.getFilterSqlWarehouseCondition(modelCode, warehouseColumn);
        }
        return null;
    }

    @Signal("DisposalUnitRefQueryCond")
    private void DisposalUnitRefQueryCond(QueryEntity queryEntity) {
        // 处置单位参照，默认查询合同期内的
        String fastQueryCond = queryEntity.getFastQueryCond();
        if (fastQueryCond != null && fastQueryCond.length() > 0) {
            int index = fastQueryCond.indexOf("[", fastQueryCond.indexOf("subconds"));
            if (index > -1) {
                String nowDateFormat = new SimpleDateFormat("yyy-MM-dd").format(new Date());
                String startDateCond = "{\"type\":\"0\",\"columnName\":\"START_DATE\",\"dbColumnType\":\"DATE\",\"operator\":\"<=\",\"paramStr\":\"?\",\"value\":\"" + nowDateFormat + " 23:59:59\"}";
                String endDateCond = "{\"type\":\"0\",\"columnName\":\"END_DATE\",\"dbColumnType\":\"DATE\",\"operator\":\">=\",\"paramStr\":\"?\",\"value\":\"" + nowDateFormat + " 00:00:00\"}";

                queryEntity.setFastQueryCond(fastQueryCond.substring(0, index + 1)
                        + startDateCond + ","
                        + endDateCond
                        + fastQueryCond.substring(index + 1));
            }
        }
    }

}
