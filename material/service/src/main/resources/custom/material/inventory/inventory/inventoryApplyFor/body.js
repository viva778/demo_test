
function wareInventory(){
     var ware = ReactAPI.getComponentAPI("Reference").APIs("inventory.wareHouse.name").getValue();
    if (ware.length == 0) {
        // 请先选择待盘点仓库
        ReactAPI.showMessage("w", ReactAPI.international.getText("material.custom.inventory.emptyWare"));
        return;
    }

    var dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_inventory_inventoryApplyFordg1632645652780");
    var urlParams = ReactAPI.getParamsInRequestUrl();
    var tableId = urlParams.id;
    debugger
    // ID参数获取不到时, 说明是新增的单据, 需要先保存
    if (!tableId) {
        ReactAPI.openConfirm({
            // 确认是否保存单据？
            message: ReactAPI.international.getText("material.custom.inventory.confirmToSave"),
            buttons: [{
                    operatetype: "yes",
                    // 确定
                    text: ReactAPI.international.getText("ec.common.confirm"),
                    type: "primary",
                    onClick: function () {
                        // 先关闭确认框
                        debugger
                        ReactAPI.closeConfirm();
                        // res 为true时表示成功; message 为错误提示信息
                        ReactAPI.submitFormData("save", function (res, message) {
                            if (!res) {
                                // 关闭确认框后提示错误信息
                                ReactAPI.showMessage("w", message);
                            }
                        });
                    }
                },
                {
                    operatetype: "cancel",
                    // 取消
                    text: ReactAPI.international.getText("ec.common.cancel"),
                    onClick: function () {
                        ReactAPI.closeConfirm();
                    }
                }
            ]
        });
    } else {
        if (dataGrid.getDatagridData().length > 0) {
            ReactAPI.openConfirm({
                // 仓库盘点会清空表体数据，是否继续？
                message: ReactAPI.international.getText("material.custom.inventory.wareHouseInventoryWillClearTableBody"),
                buttons: [{
                        operatetype: "yes",
                        // 确定
                        text: ReactAPI.international.getText("ec.common.confirm"),
                        type: "primary",
                        onClick: function () {
                            // 先关闭确认框
                            ReactAPI.closeConfirm();
							inventory(tableId, ware);
							ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_inventory_inventoryApplyFordg1632645652780").refreshDataByRequst({});				
                        }
                    },
                    {
                        operatetype: "cancel",
                        // 取消
                        text: ReactAPI.international.getText("ec.common.cancel"),
                        onClick: function () {
                            ReactAPI.closeConfirm();
                        }
                    }
                ]
            });
        } else {
            inventory(tableId, ware);
        }

    }
}
	var inventory = (tableId, ware) => {
  
        var inventoryId = tableId;
        var wareHouseId = ware[0].id;
        var result = ReactAPI.request({
            type: "get",
            data: {
                "inventoryId": inventoryId,
                "wareHouseId": wareHouseId
            },
            url: "/msService/material/inventory/inventory/wareHouseInventory",
            async: false
        });
        console.log(result);
        queryData();
      
    }
    
    var queryData = () => {
        // 获取请求 url 参数接口
        var urlParams = ReactAPI.getParamsInRequestUrl();
        // 重新刷新单元格数据
        ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_inventory_inventoryApplyFordg1632645652780").refreshDataByRequst({
            type: "post",
            url: "/msService/material/inventory/inventory/data-dg1632645652780?datagridCode=material_1.0.0_inventory_inventoryApplyFordg1632645652780&id=" + urlParams.id,
            param: {pageSize:200}
        });
      	
      
    }