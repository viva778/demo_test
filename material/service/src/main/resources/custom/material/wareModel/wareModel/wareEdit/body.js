var scWareType;
var rfWarehouseClass;
var lbWarehouseClass;
var ipCode;

function dataInit() {
    scWareType = ReactAPI.getComponentAPI("SystemCode").APIs("wareModel.wareType");
    ipCode = ReactAPI.getComponentAPI("Input").APIs("wareModel.code");
    rfWarehouseClass = ReactAPI.getComponentAPI("Reference").APIs("wareModel.warehouseClass.name");
    lbWarehouseClass = ReactAPI.getComponentAPI("Label").APIs("wareModel.warehouseClass.name");
    dataInit = () => { }
}

function onLoad() {
    dataInit();
    var wareType = scWareType.getValue().value;
    //编码，类型不允许修改
    if (ReactAPI.getParamsInRequestUrl().id) {
        ipCode.setReadonly(true);
        refreshWareType(wareType);
    } else {
        //默认隐藏所有
        showAreaFields();
    }
    var parentId = ReactAPI.getParamsInRequestUrl().parentId;
    //根节点只能选择仓库
    if (-1 == parentId || (wareType && types_of_warehouse.has(wareType))) {
        setWareTypeToWarehouse();
        showWarehouseFields();
    } else {
        if (wareType) {
            scWareType.setReadonly(true);
        } else {
            setWareTypeAreaOrPlace();
        }
    }
}

const types_of_warehouse = new Set(["material_wareType/wareYL", "material_wareType/wareCP", 'material_wareType/factory', 'material_wareType/logistics']);

function onchangeType(val) {
    refreshWareType(val);
}


function setWareTypeToWarehouse() {
    //移除非仓库系统编码
    scWareType.removeOption("material_wareType/cargoArea");
    scWareType.removeOption("material_wareType/storeSet");
}

function setWareTypeAreaOrPlace() {
    //移除非货位、货区系统编码
    scWareType.removeOption("material_wareType/wareYL");
    scWareType.removeOption("material_wareType/wareCP");
    scWareType.removeOption('material_wareType/factory');
    scWareType.removeOption('material_wareType/logistics');
}

function setWareTypePlace() {
    //移除非货位系统编码
    scWareType.removeOption("material_wareType/wareYL");
    scWareType.removeOption("material_wareType/wareCP");
    scWareType.removeOption('material_wareType/factory');
    scWareType.removeOption('material_wareType/logistics');
    scWareType.removeOption("material_wareType/cargoArea");
}


function refreshWareType(val) {
    switch (val) {
        case "material_wareType/storeSet":
            showPlaceFields();
            break;
        case "material_wareType/cargoArea":
            showAreaFields();
            break;
        case "material_wareType/wareYL":
        case "material_wareType/wareCP":
        case "material_wareType/factory":
        case "material_wareType/logistics":
            showWarehouseFields();
            break;
    }
}



function showPlaceFields() {
    rfWarehouseClass.setRequired(false);
    lbWarehouseClass.setNullableStyle(false);
    $("#app > div > form > div.m-layout-mian > div > div > div > div > div > div > div > div > table > tbody > tr:nth-child(4)").hide();
    $("#app > div > form > div.m-layout-mian > div > div > div > div > div > div > div > div > table > tbody > tr:nth-child(5)").show();
    $("#app > div > form > div.m-layout-mian > div > div > div > div > div > div > div > div > table > tbody > tr:nth-child(6)").show();
    $("#app > div > form > div.m-layout-mian > div > div > div > div > div > div > div > div > table > tbody > tr:nth-child(7)").show();
}


function showAreaFields() {
    rfWarehouseClass.setRequired(false);
    lbWarehouseClass.setNullableStyle(false);
    $("#app > div > form > div.m-layout-mian > div > div > div > div > div > div > div > div > table > tbody > tr:nth-child(4)").hide();
    $("#app > div > form > div.m-layout-mian > div > div > div > div > div > div > div > div > table > tbody > tr:nth-child(5)").hide();
    $("#app > div > form > div.m-layout-mian > div > div > div > div > div > div > div > div > table > tbody > tr:nth-child(6)").hide();
    $("#app > div > form > div.m-layout-mian > div > div > div > div > div > div > div > div > table > tbody > tr:nth-child(7)").hide();
}


function showWarehouseFields() {
    rfWarehouseClass.setRequired(true);
    lbWarehouseClass.setNullableStyle(true);
    $("#app > div > form > div.m-layout-mian > div > div > div > div > div > div > div > div > table > tbody > tr:nth-child(4)").show();
    $("#app > div > form > div.m-layout-mian > div > div > div > div > div > div > div > div > table > tbody > tr:nth-child(5)").hide();
    $("#app > div > form > div.m-layout-mian > div > div > div > div > div > div > div > div > table > tbody > tr:nth-child(6)").hide();
    $("#app > div > form > div.m-layout-mian > div > div > div > div > div > div > div > div > table > tbody > tr:nth-child(7)").hide();
}