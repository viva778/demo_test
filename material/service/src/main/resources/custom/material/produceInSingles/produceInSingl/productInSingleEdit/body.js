//-----生产入库-----

//引入datagrid.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");

const dgDetailName = "material_1.0.0_produceInSingles_productInSingleEditdg1573804983627";
var dgDetail;
var rfWareCode;
var rfWareName;
var ipTaskReportNo;
var ipDirectiveNo;
var scRedBlue;
var rfDeptName;
var cbStoreState;
var rfServiceTypeExplain;
var rfInComeReason;
var generateTask;
var wareCache;

function onLoad() {
    dataInit();
    //业务类型赋值
    rfServiceTypeExplain.setValue({
        serviceTypeCode: "produceStorageIn",
        id: 1005,
        serviceTypeExplain: ReactAPI.international.getText("material.custom.ProductionWarehousingTransaction")
    });
    var inCome = rfInComeReason.getValue()[0];
    if (!inCome) {
        rfInComeReason.setValue({
            id: 1014,
            reasonExplain: ReactAPI.international.getText("material.custom.ProductionWarehousing")
        });
    }
    //红蓝字设置
    var isRed = scRedBlue.getValue().value == "BaseSet_redBlue/red";
    ReactAPI.setHeadBtnAttr('redRef', {icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: !isRed});
    isRed ? $("#btn-addLine").hide() : $("#btn-addLine").show();

    //添加超链接
    var taskReportNo = ipTaskReportNo.getValue();
    ipTaskReportNo.replace("<div id='link' class='supplant-readonly-wrap supplant-comp readonly'>" + taskReportNo + "</div>", {
        onClick: function (e) {
            onclickTaskReportNo(taskReportNo);
        },
    });
    var directiveNo = ipDirectiveNo.getValue();
    ipDirectiveNo.replace("<div id='link1'  class='supplant-readonly-wrap supplant-comp readonly'>" + directiveNo + "</div>", {
        onClick: function (e) {
            onclickDirectiveNo(directiveNo);
        },
    });
    //设置仓库缓存
    wareCache = rfWareName.getValue()
}

var ignoreConfirmFlag = false;

function onSave() {
    if ("submit" == ReactAPI.getOperateType()) {
        var dgData = dgDetail.getDatagridData();

        if (!dgData.length) {
            ReactAPI.showMessage(
                "w",
                ReactAPI.international.getText("material.custom.randon1573634425210")
            ); //	表体数据不能为空！
            return false;
        }

        var check_result = datagrid.validator.check(dgDetailName);
        if (!check_result) {
            return false;
        }

        var warehouse = rfWareName.getValue()[0];

        //校验超储,归并同物料
        if (!ignoreConfirmFlag) {
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
                    String(data.UpAlarm),
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

function dataInit() {
    cbEnablePlace = ReactAPI.getComponentAPI("Checkbox")
        .APIs("produceInSingl.ware.storesetState");
    enablePlace = cbEnablePlace.getValue().value;
    integrateWmsPro = (ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.wmspro",
    })["material.wmspro"] == 'true');
    generateTask = (integrateWmsPro && enablePlace);

    dgDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgDetailName);
    rfWareCode = ReactAPI.getComponentAPI("Reference").APIs("produceInSingl.ware.code");
    rfWareName = ReactAPI.getComponentAPI("Reference").APIs("produceInSingl.ware.name");
    rfDeptName = ReactAPI.getComponentAPI("Reference").APIs("produceInSingl.dept.name");
    rfInComeReason = ReactAPI.getComponentAPI("Reference").APIs("produceInSingl.inCome.reasonExplain");
    rfServiceTypeExplain = ReactAPI.getComponentAPI("Reference").APIs("produceInSingl.serviceTypeID.serviceTypeExplain");
    ipTaskReportNo = ReactAPI.getComponentAPI("Input").APIs("produceInSingl.taskReportNo");
    ipDirectiveNo = ReactAPI.getComponentAPI("Input").APIs("produceInSingl.directiveNo");
    scRedBlue = ReactAPI.getComponentAPI("SystemCode").APIs("produceInSingl.redBlue");
    cbStoreState = ReactAPI.getComponentAPI("Checkbox").APIs("produceInSingl.ware.storesetState");
    dataInit = () => {
    }
}


