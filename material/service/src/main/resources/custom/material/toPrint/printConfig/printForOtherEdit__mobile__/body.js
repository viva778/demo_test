// ----打印列表-模板设置 移动视图----

var rfPrintName;        // 打印机名称
var ipClientIp;         // 客户端IP
var rfPrintTempName;    // 模版名称
var ipnPrintNum;        // 打印数量
var ipnSpecifications;  // 货量


// ---------------------- 初始化 start ----------------------
function onLoad() {
    dataInit();
    viewInit();
}

function dataInit() {
    rfPrintName         = ReactAPI.getComponentAPI("Reference").APIs("printConfig.printer.printName");
    ipClientIp          = ReactAPI.getComponentAPI("Input").APIs("printConfig.printer.clientIp");
    rfPrintTempName     = ReactAPI.getComponentAPI("Reference").APIs("printConfig.printTemp.name");
    ipnPrintNum         = ReactAPI.getComponentAPI("InputNumber").APIs("printConfig.printNum");
    ipnSpecifications   = ReactAPI.getComponentAPI("InputNumber").APIs("printConfig.specifications");

    initPrintInfoByTime();
}

function viewInit(){
    // 隐藏打印数量和货量
    ReactAPI.getComponentAPI("InputNumber").APIs("printConfig.printNum").hide().row();
    ReactAPI.getComponentAPI("InputNumber").APIs("printConfig.specifications").hide().row();
    
}

function initPrintInfoByTime() {
    ReactAPI.request(
        {
            type: 'get',
            url: "/msService/material/qrDetailInfo/qrDetailInfo/getPrintInfoByTime",
            async: false
        },
        function (result) {
            console.log(result);
            if (200 == result.code) {
                var printInfo = result.data;
                // 设置打印机
                rfPrintName.setValue({
                    printName: printInfo.printerName,
                    id: printInfo.printerId,
                    clientIp: printInfo.printIp
                });
                
                // 设置打印模板
                rfPrintTempName.setValue({
                    name: printInfo.printTempName,
                    id: printInfo.printTempId
                });
            }
        }
    );
}
