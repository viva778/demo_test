//-----拆包-----
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagridMobile");
loader.import("decimal");

const decimal_round = 2;
const dgDetailName = "material_1.0.0_unpack_unpackBatchEdit__mobile__dg1675157646717";

var rfWarehouse;
var rfStock;
var dgDetail;
var ipnSplitNumber;

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

function openHistory() {
    $('head').append('<style>.api-dialog-content .m-content .m-iframe{margin-bottom:0!important};</style>');
    $('head').append('<style>.api-dialog-content  .iframe-footer{display:none};</style>')
    //弹出拆批记录
    let baseUrl = "/msService/material/unpack/unpackInfo/unpackList?workFlowMenuCode=material_1.0.0_unpack_unpackList&openType=page&clientType=mobile&hideWebTitle=true";
    ReactAPI.createDialog({
        id: "unpackBatchView",
        title: "拆批记录",//拆批记录
        url: baseUrl + "&customConditionKey=splitType&splitType=" + encodeURI("material_splitType/batch"),
        footer: [
            {
                name: "提交",
                style: {color: "#fff", background: "#ff0000", 'display': 'none'}
            }]
    });

}

function dataInit() {
    rfWarehouse = ReactAPI.getComponentAPI("Reference").APIs("unpackInfo.warehouse.name");
    rfStock = ReactAPI.getComponentAPI("Reference").APIs("unpackInfo.stock.availiQuantity");
    dgDetail = ReactAPI.getComponentAPI("Datagrid").APIs("material_1.0.0_unpack_unpackBatchEdit__mobile__dg1675157646717");
    ipnSplitNumber = ReactAPI.getComponentAPI("InputNumber").APIs("unpackInfo.splitNumber");
    $("div[data-key='unpackInfo.splitType']").parent().parent().hide();
    //增加拆批记录入口
    $(".sup-navbar-right").html("<b>拆批记录</b>")
    $(".sup-navbar-right").click(() => openHistory())
    dataInit = () => {
    };
}

var selRowIdx = [];

function onLoad() {
    dataInit();
}

function ptInit() {
    dataInit();
    $(".layout-panel").prepend('<div class="am-flexbox sup-row-list custom am-flexbox-dir-row am-flexbox-align-center" style="border: solid;border-color: #dedede;border-radius: 15px;margin-top: 10px;" ><div class="am-flexbox-item"><div><div data-testid="SupLabel" class="sup-label comp-label"><div class="label-text wrap">扫二维码或条码</div></div></div></div><div class="am-flexbox-item" style="text-align: right;"><div><svg t="1675417736461" class="icon" viewBox="0 0 1024 1024" ' +
        'version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="2820" width="60" height="30">' +
        '<path d="M928 544 96 544c-17.664 0-32-14.336-32-32s14.336-32 32-32l832 0c17.696 0 32 14.336 32 32S945.696 544 928 544zM832 928l-192 0c-17.696 0-32-14.304-32-32s14.304-32 32-32l192 0c17.664 0 32-14.336 32-32l0-160c0-17.696 14.304-32 32-32s32 14.304 32 32l0 160C928 884.928 884.928 928 832 928zM352 928 192 928c-52.928 0-96-43.072-96-96l0-160c0-17.696 14.336-32 32-32s32 14.304 32 32l0 160c0 17.664 14.368 32 32 32l160 0c17.664 0 32 14.304 32 32S369.664 928 352 928zM128 384c-17.664 0-32-14.336-32-32L96 192c0-52.928 43.072-96 96-96l160 0c17.664 0 32 14.336 32 32s-14.336 32-32 32L192 160C174.368 160 160 174.368 160 192l0 160C160 369.664 145.664 384 128 384zM896 384c-17.696 0-32-14.336-32-32L864 192c0-17.632-14.336-32-32-32l-192 0c-17.696 0-32-14.336-32-32s14.304-32 32-32l192 0c52.928 0 96 43.072 96 96l0 160C928 369.664 913.696 384 896 384z" ' +
        'fill="#1296db" p-id="2821"></path></svg></div></div></div>')
        .children().first().click(() => {
        scanningClick()
    })
    dgDetail.setBtnImg("print", "PTicon_dysz");
    dgDetail.setBtnImg("autoSplit", "PTicon_fuj");
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
        ReactAPI.showMessage('w', getIntlValue("material.validator.data_required"));
        return false;
    }
    var totalQuan = dgData.map(val => val.decimalQuantity || "0").reduce((d1, d2) => countSum([d1, d2]));
    if (totalQuan > rfStock.getValue()[0].availiQuantity) {
        //拆包总数不能大于现存量
        ReactAPI.showMessage('w', getIntlValue("material.custom.random1675935306995"));
        return false;
    }
    ReactAPI.submitCallback(function (res) {
        // 执行逻辑
        // res 接口返回值 类比submitFormData 回调方法
        // 在窗口关闭之前执行,return false 可以阻止平台默认的页面关闭事件
        console.log(res);
        window.location.reload(window.location.href);
        return false;
    });

}

