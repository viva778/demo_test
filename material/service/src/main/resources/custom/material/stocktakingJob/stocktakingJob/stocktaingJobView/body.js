//-----盘点任务查看-----

const dgDetailName = "material_1.0.0_stocktakingJob_stocktaingJobViewdg1661307637478";
const dgStockRecordName = "material_1.0.0_stocktakingJob_stocktaingJobViewdg1661307637721";

var dgDetail;
var dgStockRecord;
var vShowOrNot;

function dataInit() {
    dgDetail = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgDetailName);
    dgStockRecord = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgStockRecordName);
    vShowOrNot = ReactAPI.getComponentAPI("SystemCode").APIs("stocktakingJob.stocktaking.showStockOrNot").getValue().value;
    dataInit = () => { }
}


function onLoad() {
    dataInit();

    //绑定点击事件
    dgDetail.setClickEvt(function () {
        detailOnClick();
    });
    dgDetail.setCheckBoxClickEvt(function () {
        detailOnClick();
    });
}

function ptInit() {
    dataInit();
}


function ptRenderOver() {
    //设置明盘暗盘隐藏
    if (vShowOrNot == 'material_stockTakingWay/implicitWay') {
        dgStockRecord.setColumnsHideOrShow("quantityOnBook");
        dgStockRecord.setColumnsHideOrShow("quantityOffset");
    }
    var dgData = dgStockRecord.getDatagridData();
    dgData.forEach(val => {
        //计算盈亏量
        if (typeof val.quantityByCount == "number") {
            val.quantityOffset = val.quantityByCount - val.quantityOnBook;
        }
        //设置颜色
        var color = getQuantityColor(val.quantityOnBook, val.quantityByCount);
        val.quantityOffset_attr = {
            style: {
                color: color
            }
        }
        //注册行
        registerNewLine(val);
    });
    dgStockRecord.setDatagridData(dgData);
}



var vSelectDistModelCodes = new Set();
//分配任务点击，刷新右侧
function detailOnClick() {
    setTimeout(() => {
        //判断本次选择和上次选择是否一致，不一致则刷新
        var flush = false;
        var selRows = dgDetail.getSelecteds();
        if (vSelectDistModelCodes.size != selRows.length) {
            //先根据长度判断
            flush = true;
        } else {
            //再根据全包含判断
            selRows.map(val => val.distribution.target.onlyCode).forEach(onlyCode => {
                if (!flush) {
                    if (!vSelectDistModelCodes.has(onlyCode)) {
                        flush = true;
                    }
                }
            })
        }
        if (flush) {
            vSelectDistModelCodes = new Set(selRows.map(val => val.distribution.target.onlyCode));
            refreshStockRecord();
        }
    });
}

function refreshStockRecord() {
    //根据所选的货位编码过滤右侧数据
    if (vSelectDistModelCodes.size) {
        var remainList = records.filter(val => (!val.place || !val.place.id) || vSelectDistModelCodes.has(val.place.onlyCode));
        dgStockRecord.setDatagridData(remainList);
    } else {
        dgStockRecord.setDatagridData(records);
    }
}

//每次表体变化，标记变化行，等待重新注册

const records = [];


function registerNewLine(rowData) {
    //存储记录
    records.push(rowData);
}


const base_col = 0x50;
const minimum_col = 0xA0;
const maximum_col = 0xFF;

function rgbToHex(rgb) {
    return '#' + ((rgb[0] << 16) + (rgb[1] << 8) + rgb[2]).toString(16);
}

function getQuantityColor(quantityOnBook, quantityByCount) {
    var quantityOffset = quantityByCount - quantityOnBook;
    var color;
    if (quantityOffset > 0) {
        //越盈越绿，2倍封顶
        var rate = 2 - quantityByCount / quantityOnBook;
        if (rate < 0) {
            color = rgbToHex([base_col, maximum_col, base_col]);
        } else {
            color = rgbToHex([base_col, maximum_col * (1 - rate) + minimum_col * rate, base_col]);
        }
    } else if (quantityOffset < 0) {
        //越亏越红，2倍封顶
        var rate = 2 - quantityOnBook / quantityByCount;
        if (rate < 0) {
            color = rgbToHex([maximum_col, base_col, base_col]);
        } else {
            color = rgbToHex([maximum_col * (1 - rate) + minimum_col * rate, base_col, base_col]);
        }
    } else {
        color = "#111";
    }
    return color;
}