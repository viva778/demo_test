//-----拆包查看-----
var rfStock;
var dgDetail;

function dataInit() {
    rfStock = ReactAPI.getComponentAPI().Reference.APIs("unpackInfo.stock.availiQuantity");
    dgDetail = ReactAPI.getComponentAPI().SupDataGrid.APIs("material_1.0.0_unpack_unpackViewdg1667443794688");
    dataInit = () => { };
}

function onLoad() {
    dataInit();
}

function ptInit() {
    dataInit();
    dgDetail.setBtnImg("print", "sup-btn-own-print");
}

function ptBtnPrint() {
    var stock = rfStock.getValue()[0];
    var selData = dgDetail.getSelecteds();
    if (!selData.length) {
        //请至少选择一条数据
        ReactAPI.showMessage('w', ReactAPI.international.getText("ec.ec_view_select_property.selectNullData"));
        return false;
    }
    //过滤已打印的数据
    var printedRows = selData.filter(val => !val.toBePrinted).map(val => val.rowIndex + 1).join(",");
    if (printedRows) {
        //第{0}行，已被打印
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.printer.dg_data_printed", printedRows));
        return false;
    }

    //弹出窗口选择打印机
    ReactAPI.createDialog("print_modal", {
        title: ReactAPI.international.getText("material.custom.random1626763566035"),//打印配置
        size: 3,
        url: "/msService/material/toPrint/printConfig/printEdit?printer_only=true",
        buttons: [
            {
                text: ReactAPI.international.getText("material.custom.random1626763807232"),//打印
                type: "primary",
                onClick: function (event) {
                    var printConfig = event.tryGetSaveData();
                    if (printConfig) {
                        //组织打印参数
                        var stockId = stock.id;
                        var printer = {
                            clientIp: printConfig.printer.clientIp,
                            printerName: printConfig.printer.printName,
                            templateName: printConfig.printTemp.name
                        };
                        var details = selData.map(val => new Object({
                            unpackDetailId: val.id,
                            dataKey: val.key,
                            quantity: val.decimalQuantity
                        }));
                        //打印中，请稍后...
                        event.ReactAPI.openLoading(ReactAPI.international.getText("material.hint.wait_for_print"));
                        //调用拆包并打印接口
                        ReactAPI.request({
                            type: "post",
                            url: "/msService/material/printer/unpackStockAndPrint",
                            async: true,
                            data: {
                                stockId: stockId,
                                printer: printer,
                                details: details
                            }
                        }, result => {
                            if (result.code == 200) {
                                event.ReactAPI.closeLoading();
                                event.ReactAPI.openLoading(result.message, "2");
                                setTimeout(() => {
                                    event.ReactAPI.closeLoading();
                                    ReactAPI.destroyDialog("print_modal");
                                    dgDetail.refreshDataByRequst({
                                        type: "POST",
                                        url: "/msService/material/unpack/unpackInfo/data-dg1667443794688?datagridCode=material_1.0.0_unpack_unpackViewdg1667443794688&id=" + ReactAPI.getFormData().id
                                    });
                                }, 1000);
                            } else {
                                event.ReactAPI.closeLoading();
                                event.ReactAPI.showMessage('f', result.message);
                            }
                        });
                    }
                }
            }
        ]
    });
}