function ocgStock(val) {
    if (val && val[0]) {
        setTimeout(function () {
            rfWarehouse.setValue(val[0].ware)
        })
    }
    dgDetail.deleteLine();
}

function ocgWarehouse(value) {
    setTimeout(function () {
        if (value && value[0]) {
            if (value[1]) {
                rfWarehouse.setValue(value[1])
            } else {
                rfWarehouse.setValue(value[0])
            }
        }
        rfStock.removeValue();
        dgDetail.deleteLine();
    })
}


function getStockRefParam() {
    var warehouse = rfWarehouse.getValue()[0];
    if (warehouse && warehouse.id) {
        return "isAble=true&enableBatch=true&wareId=" + warehouse.id;
    } else {
        return "isAble=true&enableBatch=true";
    }
}

function ptocgQuantity(val, oldValue, index) {
    val = parseFloat(val)
    let data = dgDetail.getRows(index + "")[0]
    if (isNaN(val) || parseFloat(val) <= 0) {
        data.decimalQuantity = null
        dgDetail.setRowData(index, data);
        //请输入一个正数
        ReactAPI.showMessage('w', getIntlValue("material.validator.positive_number"));
    } else {
        data.decimalQuantity = val.toFixed(2)
        dgDetail.setRowData(index, data);
    }
}