/**
 * 参照红字冲销
 */
function btnRefRed() {
    //打开参照
    ReactAPI.createDialog("refRed", {
        title: ReactAPI.international.getText("material.custom.RedInkOffsetReference"),
        url: "/msService/material/produceInSingles/produceInDetai/productInPartRef",
        size: 5,
        callback: (data, event) => {
            partCallback(data, event);
        },
        isRef: true, // 是否开启参照
        onOk: (data, event) => {
            partCallback(data, event);
        },
        onCancel: () => {
            ReactAPI.destroyDialog("refRed");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
    });

    //确定回调
    var partCallback = (selRows, event) => {
        if (selRows && selRows[0]) {
            var firstRow = selRows[0];
            var dgData = dgDetail.getDatagridData();

            //1.校验所选仓库是否一致
            var ware = rfWareCode.getValue() && rfWareCode.getValue()[0];
            var headWareCode = ware && ware.code;
            if (headWareCode) {
                //表头仓库存在，根据表头校验
                var selWareCodes = selRows.map(selData => selData.inSingle && selData.inSingle.ware && selData.inSingle.ware.code).filter(code => code);
                if (selWareCodes.find(code => code != headWareCode) !== undefined) {
                    //所选仓库不同，无法同时参照！
                    event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.wareisNotDifferent"));
                    return false;
                }
            } else {
                //表头不存在，两两校验
                var bodyWareCode = firstRow.inSingle && firstRow.inSingle.ware && firstRow.inSingle.ware.code;
                for (let i = 1; i < selRows.length; i++) {
                    var row = selRows[i];
                    var wareCode = row.inSingle && row.inSingle.ware && row.inSingle.ware.code;
                    if (wareCode != bodyWareCode) {
                        //所选仓库不同，无法同时参照！
                        event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.wareDifferent", "" + i, "" + (i + 1)));
                        return false;
                    }
                }
            }


            //2.校验id重复
            var includeIds = new Set(dgData.map(selData => selData.partId).filter(id => id));
            var repeatData = selRows.find(selData => includeIds.has(selData.id));
            if (repeatData) {
                event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.CannotBeReferencedRepeatedly", "" + (repeatData.rowIndex + 1)));
                return false;
            }

            //3.将数据添加至表体
            selRows.forEach(selData => {
                var rowIndex = dgDetail.addLine().rowIndex;
                dgDetail.setRowData(rowIndex, {
                    good: selData.good,
                    redPartID: selData.id,
                    batchText: selData.batchText,
                    inQuantity: selData.inQuantity - selData.redNumber,
                    appliQuanlity: selData.inQuantity - selData.redNumber,
                    placeSet: selData.placeSet,
                    "good.name_attr": {readonly: true},

                    "placeSet.name_attr": {readonly: true}
                });
            });
            //4.若表头仓库不存在，回填仓库
            if (!headWareCode) {
                rfWareName.setReadonly(true);
                rfWareName.setValue(firstRow.inSingle.ware);
            }


            //5.添加超链接
            var taskReportNo = firstRow.inSingle.taskReportNo;
            if (taskReportNo) {
                ipTaskReportNo.setValue(taskReportNo);
                ipTaskReportNo.replace("<div class='supplant-readonly-wrap supplant-comp readonly'>" + taskReportNo + "</div>", {
                    onClick: function (e) {
                        onclickTaskReportNo(taskReportNo);
                    },
                });
            }
            var directiveNo = firstRow.inSingle.directiveNo;
            if (directiveNo) {
                ipDirectiveNo.setValue(directiveNo);
                ipDirectiveNo.replace("<div class='supplant-readonly-wrap supplant-comp readonly'>" + directiveNo + "</div>", {
                    onClick: function (e) {
                        onclickDirectiveNo(directiveNo);
                    },
                });
            }
            ReactAPI.showMessage('s', ReactAPI.international.getText("foundation.common.tips.addsuccessfully"));
        } else {
            //请至少选中一行
            event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1574406106043"));
            return false;
        }
    }
}

function ocgWarehouse(value) {
    function wareChange() {
        wareCache = value
        var cargoEnabled = value[0].storesetState;
        if (cargoEnabled != enablePlace) {
            enablePlace = cargoEnabled;
            generateTask = (integrateWmsPro && enablePlace);
            refreshRequired();
            refreshReadonly();
        }
    }
    if (value && value[0]) {
        var length = dgDetail.getDatagridData().length;
        if (length > 0) {
            ReactAPI.openConfirm({
                //"修改后将清空表体数据，是否继续？",
                message: ReactAPI.international.getText("material.custom.randonAfterModifyBody"),
                okText: ReactAPI.international.getText("attendence.attStaff.isInstitutionYes"),//是
                cancelText: ReactAPI.international.getText("attendence.attStaff.isInstitutionNo"),//否
                onOk: () => {
                    ReactAPI.closeConfirm();
                    // 清除表头供应商时清空表体
                    dgDetail.deleteLine();
                    wareChange()
                },
                onCancel: () => {
                    ReactAPI.closeConfirm();
                    if (wareCache && wareCache[0]) {
                        rfWareName.setValue(wareCache[0])
                    }else {
                        rfWareName.setValue({})
                    }
                    return false;
                }
            });
        } else {
            wareChange()
        }

    }
}


/**
 * 红蓝字on change
 */
function ocgRedBlue(value) {
    var prvValue = scRedBlue.getValue().value;
    var isRed = (value == "BaseSet_redBlue/red");

    const setBtn = function () {
        ReactAPI.setHeadBtnAttr('redRef', {icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: !isRed});
        if (isRed) {
            $("#btn-addLine").hide();
        } else {
            $("#btn-addLine").show();
            rfWareCode.setReadonly(false);
        }
        refreshReadonly();
    }


    if (dgDetail.getDatagridData().length != 0) {
        ReactAPI.openConfirm({
            //切换红蓝字会同时清空表体！
            message: ReactAPI.international.getText("material.custom.clearTheTableBodyAtTheSameTime"),
            //确定
            okText: ReactAPI.international.getText("ec.common.confirm"),
            //取消
            cancelText: ReactAPI.international.getText("foundation.signature.cancel"),
            onOk: () => {
                ReactAPI.closeConfirm();
                // 清空表体
                dgDetail.deleteLine();
                setBtn();
                return false;
            },
            onCancel: () => {
                //撤销变化
                scRedBlue.setValue(prvValue);
                ReactAPI.closeConfirm();
                return false;
            }
        });
    } else {
        setBtn();
    }

}


/**
 * 入库人 callback
 */
function cbStaff(value) {
    var dept = value && value[0] && value[0].department;
    if (dept) {
        rfDeptName.setValue({
            id: dept.id,
            name: dept.name
        });
    }
}

/**
 * 入库人 after clear
 */
function oacStaff() {
    rfDeptName.removeValue();
}


function ptInit() {
    dataInit();
    dgDetail.setBtnImg("btn-goodRef", "sup-btn-eighteen-dt-op-reference");

    refreshRequired();
    datagrid.validator.add(dgDetailName, "appliQuanlity", rowData => {
        //物品启用按件管理，入库数量只能为1件
        var batchType = rowData.good && rowData.good.isBatch && rowData.good.isBatch.id;
        if (batchType == "BaseSet_isBatch/piece") {
            return rowData.appliQuanlity == 1;
        } else {
            return true;
        }
    }, rowIndex => {
        return ReactAPI.international.getText(
            "material.custom.can.only.beOne",
            String(rowIndex)
        );
    });

    //设置批号条件必填
    datagrid.validator.required.setRequiredByCondition(dgDetailName, [{key: "batchText", type: "plain"}], rowData => {
        var batchType = rowData.good && rowData.good.isBatch && rowData.good.isBatch.id;
        return batchType == "BaseSet_isBatch/batch" || batchType == "BaseSet_isBatch/piece";
    });
}

function ptRenderOver() {
    refreshReadonly();
    //设置批号只读条件（由于是固定的，所以不需要反复刷新
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "batchText", rowData => {
        var batchType = rowData.good && rowData.good.isBatch && rowData.good.isBatch.id;
        return batchType != "BaseSet_isBatch/batch" && batchType != "BaseSet_isBatch/piece";
    });
}


