var warehouseId;
var fromPlaceId;
var toPlaceId;

function onLoad() { }

function onSave() {
    if (fromPlaceId == toPlaceId) {
        ReactAPI.showMessage('w', "调整货位不允许相等");
        return false;
    }
    window.parent.overallCallback && window.parent.overallCallback(fromPlaceId, toPlaceId);
    return false;
}

function ocgOutPlace(value) {
    if (value && value[0]) {
        fromPlaceId = value[0].id;
        warehouseId = value[0].warehouse.id;
    } else {
        fromPlaceId = undefined;
        if (!toPlaceId) {
            warehouseId = undefined;
        }
    }
}

function ocgInPlace(value) {
    if (value && value[0]) {
        toPlaceId = value[0].id;
        warehouseId = value[0].warehouse.id;
    } else {
        toPlaceId = undefined;
        if (!fromPlaceId) {
            warehouseId = undefined;
        }
    }
}

function getRefParams() {
    if (warehouseId) {
        return "warehouse=" + warehouseId;
    }
}