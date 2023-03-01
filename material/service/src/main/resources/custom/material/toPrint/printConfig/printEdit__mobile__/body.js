//-----打印配置-----

var rfPrinter;
var rfTemplate;
var ipClientIp;
var ipnQuantity;
var ipnPrintNum;

function dataInit() {
    rfPrinter = ReactAPI.getComponentAPI("Reference").APIs("printConfig.printer.printName");
    rfTemplate = ReactAPI.getComponentAPI("Reference").APIs("printConfig.printTemp.name");
    ipnQuantity = ReactAPI.getComponentAPI("InputNumber").APIs("printConfig.specifications");
    ipnPrintNum = ReactAPI.getComponentAPI("InputNumber").APIs("printConfig.printNum");
    ipClientIp = ReactAPI.getComponentAPI("Input").APIs("printConfig.printer.clientIp");
    dataInit = () => {
    };
}

function onLoad() {
    dataInit();
    //获取默认打印机配置
    ReactAPI.request({
        url: "/msService/material/qrDetailInfo/qrDetailInfo/getPrintInfoByTime",
        type: 'get',
        async: true
    }, result => {
        if (result && 200 == result.code) {
            var data = result.data;
            rfPrinter.setValue({
                printName: data.printerName,
                id: data.printerId,
                clientIp: data.printIp
            });
            rfTemplate.setValue({
                name: data.printTempName,
                id: data.printTempId
            });
        }
    });
    var urlParams = ReactAPI.getParamsInRequestUrl();
    if (urlParams.printer_only) {
        $("div[data-key='printConfig.specifications']").parent().parent().hide();
        $("div[data-key='printConfig.printNum']").parent().parent().hide();
        ipnQuantity.setRequired(false);
        ipnPrintNum.setRequired(false);
    }
}


function onSave() {
    save_data = ReactAPI.getSaveData().printConfig;
    return false;
}

var save_data;

function tryGetSaveData() {
    //进行验证
    save_data = undefined;
    ReactAPI.submitFormData();
    return save_data;
}

function preview() {
    var map = new Map();
    var printTemp = parent.ReactAPI.getComponentAPI("Reference").APIs("printConfig.printer.printName").getValue()[0];
    if (printTemp == undefined) {
        //请至少选中一行
        ReactAPI.showMessage('w', "模版名称不能为空！");
        return false;
    }
    var templateName = printTemp.printName;
    var fileName = Date.parse(new Date());
    map.put("fileName", fileName);
    map.put("templateName", templateName);
    var paramMap = new Map();
    paramMap.put("code", "001");
    map.put("parameterMap", parameterMap);
    var obj = Object.create(null);
    for (let [k, v] of map) {
        obj[k] = v;
    }
    var jsonStr = JSON.stringify(obj);
    $.ajax({
        url: "/msService/pdf-generator/generate",
        type: 'get',
        data: {"jsonStr": jsonStr},
        success: function (res) {
            var url = res;
        }
    });
}