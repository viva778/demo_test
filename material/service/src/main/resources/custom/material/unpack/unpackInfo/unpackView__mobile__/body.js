//-----拆包查看-----
var rfStock;
var dgDetail;
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagridMobile");

var dgDetailName = "material_1.0.0_unpack_unpackView__mobile__dg1675216174742";

function dataInit() {
    rfStock = ReactAPI.getComponentAPI("Reference").APIs("unpackInfo.stock.availiQuantity");
    dgDetail = ReactAPI.getComponentAPI("Datagrid").APIs("material_1.0.0_unpack_unpackView__mobile__dg1675216174742");
    dataInit = () => {
    };
}

// ---------------------- 通用方法 start ----------------------
/**
 * 获取国际化值
 * @param key 国际化key
 */
function getIntlValue(key) {
    var intlValue;
    var result = ReactAPI.request(
        {
            type: "get",
            data: {},
            url: "/inter-api/i18n/v1/internationalConvert?key=" + key,
            async: false
        },
        function (res) {
            intlValue = res && res.data;
            return intlValue
        }
    );
    return intlValue.replace('</b>', '').replace('<b>', "").replace('<br/>', "");
}

//占位符匹配
String.prototype.format = function () {
    if (arguments.length === 0) return this;
    for (var s = this, i = 0; i < arguments.length; i++)
        s = s.replace(new RegExp("\\{" + i + "\\}", "g"), arguments[i]);
    return s;
};

function uuid() {
    function S4() {
        return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
    }

    return (S4() + S4() + "-" + S4() + "-" + S4() + "-" + S4() + "-" + S4() + S4() + S4());
}

// ---------------------- 通用方法 end ----------------------


function onLoad() {
    dataInit();
}


function ptInit() {
    dataInit();
    dgDetail.setBtnImg("print", "PTicon_dysz");
    $("div[data-key='unpackInfo.stock.availiQuantity']").parent().parent().hide();
}


function ptBtnPrint() {
    var stock = rfStock.getValue()[0];
    var selData = dgDetail.getSelecteds();
    if (!selData || !selData.length) {
        //请至少选择一条数据
        ReactAPI.showMessage('w', getIntlValue("material.custom.random1675932165205"));
        return false;
    }
    //过滤已打印的数据
    var printedRows = selData.filter(val => !val.toBePrinted).map(val => val.rowIndex + 1).join(",");
    if (printedRows) {
        //第{0}行，已被打印
        ReactAPI.showMessage('w', getIntlValue("material.printer.dg_data_printed").format(printedRows));
        return false;
    }
    //弹出窗口选择打印机
    ReactAPI.createDialog({
        id: "print_modal",
        title: getIntlValue("material.custom.random1626763566035"),//打印配置
        // size: 3,
        url: "/msService/material/toPrint/printConfig/printEdit?printer_only=true&clientType=mobile",
        footer: [
            {
                name: getIntlValue("material.custom.random1626763807232"),//打印
                // type: "primary",
                onClick: function (event) {
                    try {
                        // var printConfig = event.tryGetSaveData();
                        event.ReactAPI.submitFormData();
                        let printConfig = {
                            printTemp: event.ReactAPI.getComponentAPI("Reference").APIs("printConfig.printTemp.name").getValue()[0],
                            printer: event.ReactAPI.getComponentAPI("Reference").APIs("printConfig.printer.printName").getValue()[0]
                        }
                        if (printConfig) {
                            //组织打印参数
                            var stockId = stock.id;
                            var printer = {
                                clientIp: printConfig.printer.clientIp,
                                printerName: printConfig.printer.printName,
                                templateName: printConfig.printTemp.name
                            };
                            var details = selData.map(val => new Object({
                                dataKey: val.id,
                                quantity: val.decimalQuantity,
                                unpackDetailId: val.id
                            }));
                            //打印中，请稍后...
                            event.ReactAPI.openLoading("1", getIntlValue("material.hint.wait_for_print"));
                            //调用拆包并打印接口
                            ReactAPI.request({
                                type: "post",
                                url: "/msService/material/printer/unpackStockAndPrint",
                                async: false,
                                data: {
                                    stockId: stockId,
                                    printer: printer,
                                    details: details
                                }
                            }, result => {
                                if (result.code == 200) {
                                    result.data.forEach(rtnData => {
                                        selData.forEach(rowData => {
                                            if (rowData.id == rtnData.dataKey) {
                                                //刷新只读条件
                                                let newLine = dgDetail.getRows(rowData.rowIndex.toString())[0];
                                                newLine.barCode = rtnData.barCode
                                                newLine.toBePrinted = false
                                                datagrid.appendRowAttr(dgDetailName, newLine);
                                                dgDetail.setRowData(rowData.rowIndex, newLine);
                                            }
                                        })
                                    });
                                    event.ReactAPI.closeLoading();
                                    event.ReactAPI.openLoading("2", result.message);
                                    setTimeout(() => {
                                        event.ReactAPI.closeLoading();
                                        ReactAPI.destroyDialog("print_modal");
                                        dgDetail.refreshDataByRequst({
                                            type: "POST",
                                            url: "/msService/material/unpack/unpackInfo/data-dg1675216174742?datagridCode=material_1.0.0_unpack_unpackView__mobile__dg1675216174742&id=" + ReactAPI.getFormData().id
                                        });
                                    }, 1000);
                                } else {
                                    event.ReactAPI.closeLoading();
                                    alert('打印服务接口调用失败:'+result.message);
                                    //event.ReactAPI.showMessage('f', result.message);
                                }
                            });
                        }
                    } catch (e) {
                        console.log(e)
                        event.ReactAPI.showMessage('f', "打印参数错误");
                    }
                }
            }
        ]
    });
}