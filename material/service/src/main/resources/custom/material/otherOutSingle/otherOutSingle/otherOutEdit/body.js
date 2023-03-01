//-----其他出库-----

//引入datagrid.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");
loader.import("material");

const dgDetailName = "material_1.0.0_otherOutSingle_otherOutEditdg1573122235133";
var dgDetail;
var rfWare;
var cbPlaceEnableState;
//获取是否启用wmsPro模块,并设成全局常量
var integrateWmsPro;
var generateTask;
var enablePlace;

var scRedBlue;
var rfWarehouse;
var rfWarehouseIdBeforeChange;

function dataInit() {
    rfWarehouse = ReactAPI.getComponentAPI("Reference").APIs("otherOutSingle.ware.name");
    rfWare = ReactAPI.getComponentAPI().Reference.APIs("otherOutSingle.ware.name");
    dgDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgDetailName);
    cbPlaceEnableState = ReactAPI.getComponentAPI("Checkbox").APIs("otherOutSingle.ware.storesetState");
    integrateWmsPro = (ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.wmspro",
    })["material.wmspro"] == 'true');
    enablePlace = cbPlaceEnableState.getValue().value;
    generateTask = enablePlace && integrateWmsPro;
    scRedBlue = ReactAPI.getComponentAPI("SystemCode").APIs("otherOutSingle.redBlue");
    dataInit = () => { };
}

