var dgDetail;
var rfWarehouse;

function dataInit() {
    dgDetail = ReactAPI.getComponentAPI().SupDataGrid.APIs("material_1.0.0_placeAjust_tonTankAdjEditdg1654675456790");
    rfWarehouse = ReactAPI.getComponentAPI().Reference.APIs("tankAdjust.warehouse.name");
    dataInit = () => { };
}


function ptInit() {
    dataInit();
    dgDetail.setBtnImg("btn-tonTank", "sup-btn-own-db");
}


function ptBtnTonTank() {
    ReactAPI.createDialog("tankEdit", {
        title: "单吨罐调整",
        url: "/msService/HfWareCustom/tankTraceRecord/tankTraceRec/tankEdit",
        size: 1,
        onOk: (event) => {
            var tankCode = event.ReactAPI.getComponentAPI().Input.APIs("tankTraceRec.tankCode").getValue();
            if (!tankCode) {
                event.ReactAPI.showMessage('w', "请输入罐编码");
            } else {
                var tankCodes = dgDetail.getDatagridData().map(data => data.fromTonTankCode);
                if (tankCodes.includes(tankCode)) {
                    event.ReactAPI.showMessage('w', "该罐已被添加");
                } else {
                    var result = ReactAPI.request({
                        url: "/msService/HfWareCustom/getTankInfo",
                        type: 'get',
                        async: false,
                        data: {
                            "tankCode": tankCode
                        }
                    });
                    if (result.code != 200 || !result.data) {
                        event.ReactAPI.showMessage('w', result.message || "系统错误，请联系管理员");
                    } else {
                        var data = result.data;
                        ReactAPI.destroyDialog("tankEdit");
                        if (!data.place) {
                            ReactAPI.showMessage('w', "找不到单吨罐入库信息！");
                        } else if (data.batchNum && !data.batchInfo) {
                            //只有批号未创建批次信息
                            ReactAPI.showMessage('w', "找不到单吨罐批次" + data.batchNum + "，请确保数据已入库");
                        } else {
                            ptocgFromPlace(data.place);
                            //进行增行
                            dgDetail.addLine([{
                                material: data.material,
                                batchInfo: data.batchInfo,
                                tonTankRemain: data.quantity,
                                fromTonTankCode: tankCode,
                                fromPlace: data.place
                            }], true)[0];
                        }
                    }
                }
            }
        }
    });
}

function ptocgContainerType(value, rowIndex) {
    var rowData = {};
    //1.清空调入货位
    rowData.toPlace = null;
    //2.如果是普通货位，调整量清除并设置只读(整体调整)
    if (value == "material_hfContainerType/bucket") {
        rowData.adjustQuantity = null;
        rowData.adjustQuantity_attr = {
            readonly: true
        }
    } else {
        //否则取消只读
        rowData.adjustQuantity_attr = {
            readonly: false
        }
    }
    dgDetail.setRowData(rowIndex, rowData);
}

function getToPlaceRefParam() {
    var selRow = dgDetail.getSelecteds()[0];
    if (selRow && selRow.containerType) {
        var conditions = [];
        var warehouse = rfWarehouse.getValue()[0];
        if (warehouse && warehouse.id) {
            conditions.push("warehouse=" + warehouse.id);
        }
        if (selRow.containerType.id == "material_hfContainerType/tank") {
            conditions.push("tank=1");
        } else {
            conditions.push("tank=0");
        }
        return conditions.join("&");
    } else {
        return "none=1"
    }
}

function getFromPlaceRefParam() {
    var conditions = [];
    var warehouse = rfWarehouse.getValue()[0];
    if (warehouse && warehouse.id) {
        conditions.push("warehouse=" + warehouse.id);
    }
    conditions.push("tank=0");
    return conditions.join("&");
}


function ptocgFromPlace(value) {
    var warehouse = rfWarehouse.getValue()[0];
    if (!warehouse || !warehouse.id && value && value.warehouse) {
        rfWarehouse.setValue(value.warehouse);
    }
}

function ptocgToPlace(value) {
    var warehouse = rfWarehouse.getValue()[0];
    if (!warehouse || !warehouse.id && value && value.warehouse) {
        rfWarehouse.setValue(value.warehouse);
    }
}

function ocgWarehouse() {
    //清空货位
    var dgData = dgDetail.getDatagridData();
    dgData.forEach(data => {
        data.fromPlace = null;
        data.toPlace = null;
    });
    dgDetail.setDatagridData(dgData);
}


function onSave() {
    var dgData = dgDetail.getDatagridData();
    if (!dgData.length) {
        ReactAPI.showMessage('w', "请至少添加一条数据");
        return false;
    } else {
        for (const data of dgData) {
            if (data.containerType.id == "material_hfContainerType/tank" && !data.adjustQuantity) {
                ReactAPI.showMessage('w', "调整至罐区货位需要填写调整量！");
                return false;
            }
        }
    }
}