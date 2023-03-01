//-----拆批-----
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");
loader.import("decimal");

Decimal.rounding = Decimal.ROUND_DOWN;
const decimal_round = 2;
const dgDetailName = "material_1.0.0_unpack_unpackBatchEditdg1668062341027";

var rfWarehouse;
var rfStock;
var dgDetail;
var ipnSplitNumber;

function dataInit() {
    rfWarehouse = ReactAPI.getComponentAPI().Reference.APIs("unpackInfo.warehouse.name");
    rfStock = ReactAPI.getComponentAPI().Reference.APIs("unpackInfo.stock.availiQuantity");
    dgDetail = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgDetailName);
    ipnSplitNumber = ReactAPI.getComponentAPI().InputNumber.APIs("unpackInfo.splitNumber");
    dataInit = () => {
    };
}

var selRowIdx = [];

function onLoad() {
    dataInit();
}

function ptInit() {
    dataInit();
    dgDetail.setBtnImg("print", "sup-btn-own-print");
    dgDetail.setBtnImg("autoSplit", "sup-btn-own-jc");
    //设置必填校验点
    datagrid.validator.required.setRequiredByCondition(dgDetailName, [{
        key: "decimalQuantity",
        type: "plain"
    }], function (rowData) {
        //选中时必填
        return selRowIdx.includes(rowData.rowIndex);
    });
    datagrid.validator.required.setRequiredByCondition(dgDetailName, [{
        key: "batchNum",
        type: "plain"
    }], function (rowData) {
        //选中时必填
        return selRowIdx.includes(rowData.rowIndex);
    });
    //打印完成时只读
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "decimalQuantity", function (rowData) {
        return !rowData.toBePrinted;
    });
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "batchNum", function (rowData) {
        return !rowData.toBePrinted;
    });
}

function onSave() {
    var dgData = dgDetail.getDatagridData();
    if (!dgData.length) {
        //请至少添加一条数据！
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.validator.data_required"));
        return false;
    }
    var totalQuan = dgData.map(val => new Decimal(val.decimalQuantity) || new Decimal("0")).reduce((d1, d2) => d1.add(d2));
    if (totalQuan.greaterThan(rfStock.getValue()[0].availiQuantity)) {
        //拆包总数不能大于现存量
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.random1675935306995"));
        return false;
    }
}

function ocgStock(val) {
    if (val && val[0]) {
        rfWarehouse.setValue(val[0].ware);
    }
    dgDetail.deleteLine();
}

function ocgWarehouse(val) {
    rfStock.removeValue();
    dgDetail.deleteLine();
}


function getStockRefParam() {
    var warehouse = rfWarehouse.getValue()[0];
    if (warehouse && warehouse.id) {
        return "isAble=true&enableBatch=true&wareId=" + warehouse.id;
    } else {
        return "isAble=true&enableBatch=true";
    }
}

function ptocgQuantity(val, rowIdx) {
    setTimeout(() => {
        if (isNaN(val) || parseFloat(val) <= 0) {
            dgDetail.setRowData(rowIdx, {
                decimalQuantity: null
            });
            //请输入一个正数
            ReactAPI.showMessage('w', ReactAPI.international.getText("material.validator.positive_number"));
        } else {
            dgDetail.setRowData(rowIdx, {
                decimalQuantity: new Decimal(val).toFixed(decimal_round)
            });
        }
    },100);
}


function ptBtnAutoSplit() {
    var spn = ipnSplitNumber.getValue();
    if (typeof spn != "number" || spn <= 0) {
        //请输入一个正确的拆分数
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.unpack.split_number_constraint"));
        return false;
    }
    var stock = rfStock.getValue()[0];
    if (!stock || !stock.availiQuantity > 0) {
        //请选择一条正确的现存量
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.validator.stock_chosen_constraint"));
        return false;
    }
    //提示框
    let data = dgDetail.getDatagridData()
    if (data && data.length > 0) {
        ReactAPI.openConfirm({
            message: ReactAPI.international.getText(
                "material.custom.random1675672923546"
            ),
            okText: ReactAPI.international.getText(
                "attendence.attStaff.isInstitutionYes"
            ), //是
            cancelText: ReactAPI.international.getText(
                "attendence.attStaff.isInstitutionNo"
            ), //否
            onOk: () => {
                ReactAPI.closeConfirm();
                dgDetail.deleteLine();
                autoSplitFun(spn, stock)
                return true;
            },
            onCancel: () => {
                ReactAPI.closeConfirm();
                return false;
            },
        });
    } else {
        autoSplitFun(spn, stock)
    }

}

