//-----调拨单-----

//引入datagrid.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");
loader.import("material");

const dgDetailName = "material_1.0.0_appropriation_appEditdg1574661994369";
var dgDetail;

//获取是否启用wmsPro模块,并设成全局常量
var integrateWmsPro;
var inEnablePlace;
var outEnablePlace;
//是否生成任务（开启PRO并且开启货位
var generateInTask;
var generateOutTask;

var cbInEnablePlace;
var cbOutEnablePlace;

var rfOutWarehouse;
var rfInWarehouse;


var scRedBlue;

function dataInit() {
    dgDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgDetailName);
    cbInEnablePlace = ReactAPI.getComponentAPI("Checkbox")
        .APIs("appropriation.toWare.storesetState");
    cbOutEnablePlace = ReactAPI.getComponentAPI("Checkbox")
        .APIs("appropriation.fromWare.storesetState");
    scRedBlue = ReactAPI.getComponentAPI("SystemCode").APIs("appropriation.redBlue");
    rfOutWarehouse = ReactAPI.getComponentAPI("Reference").APIs("appropriation.fromWare.name");
    rfInWarehouse = ReactAPI.getComponentAPI("Reference").APIs("appropriation.toWare.name");
    integrateWmsPro = (ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.wmspro",
    })["material.wmspro"] == 'true');
    inEnablePlace = cbInEnablePlace.getValue().value;
    outEnablePlace = cbOutEnablePlace.getValue().value;
    generateInTask = (integrateWmsPro && inEnablePlace);
    generateOutTask = (integrateWmsPro && outEnablePlace);
}

function onLoad() {
    dataInit();
    //按照红蓝字显示参照按钮
    var value = ReactAPI.getComponentAPI("SystemCode").APIs("appropriation.redBlue").getValue().value;
    if (value == "BaseSet_redBlue/red") {
        ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: false });
        $("#btn-add").hide();
        $("#btn-autoAssign").hide();
    } else {
        ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: true });
        $("#btn-onhandRef").show();
    }
    if (ReactAPI.getComponentAPI("Reference").APIs("appropriation.fromWare.name").getValue().length > 0) {
        outWareId = ReactAPI.getComponentAPI("Reference").APIs("appropriation.fromWare.name").getValue()[0].id;
    }

    $("#btn-materialRef").hide();
    $("#btn-copyRow").hide();
    $("#btn-own-fzh").hide();
}