/**
 * pt删行按钮
 */
function ptBtnDeleteLine() {
    var selRows = dgDetail.getSelecteds();
    if (selRows && selRows[0]) {
        var deleteRowIndexs = selRows.map(selData => selData.rowIndex).join(",");
        dgDetail.deleteLine(deleteRowIndexs);
        //内容清空时则取消只读
        if (0 == dgDetail.getDatagridData().length) {
            rfWareCode.setReadonly(false);
        }
    } else {
        //请至少选择一条数据！
        ReactAPI.showMessage('w', ReactAPI.international.getText("ec.ec_view_select_property.selectNullData"));
    }
}


//查询仓库下的库存对应的所有物料
function getMaterialsByWare() {
    let warehouseArr = rfWareName.getValue();
    if (warehouseArr.length > 0) {
        var warehouseId = warehouseArr[0].id;
        var response = ReactAPI.request({
            type: "get",
            url: "/msService/material/wareModel/findGoodIdByWare",
            async: false,
            data: {
                wareId: warehouseId
            },
        });
        if (response.code === 200) {
            let data = response.data;
            if (data && data.length > 0) {
                return data;
            } else {
                return null;
            }
        } else {
            ReactAPI.showMessage("w", result.message);
        }
    }
    return false;
}


/**
 * pt参照按钮
 */
