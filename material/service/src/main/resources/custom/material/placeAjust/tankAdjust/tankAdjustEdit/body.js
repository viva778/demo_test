var rfPlace, rfStock, dgDetail, nbAvaQuan;

function dataInit() {
    rfPlace = ReactAPI.getComponentAPI().Reference.APIs("tankAdjust.tank.name");
    rfStock = ReactAPI.getComponentAPI().Reference.APIs("tankAdjust.stock.batchText");
    dgDetail = ReactAPI.getComponentAPI().SupDataGrid.APIs("material_1.0.0_placeAjust_tankAdjustEditdg1653448039874");
    nbAvaQuan = ReactAPI.getComponentAPI().InputNumber.APIs("tankAdjust.stock.availiQuantity");
    dataInit = () => { }
}

function onLoad() {
    dataInit();
}

function ptInit() {
    dataInit();
    dgDetail.setBtnImg("print", "sup-btn-own-dy");
}


function ptBtnPrint() {
    var stock = rfStock.getValue()[0];
    if (!stock || !stock.id) {
        ReactAPI.showMessage('w', "请先选择批次");
        return;
    }
    var selRow = dgDetail.getSelecteds()[0];
    if (selRow && selRow.containerType && selRow.containerType.id == 'material_hfContainerType/bucket') {
        if (!selRow.adjustQuantity) {
            ReactAPI.showMessage('w', "请先输入调整量");
            return;
        }
        ReactAPI.createDialog("print_modal", {
            title: "打印配置",
            size: 3,
            url: "/msService/material/toPrint/printConfig/printEdit?non_config=true",
            buttons: [
                {
                    text: "打印",
                    type: "primary",
                    onClick: function (event) {
                        event.ReactAPI.openLoading("打印中,请稍后！");
                        try {
                            var printerName = event.ReactAPI.getComponentAPI("Reference").APIs("printConfig.printer.printName").getValue()[0].printName;
                            if (!printerName) {
                                ReactAPI.showMessage('w', "打印机名称不能为空！");
                                return false;
                            }

                            var templateName = event.ReactAPI.getComponentAPI("Reference").APIs("printConfig.printTemp.name").getValue()[0].name;
                            if (!templateName) {
                                ReactAPI.showMessage('w', "模版名称不能为空！");
                                return false;
                            }
                            var clientIp = event.ReactAPI.getComponentAPI("Input").APIs("printConfig.printer.clientIp").getValue();
                            var result = ReactAPI.request({
                                type: "post",
                                url: "/msService/HfWareCustom/qrDetail/printByStock?stockId=" + stock.id + "&quantity=" + selRow.adjustQuantity,
                                async: false
                            });
                            if (result.code != 200) {
                                ReactAPI.showMessage('f', result.message || "系统错误，请联系管理员")
                                return false;
                            }
                            var qrDetail = result.data;
                            ReactAPI.request({
                                url: "/msService/pdf-generator/generateAndPrint",
                                type: 'post',
                                async: true,
                                dataType: "json",
                                contentType: "application/json",
                                data: {
                                    fileName: "wms" + new Date().getTime() + ".pdf",
                                    clientIp: clientIp,
                                    count: 1,
                                    printerName: printerName,
                                    templateName: templateName,
                                    parameterMap: {
                                        pk: qrDetail.id
                                    }
                                },
                            }, response => {
                                event.ReactAPI.closeLoading();
                                if (response && response.responseText == "SUCEESS") {
                                    ReactAPI.destroyDialog("print_modal");
                                    ReactAPI.showMessage('s', "打印成功！");
                                    //回填条码，并设置只读
                                    dgDetail.setRowData(selRow.rowIndex, {
                                        qrDetail: qrDetail,
                                        containerType_attr: {
                                            readonly: true
                                        },
                                        adjustQuantity_attr: {
                                            readonly: true
                                        }
                                    });
                                } else {
                                    ReactAPI.showMessage('f', "打印失败，请检查打印服务");
                                }
                            });
                        } catch (e) {
                            event.ReactAPI.closeLoading();
                        }
                    }
                }
            ]
        });
    } else {
        ReactAPI.showMessage('w', "请选择一条需要装桶的数据");
    }
}


function onSave() {
    var dgData = dgDetail.getDatagridData();
    if (dgData.length) {
        //1.校验总量
        var adjustQuan = dgDetail.getDatagridData().map(data => data.adjustQuantity).reduce((v1, v2) => v1 + v2);
        var availableQuan = nbAvaQuan.getValue();
        if (adjustQuan > availableQuan) {
            ReactAPI.showMessage('w', "调出量大于可用现存量！");
            return false;
        }
        var fromPlace = rfPlace.getValue()[0].id;
        for (const rowData of dgData) {
            //2.检查至储料桶的物品是否有贴码
            if (rowData.containerType.id == 'material_hfContainerType/bucket' && (!rowData.qrDetail || !rowData.qrDetail.id)) {
                ReactAPI.showMessage('w', "请先将移至储料桶的物料打印并贴码");
                return false;
            }
            //3.校验调入货位是否非原货位
            if (rowData.toPlace.id == fromPlace) {
                ReactAPI.showMessage('w', "调入货位不允许于调出货位一致！");
                return false;
            }
        }
    } else {
        ReactAPI.showMessage('w', "请至少添加一条数据");
        return false;
    }
}

function getStockRefParam() {
    var place = rfPlace.getValue()[0];
    var conditions = [
        "available=1", "hasBatch=1", "tank=1"
    ];
    if (place && place.id) {
        conditions.push("place=" + place.id);
    }
    return conditions.join("&");
}

function ptocgContainerType(value, rowIndex) {
    dgDetail.setRowData(rowIndex, {
        toPlace: null
    });
}

function getPlaceRefParam() {
    var selRow = dgDetail.getSelecteds()[0];
    if (selRow && selRow.containerType && selRow.containerType.id == 'material_hfContainerType/tank') {
        return "tank=1";
    } else {
        return "tank=0";
    }
}


function ocgStock(value) {
    if (value && value[0]) {
        rfPlace.setReadonly(true);
        var place = rfPlace.getValue()[0];
        if (!place) {
            rfPlace.setValue(value[0].placeSet);
        }
    } else {
        rfPlace.setReadonly(false);
    }
}