function refreshRequired() {
    //如果不生成入库任务，且启用货位，则调入货位必填
    if (!generateInTask && inEnablePlace) {
        datagrid.validator.required.setColRequired(dgDetailName, [{
            key: "inPlaceSet.name",
            type: "object"
        }]);
    } else {
        datagrid.validator.required.removeColRequired(dgDetailName, [{
            key: "inPlaceSet.name"
        }]);
    }
    //不生成出库任务，则可用量必填
    if (!generateOutTask) {
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

function refreshReadonly() {
    //如果未开启调入仓库货位，调入货位只读
    //如果红字，调入货位只读，可用量只读
    var readonly_keys = [];
    var rw_keys = [];
    var isRed = scRedBlue.getValue().value == "BaseSet_redBlue/red";
    if (!inEnablePlace || isRed) {
        readonly_keys.push("inPlaceSet.name");
    } else {
        rw_keys.push("inPlaceSet.name");
    }
    if (isRed) {
        readonly_keys.push("onhand.availiQuantity");
    } else {
        rw_keys.push("onhand.availiQuantity");
    }
    if (readonly_keys.length) {
        datagrid.clearColValue(dgDetail, readonly_keys);
        datagrid.readonly.setColReadonly(dgDetailName, readonly_keys);
    }
    if (rw_keys.length) {
        datagrid.readonly.removeColReadonly(dgDetailName, rw_keys);
    }
}



var ignoreConfirmFlag = false;
function onSave() {
    var type = ReactAPI.getOperateType();
    if ("submit" == type) {
        var dgData = dgDetail.getDatagridData();
        if (dgData.length == 0) {
            //表体不能为空！
            ReactAPI.showMessage("w", ReactAPI.international.getText("material.custom.randon1573634425210"));
            return false;
        }

        var check_result = datagrid.validator.check(dgDetailName);
        if (!check_result) {
            return false;
        }
        //校验调入调出货位不同
        var outWarehouse = rfOutWarehouse.getValue()[0];
        var inWarehouse = rfInWarehouse.getValue()[0];
        if (outWarehouse.id == inWarehouse.id) {
            //调入仓库不能与调出仓库相同！
            ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.SameAsThewarehouse"));
            return false;
        }

        //校验超储,归并同物料
        if (!ignoreConfirmFlag) {
            var checked_idx = new Set();
            for (var i = 0; i < dgData.length; i++) {
                if (!checked_idx.has(i)) {
                    var material = dgData[i].productId;
                    //统计物料数量
                    var quan = dgData[i].appliQuantity;
                    for (var j = i + 1; j < dgData.length; j++) {
                        if (!checked_idx.has(j)) {
                            if (material.id == dgData[j].productId.id) {
                                checked_idx.add(j);
                                quan += dgData[k].appliQuantity;
                            }
                        }
                    }
                    //统计完成，进行校验
                    if (!check_material_in_limit(inWarehouse, material, quan) || !check_material_out_limit(outWarehouse, material, quan)) {
                        return false;
                    }
                }
            }
        }
    }
}


function check_material_out_limit(warehouse, material, quan) {
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
                    data.DownAlarm,
                    data.Onhand,
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


function check_material_in_limit() {
    var result = ReactAPI.request({
        type: "get",
        url: "/msService/material/socketSet/socketSetInfo/findSocketSet",
        async: false,
        data: {
            wareId: String(warehouse.id),
            goodId: String(material.id),
            direction: "directionReceive",
            num: String(quan),
        },
    });
    if (result.code == 200) {
        let data = result.data;
        if (data.isNo) {
            ReactAPI.openConfirm({
                //<b>【{0}】</b>中<b>【{1}】</b>的最高库存为<b>{2}</b>，现存量<b>{3}</b>。<br><b>【{4}】</b>入库后库存数量将超过最大库存！
                message: ReactAPI.international.getText(
                    "material.custom.SocketSet.confirm",
                    warehouse.name,
                    material.name,
                    data.UpAlarm,
                    data.Onhand,
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



function ipSrcTableChanged() {
    var length = dgDetail.getDatagridData().length;
    var srcTableNo = ReactAPI.getComponentAPI("Input").APIs("appropriation.srcTableNo").getValue();
    for (var i = 0; i < length; i++) {
        if (null == srcTableNo || undefined == srcTableNo || srcTableNo == "") {
            // 若启用货位, 将表体的申请数量字段设为可编辑
            dgDetail.setDatagridCellAttr(i, "appliQuantity", {
                readonly: false
            });
        } else {
            // 若未启用货位, 将表体的申请数量字段设为只读
            dgDetail.setDatagridCellAttr(i, "appliQuantity", {
                readonly: true
            });
        }
    }
}

var warehouseBeforeChange = {};
function rfFromWareChanged(value) {
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
                    ReactAPI.getComponentAPI("Reference").APIs("appropriation.fromWare.name").setValue(warehouseBeforeChange);
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
                    ReactAPI.getComponentAPI("Reference").APIs("appropriation.fromWare.name").setValue(warehouseBeforeChange);
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

function refreshPlaceEnable(val) {
    if (val != enablePlace) {
        enablePlace = val;
        generateTask = enablePlace && integrateWmsPro;
        refreshRequired();
        refreshReadonly();
    }
}

function rfToWareChanged(value) {
    //清空货位
    datagrid.clearColValue(dgDetail, "inPlaceSet");
    //刷新表体
    if (value && value[0]) {
        var enablePlace = value[0].storesetState;
        if (enablePlace != inEnablePlace) {
            inEnablePlace = enablePlace;
            generateInTask = (integrateWmsPro && inEnablePlace);
            refreshRequired();
            refreshReadonly();
        }
    }
}


function scRedBlueChanged(value) {
    var befor = ReactAPI.getComponentAPI("SystemCode").APIs("appropriation.redBlue").getValue().value;
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
                    $("#btn-add").hide();
                    $("#btn-autoAssign").hide();
                    refreshReadonly();
                    return false;
                },
                onCancel: () => {
                    ReactAPI.getComponentAPI("SystemCode").APIs("appropriation.redBlue").setValue(befor);
                    ReactAPI.closeConfirm();
                    return false;
                }
            });
        } else {
            ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: false });
            $("#btn-add").hide();
            $("#btn-autoAssign").hide();
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
                    $("#btn-add").show();
                    $("#btn-autoAssign").show();
                    ReactAPI.getComponentAPI("Reference").APIs("appropriation.fromWare.name").setReadonly(false);
                    ReactAPI.getComponentAPI("Reference").APIs("appropriation.toWare.name").setReadonly(false);
                    return false;
                },
                onCancel: () => {
                    ReactAPI.getComponentAPI("SystemCode").APIs("appropriation.redBlue").setValue(befor);
                    ReactAPI.closeConfirm();
                    return false;
                }
            });
        } else {
            ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: true });
            $("#btn-add").show();
            $("#btn-autoAssign").show();
            ReactAPI.getComponentAPI("Reference").APIs("appropriation.fromWare.name").setReadonly(false);
            ReactAPI.getComponentAPI("Reference").APIs("appropriation.toWare.name").setReadonly(false);
        }
    }
}