function ptBtnAutoSplit() {
    var spn = parseInt(ipnSplitNumber.getValue());
    if (typeof spn != "number" || spn <= 0 || isNaN(spn)) {
        //请输入一个正确的拆分数
        ReactAPI.showMessage('w', getIntlValue("material.unpack.split_number_constraint"));
        return false;
    }
    var stock = rfStock.getValue()[0];
    if (!stock || !stock.availiQuantity > 0) {
        //请选择一条正确的现存量
        ReactAPI.showMessage('w', getIntlValue("material.validator.stock_chosen_constraint"));
        return false;
    }
    //提示框
    let data = dgDetail.getDatagridData();
    if (data && data.length > 0) {
        ReactAPI.openConfirm({
            message: getIntlValue(
                "material.custom.random1675672923546"
            ),
            okText: getIntlValue(
                "attendence.attStaff.isInstitutionYes"
            ), //是
            cancelText: getIntlValue(
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
    var total = stock.availiQuantity
    var singleQuan = towPointFun((total / spn))
    var newLineData = [];
    var max_pad = Math.max(String(spn).length, 3);
    let startRowIndex = dgDetail.getDatagridData().length;
    for (let i = 0; i < spn; i++) {
        newLineData[i] = {
            decimalQuantity: singleQuan.toString(),
            toBePrinted: true,
            //设置子批号
            batchNum: stock.batchText + "-" + pad(i + 1, max_pad),
            rowIndex: startRowIndex + i
        }
    }
    //最后一条数量补齐remain  - (singleQuan * (spn - 1))
    newLineData[spn - 1].decimalQuantity = towPointFun(countSum([total, -countSum([countSum([spn, -1]) * singleQuan, 0])])).toString();
    //进行增行
    dgDetail.addLine(newLineData, true);
}



/**
 * 两位截断
 * @param a
 * @returns {number}
 */
function towPointFun(a) {
    return countSum([Math.floor(a * 100) / 100, 0])
}

function countSum(arr) {
    if (!arr.length) return 0;
    arr = arr.map((v) => {
        if (v && !Number.isNaN(Number(v))) return Math.round(v * 10000);
        return 0;
    });
    const result = arr.reduce((prev, curr) => {
        return prev + curr
    }, 0);
    return result / 10000;
}

function ptBtnDelete() {
    var selData = dgDetail.getSelecteds();
    if (selData && !selData.length > 0) {
        //请至少选择一条数据
        ReactAPI.showMessage('w', getIntlValue("foundation.common.currency.pleaseselectatleastonepieceofdata"));
        return false;
    }
    //过滤已打印的数据
    var printedRows = selData.filter(val => !val.toBePrinted).map(val => val.rowIndex + 1).join(",");
    if (printedRows) {
        //第{0}行，已被打印
        ReactAPI.showMessage('w', getIntlValue("material.printer.dg_data_printed").format(printedRows));
        return false;
    }
    dgDetail.deleteLine(selData.filter(val => val.toBePrinted).map(val => val.rowIndex).join(","));
}

function ptBtnPrint() {
    var stock = rfStock.getValue()[0];
    if (!stock || !stock.availiQuantity > 0) {
        //请选择一条正确的现存量
        ReactAPI.showMessage('w', getIntlValue("material.validator.stock_chosen_constraint"));
        return false;
    }
    var selData = dgDetail.getSelecteds();
    if (!selData.length) {
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
    //校验选中区数据
    // selRowIdx = selData.map(val => val.rowIndex);
    // var check_result = datagrid.validator.check(dgDetailName);
    // if (!check_result) {
    //     return false;
    // }
    //计算已打印数量
    var dgData = dgDetail.getDatagridData();
    var printedData = dgData.filter(val => !val.toBePrinted);
    var printedQuan = printedData.length && printedData.map(val => val.decimalQuantity || 0).reduce((d1, d2) => countSum([d1, d2])) || 0;
    //计算当前打印总数
    var printQuan = selData.map(val => val.decimalQuantity || 0).reduce((d1, d2) => countSum([d1, d2]));
    var printTotal = countSum([printedQuan, printQuan]);
    //比较剩余量
    if (printTotal > stock.availiQuantity) {
        //无剩余可用量
        ReactAPI.showMessage('w', getIntlValue("material.validator.no_available_stock"));
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
                    //var printConfig = event.tryGetSaveData();
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
                            batchNum: val.batchNum,
                            quantity: val.decimalQuantity
                        }));
                        //打印中，请稍后...
                        event.ReactAPI.openLoading("1", getIntlValue("material.hint.wait_for_print"));
                        //调用拆包并打印接口
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
                                event.ReactAPI.openLoading("2", result.message);
                                //回填条码明细
                                result.data.forEach(rtnData => {
                                    selData.forEach(rowData => {
                                        if (rowData.id == rtnData.dataKey) {
                                            //刷新只读条件
                                            let newLine = dgDetail.getRows(rowData.rowIndex.toString())[0];
                                            newLine.barCode = rtnData.barCode
                                            newLine.toBePrinted = false
                                            datagrid.appendRowAttr("material_1.0.0_unpack_unpackBatchEdit__mobile__dg1675157646717", newLine);
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
                                alert('打印服务接口调用失败' + result.message);
                                //event.ReactAPI.showMessage('f', result.message);
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

function scanningClick() {
    console.log("扫码打开");
    window.mobilejs.scanQRCode(cbScanQrCode);

    function cbScanQrCode(qrData) {
        // alert(qrData);
        var qrInfoId = qrData && JSON.parse(qrData) && JSON.parse(qrData).PK;
        if (!qrInfoId) {
            alert("未识别到条码台账信息");
        }
        //请求对应条码台账
        ReactAPI.request({
            url: "/msService/material/entity/getEntityList",
            type: "get",
            async: false,
            data: {
                moduleName: "material",
                entityName: "MaterialQrDetailInfo",
                conditions: "valid=true,id=" + Number(qrInfoId),
                includes: "id,batchText,material.id,placeSet.id,warehouse.id"
            }
        }, function (result) {
            // alert(JSON.stringify(result.data))
            if (result && result.code == 200 && result.data) {
                var qrDetailInfo = result.data[0];
                queryStockAndFillForm(qrDetailInfo);
            } else {
                alert("条码台账不存在");
            }
        });
    }
}

function queryStockAndFillForm(qrDetailInfo) {
    // alert("条码台账:" + JSON.stringify(qrDetailInfo));
    if (!qrDetailInfo.material || !qrDetailInfo.material.id) {
        alert("条码台账缺少物料信息");
        return;
    }
    if (!qrDetailInfo.warehouse || !qrDetailInfo.warehouse.id) {
        alert("条码台账缺少仓库信息");
        return;
    }
    let conditionStr = "valid=true,batchText=" + qrDetailInfo.batchText + ",good.id=" + qrDetailInfo.material.id + ",ware.id=" + qrDetailInfo.warehouse.id + ",placeSet.id=";
    if (qrDetailInfo.placeSet && qrDetailInfo.placeSet.id) {
        conditionStr += qrDetailInfo.placeSet.id;
    } else {
        conditionStr += 'null';
    }
    try {
        ReactAPI.request({
            url: "/msService/material/entity/getEntityList",
            type: "get",
            async: false,
            data: {
                moduleName: "material",
                entityName: "MaterialStandingcrop",
                conditions: conditionStr,
                includes: "id,ware.id,ware.code,ware.name,placeSet.id,placeSet.name,placeSet.code,good.id,good.name,availiQuantity,batchText"
            }
        }, function (result) {
            // alert(JSON.stringify(result))
            if (result && result.code == 200 && result.data.length > 0) {
                var stockInfo = result.data[0];
                fillForm(stockInfo);
            } else {
                alert("未找到现存量");
            }
        });
    } catch (e) {
        alert(e);
    }
}

function fillForm(stockInfo) {
    //将现存量信息回填到表头
    // alert("现存量仓库:" + JSON.stringify(stockInfo.ware));
    var wareReference = window.ReactAPI.getComponentAPI("Reference").APIs("unpackInfo.warehouse.name");
    var stockReference = window.ReactAPI.getComponentAPI("Reference").APIs("unpackInfo.stock.availiQuantity");
    wareReference.setValue(stockInfo.ware);
    wareCache = [stockInfo.ware]
    stockReference.setValue(stockInfo);
}

function addLine(value) {
    dgDetail.addLine();
}