// ----容器档案-容器列表 移动视图----

// ---------------------- 初始化 start ----------------------

function onLoad() {
    viewInit();
    actionInit();
}

function actionInit() {
    ReactAPI.setClickEvt(function () {
        return false;
    }); // 阻止跳转链接
}

function viewInit() {
    // 移除原视图底部按钮
    $(".sup-el-footer").html("");

    // 新增[条码打印]和[RFID写入]按钮
    $(".sup-el-footer").append('<div id="wmsPrintBtn" onclick="wmsPrint()" style="flex:1; text-align:center">条码打印</div>');
    
    // RFID写入按钮暂时隐藏，待后续实现后再放开 by wcy 2022/10/12
    // $(".sup-el-footer").append('<div id="writeRFIDBtn" style="flex:1; text-align:center">RFID写入</div>');
}

// ---------------------- 初始化 end ----------------------


// ---------------------- 条码打印按钮 start ----------------------

// 条码打印按钮功能
// 选中一条容器档案后, 打开打印配置页面, 当前版本仅支持单选 by wcy 2022/09/21
function wmsPrint() {
    // 选中的容器
    var selectedCont = ReactAPI.getSelected();

    // 请至少选中一行
    if (selectedCont.length == 0) {
        ReactAPI.showMessage('w', "请至少选中一行");
        return false;
    }

    // 容器状态为报废，无二维码信息!
    if (selectedCont[0].useState.id == 'material_ruleState/scrap') {
        ReactAPI.showMessage('w', "容器状态为报废，无二维码信息!");
        return false;
    }

    // 选中的容器编码
    var contCode = selectedCont[0].code;

    // 打开打印配置页面
    ReactAPI.createDialog({
        id: "wmsPrintDialog",
        title: "打印配置",
        url: "/msService/material/toPrint/printConfig/printForOtherEdit?clientType=mobile",
        footer: [{
            name: "打印",
            style: {
                background: "#2874D2",
                borderColor: "transparent",
                color: "#fff"
            },
            onClick: function onClick(event) {
                event.ReactAPI.openLoading("1", "打印中,请稍后！");
                try {
                    // 打印机名称
                    var printerName = event.rfPrintName.getValue()[0].printName;
                    if (!printerName) {
                        ReactAPI.showMessage('w', "打印机名称不能为空！");
                        return false;
                    }
                    // 模版名称
                    var templateName = event.rfPrintTempName.getValue()[0].name;
                    if (!templateName) {
                        ReactAPI.showMessage('w', "模版名称不能为空！");
                        return false;
                    }
                    // 客户端IP
                    var clientIp = event.ipClientIp.getValue();
                    // 打印数量
                    var printNum = event.ipnPrintNum.getValue();
                    if (!printNum || printNum <= 0) {
                        ReactAPI.showMessage('w', "打印数量需要填写正整数!");
                        return false;
                    }

                    // 调用打印接口
                    for (var i = 0; i < printNum; i++) {
                        var response = ReactAPI.request({
                            url: "/msService/pdf-generator/generateAndPrint",
                            type: 'post',
                            async: false,
                            dataType: "json",
                            contentType: "application/json",
                            data: {
                                fileName: Date.parse(new Date()),
                                clientIp: clientIp,
                                count: 1,
                                printerName: printerName,
                                templateName: templateName,
                                parameterMap: {
                                    code: contCode
                                }
                            }
                        }).responseText;

                        if ("SUCCESS" != response) {
                            ReactAPI.showMessage('w', "打印失败！请查看打印服务");
                            return false;
                        }
                    }
                    ReactAPI.destroyDialog("wmsPrintDialog");
                    ReactAPI.showMessage('s', "打印成功！");
                    event.ReactAPI.submitFormData("save", function (res) {
                        console.log(res);
                    });
                } catch(error){
                    console.log("error:" + error);
                    ReactAPI.showMessage('w', "打印失败！请查看打印服务");
                    return false;
                } finally {
                    event.ReactAPI.closeLoading();
                }
            }
        }, {
            name: "取消",
            style: {
                background: "#e64c66",
                borderColor: "transparent",
                color: "#fff"
            },
            onClick: function onClick(event) {
                ReactAPI.destroyDialog("wmsPrintDialog");
            }
        }]
    });
}

// ---------------------- 条码打印按钮 end ----------------------