function ptRenderOver() {
    refreshReadonly();
    var ware = ReactAPI.getComponentAPI("Reference").APIs("appropriation.toWare.name").getValue();
    var totalRowLength = dgDetail.getDatagridData().length;
    if (ware.length > 0 && ware[0].storesetState) {
        // 选择了调入仓库 并且 调入仓库启用了货位, 将调入货位设置可编辑
        for (var i = 0; i < totalRowLength; i++) {
            dgDetail.setDatagridCellAttr(i, "inPlaceSet.name", {
                readonly: false
            });
        }
    } else {
        // 未选择调入仓库 或 调入仓库未启用货位, 将调入货位设置只读
        for (var i = 0; i < totalRowLength; i++) {
            dgDetail.setDatagridCellAttr(i, "inPlaceSet.name", {
                readonly: true
            });
        }
    };

    var value = ReactAPI.getComponentAPI("SystemCode").APIs("appropriation.redBlue").getValue().value;
    if (value == "BaseSet_redBlue/red") {
        var length = dgDetail.getDatagridData().length;
        for (var i = 0; i < length; i++) {
            dgDetail.setDatagridCellAttr(i, "onhand.batchText", { readonly: true });
            dgDetail.setDatagridCellAttr(i, "inPlaceSet.name", { readonly: true });
            dgDetail.setDatagridCellAttr(i, "productId.code", { readonly: true });
        }
        ReactAPI.getComponentAPI("Reference").APIs("appropriation.fromWare.name").setReadonly(true);
        ReactAPI.getComponentAPI("Reference").APIs("appropriation.toWare.name").setReadonly(true);
    }
}


function ptInit() {
    // 设置复制图标样式
    $("#btn-copyRow i").attr("class", "sup-btn-icon sup-btn-own-fzh");
    refreshRequired();
    //设置校验
    datagrid.validator.add(dgDetailName, "appliQuantity", rowData => {
        //按件时
        if (rowData.productId && rowData.productId.isBatch && rowData.productId.isBatch.id == "BaseSet_isBatch/piece") {
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
                "[" + rowData.productId.name + "/" + rowData.onhand.batchText + "]"
            );
        } else {
            //物品批号{0}已开启按件管理，数量只能为整数。
            return ReactAPI.international.getText(
                "material.validator.by_piece_int_quantity_check",
                "[" + rowData.productId.name + "]"
            );
        }
    });
}



