//-----货位调整-----

const dgDetailName = "material_1.0.0_placeAjust_placeAdjustEditdg1578287126570";

var dgDetail;
var rfWarehouse;

function dataInit() {
    dgDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_placeAjust_placeAdjustEditdg1578287126570");
    rfWarehouse = ReactAPI.getComponentAPI("Reference").APIs("placeAjustInfo.ware.name");
    dataInit = () => { };
}

function onLoad() {
    dataInit();
}

function ptInit() {
    dataInit();
}

function onSave() {
    if ("submit" == ReactAPI.getOperateType()) {
        var dgData = dgDetail.getDatagridData();
        if (!dgData.length) {
            //表体不能为空！
            ReactAPI.showMessage("w", ReactAPI.international.getText("material.custom.randon1573634425210"));
            return false;
        }

        var samePlaceRowIndex = dgData.filter(rowData => rowData.toPlaceSet.id == rowData.fromPlaceSet.id).map(rowData => rowData.rowIndex + 1).join(",");
        if (samePlaceRowIndex) {
            //第{0}行转出货位与转入货位相同
            ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.placeAjust", samePlaceRowIndex));
            return false;
        }
    }
}


function toStringCondition(condition) {
    var valid_keys = Object.keys(condition).filter(key => condition[key]);
    if (valid_keys.length) {
        return "customConditionKey=" + valid_keys.join(",") + "&" + valid_keys.map(key => key + "=" + condition[key]).join("&");
    }
    return ""
}

function ptBtnStock() {
    var warehouse = rfWarehouse.getValue()[0];
    var condition = toStringCondition({
        wareId: warehouse && warehouse.id,
        isAble: true
    });

    ReactAPI.createDialog("stock_ref", {
        title: ReactAPI.international.getText("material.custom.onhand.ref"),//现存量参照
        url: "/msService/material/standingcrop/standingcrop/onhandRef?" + condition,
        size: 5,
        callback: (data, event) => {
            stock_callback(data, event);
        },
        isRef: true, // 是否开启参照
        onOk: (data, event) => {
            stock_callback(data, event);

        },
        onCancel: (data, event) => {
            ReactAPI.destroyDialog("stock_ref");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
    });

    var stock_callback = (stocks, event) => {
        if (!stocks.length) {
            //请至少选中一行
            event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1574406106043"));
            return false;
        }
        var dgData = dgDetail.getDatagridData();
        if (!dgData.length && (!warehouse || !warehouse.id)) {
            var warehouseChooses = new Set(stocks.map(stock => stock.ware.id));
            if (warehouseChooses.size > 1) {
                //勾选货位的仓库不同，无法同时参照！
                ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.placeset.ware.diff"));
                return false;
            }
            rfWarehouse.setValue(stocks[0].ware);
        }
        var existedStockIds = dgData.map(val => val.onhand.id);
        var conflictStockRowIdx = stocks.filter(stock => existedStockIds.includes(stock.id)).map(stock => stock.rowIndex + 1).join(",");
        if (conflictStockRowIdx) {
            //不能重复参照！
            event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1574404333334", conflictStockRowIdx));
            return false;
        }
        dgDetail.addLine(stocks.map(stock => new Object({
            onhand: stock,
            fromPlaceSet: stock.placeSet,
            good: stock.good
        })), true);
        ReactAPI.destroyDialog("stock_ref");
    }
}