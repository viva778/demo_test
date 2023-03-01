//======================================================================================
//自定义代码
//======================================================================================

function openOrCloseOrder(type) {
    debugger;
    var dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_purchaseInfos_purchasePartList_purchasePart_sdg");
    var selectedRow = dataGrid.getSelecteds();
    if (selectedRow.length == 0) {
        // 提示: 请选择一条记录进行操作
        ReactAPI.showMessage("w", ReactAPI.international.getText("SupDatagrid.button.error"));
        return;
    }

    var errorMsg = "";
    var partIds = "";
    for (let i = 0; i < selectedRow.length; i++) {
        const element = selectedRow[i];
        const arrivalState = element.arrivalState.id;

        if (type == 1) {
            // 开启订单
            if (arrivalState == 'material_arrivalState/all_arrived' || arrivalState == 'material_arrivalState/manual_closed') {
                // 拼接选中行表体ID
                var partId = element.id;
                partIds += "_" + partId;
            } else {
                // 第<b>{0}</b>行订单已开启，请勿重复操作！<br/>
                errorMsg += ReactAPI.international.getText("material.custom.purchaseInfo.orderAlreadyOpened", "" + (element.rowIndex + 1));
            }
        } else {
            // 关闭订单
            if (arrivalState == 'material_arrivalState/not_arrived' || arrivalState == 'material_arrivalState/partial_arrived' || arrivalState == 'material_arrivalState/manual_opened') {
                // 拼接选中行表体ID
                var partId = element.id;
                partIds += "_" + partId;
            } else {
                // 第<b>{0}</b>行订单已关闭，请勿重复操作！<br/>
                errorMsg += ReactAPI.international.getText("material.custom.purchaseInfo.orderAlreadyClosed", "" + (element.rowIndex + 1));
            }
        }
    }

    if (errorMsg.length > 0) {
        ReactAPI.showMessage("w", errorMsg);
        return;
    }

    var result = ReactAPI.request({
        type: "get",
        url: "/msService/material/purchaseInfos/purchaseInfo/manualOpenOrCloseOrder/" + partIds.substr(1) + "/" + type,
        async: false
    });

    if (result.code == 200) {
        ReactAPI.showMessage("s", ReactAPI.international.getText("material.custom.OperateSuccess"));

        ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_purchaseInfos_purchasePartList_purchasePart_sdg").refreshDataByRequst({
            type: "post",
            url: "/msService/material/purchaseInfos/purchasePart/purchasePartList-query",
            param: {}
        });
    } else {
        ReactAPI.showMessage("f", result.message);
    }
}


/**
 * 重新刷新单元格数据
 */
function refreshDataGrid() {
    // 获取请求 url 参数接口
    var urlParams = ReactAPI.getParamsInRequestUrl();
    // 重新刷新单元格数据
    ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_purchaseInfos_purchasePartList_purchasePart_sdg").refreshDataByRequst({
        type: "post",
        url: "/msService/material/purchaseInfos/purchasePart/purchasePartList/data-sdg?datagridCode=material_1.0.0_purchaseInfos_purchasePartList_purchasePart_sdg&id=" + urlParams.id,
        param: {}
    });
}