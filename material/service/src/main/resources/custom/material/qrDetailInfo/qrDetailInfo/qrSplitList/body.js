//------------
//======================================================================================
//自定义代码
//======================================================================================

/**
 * 拆包保存按钮
 * @author yaoyao
 * @changeLog
 *  1. 新增 2022-03-25 by yaoyao
 *  2. 修改，增加打印功能 2022-04-09 by yaoyao
 */
function saveQrSplitInfo(event) {
    var searchBtn = ReactAPI.getComponentAPI("SearchPanel").APIs(
        "material_1.0.0_qrDetailInfo_qrSplitList_qrDetailInfo_sp"
    );
    var tsequenceCode = event.ReactAPI.getComponentAPI("Input")
        .APIs("qrDetailInfo.sequenceCode")
        .getValue();
    var tsplitLeftNum = event.ReactAPI.getComponentAPI("InputNumber")
        .APIs("qrDetailInfo.splitLeftNum")
        .getValue();
    var allQty = event.ReactAPI.getComponentAPI("InputNumber")
        .APIs("qrDetailInfo.availableQty")
        .getValue();

    var gridTable = event.ReactAPI.getComponentAPI("SupDataGrid").APIs(
        "material_1.0.0_qrDetailInfo_qrSPlitEditdg1647748494109"
    );
    var tableData = gridTable.getDatagridData();
    var tableLength = tableData.length;
    var rowArray = [];
    var checkNum = 0;

    for (let i = 0; i < tableLength; i++) {
        var tempRow = {};

        if (!tableData[i].newQuantity || tableData[i].newQuantity <= 0) {
            event.ReactAPI.showMessage("w", "数量需要是个大于0的值");
            return false;
        }

        tempRow["materialCode"] = tableData[i].materialCode;
        tempRow["materialId"] = tableData[i].materialId;
        tempRow["newQuantity"] = tableData[i].newQuantity;
        tempRow["batchText"] = tableData[i].batchText;

        if (tableData[i].cargoPlace) {
            tempRow["cargoPlaceCode"] = tableData[i].cargoPlace.code;
        }

        checkNum += tableData[i].newQuantity;

        rowArray.push(tempRow);
    }

    if (checkNum > allQty) {
        event.ReactAPI.showMessage("w", "拆后的总数量大于原来的可用量！");
        return false;
    }

    var printerInfo = event.ReactAPI.getComponentAPI("Reference")
        .APIs("qrDetailInfo.qrPrinter.printName")
        .getValue()[0];

    if (!printerInfo) {
        event.ReactAPI.showMessage("w", "请先选择打印机！");
        return false;
    }

    var templateInfo = event.ReactAPI.getComponentAPI("Reference")
        .APIs("qrDetailInfo.qrPrinterTemp.name")
        .getValue()[0];

    if (!templateInfo) {
        event.ReactAPI.showMessage("w", "请先选择打印模板！");
        return false;
    }

    var result = ReactAPI.request({
        url: "/msService/material/qrSplitEdit/dealQrSplitInfoWithIds",
        //url: "/msService/material/qrSplitEdit/dealQrSplitInfo",
        type: "POST",
        data: JSON.stringify({
            detailDTOs: rowArray,
            srcDTO: {
                sequenceCode: tsequenceCode,
                splitLeftNum: tsplitLeftNum,
            },
        }),
        async: false,
    });

    if (result.code == 200) {
        event.ReactAPI.openLoading("打印中,请稍后！");
        //为了显示效果好些，使用异步延时1s来完成打印操作
        setTimeout(function () {
            var printIds = result.data;
            var printFlag = true;
            for (var i = 0; i < printIds.length; i++) {
                printFlag = printQrInfo(event, printIds[i], printerInfo, templateInfo);
            }
            searchBtn.updateSearch();
            if (printFlag == true || printFlag == undefined) {
                ReactAPI.destroyDialog("qrSplitEdit");
            } else {
                //打印失败！请查看打印服务
                event.ReactAPI.closeLoading();
                event.ReactAPI.showMessage("w", "打印失败！请检查打印服务!");
            }
        }, 1000);

    } else {
        event.ReactAPI.showMessage("w", result.message || "保存失败，请联系管理员处理！");
        return false;
    }
}

/**
 * 打印功能
 * @author yaoyao
 * @changeLog
 *  1. 新增 2022-04-09 by yaoyao
 **/
function printQrInfo(event, qrId, printerInfo, templateInfo) {
    var templateName = templateInfo.name;
    var clientIp = printerInfo.clientIp;
    var count = 1;
    var printerName = printerInfo.printName;
    var fileName = "wms" + new Date().getTime() + ".pdf";
    var params = {};
    params["pk"] = qrId;

    var result = ReactAPI.request({
        url: "/msService/pdf-generator/generateAndPrint",
        type: "post",
        async: false,
        dataType: "json",
        contentType: "application/json",
        data: {
            fileName: fileName,
            clientIp: clientIp,
            count: count,
            printerName: printerName,
            templateName: templateName,
            parameterMap: params,
        },
    });
    var data = result.responseText;
    
    if ("SUCCESS" == data) {
    } else {
        //失败时,将生成的条码台帐清除并回滚数据
        ReactAPI.request({
            url: "/msService/material/qrSplitEdit/printErrorRollback?qrId=" + qrId,
            type: "post",
            async: false
        });
        return false;
    }
}

/**
 * 打开条码拆分（拆包）界面
 * @author yaoyao
 * @changeLog
 *  1. 新增 2022-03-25 by yaoyao
 **/
function openSplitEdit() {
    var dgTable = ReactAPI.getComponentAPI("SupDataGrid").APIs(
        "material_1.0.0_qrDetailInfo_qrSplitList_qrDetailInfo_sdg"
    );
    var selectedRows = dgTable.getSelecteds();

    if (selectedRows.length == 0) {
        ReactAPI.showMessage("w", "请至少选中一行!");
        return false;
    }

    var selectedRowData = selectedRows[0];

    var qrId = selectedRowData.id;
    var batchText = selectedRowData.batchCode.batchNum;
    var sequenceCode = selectedRowData.sequenceCode;
    var materialId = selectedRowData.material.id;
    var materialCode = selectedRowData.material.code;
    var materialName = selectedRowData.material.name;
    var warehouseId = selectedRowData.warehouse.id;
    var warehouseCode = selectedRowData.warehouse.code;
    var warehouseName = selectedRowData.warehouse.name;
    var avlQty = selectedRowData.availableQty;

    ReactAPI.createDialog("qrSplitEdit", {
        title: "拆包",
        url:
            "/msService/material/qrDetailInfo/qrDetailInfo/qrSPlitEdit?" +
            encodeURI([
                "batchText=" + batchText,
                "sequenceCode=" + sequenceCode,
                "materialId=" + materialId,
                "materialCode=" + materialCode,
                "materialName=" + materialName,
                "warehouseId=" + warehouseId,
                "warehouseName=" + warehouseName,
                "availableQty=" + avlQty,
                "qrId=" + qrId
            ].join("&"))
        ,
        isRef: false,
        width: "800px",
        height: "600px",
        buttons: [
            {
                text: "保存并打印",
                style: { width: "120px" },
                type: "primary",
                onClick: function (event) {
                    saveQrSplitInfo(event);
                },
            },
            {
                text: "取消",
                type: "cancel",
                onClick: function (event) {
                    ReactAPI.destroyDialog("qrSplitEdit");
                },
            },
        ],
    });
}