function ptBtnGoodRef() {
    var url = "/msService/BaseSet/material/material/materialRefLayout?multiSelect=true";
    let materialIds = getMaterialsByWare()
    if (materialIds && materialIds.length > 0) {
        url += "&cappMaterialIds=" + materialIds + "&customConditionKey=cappMaterialIds"
    }
    ReactAPI.createDialog("newDialog", {
        title: ReactAPI.international.getText(
            "BaseSet.viewtitle.randon1569570764419"
        ), //物料参照
        url: url,
        size: 5,
        callback: (data, event) => {
            material_callback(data, event);
        },
        isRef: true, // 是否开启参照
        onOk: (data, event) => {
            material_callback(data, event);
        },
        onCancel: () => {
            ReactAPI.destroyDialog("newDialog");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close"), // 关闭
    });

    var material_callback = (data, event) => {
        if (data && data.length) {
            dgDetail.addLine(data.map(material => {
                var newLine = {
                    good: material,
                    genPrintInfo: true
                };
                //将之前的属性附加上去
                datagrid.appendRowAttr(dgDetailName, newLine);
                return newLine;
            }), true);
        } else {
            //请至少选中一行
            event.ReactAPI.showMessage(
                "w",
                ReactAPI.international.getText("material.custom.randon1574406106043")
            );
            return false;
        }
        ReactAPI.destroyDialog("newDialog");
        ReactAPI.showMessage(
            "s",
            ReactAPI.international.getText("foundation.common.tips.addsuccessfully")
        );
    };
}


function onclickDirectiveNo(tableNo) {
    var tableInfo = "";
    var id = '';
    if (tableNo) {
        $.ajax({
            url: "/msService/material/foreign/foreign/findTableInfoId",
            type: "GET",
            async: false,
            data: {
                tableNo: tableNo,
                deal: "directiveNo"
            },
            success: function (res) {
                var data = res.data;
                if (res != null && data && data.tableInfoId) {
                    tableInfo = data.tableInfoId;
                    id = data.tableId;
                }
            }

        });
    } else {
        return false;
    }

    if (tableInfo) {
        // 实体的entityCode
        var entityCode = "WOM_1.0.0_produceTask";
        // 实体的查看视图URL
        var url = "/msService/WOM/produceTask/produceTask/makeTaskView";
        // 菜单操作编码
        var operateCode = "WOM_1.0.0_produceTask_makeTaskList_self";
        var pcMap = ReactAPI.getPowerCode(operateCode);
        var pc = pcMap[operateCode];
        url += "?tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + id + "&__pc__=" + pc;
        window.open(url);
    }
}


function onclickTaskReportNo(tableNo) {
    var tableInfo = "";
    var id = '';
    if (tableNo) {
        $.ajax({
            url: "/msService/material/foreign/foreign/findTableInfoId",
            type: "GET",
            async: false,
            data: {
                tableNo: tableNo,
                deal: "taskReportNo"
            },
            success: function (res) {
                var data = res.data;
                if (res != null && data && data.tableInfoId) {
                    tableInfo = data.tableInfoId;
                    id = data.tableId;
                }
            }

        });
    } else {
        return false;
    }

    if (tableInfo) {

        // 实体的entityCode
        var entityCode = "WOM_1.0.0_produceTask";
        // 实体的查看视图URL
        var url = "/msService/WOM/produceTask/produceTask/makeTaskView";
        // 菜单操作编码
        var operateCode = "WOM_1.0.0_produceTask_makeTaskList_self";
        var pcMap = ReactAPI.getPowerCode(operateCode);
        var pc = pcMap[operateCode];
        url += "?tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + id + "&__pc__=" + pc;
        window.open(url);
    }
}


function refreshReadonly() {
    //如果未开启货位，货位只读
    //如果红字，货位只读，批号只读
    var readonly_keys = [];
    var rw_keys = [];
    var isRed = scRedBlue.getValue().value == "BaseSet_redBlue/red";
    if (!enablePlace || isRed) {
        readonly_keys.push("placeSet.name");
    } else {
        rw_keys.push("placeSet.name");
    }
    if (isRed) {
        readonly_keys.push("batchText");
    } else {
        rw_keys.push("batchText");
    }
    if (readonly_keys.length) {
        datagrid.clearColValue(dgDetail, readonly_keys);
        datagrid.readonly.setColReadonly(dgDetailName, readonly_keys);
    }
    if (rw_keys.length) {
        datagrid.readonly.removeColReadonly(dgDetailName, rw_keys);
    }
}

/**
 * 刷新字段必填、只读
 */
function refreshRequired() {
    //如果不生成入库任务，且启用货位，则货位必填
    if (!generateTask && enablePlace) {
        datagrid.validator.required.setColRequired(dgDetailName, [{
            key: "placeSet.name",
            type: "object"
        }]);
    } else {
        datagrid.validator.required.removeColRequired(dgDetailName, [{
            key: "placeSet.name"
        }]);
    }
}

/**
 * 清除表头仓库
 * @param afterClearWare
 * @returns {boolean}
 */
function afterClearWare(value) {
    var length = dgDetail.getDatagridData().length;
    if (length == 0) {
        //清空仓库缓存
        wareCache = null;
        return true;
    }
    var id = null;
    var name = null;
    var code = null;
    if (undefined != rfWareName.getValue()[0]) {
        id = rfWareName.getValue()[0].id;
        name = rfWareName.getValue()[0].name;
        code = rfWareName.getValue()[0].code;
    }
    // 清除仓库后一并清除表体的货位
    ReactAPI.openConfirm({
        //"清除后将清空表体数据，是否继续？",
        message: ReactAPI.international.getText("material.custom.randonAfterclearingBody"),
        okText: ReactAPI.international.getText("attendence.attStaff.isInstitutionYes"), //是
        cancelText: ReactAPI.international.getText("attendence.attStaff.isInstitutionNo"), //否
        onOk: function onOk() {
            ReactAPI.closeConfirm();
            // 清除表头货位
            rfWareName.removeValue();
            // 清除表头供应商时清空表体
            dgDetail.deleteLine();
            //清空仓库缓存
            wareCache = null;
            return true;
        },
        onCancel: function onCancel() {
            ReactAPI.closeConfirm();
            if (null != id) {
                rfWareName.setValue({
                    code: code,
                    id: id,
                    name: name
                });
            }
            return false;
        }
    });
    return false;
}


