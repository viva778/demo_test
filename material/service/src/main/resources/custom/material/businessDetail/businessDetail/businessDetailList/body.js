const BizContext = {
    // 货位调整
    placeAjust: {
        operateCode: "material_1.0.0_placeAjust_placeAdjustList_self",
        url: "/msService/material/placeAjust/placeAjustInfo/placeAdjustView",
        entityCode: "material_1.0.0_placeAjust"
    },
    // 销售出库
    saleOut: {
        operateCode: "material_1.0.0_saleOut_saleOutList_self",
        url: "/msService/material/saleOut/saleOutSingle/saleOutView",
        entityCode: "material_1.0.0_saleOut"
    },
    // 采购入库
    purchaseInSingles: {
        operateCode: "material_1.0.0_purchaseInSingles_purchaseInSingleList_self",
        url: "/msService/material/purchaseInSingles/purchInSingle/purchaseInsingleView",
        entityCode: "material_1.0.0_purchaseInSingles"
    },
    // 生产入库
    produceInSingles: {
        operateCode: "material_1.0.0_produceInSingles_productInSingleList_self",
        url: "/msService/material/produceInSingles/produceInSingl/productInSingleView",
        entityCode: "material_1.0.0_produceInSingles"
    },
    // 生产退料
    produceBackSingles: {
        operateCode: "material_1.0.0_produceBackSingles_productBackList_self",
        url: "/msService/material/produceBackSingles/prodReturn/productBackView",
        entityCode: "material_1.0.0_produceBackSingles"
    },
    // 其他出库
    otherOutSingle: {
        operateCode: "material_1.0.0_otherOutSingle_otherOutList_self",
        url: "/msService/material/otherOutSingle/otherOutSingle/otherOutView",
        entityCode: "material_1.0.0_otherOutSingle"
    },
    // 生产出库
    produceOutSingle: {
        operateCode: "material_1.0.0_produceOutSingle_produceOutSingleList_self",
        url: "/msService/material/produceOutSingle/produceOutSing/produceOutSingleView",
        entityCode: "material_1.0.0_produceOutSingle"
    },
    // 其他入库单
    otherInSingle: {
        operateCode: "material_1.0.0_otherInSingle_inSingleList_self",
        url: "/msService/material/otherInSingle/otherInSingle/inSingleview",
        entityCode: "material_1.0.0_otherInSingle"
    },
    // 混批单
    mixtureBatchSingles: {
        operateCode: "material_1.0.0_mixtureBatchSingles_mixtureBatchList_self",
        url: "/msService/material/mixtureBatchSingles/mixBatchSingle/mixtureBatchView",
        entityCode: "material_1.0.0_mixtureBatchSingles"
    },
    // 调拨单
    appropriation: {
        operateCode: "material_1.0.0_appropriation_appList_self",
        url: "/msService/material/appropriation/appropriation/appView",
        entityCode: "material_1.0.0_appropriation"
    },
    // 销售退货
    saleReturn: {
        operateCode: "material_1.0.0_saleReturn_saleReturnList_self",
        url: "/msService/material/saleReturn/saleReturn/saleReturnView",
        entityCode: "material_1.0.0_saleReturn"
    },
    // 采购退货
    returnedPurchase: {
        operateCode: "material_1.0.0_purchaseReturn_purReturnList_self",
        url: "/msService/material/purchaseReturn/purReturn/purReturnView",
        entityCode: "material_1.0.0_purchaseReturn"
    },
    // 盘点单
    inventory: {
        operateCode: "material_1.0.0_inventory_inventoryList_self",
        url: "/msService/material/inventory/inventory/inventoryView",
        entityCode: "material_1.0.0_inventory"
    },
    // 废料入库单
    wasteInSingle: {
        operateCode: "material_1.0.0_wasteInSingle_hazardousInList_self",
        url: "/msService/material/wasteInSingle/wasteInSingle/hazardousInViews",
        entityCode: "material_1.0.0_wasteInSingle"
    },
    // 废料出库单
    wasteOutSingle: {
        operateCode: "material_1.0.0_wasteOutSingle_garbageOutList",
        url: "/msService/material/wasteOutSingle/wasteOutSingle/garbageOutView",
        entityCode: "material_1.0.0_wasteOutSingle"
    },
    // 原料调拨入库单
    MatTransIn: {
        operateCode: "wipmw_6.1.3.2_matTransIn_matTransInList",
        url: "/msService/wipmw/matTransIn/matTransIn/matTransInView",
        entityCode: "wipmw_6.1.3.2_matTransIn"
    },
    // 生产退料出库单
    MatRetOut: {
        operateCode: "wipmw_6.1.3.2_matRetOut_matRtnOutList",
        url: "/msService/wipmw/matRetOut/matRetOut/matRtnOutView",
        entityCode: "wipmw_6.1.3.2_matRetOut"
    },
    // 备件退库
    spareBack: {
        operateCode: "SpareManage_1.0.0_spareBack_spareBackList",
        url: "/msService/SpareManage/spareBack/spWarehousing/spareBackView",
        entityCode: "SpareManage_1.0.0_spareBack"
    },
    // 备件入库
    spareStorage: {
        operateCode: "SpareManage_1.0.0_spareStorage_spareStorageList",
        url: "/msService/SpareManage/spareStorage/purchInSingle/spareStorageView",
        entityCode: "SpareManage_1.0.0_spareStorage"
    },
    // 备件领用
    spareUse: {
        operateCode: "SpareManage_1.0.0_spareUse_spareUseList",
        url: "/msService/SpareManage/spareStorage/purchInSingle/spareStorageView",
        entityCode: "SpareManage_1.0.0_spareUse"
    },
}



function hyperlink(nRow) {
    var tableNo = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_businessDetail_businessDetailList_businessDetail_sdg").getValueByKey(nRow, "billCode");
    var bizType = tableNo.substr(0, tableNo.indexOf("_"));
    var bizContext = BizContext[bizType];
    if (bizContext) {
        var id = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_businessDetail_businessDetailList_businessDetail_sdg").getValueByKey(nRow, "tableHeadID");
        var tableInfoId = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_businessDetail_businessDetailList_businessDetail_sdg").getValueByKey(nRow, "billInfoId"); //billInfoId
        var pc = ReactAPI.getPowerCode(bizContext.operateCode)[bizContext.operateCode];
        var url = bizContext.url + "?" + [
            "tableInfoId=" + tableInfoId,
            "entityCode=" + bizContext.entityCode,
            "id=" + id,
            "__pc__=" + pc
        ].join("&");
        window.open(url);
    }
}