function ptBtnStandingCrop() {
    var ware = ReactAPI.getComponentAPI("Reference").APIs("appropriation.fromWare.name").getValue();
    var url;
    if (ware.length > 0) {
        url = "/msService/material/standingcrop/standingcrop/onhandRef?customConditionKey=wareCode&wareCode=" + ware[0].code;
    } else {
        url = "/msService/material/standingcrop/standingcrop/onhandRef?customConditionKey=wareCode&wareCode=";
    }

    ReactAPI.createDialog("newDialog", {
        title: ReactAPI.international.getText("material.buttonPropertyshowName.randon1573694108459.flag"), // 现存量参照
        url: url,
        size: 5,
        callback: (data, event) => {
            callback(data, event);
        },
        isRef: true, // 是否开启参照
        onOk: (data, event) => {
            callback(data, event);
        },
        onCancel: (data, event) => {
            ReactAPI.destroyDialog("newDialog");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.cancel") // 取消
    });

    // 回调函数
    var callback = (data, event) => {
        // 参照之前的行数
        var dataGridLength = dgDetail.getDatagridData().length;
        debugger

        // 组织已经存在的行, 用于后续判断
        var existed = {};
        for (let i = 0; i < dataGridLength; i++) {
            // 现存量表ID
            var standingCropID = dgDetail.getValueByKey(i, 'onhand.id');
            existed[standingCropID] = true;
        }

        // 对PT进行赋值
        for (let i = 0; i < data.length; i++) {
            const item = data[i];
            // 第<b>{0}</b>行重复参照
            if (existed[item.id]) {
                ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1578980024977", "" + (Number(item.rowIndex) + 1)));
                return false;
            }

            var index = dataGridLength + i;
            dgDetail.addLine();
            // 现存量对象
            dgDetail.setValueByKey(index, 'onhand', item);
            // 单位
            dgDetail.setValueByKey(index, 'onhand.good.mainUnit.name', item.goodUnit);
            //有效期
            dgDetail.setValueByKey(index, 'validDate', item.materBatchInfo.validDate);
            //近效期
            dgDetail.setValueByKey(index, 'onhand.approchTime', item.materBatchInfo.approchTime);

            dgDetail.setValueByKey(index, 'productId', {
                id: item.good.id,
                code: item.good.code,
                name: item.good.name,
                specifications: item.good.specifications,
                model: item.good.model,
                storeUnit: { name: item.goodUnit }
            });

            //仓库
            dgDetail.setValueByKey(index, 'onhand.ware.id', item.ware.id);
            ReactAPI.getComponentAPI("Reference").APIs("appropriation.fromWare.name").setValue(item.ware);
        }
        // 关闭窗口
        ReactAPI.destroyDialog("newDialog");
    };
}


function ptBtnDeleteLine() {
    if (undefined == dgDetail.getSelecteds()[0]) {
        //请至少选择一条数据！
        ReactAPI.showMessage('w', ReactAPI.international.getText("ec.ec_view_select_property.selectNullData"));
        return false;
    }

    var selectedrow = dgDetail.getSelecteds();
    var toDeleteIndex = "";
    for (var i = 0; i < selectedrow.length; i++) {
        //拼接要删除的行
        toDeleteIndex += "," + selectedrow[i].rowIndex;
    }
    toDeleteIndex = toDeleteIndex.substr(1);
    dgDetail.deleteLine(toDeleteIndex);

}

function ptBtnCopyRow() {
    // 选中行对象
    var row = dgDetail.getSelecteds();
    if (row.length == 0) {
        // 请选择一条记录进行操作
        ReactAPI.showMessage('w', ReactAPI.international.getText("SupDatagrid.button.error"));
        return;
    }
    for (var i = 0; i < row.length; i++) {
        // 新增一行
        dgDetail.addLine();
        // 新增之后的总行数(从0开始)
        var length = dgDetail.getDatagridData().length - 1;
        // 获取新增行对象
        var newRow = dgDetail.getRows("" + length);
        // 记录新增行的key属性
        var rowKey = newRow[0].key;

        /**
         * 拷贝对象
         * 这里不能直接引用对象(即rowObj=row[0]), 
         * 若直接引用, 下面移除id属性等操作会影响到原对象: 
         * 会导致将原对象的id属性被移除, 那么原对象被视为新增数据, 
         * 而被复制的那一行又没有被删除, 从而导致保存后多出来一条重复数据
         */
        var rowObj = $.extend({}, row[i]);
        rowObj.rowIndex = length;
        rowObj.key = rowKey;
        rowObj.isChecked = true;

        // 移除不必要的属性
        delete rowObj.id;
        delete rowObj.version;
        delete rowObj.sort;
        delete rowObj.currClickColKey;
        delete rowObj.edited;

        // 取消选中被复制行
        dgDetail.setRowData(row[i].rowIndex, {
            isChecked: false
        });

        // 整行赋值
        dgDetail.setRowData(length, rowObj);
    }
}


function ptRfOnhandChanged(value, rowIndex) {
    if (value && value[0]) {
        dgDetail.setRowData(rowIndex, {
            outPlaceSet: value[0].placeSet
        });
    } else {
        dgDetail.setRowData(rowIndex, {
            outPlaceSet: null
        });
    }
}


function ptScMaterialChanged() {
    clearOnhand();
    var rowIndex = dgDetail.getSelecteds()[0].rowIndex;
    var storesetState = ReactAPI.getComponentAPI("Reference").APIs("appropriation.toWare.name").getValue()[0].storesetState;
    if (!storesetState) {
        dgDetail.setDatagridCellAttr(rowIndex, "inPlaceSet.name", {
            readonly: true
        });
    }
}

function ptRfGoodChanged(value, rowIndex) {
    dgDetail.setRowData(rowIndex, { onhand: null, outPlaceSet: null });
}

function ptnBtnRefGoods() {
    var warehouse = rfOutWarehouse.getValue()[0];
    if (!warehouse || !warehouse.id) {
        // 请先选择仓库
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.random1632472693120"));
        return false;
    }

    var cb = function (data, event) {
        if (data && data[0]) {
            dgDetail.addLine(data.map(material => {
                var newLine = {
                    productId: material
                };
                //将之前的属性附加上去
                datagrid.appendRowAttr(dgDetailName, newLine);
                return newLine;
            }), true);
        }
        ReactAPI.destroyDialog("materialRefLayout");
    }

    var url = "/msService/BaseSet/material/material/materialRefLayout?multiSelect=true";
    url = material.getMaterialsByWare(rfOutWarehouse, url);
    if(!url){
        return;
    }

    ReactAPI.createDialog("materialRefLayout", {
        title: ReactAPI.international.getText("material.buttonPropertyshowName.randon1592465499893.flag"), //物料参照
        url: url,
        size: 5,
        callback: (data, event) => {
            cb(data, event);
        },
        isRef: true, // 是否开启参照
        onOk: (data, event) => {
            cb(data, event);
        },
        onCancel: (data, event) => {
            ReactAPI.destroyDialog("materialRefLayout");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
    });
}

var outWareId, inWareId;

function clearOnhand() {
    var rowIndex = dgDetail.getSelecteds()[0].rowIndex
    dgDetail.setValueByKey(rowIndex, 'onhand', {
        id: null,
        placeSet: {
            id: null,
            name: null
        },
        batchText: null,
        approchTime: null,
        onhand: null,
        availiQuantity: null
    });
}


function ptnBtnRefGoodsEntry() {
    ptnBtnRefGoods();
}

function btnRedRef() {
    ReactAPI.createDialog("newDialog", {
        title: ReactAPI.international.getText("material.custom.RedInkOffsetReference"), //红字冲销参照
        url: "/msService/material/saleOut/saleOutDetail/saleRedRef",
        size: 5,
        callback: (data, event) => {
            partCallback(data, event);
        },
        isRef: true, // 是否开启参照
        onOk: (data, event) => {
            partCallback(data, event);
        },
        onCancel: (data, event) => {
            ReactAPI.destroyDialog("newDialog");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
    });

    var partCallback = (data, event) => {
        if (data != null && data.length != 0) {
            var dg = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_saleOut_saleOutEditdg1574227484051");
            var dgData = dg.getDatagridData();
            for (var i = 0; i < data.length; i++) {
                var id = data[i].id;
                for (var j = 0; j < dgData.length; j++) {
                    var partId = dg.getValueByKey(j, "partId");
                    if (id == partId) {
                        //校验重复 
                        ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.CannotBeReferencedRepeatedly", "" + (i + 1)));
                        return false;
                    }
                }
                var wareCode = data[i].outSingle.ware.code;
                for (var t = 0; t < data.length; t++) {
                    var wareCodet = data[t].outSingle.ware.code;
                    if (wareCodet != wareCode) {
                        //所选仓库不同，无法同时参照！ 	 
                        ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.wareDifferent", "" + (t + 1), "" + (i + 1)));
                        return false;
                    }
                }
                var ware = ReactAPI.getComponentAPI("Reference").APIs("saleOutSingle.ware.code").getValue()[0];
                if (undefined != ware && wareCode != ware.code) {
                    //所选仓库不同，无法同时参照！
                    ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.wareisNotDifferent"));
                    return false;
                }
            }
            for (var j = 0; j < data.length; j++) {
                dg.addLine();
                var rowIndex = dg.getDatagridData().length - 1;
                dg.setValueByKey(rowIndex, "good", data[j].good);
                dg.setValueByKey(rowIndex, "redPartID", data[j].id);
                dg.setValueByKey(rowIndex, "onhand", data[j].onhand);
                dg.setValueByKey(rowIndex, "outQuantity", data[j].outQuantity - data[j].redNum);
                dg.setValueByKey(rowIndex, "appliQuanlity", data[j].outQuantity - data[j].redNum);
                dg.setValueByKey(rowIndex, "customer", data[j].customer);
                dg.setValueByKey(rowIndex, "srcTableNo", data[j].srcTableNo);
                dg.setValueByKey(rowIndex, "onhand.availiQuantity", data[j].onhand.availiQuantity);
                dg.setDatagridCellAttr(rowIndex, "onhand.batchText", { readonly: true });
            }
            ReactAPI.getComponentAPI("Reference").APIs("saleOutSingle.ware.name").setValue(data[0].outSingle.ware);
            ReactAPI.getComponentAPI("Reference").APIs("saleOutSingle.outPerson.name").setValue(data[0].outSingle.outPerson);
            ReactAPI.getComponentAPI("Reference").APIs("saleOutSingle.outDept.name").setValue(data[0].outSingle.outDept);
            ReactAPI.showMessage('s', ReactAPI.international.getText("foundation.common.tips.addsuccessfully"));
        } else {
            //请至少选中一行
            ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1574406106043"));
            return false;
        }
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
            ReactAPI.getComponentAPI("Reference").APIs("appropriation.fromWare.name").removeValue();
            // 清除表头仓库时清空表体
            dgDetail.deleteLine();
            return false;
        },
        onCancel: () => {
            ReactAPI.getComponentAPI("Reference").APIs("appropriation.fromWare.name").setValue(deleteObj[0]);
            ReactAPI.closeConfirm();
            return false;
        }
    });
    ReactAPI.getComponentAPI("Reference").APIs("appropriation.fromWare.name").setValue(deleteObj[0]);
    return false;
}