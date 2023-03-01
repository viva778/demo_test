//-----现存量盘点清单-----

const dgRecordListName = "material_1.0.0_stocktakingJob_stockRecordListViewdg1661218631826";
const dgDiffListName = "material_1.0.0_stocktakingJob_stockRecordListViewdg1661218637883";

var dgRecordList;
var dgDiffList;
var vMaterialId;
var vStocktakingId;

function dataInit() {
    dgRecordList = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgRecordListName);
    dgDiffList = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgDiffListName);
    var urlParams = ReactAPI.getParamsInRequestUrl();
    vMaterialId = urlParams.materialId;
    vStocktakingId = urlParams.stocktakingId;
    dataInit = () => { }
}


var curStockKey;

function ptRecordInit() {
    dataInit();
    setDiffBtnImg();
    //绑定点击事件
    dgRecordList.setClickEvt(function (e, data) {
        //根据stockKey刷新右侧差异清单表体
        curStockKey = data.stockKey;
        refreshDiffData();
    });
}

function setDiffBtnImg() {
    // 设置 确认图标
    dgDiffList.setBtnImg("btn-confirm", "sup-btn-own-tj");
}

function ptRecordRenderOver() {
    var dgData = dgRecordList.getDatagridData();
    dgData.forEach(rowData => {
        //设置颜色
        var color = getQuantityColor(rowData.quantityOnBook, rowData.quantityByCount);
        rowData.quantityOffset_attr = {
            style: {
                color: color
            }
        }
        if (rowData.dispute) {
            rowData.row_attr = {
                background: rgbToHex([220, 211, 251])
            }
        }
    });
    dgRecordList.setDatagridData(dgData);
}

function refreshDiffData() {
    dgDiffList.refreshDataByRequst({
        type: "POST",
        url: "/msService/material/stocktakingJob/stjStockRecord/data-dg1661218637883?datagridCode=material_1.0.0_stocktakingJob_stockRecordListViewdg1661218637883&id=-1",
        param: {
            customCondition: {
                stockKey: curStockKey,
                stocktakingId: vStocktakingId
            }
        },
    });
}

function refreshRecordData() {
    dgRecordList.refreshDataByRequst({
        type: "POST",
        url: "/msService/material/stocktakingJob/stjStockRecord/data-dg1661218631826?datagridCode=material_1.0.0_stocktakingJob_stockRecordListViewdg1661218631826&id=-1",
        param: {
            customCondition: {
                materialId: vMaterialId,
                stocktakingId: vStocktakingId
            }
        },
    });
}


function ptBtnConfirm() {
    var selRow = dgDiffList.getSelecteds()[0];
    if (!selRow) {
        //至少选择一行
        ReactAPI.showMessage('w', ReactAPI.international.getText("ec.common.checkselected"));
        return false;
    }
    if (selRow.checked) {
        //material.stocktaking.line_data_was_checked
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.stocktaking.line_data_was_checked"));
        return false;
    }
    //调用接口然后刷新两侧表体
    var result = ReactAPI.request({
        url: "/msService/material/stocktakingJob/confirmResult?recordId=" + selRow.id,
        type: "post",
        async: false
    });
    if (result.code != 200) {
        ReactAPI.showMessage('f', result.message);
        return false;
    }
    refreshDiffData();
    refreshRecordData();
    rechecked = true;
}


var rechecked;


const base_col = 0x50;
const minimum_col = 0xA0;
const maximum_col = 0xFF;

function rgbToHex(rgb) {
    return '#' + ((parseInt(rgb[0]) << 16) + (parseInt(rgb[1]) << 8) + parseInt(rgb[2])).toString(16);
}

function getQuantityColor(quantityOnBook, quantityByCount) {
    var quantityOffset = quantityByCount - quantityOnBook;
    var color;
    if (quantityOffset > 0) {
        //越盈越蓝，2倍封顶
        var rate = 2 - quantityByCount / quantityOnBook;
        if (rate < 0) {
            color = rgbToHex([base_col, base_col, maximum_col]);
        } else {
            color = rgbToHex([base_col, base_col, maximum_col * (1 - rate) + minimum_col * rate]);
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

function ptDiffInit() {
    dataInit();
    if (ReactAPI.getParamsInRequestUrl().just4view) {
        dgDiffList.setBtnHidden(["btn-confirm"]);
    }
}