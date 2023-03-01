//-----生产出库-----
//引入datagrid.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");
loader.import("material");

const dgDetailName = "material_1.0.0_produceOutSingle_produceOutSingleEditdg1574141892290";

//获取是否启用wmsPro模块,并设成全局常量
var integrateWmsPro;
var enablePlace;
//是否生成任务（开启PRO并且开启货位
var generateTask;
var dgDetail;
var rfWarehouse;
var rfBizType;
var rfStorageType;
var ipSrcTableNo;
var cbPlaceEnableState;

function dataInit() {
    rfWarehouse = ReactAPI.getComponentAPI().Reference.APIs("produceOutSing.ware.name");
    rfBizType = ReactAPI.getComponentAPI("Reference").APIs("produceOutSing.serviceTypeID.serviceTypeExplain");
    rfStorageType = ReactAPI.getComponentAPI("Reference").APIs("produceOutSing.outCome.reasonExplain");
    dgDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgDetailName);
    cbPlaceEnableState = ReactAPI.getComponentAPI("Checkbox").APIs("produceOutSing.ware.storesetState");
    ipSrcTableNo = ReactAPI.getComponentAPI("Input").APIs("produceOutSing.srcTableNo");
    integrateWmsPro = (ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.wmspro",
    })["material.wmspro"] == 'true');
    enablePlace = cbPlaceEnableState.getValue().value;
    generateTask = enablePlace && integrateWmsPro;
    dataInit = () => { };
}

const default_storage_type = { id: 1015, reasonExplain: ReactAPI.international.getText("material.custom.ProductionDelivery") };

function onLoad() {
    dataInit();
    //业务类型赋值
    rfBizType.setValue({ serviceTypeCode: "produceStorageOut", id: 1006, serviceTypeExplain: ReactAPI.international.getText("material.custom.ProductionIssueTransaction") });
    var storageType = rfStorageType.getValue()[0];
    if (!storageType) {
        rfStorageType.setValue(default_storage_type);
    }
    var srcTableNo = ipSrcTableNo.getValue();
    if (srcTableNo) {
        //上游推送单据隐藏添加按钮
        dgDetail.setBtnDisplay(["btn-goodRef"]);
        //添加超链接
        ipSrcTableNo.replace("<div class='supplant-readonly-wrap supplant-comp readonly'>" + srcTableNo + "</div>", {
            onClick: function () {
                onclickBatchNum(srcID);
            }
        });
    }
}

function ptInit() {
    dataInit();
    refreshRequired();
    //设置校验
    datagrid.validator.add(dgDetailName, "appliQuanlity", rowData => {
        //按件时
        if (rowData.good && rowData.good.isBatch && rowData.good.isBatch.id == "BaseSet_isBatch/piece") {
            //如果存在现存量，则数量必须为1
            if (rowData.onhand && rowData.onhand.id) {
                return rowData.appliQuanlity == 1;
            } else {
                //如果不存在现存量，则申请量只能为整数
                return rowData.appliQuanlity % 1 == 0;
            }
        } else {
            return true;
        }
    }, (rowIndex, titile, rowData) => {
        if (rowData.onhand && rowData.onhand.id) {
            //物品批号{0}已开启按件管理，数量只能为1。
            return ReactAPI.international.getText(
                "material.validator.by_piece_quantity_check",
                "[" + rowData.good.name + "/" + rowData.onhand.batchText + "]"
            );
        } else {
            //物品批号{0}已开启按件管理，数量只能为整数。
            return ReactAPI.international.getText(
                "material.validator.by_piece_int_quantity_check",
                "[" + rowData.good.name + "]"
            );
        }
    });
}

function ptRenderOver() {

}


var ignoreConfirmFlag = false;

function onSave() {
    var type = ReactAPI.getOperateType();
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

        //校验超储,归并同物料
        if (!ignoreConfirmFlag) {
            var warehouse = rfWarehouse.getValue()[0];
            var checked_idx = new Set();
            for (var i = 0; i < dgData.length; i++) {
                if (!checked_idx.has(i)) {
                    var material = dgData[i].good;
                    //统计物料数量
                    var quan = dgData[i].appliQuanlity;
                    for (var j = i + 1; j < dgData.length; j++) {
                        if (!checked_idx.has(j)) {
                            if (material.id == dgData[j].good.id) {
                                checked_idx.add(j);
                                quan += dgData[k].appliQuanlity;
                            }
                        }
                    }
                    //统计完成，进行校验
                    if (!check_material_limit(warehouse, material, quan)) {
                        return false;
                    }
                }
            }
        }
    }
}