function onLoad() {
    dataInit();
    //业务类型赋值
    ReactAPI.getComponentAPI("Reference").APIs("otherOutSingle.serviceTypeID.serviceTypeExplain").setValue({ serviceTypeCode: "otherStorageOut", id: 1001, serviceTypeExplain: ReactAPI.international.getText("material.custom.OtherIssueTransaction") });
    var outCome = ReactAPI.getComponentAPI("Reference").APIs("otherOutSingle.outCome.reasonExplain").getValue()[0];
    if (undefined == outCome) {
        ReactAPI.getComponentAPI("Reference").APIs("otherOutSingle.outCome.reasonExplain").setValue({ id: 1007, reasonExplain: ReactAPI.international.getText("material.custom.NormalMaterialDelivery") });
    }
    //隐藏增行按钮

    var vRedBlue = scRedBlue.getValue().value;

    if (vRedBlue == "BaseSet_redBlue/red") {
        ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: false });
        $("#btn-onhandRef").hide();
    } else {
        ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: true });
        $("#btn-onhandRef").show();
    }
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
            var warehouse = rfWare.getValue()[0];
            var checked_idx = new Set();
            for (var i = 0; i < dgData.length; i++) {
                if (!checked_idx.has(i)) {
                    var material = dgData[i].good;
                    //统计物料数量
                    var quan = dgData[i].appliQuantity;
                    for (var j = i + 1; j < dgData.length; j++) {
                        if (!checked_idx.has(j)) {
                            if (material.id == dgData[j].good.id) {
                                checked_idx.add(j);
                                quan += dgData[k].appliQuantity;
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

function ptBtnGoodRef() {
    var warehouse = rfWarehouse.getValue()[0];
    if (!warehouse || !warehouse.id) {
        // 请先选择仓库
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.random1632472693120"));
        return false;
    }

    var url = "/msService/BaseSet/material/material/materialRefLayout?multiSelect=true";
    url = material.getMaterialsByWare(rfWarehouse, url);
    if (!url) {
        return;
    }

    ReactAPI.createDialog("material_ref", {
        title: ReactAPI.international.getText("BaseSet.viewtitle.randon1569570764419"), //物料参照
        url: url,
        size: 5,
        callback: (data, event) => {
            material_callback(data, event)
            ReactAPI.destroyDialog("material_ref");

        },
        isRef: true, // 是否开启参照
        onOk: (data, event) => {
            material_callback(data, event)
            ReactAPI.destroyDialog("material_ref");

        },
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
                //将之前的只读属性附加上去
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


function ptRfOnhandChanged(value, index) {
    var ware = rfWare.getValue() && rfWare.getValue()[0];
    if ((!ware || !ware.id) && value && value[0]) {
        rfWare.setValue(value[0].ware);
        refreshPlaceEnable(value[0].ware.storesetState);
    }
    // 清空订单号
    dgDetail.setRowData(index, { orderNumber: null });
}

function refreshPlaceEnable(val) {
    if (val != enablePlace) {
        enablePlace = val;
        generateTask = enablePlace && integrateWmsPro;
        refreshRequired();
    }
}

function scRedBlueChanged(value) {
    var befor = scRedBlue.getValue().value;

    if (value == "BaseSet_redBlue/red") {
        if (dgDetail.getDatagridData().length != 0) {
            ReactAPI.openConfirm({
                message: ReactAPI.international.getText("material.custom.clearTheTableBodyAtTheSameTime"),//切换红蓝字会同时清空表体！
                okText: ReactAPI.international.getText("ec.common.confirm"),//确定
                cancelText: ReactAPI.international.getText("foundation.signature.cancel"),//取消
                onOk: () => {
                    ReactAPI.closeConfirm();
                    // 清空表体
                    dgDetail.deleteLine();
                    ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: false });
                    $("#btn-onhandRef").hide();
                    return false;
                },
                onCancel: () => {
                    scRedBlue.setValue(befor);
                    ReactAPI.closeConfirm();
                    return false;
                }
            });
        } else {
            ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: false });
            $("#btn-onhandRef").hide();
        }

    } else {
        if (dgDetail.getDatagridData().length != 0) {
            ReactAPI.openConfirm({
                message: ReactAPI.international.getText("material.custom.clearTheTableBodyAtTheSameTime"),//切换红蓝字会同时清空表体！
                okText: ReactAPI.international.getText("ec.common.confirm"),//确定
                cancelText: ReactAPI.international.getText("foundation.signature.cancel"),//取消
                onOk: () => {
                    ReactAPI.closeConfirm();
                    // 清空表体
                    dgDetail.deleteLine();
                    ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: true });
                    $("#btn-onhandRef").show();
                    return false;
                },
                onCancel: () => {
                    scRedBlue.setValue(befor);
                    ReactAPI.closeConfirm();
                    return false;
                }
            });
        } else {
            ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: true });
            $("#btn-onhandRef").show();
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
                    ReactAPI.getComponentAPI("Reference").APIs("otherOutSingle.ware.name").setValue(warehouseBeforeChange);
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
                    ReactAPI.getComponentAPI("Reference").APIs("otherOutSingle.ware.name").setValue(warehouseBeforeChange);
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


function rfWareAfterClear(deleteObj) {
    var length = dgDetail.getDatagridData().length;
    if (length == 0) {
        return false;
    }
    ReactAPI.openConfirm({
        //"清除后将清空表体数据，是否继续？",
        message: ReactAPI.international.getText("material.custom.randonAfterclearingBody"),
        okText: ReactAPI.international.getText("attendence.attStaff.isInstitutionYes"),//是
        cancelText: ReactAPI.international.getText("attendence.attStaff.isInstitutionNo"),//否
        onOk: () => {

            ReactAPI.closeConfirm();
            // 清除表头仓库
            ReactAPI.getComponentAPI("Reference").APIs("otherOutSingle.ware.code").removeValue();
            // 清除表头仓库时清空表体
            dgDetail.deleteLine();
            return false;
        },
        onCancel: () => {
            ReactAPI.getComponentAPI("Reference").APIs("otherOutSingle.ware.code").setValue(deleteObj[0]);
            ReactAPI.closeConfirm();
            return false;
        }
    });
    ReactAPI.getComponentAPI("Reference").APIs("otherOutSingle.ware.name").setValue(deleteObj[0]);
    return false;
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


function ptInit() {
    dataInit();
    refreshRequired();
    //设置校验
    datagrid.validator.add(dgDetailName, "appliQuantity", rowData => {
        //按件时
        if (rowData.good && rowData.good.isBatch && rowData.good.isBatch.id == "BaseSet_isBatch/piece") {
            //如果存在现存量，则数量必须为1
            if (rowData.onhand && rowData.onhand.id) {
                return rowData.appliQuantity == 1;
            } else {
                //如果不存在现存量，则申请量只能为整数
                return rowData.appliQuantity % 1 == 0;
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

function ptRenderOver() {

}

function ptBtnBizDetail() {
    //拼接查询条件
    var keys = ["bizType"];
    var conditions = ["multiSelect=true", "bizType=otherStorageIn"];
    var warehouse = rfWarehouse.getValue()[0];
    if (warehouse && warehouse.id) {
        keys.push("warehouse");
        conditions.push("warehouse=" + warehouse.id);
    }
    conditions.push("customConditionKey=" + keys.join(","));
    var url = "/msService/material/businessDetail/businessDetail/businessDetailRef?" + conditions.join("&");
    //打开参照
    ReactAPI.createDialog("biz_detail_ref", {
        title: ReactAPI.international.getText("material.viewtitle.randon1657611447203"), //流水明细
        url: url,
        size: 5,
        callback: (data, event) => {
            biz_detail_callback(data, event)
        },
        isRef: true,
        onOk: (data, event) => {
            biz_detail_callback(data, event)
        },
        onCancel: () => {
            ReactAPI.destroyDialog("biz_detail_ref");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
    });

    var biz_detail_callback = (data, event) => {
        if (data && data.length != 0) {
            //1.校验仓库一致
            var lastWarehouse;
            var bSameWarehouse = true;
            data.forEach(rowData => {
                if (lastWarehouse && rowData.ware.id != lastWarehouse.id) {
                    bSameWarehouse = false;
                }
                lastWarehouse = rowData.ware;
            });
            if (!bSameWarehouse) {
                event.ReactAPI.showMessage('w', ReactAPI.international.getText("请选择同一仓库下的数据"));
                return false;
            }
            //查询现存量并增行
            var result = ReactAPI.request({
                type: "post",
                url: "/msService/material/cargoStock/cargoStock/getCargoStockList",
                data: data.map(rowData => new Object({
                    warehouseId: rowData.ware.id,
                    placeId: rowData.placeSet && rowData.placeSet.id,
                    materialId: rowData.good.id,
                    batchNum: rowData.batchText
                })),
                async: false
            });
            if (result.code != 200) {
                event.ReactAPI.showMessage('w', result.message);
                return false;
            }
            var stocks = result.data;
            //如果存在找不到现存量的流水则报错
            var notFoundHints = data.map((rowData, idx) => {
                if (!stocks[idx]) {
                    return "第" + (rowData.rowIndex + 1) + "行，找不到现存量！";
                } else if (!stocks[idx].isAvailable) {
                    return "第" + (rowData.rowIndex + 1) + "行，现存量不可用！";
                }
            }).filter(v => v);
            if (notFoundHints.length) {
                event.ReactAPI.showMessage('w', notFoundHints.join("<br>"));
                return false;
            }
            //查询对应其他入库单表体
            var ids = data.map(rowData => rowData.tableBodyID).filter(val => val).join(",");
            var id$srcDetail = {};
            if (ids) {
                var result = ReactAPI.request({
                    type: "get",
                    url: "/msService/material/entity/getEntityList",
                    data: {
                        moduleName: "material",
                        entityName: "MaterialInSingleDetail",
                        ids: ids,
                        includes: "id,orderNumber"
                    },
                    async: false
                });
                if (result.code != 200) {
                    event.ReactAPI.showMessage('w', result.message);
                    return false;
                }
                result.data.forEach(detail => {
                    id$srcDetail[detail.id] = detail;
                });
            }
            //增加对应行
            dgDetail.addLine(data.map((rowData, idx) => {
                var srcDetail = rowData.tableBodyID && id$srcDetail[parseInt(rowData.tableBodyID)];
                var newLine = {
                    onhand: stocks[idx],
                    good: rowData.good,
                    orderNumber: srcDetail.orderNumber
                };
                //将之前设置的属性附加上去
                datagrid.appendRowAttr(dgDetailName, newLine);
                return newLine;
            }), true);

            //回填表头货位
            var warehouse = rfWarehouse.getValue()[0];
            if (!warehouse || !warehouse.id) {
                rfWarehouse.setValue(lastWarehouse);
                warehouseBeforeChange = lastWarehouse;
                refreshPlaceEnable(lastWarehouse.storesetState);
            }
            ReactAPI.destroyDialog("biz_detail_ref");
            ReactAPI.showMessage(
                "s",
                ReactAPI.international.getText("foundation.common.tips.addsuccessfully")
            );
        } else {
            //请至少选中一行
            event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1574406106043"));
            return false;
        }
    }
}