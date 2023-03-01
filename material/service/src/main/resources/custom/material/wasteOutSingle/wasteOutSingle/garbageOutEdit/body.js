//-----废料出库-----

//引入datagrid.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");

const dgDetailName = "material_1.0.0_wasteOutSingle_garbageOutEditdg1632383078081";

var dgDetail;
var rfServiceType;
var rfWarehouse;
var cbPlaceEnableState;
var rfDisposalUnit;
//获取是否启用wmsPro模块,并设成全局常量
var integrateWmsPro;
//废料出库单不生成任务,此变量暂时不用
var generateTask;


function dataInit() {
    rfServiceType = ReactAPI.getComponentAPI("Reference").APIs("wasteOutSingle.serviceType.serviceTypeExplain");
    rfWarehouse = ReactAPI.getComponentAPI("Reference").APIs("wasteOutSingle.ware.name");
    rfDisposalUnit = ReactAPI.getComponentAPI("Reference").APIs("wasteOutSingle.disposalUnit.merchants.name");
    cbPlaceEnableState = ReactAPI.getComponentAPI("Checkbox").APIs("wasteOutSingle.ware.storesetState");
    dgDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgDetailName);

    enablePlace = cbPlaceEnableState.getValue().value;
    integrateWmsPro = (ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.wmspro",
    })["material.wmspro"] == 'true');
    generateTask = enablePlace && integrateWmsPro;
    dataInit = () => { }
}

function onLoad() {
    dataInit();
    rfServiceType.setValue({ serviceTypeCode: "wasteOut", id: 1016, serviceTypeExplain: "废物出库事务" });
}

function ocgWarehouse(value) {
    if (value && value[0]) {
        refreshPlaceEnable(value[0].storesetState);
    }
    //清空可用量
    datagrid.clearColValue(dgDetail, "nowStock");
}

function ptocgStock(value) {
    var warehouse = rfWarehouse.getValue() && rfWarehouse.getValue()[0];
    if ((!warehouse || !warehouse.id) && value && value[0]) {
        rfWarehouse.setValue(value[0].ware);
        refreshPlaceEnable(value[0].ware.storesetState);
    }
}

function refreshPlaceEnable(val) {
    if (val != enablePlace) {
        enablePlace = val;
        generateTask = enablePlace && integrateWmsPro;
        refreshRequired();
    }
}

function ptInit() {
    dataInit();
    refreshRequired();
    //设置校验
    datagrid.validator.add(dgDetailName, "applyNumber", rowData => {
        //按件时
        if (rowData.waste && rowData.waste.isBatch && rowData.waste.isBatch.id == "BaseSet_isBatch/piece") {
            //如果存在现存量，则数量必须为1
            if (rowData.nowStock && rowData.nowStock.id) {
                return rowData.applyNumber == 1;
            } else {
                //如果不存在现存量，则申请量只能为整数
                return rowData.applyNumber % 1 == 0;
            }
        } else {
            return true;
        }
    }, (rowIndex, titile, rowData) => {
        if (rowData.nowStock && rowData.nowStock.id) {
            //物品批号{0}已开启按件管理，数量只能为1。
            return ReactAPI.international.getText(
                "material.validator.by_piece_quantity_check",
                "[" + rowData.waste.name + "/" + rowData.nowStock.batchText + "]"
            );
        } else {
            //物品批号{0}已开启按件管理，数量只能为整数。
            return ReactAPI.international.getText(
                "material.validator.by_piece_int_quantity_check",
                "[" + rowData.waste.name + "]"
            );
        }
    });
}

function ptRenderOver() {

}

function ocgDisposalUnit(value) {
    if (value && value[0]) {
        if (!getForDisposalInfo(value[0])) {
            return;
        }
        //更新表体处置单位信息
        var dgData = dgDetail.getDatagridData();
        dgData.forEach(rowData => {
            var unitDetail = disposalUnitWasteMap[rowData.waste.id];
            rowData.residualNumber = unitDetail;
            rowData.disposalNumber = unitDetail.eiaNumber - unitDetail.residualNumber;
        });
        dgDetail.setDatagridData(dgData);
    } else {
        //清空处置单位信息
        datagrid.clearColValue(dgDetail, "residualNumber");
    }
}


function ptBtnMaterial() {
    var url = "/msService/BaseSet/material/material/materialRefLayout?multiSelect=true";

    ReactAPI.createDialog("material_ref", {
        title: ReactAPI.international.getText("BaseSet.viewtitle.randon1569570764419"), //物料参照
        url: url,
        size: 5,
        callback: (data, event) => material_callback(data, event),
        isRef: true, // 是否开启参照
        onOk: (data, event) => material_callback(data, event),
        onCancel: () => {
            ReactAPI.destroyDialog("material_ref");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
    });

    var material_callback = (data, event) => {
        if (data && data.length != 0) {
            if (!disposalUnitWasteMap) {
                getForDisposalInfo(rfDisposalUnit.getValue()[0]);
            }
            dgDetail.addLine(data.map(rowData => {
                var newLine = {
                    waste: rowData,
                };
                //追加处置单位信息
                if (disposalUnitWasteMap) {
                    var unitDetail = disposalUnitWasteMap[rowData.id];
                    if (unitDetail) {
                        newLine.residualNumber = unitDetail;
                        newLine.disposalNumber = unitDetail.eiaNumber - unitDetail.residualNumber;
                    }
                }
                //将之前设置的属性附加上去
                datagrid.appendRowAttr(dgDetailName, newLine);
                return newLine;
            }), true);
            ReactAPI.destroyDialog("material_ref");
            ReactAPI.showMessage('s', ReactAPI.international.getText("foundation.common.tips.addsuccessfully"));
        } else {
            //请至少选中一行
            event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1574406106043"));
            return false;
        }
    }
}

var disposalUnitWasteMap;

function getForDisposalInfo(unit) {
    if (unit && unit.id) {
        var result = ReactAPI.request({
            url: "/msService/material/entity/getEntityList",
            type: "get",
            async: false,
            data: {
                "moduleName": "material",
                "entityName": "MaterialUnitDetails",
                "conditions": "disposalUnit.id=" + unit.id,
                "includes": "id,residualNumber,eiaNumber,waste.id"
            }
        });
        if (result.code != 200) {
            ReactAPI.showMessage('f', result.message);
            return false;
        }
        disposalUnitWasteMap = {};
        result.data.forEach(unitDetail => {
            disposalUnitWasteMap[unitDetail.waste.id] = unitDetail;
        });
    } else {
        disposalUnitWasteMap = undefined;
    }
    return true;
}

/**
 * 刷新字段必填、只读
 */
function refreshRequired() {
    //不生成上下架，则可用量必填
    if (true) {//!generateTask
        datagrid.validator.required.setColRequired(dgDetailName, [{
            key: "nowStock.availiQuantity",
            type: "object"
        }]);
    } else {
        datagrid.validator.required.removeColRequired(dgDetailName, [{
            key: "nowStock.availiQuantity"
        }]);
    }
}


function onSave() {
    var type = ReactAPI.getOperateType();
    //提交时检验
    if ("submit" == type) {
        var dgData = dgDetail.getDatagridData();
        if (dgData.length == 0) {
            //表体数据不能为空！
            ReactAPI.showMessage("w", ReactAPI.international.getText("material.custom.randon1573634425210"));
            return false;
        }
        var check_result = datagrid.validator.check(dgDetailName);
        if (!check_result) {
            return false;
        }
    }
}