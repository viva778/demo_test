function hyperlink(nRow) {
    var tableInfo = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_businessDetail_businessTracelList_businessDetail_sdg").getValueByKey(nRow, "billCode");
    var id = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_businessDetail_businessTracelList_businessDetail_sdg").getValueByKey(nRow, "tableHeadID");
    var tableInfoId = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_businessDetail_businessTracelList_businessDetail_sdg").getValueByKey(nRow, "billInfoId"); //billInfoId
    var operateCode;
    var pcMap;
    var pc;
    // 销售出库
    if (tableInfo.indexOf("saleOut") >= 0) {
        operateCode = "material_1.0.0_saleOut_saleOutList_self";
        pcMap = ReactAPI.getPowerCode(operateCode);
        pc = pcMap[operateCode];
        var url = "/msService/material/saleOut/saleOutSingle/saleOutView?tableInfoId=" + tableInfoId + "&entityCode=material_1.0.0_saleOut&id=" + id + "&__pc__=" + pc;
    } else if (tableInfo.indexOf("purchaseInSingles") >= 0) { // 采购入库

        operateCode = "material_1.0.0_purchaseInSingles_purchaseInSingleList_self";
        pcMap = ReactAPI.getPowerCode(operateCode);
        pc = pcMap[operateCode];
        var url = "/msService/material/purchaseInSingles/purchInSingle/purchaseInsingleView?tableInfoId=" + tableInfoId + "&entityCode=material_1.0.0_purchaseInSingles&id=" + id + "&__pc__=" + pc;

    } else if (tableInfo.indexOf("produceInSingles") >= 0) { // 生产入库
        operateCode = "material_1.0.0_produceInSingles_productInSingleList_self";
        pcMap = ReactAPI.getPowerCode(operateCode);
        pc = pcMap[operateCode];
        var url = "/msService/material/produceInSingles/produceInSingl/productInSingleView?tableInfoId=" + tableInfoId + "&entityCode=material_1.0.0_produceInSingles&id=" + id + "&__pc__=" + pc;

    } else if (tableInfo.indexOf("otherOutSingle") >= 0) { // 其他出库
        operateCode = "material_1.0.0_otherOutSingle_otherOutList_self";
        pcMap = ReactAPI.getPowerCode(operateCode);
        pc = pcMap[operateCode];
        var url = "/msService/material/otherOutSingle/otherOutSingle/otherOutView?tableInfoId=" + tableInfoId + "&entityCode=material_1.0.0_otherOutSingle&id=" + id + "&__pc__=" + pc;

    } else if (tableInfo.indexOf("placeAjust") >= 0) { // 货位调整

    } else if (tableInfo.indexOf("produceOutSingle") >= 0) { // 生产出库
        operateCode = "material_1.0.0_produceOutSingle_produceOutSingleList_self";
        pcMap = ReactAPI.getPowerCode(operateCode);
        pc = pcMap[operateCode];
        var url = "/msService/material/produceOutSingle/produceOutSing/produceOutSingleView?tableInfoId=" + tableInfoId + "&entityCode=material_1.0.0_produceOutSingle&id=" + id + "&__pc__=" + pc;

    } else if (tableInfo.indexOf("otherInSingle") >= 0) { // 其他入库单
        operateCode = "material_1.0.0_otherInSingle_inSingleList_self";
        pcMap = ReactAPI.getPowerCode(operateCode);
        pc = pcMap[operateCode];
        var url = "/msService/material/otherInSingle/otherInSingle/inSingleview?tableInfoId=" + tableInfoId + "&entityCode=material_1.0.0_otherInSingle&id=" + id + "&__pc__=" + pc;

    } else if (tableInfo.indexOf("mixtureBatchSingles") >= 0) { // 混批单
        operateCode = "material_1.0.0_mixtureBatchSingles_mixtureBatchList_self";
        pcMap = ReactAPI.getPowerCode(operateCode);
        pc = pcMap[operateCode];
        var url = "/msService/material/mixtureBatchSingles/mixBatchSingle/mixtureBatchView?tableInfoId=" + tableInfoId + "&entityCode=material_1.0.0_mixtureBatchSingles&id=" + id + "&__pc__=" + pc;  

    } else if (tableInfo.indexOf("appropriation") >= 0) { // 调拨单 	    
        operateCode = "material_1.0.0_appropriation_appList_self";
        pcMap = ReactAPI.getPowerCode(operateCode);
        pc = pcMap[operateCode];
        var url = "/msService/material/appropriation/appropriation/appView?tableInfoId=" + tableInfoId + "&entityCode=material_1.0.0_appropriation&id=" + id + "&__pc__=" + pc;

    } else if (tableInfo.indexOf("saleReturn") >= 0) { // 销售退货
        operateCode = "material_1.0.0_saleReturn_saleReturnList_self";
        pcMap = ReactAPI.getPowerCode(operateCode);
        pc = pcMap[operateCode];
        var url = "/msService/material/saleReturn/saleReturn/saleReturnView?tableInfoId=" + tableInfoId + "&entityCode=material_1.0.0_saleReturn&id=" + id + "&__pc__=" + pc;


    } else if (tableInfo.indexOf("returnedPurchase") >= 0) {//采购退货
        operateCode = "material_1.0.0_purchaseReturn_purReturnList_self";
        pcMap = ReactAPI.getPowerCode(operateCode);
        pc = pcMap[operateCode];
        var url = "/msService/material/purchaseReturn/purReturn/purReturnView?tableInfoId=" + tableInfoId + "&entityCode=material_1.0.0_purchaseReturn&id=" + id + "&__pc__=" + pc;
      
    } else if (tableInfo.indexOf("inventory") >= 0) {//盘点单
        operateCode = "material_1.0.0_inventory_inventoryList_self";
        pcMap = ReactAPI.getPowerCode(operateCode);
        pc = pcMap[operateCode];
        var url = "/msService/material/inventory/inventory/inventoryView?tableInfoId=" + tableInfoId + "&entityCode=material_1.0.0_inventory&id=" + id + "&__pc__=" + pc;
    } else if (tableInfo.indexOf("spareBack") >= 0) {//备件退库
        operateCode = "SpareManage_1.0.0_spareBack_spareBackList";
        pcMap = ReactAPI.getPowerCode(operateCode);
        pc = pcMap[operateCode];
        var url = "/msService/SpareManage/spareBack/spWarehousing/spareBackView?tableInfoId=" + tableInfoId + "&entityCode=SpareManage_1.0.0_spareBack&id=" + id + "&__pc__=" + pc;
    
    }else if (tableInfo.indexOf("spareStorage") >= 0) {//备件入库
        operateCode = "SpareManage_1.0.0_spareStorage_spareStorageList";
        pcMap = ReactAPI.getPowerCode(operateCode);
        pc = pcMap[operateCode];
        var url = "/msService/SpareManage/spareStorage/purchInSingle/spareStorageView?tableInfoId=" + tableInfoId + "&entityCode=SpareManage_1.0.0_spareStorage&id=" + id + "&__pc__=" + pc;
    
    }else if (tableInfo.indexOf("spareUse") >= 0) {//备件领用
        operateCode = "SpareManage_1.0.0_spareUse_spareUseList";
        pcMap = ReactAPI.getPowerCode(operateCode);
        pc = pcMap[operateCode];
        var url = "/msService/SpareManage/spareUse/useApply/spareUseView?tableInfoId=" + tableInfoId + "&entityCode=SpareManage_1.0.0_spareUse&id=" + id + "&__pc__=" + pc;
    
    }
  

    window.open(url);
}