function autoSplitFun(spn, stock) {
    //计算剩余量
    var quanArray = dgDetail.getDatagridData().map(val => new Decimal(val.decimalQuantity || 0));
    var used = quanArray.length && quanArray.reduce((d1, d2) => d1.add(d2)) || new Decimal("0");
    var remain = new Decimal(stock.availiQuantity).sub(used);
    if (remain.lessThanOrEqualTo("0")) {
        //无剩余可用量
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.validator.no_available_stock"));
        return false;
    }
    //计算单件量(保留n位小数)
    var singleQuan = remain.div(spn).toFixed(decimal_round);
    //如果最后除数得出来是0，则报错
    if (!parseFloat(singleQuan)) {
        //拆分量不足
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.unpack.split_quan_insufficient"));
        return false;
    }
    var newLineData = new Array(spn);
    var max_pad = Math.max(String(spn).length, 3);
    for (let i = 0; i < spn; i++) {
        newLineData[i] = {
            decimalQuantity: singleQuan,
            toBePrinted: true,
            //设置子批号
            batchNum: stock.batchText + "-" + pad(i + 1, max_pad)
        }
    }
    //最后一条数量补齐remain  - (singleQuan * (spn - 1))
    newLineData[spn - 1].decimalQuantity = remain.sub(new Decimal(singleQuan).mul(spn - 1)).toFixed(decimal_round);
    //进行增行
    dgDetail.addLine(newLineData, true);
}


function ptBtnDelete() {
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
    dgDetail.deleteLine(selData.filter(val => val.toBePrinted).map(val => val.rowIndex).join(","));
}

function ptBtnPrint() {
    var stock = rfStock.getValue()[0];
    if (!stock || !stock.availiQuantity > 0) {
        //请选择一条正确的现存量
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.validator.stock_chosen_constraint"));
        return false;
    }
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
    //校验选中区数据
    selRowIdx = selData.map(val => val.rowIndex);
    var check_result = datagrid.validator.check(dgDetailName);
    if (!check_result) {
        return false;
    }
    //计算已打印数量
    var dgData = dgDetail.getDatagridData();
    var printedData = dgData.filter(val => !val.toBePrinted);
    var printedQuan = printedData.length && printedData.map(val => new Decimal(val.decimalQuantity || 0)).reduce((d1, d2) => d1.add(d2)) || new Decimal(0);
    //计算当前打印总数
    var printQuan = selData.map(val => new Decimal(val.decimalQuantity || 0)).reduce((d1, d2) => d1.add(d2));
    var printTotal = printedQuan.add(printQuan);
    //比较剩余量
    if (printTotal.greaterThan(stock.availiQuantity)) {
        //无剩余可用量
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.validator.no_available_stock"));
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
                            dataKey: val.key,
                            batchNum: val.batchNum,
                            quantity: val.decimalQuantity
                        }));
                        //打印中，请稍后...
                        event.ReactAPI.openLoading(ReactAPI.international.getText("material.hint.wait_for_print"));
                        //调用拆批并打印接口
                        ReactAPI.request({
                            type: "post",
                            url: "/msService/material/printer/splitBatchAndPrint",
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
                                //回填条码明细
                                result.data.forEach(rtnData => {
                                    selData.forEach(rowData => {
                                        if (rowData.key == rtnData.dataKey) {
                                            //刷新只读条件
                                            var newLine = {
                                                barCode: rtnData.barCode,
                                                toBePrinted: false
                                            };
                                            datagrid.appendRowAttr(dgDetailName, newLine);
                                            dgDetail.setRowData(rowData.rowIndex, newLine);
                                        }
                                    })
                                });
                                rfWarehouse.setReadonly(true);
                                setTimeout(() => {
                                    event.ReactAPI.closeLoading();
                                    ReactAPI.destroyDialog("print_modal");
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

function pad(num, cover) {
    return String("0".repeat(cover) + num).slice(-cover);
}