function check_material_limit(warehouse, material, quan) {
    var result = ReactAPI.request({
        type: "get",
        url: "/msService/material/socketSet/socketSetInfo/findSocketSet",
        async: false,
        data: {
            wareId: String(warehouse.id),
            goodId: String(material.id),
            direction: "directionSend",
            num: String(quan),
        },
    });
    if (result.code == 200) {
        let data = result.data;
        if (data.isNo) {
            ReactAPI.openConfirm({
                //<b>【{0}】</b>中<b>【{1}】</b>的最低库存为<b>{2}</b>，现存量<b>{3}</b>。<br><b>【{4}】</b>出库后库存数量将少于最低库存！
                message: ReactAPI.international.getText(
                    "material.custom.SocketSet.confirm1",
                    warehouse.name,
                    material.name,
                    String(data.DownAlarm),
                    String(data.Onhand),
                    material.name
                ),
                okText: ReactAPI.international.getText(
                    "attendence.attStaff.isInstitutionYes"
                ), //是
                cancelText: ReactAPI.international.getText(
                    "attendence.attStaff.isInstitutionNo"
                ), //否
                onOk: () => {
                    ReactAPI.closeConfirm();
                    ignoreConfirmFlag = true;
                    ReactAPI.submitFormData("submit");
                    ignoreConfirmFlag = false;
                    return true;
                },
                onCancel: () => {
                    ReactAPI.closeConfirm();
                    return false;
                },
            });
            return false;
        }
        return true;
    } else {
        ReactAPI.showMessage("w", result.message);
        return false;
    }
}

function ptBtnMaterial() {
    var warehouse = rfWarehouse.getValue()[0];
    if (!warehouse || !warehouse.id) {
        // 请先选择仓库
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.random1632472693120"));
        return false;
    }

    var url = "/msService/BaseSet/material/material/materialRefLayout?multiSelect=true";
    url = material.getMaterialsByWare(rfWarehouse, url);
    if(!url){
        return;
    }

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
            dgDetail.addLine(data.map(rowData => {
                var newLine = {
                    good: rowData
                };
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

var warehouseBeforeChange = {};

function ocgWarehouse(value) {
    var dgData = dgDetail.getDatagridData();
    if (dgData.length != 0) {
        if (value && value.length > 0) {
            ReactAPI.openConfirm({
                //"修改后将清空表体数据，是否继续？",
                message: ReactAPI.international.getText("material.custom.randonAfterModifyBody"),
                okText: ReactAPI.international.getText("attendence.attStaff.isInstitutionYes"),//是
                cancelText: ReactAPI.international.getText("attendence.attStaff.isInstitutionNo"),//否
                onOk: () => {
                    ReactAPI.closeConfirm();
                    // 清除表头仓库时清空表体
                    dgDetail.deleteLine();

                    refreshPlaceEnable(value[0].storesetState);
                    warehouseBeforeChange = value[0];
                },
                onCancel: () => {
                    ReactAPI.getComponentAPI("Reference").APIs("produceOutSing.ware.name").setValue(warehouseBeforeChange);
                    ReactAPI.closeConfirm();
                    return false;
                }
            });
        } else {
            ReactAPI.openConfirm({
                //"清除后将清空表体数据，是否继续？",
                message: ReactAPI.international.getText("material.custom.randonAfterclearingBody"),
                okText: ReactAPI.international.getText("attendence.attStaff.isInstitutionYes"),//是
                cancelText: ReactAPI.international.getText("attendence.attStaff.isInstitutionNo"),//否
                onOk: () => {
                    ReactAPI.closeConfirm();
                    // 清除表头仓库时清空表体
                    dgDetail.deleteLine();
                    warehouseBeforeChange = {};
                },
                onCancel: () => {
                    ReactAPI.getComponentAPI("Reference").APIs("produceOutSing.ware.name").setValue(warehouseBeforeChange);
                    ReactAPI.closeConfirm();
                    return false;
                }
            });
        }
    } else {
        warehouseBeforeChange = value[0];
    }

    if (value && value[0]) {
        refreshPlaceEnable(value[0].storesetState);
    }

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

/**
 * 刷新字段必填、只读
 */
function refreshRequired() {
    //不生成上下架，则可用量必填
    if (!generateTask) {
        datagrid.validator.required.setColRequired(dgDetailName, [{
            key: "onhand.availiQuantity",
            type: "object"
        }]);
    } else {
        datagrid.validator.required.removeColRequired(dgDetailName, [{
            key: "onhand.availiQuantity"
        }]);
    }
}



function onclickBatchNum(id) {
    if (id) {
        ReactAPI.createDialog("newDialog", {
            title: '生产批次详情',
            url: "/msService/WOM/produceTask/matConsumRecod/matConsumeRecordView?id=" + id,
            size: 5,
            callback: (data, event) => {
                partCallback(data, event);
            },
            isRef: true, // 是否开启参照

            onCancel: (data, event) => {
                ReactAPI.destroyDialog("newDialog");
            },
            cancelText: '关闭'
        });
    }
}