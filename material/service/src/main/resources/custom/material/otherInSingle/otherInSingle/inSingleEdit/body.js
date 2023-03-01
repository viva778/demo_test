//-----其他入库-----
//引入datagrid.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");

//获取是否启用wmsPro模块,并设成全局常量
var integrateWmsPro;
var enablePlace;
//是否生成任务（开启PRO并且开启货位
var generateTask;

//定义“其他入库单编辑”页面上面的表格对象，全局共用（在ptInit中初始化）
var dgDetail;
var cbEnablePlace;
var cbCheckRequire;

var rfServiceType;
var rfReasonExplain;
var rfWarehouse;
var scRedBlue;
var wareCache;
// 供应商
var vendorRf
var vendorId
var vendorCache;
// 供应商下对应供应商物料
var vendorMaterials = []
var vendorMaterialsMap = {}

//发起请检
var checkOption;

const dgDetailName = "material_1.0.0_otherInSingle_inSingleEditdg1572930875635";

function dataInit() {
    cbEnablePlace = ReactAPI.getComponentAPI("Checkbox")
        .APIs("otherInSingle.ware.storesetState");
    enablePlace = cbEnablePlace.getValue().value;
    integrateWmsPro = (ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.wmspro",
    })["material.wmspro"] == 'true');
    generateTask = (integrateWmsPro && enablePlace);

    rfWarehouse = ReactAPI.getComponentAPI("Reference")
        .APIs("otherInSingle.ware.name");

    rfServiceType = ReactAPI.getComponentAPI("Reference")
        .APIs("otherInSingle.serviceTypeID.serviceTypeExplain");

    rfReasonExplain = ReactAPI.getComponentAPI("Reference")
        .APIs("otherInSingle.inCome.reasonExplain");
    scRedBlue = ReactAPI.getComponentAPI("SystemCode")
        .APIs("otherInSingle.redBlue");

    dgDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs(
        dgDetailName
    );
    vendorRf = ReactAPI.getComponentAPI("Reference")
        .APIs("otherInSingle.vendor.name");
    cbCheckRequire = ReactAPI.getComponentAPI().Checkbox.APIs("otherInSingle.inspectRequired");
    checkOption = ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.otherInCheckOptions",
    })["material.otherInCheckOptions"];
    //设置仓库缓存
    wareCache = rfWarehouse.getValue()
    vendorCache = vendorRf.getValue()
    dataInit = () => {
    };
}


/**
 * 其他入库单编辑界面onLoad脚本,初始化部分默认值以及根据红蓝字设置功能按钮是否可用
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
 */
function onLoad() {
    dataInit();
    var serviceType = {
        serviceTypeCode: "otherStorageIn",
        id: 1000,
        serviceTypeExplain: ReactAPI.international.getText(
            "material.custom.OtherWarehousingTransactions"
        ),
    };
    var reasonExplain = {
        id: 1025,
        reasonExplain: ReactAPI.international.getText(
            "material.custom.ConventionalMaterials"
        ),
    };
    //根据配置项设置是否需要请检
    cbCheckRequire.setValue(checkOption && checkOption != "no");

    //业务类型赋值
    rfServiceType.setValue(serviceType);

    var inCome = rfReasonExplain.getValue();
    if (!inCome[0]) {
        rfReasonExplain.setValue(reasonExplain);
    }

    var redBlueValue = scRedBlue.getValue().value;

    //根据红蓝字设置隐藏和显示功能按钮
    if (redBlueValue == "BaseSet_redBlue/red") {
        ReactAPI.setHeadBtnAttr("redRef", {
            icon: "sup-btn-icon sup-btn-eighteen-dt-op-reference",
            isHide: false,
        });
        $("#btn-goodRef").hide();
    } else {
        ReactAPI.setHeadBtnAttr("redRef", {
            icon: "sup-btn-icon sup-btn-eighteen-dt-op-reference",
            isHide: true,
        });
        $("#btn-goodRef").show();
    }

    initVendor();
}

function initVendor() {
    if (vendorRf) {
        vendorId = vendorRf.getValue()[0].id;

        var result = ReactAPI.request({
            url: "/msService/material/entity/getEntityList",
            type: "get",
            async: false,
            data: {
                moduleName: "BaseSet",
                entityName: "BaseSetSupplierMater",
                conditions: "valid=1,cooperator.id=" + vendorId,
                includes: "id,material,packageWeight"
            }
        })

        if (result && result.code == 200 && result.data) {
            let suppliers = result.data;
            if (suppliers && suppliers.length > 0) {
                vendorMaterials = suppliers;
                vendorMaterials.forEach(v => {
                    vendorMaterialsMap[v.material.id] = v.packageWeight;
                })
            }
        }
    }
}


/**
 * 表体初始化时，处理相应样式
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
 */
function ptInit() {
    dataInit();
    //设置复制行按钮样式
    $("#btn-copy i").attr("class", "sup-btn-icon sup-btn-own-fzh");
    //设置参照按钮
    dgDetail.setBtnImg("btn-goodRef", "sup-btn-eighteen-dt-op-reference");
    refreshRequired();
    //设置校验
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
}

/**
 * 表体请求数据时处理信息
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
 */
function ptRenderOver() {
    refreshReadonly();
    //设置批号只读条件（由于是固定的，所以不需要反复刷新
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "batchText", rowData => {
        var batchType = rowData.good && rowData.good.isBatch && rowData.good.isBatch.id;
        return batchType != "BaseSet_isBatch/batch" && batchType != "BaseSet_isBatch/piece";
    });
    //根据物料是否“质检”,设置表体的"检验结论"字段是否只读
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "checkResult.value", rowData => {
        var isCheck = rowData.good && rowData.good.isCheck;
        return !isCheck;
    });
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


//查询仓库下的库存对应的所有物料
function getMaterialsByWare() {
    let warehouseArr = rfWarehouse.getValue();
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
 * 表体“参照”物料按钮事件
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
 */
function goodRefFnc() {
    var url = "/msService/BaseSet/material/material/materialRefLayout?multiSelect=true";
    let materialIds = getMaterialsByWare()
    if (materialIds && materialIds.length > 0) {
        if (vendorId) {
            url += "&cooperateId=" + vendorId + "&cappMaterialIds=" + materialIds + "&customConditionKey=cooperateId,cappMaterialIds";
        } else {
            url += "&cappMaterialIds=" + materialIds + "&customConditionKey=cappMaterialIds"
        }
    } else {
        if (vendorId) {
            url += "&cooperateId=" + vendorId + "&customConditionKey=cooperateId";
        }
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
                    genPrintInfo: true,
                    packageWeight: null
                };
                // 物料对应的包重添加上
                if (vendorMaterialsMap[material.id] || vendorMaterialsMap[material.id] == 0) {
                    newLine['packageWeight'] = vendorMaterialsMap[material.id]
                }
                //将之前的属性附加上去
                datagrid.appendRowAttr(dgDetailName, newLine);
                return newLine;
            }), true);
            setPackageNumberReadOnly();
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


/**
 * 根据是否有包重数据，设置包数是否只读
 */
function setPackageNumberReadOnly() {
    let dgData = dgDetail.getDatagridData();
    if (dgData.length > 0) {
        dgDetail.setCellsAttr(dgData.map(d => {
            return {
                row: d.rowIndex,
                keyToAttr: {
                    packageNumber: {readonly: d.packageWeight ? false : true}
                }
            }
        }))
    }
}


/**
 * 表体“复制”物料按钮事件
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
 */
function copyRowFnc() {
    // 选中行对象
    var selRows = dgDetail.getSelecteds();
    if (selRows.length == 0) {
        // 请至少选择一条数据！
        ReactAPI.showMessage(
            "w",
            ReactAPI.international.getText("material.custom.randon1574406106043")
        );
        return;
    }
    dgDetail.addLine(selRows.map(rowData => {
        var copy = $.extend(true, {}, rowData);
        delete copy.id;
        delete copy.version;
        delete copy.sort;
        delete copy.currClickColKey;
        delete copy.edited;
        delete copy.key;
        delete copy.rowIndex;
        delete copy.batchText;
        return copy;
    }), true);
}


var ignoreConfirmFlag = false;

/**
 * 其他入库单编辑界面onsave脚本
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
 *   2.TODO:如果是为了检测是否超储问题,建议优化为后台处理，并只做提醒，不做限制 modify by yaoyao
 */
function onSave() {
    var type = ReactAPI.getOperateType();
    if ("submit" == type) {
        var dgData = dgDetail.getDatagridData();
        var warehouse = rfWarehouse.getValue()[0];
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
    return true;
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

/**
 * 其他入库单编辑界面表头红蓝字变化时
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
 */
function redBlueOnchangeFnc(value) {
    var preValue = ReactAPI.getComponentAPI("SystemCode")
        .APIs("otherInSingle.redBlue")
        .getValue().value;

    if (value == "BaseSet_redBlue/red") {
        if (dgDetail.getDatagridData().length != 0) {
            ReactAPI.openConfirm({
                message: ReactAPI.international.getText(
                    "material.custom.clearTheTableBodyAtTheSameTime"
                ), //切换红蓝字会同时清空表体！
                okText: ReactAPI.international.getText("ec.common.confirm"), //确定
                cancelText: ReactAPI.international.getText(
                    "foundation.signature.cancel"
                ), //取消
                onOk: () => {
                    ReactAPI.closeConfirm();
                    // 清空表体
                    dgDetail.deleteLine();
                    ReactAPI.setHeadBtnAttr("redRef", {
                        icon: "sup-btn-icon sup-btn-eighteen-dt-op-reference",
                        isHide: false,
                    });
                    refreshReadonly();
                    $("#btn-goodRef").hide();
                    return false;
                },
                onCancel: () => {
                    ReactAPI.getComponentAPI("SystemCode")
                        .APIs("otherInSingle.redBlue")
                        .setValue(preValue);
                    ReactAPI.closeConfirm();
                    return false;
                },
            });
        } else {
            ReactAPI.setHeadBtnAttr("redRef", {
                icon: "sup-btn-icon sup-btn-eighteen-dt-op-reference",
                isHide: false,
            });
            $("#btn-goodRef").hide();
        }
    } else {
        if (dgDetail.getDatagridData().length != 0) {
            ReactAPI.openConfirm({
                message: ReactAPI.international.getText(
                    "material.custom.clearTheTableBodyAtTheSameTime"
                ), //切换红蓝字会同时清空表体！
                okText: ReactAPI.international.getText("ec.common.confirm"), //确定
                cancelText: ReactAPI.international.getText(
                    "foundation.signature.cancel"
                ), //取消
                onOk: () => {
                    ReactAPI.closeConfirm();
                    // 清空表体
                    dgDetail.deleteLine();
                    ReactAPI.setHeadBtnAttr("redRef", {
                        icon: "sup-btn-icon sup-btn-eighteen-dt-op-reference",
                        isHide: true,
                    });
                    $("#btn-goodRef").show();
                    refreshReadonly();
                    return false;
                },
                onCancel: () => {
                    ReactAPI.getComponentAPI("SystemCode")
                        .APIs("otherInSingle.redBlue")
                        .setValue(preValue);
                    ReactAPI.closeConfirm();
                    return false;
                },
            });
        } else {
            ReactAPI.getComponentAPI("Reference")
                .APIs("otherInSingle.ware.code")
                .setReadonly(false);
            ReactAPI.setHeadBtnAttr("redRef", {
                icon: "sup-btn-icon sup-btn-eighteen-dt-op-reference",
                isHide: true,
            });
            $("#btn-goodRef").show();
        }
    }
}

/**
 * 其他入库单编辑界面表头仓库变化方法
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
 */
function ocgWarehouse(value) {
    function wareChange() {
        wareCache = value
        var newEnablePlace = value && value[0] && value[0].storesetState;
        if (newEnablePlace != enablePlace) {
            enablePlace = newEnablePlace;
            generateTask = (integrateWmsPro && enablePlace);
            refreshRequired();
            refreshReadonly();
        }
        //清空货位
        datagrid.clearColValue(dgDetail, "placeSet");
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
                    // 清除表头时清空表体
                    dgDetail.deleteLine();
                    wareChange()
                },
                onCancel: () => {
                    ReactAPI.closeConfirm();
                    if (wareCache && wareCache[0]) {
                        rfWarehouse.setValue(wareCache[0])
                    } else {
                        rfWarehouse.setValue({})
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
 * 其他入库单编辑界面表头,参照红字冲销按钮
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
 *   2.TODO: 待优化
 */
function redRefFnc() {
    ReactAPI.createDialog("newDialog", {
        title: ReactAPI.international.getText(
            "material.custom.RedInkOffsetReference"
        ), //红字冲销参照
        url: "/msService/material/otherInSingle/inSingleDetail/inSinglePartRef",
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
        cancelText: ReactAPI.international.getText("Button.text.close"), // 关闭
    });

    var partCallback = (data, event) => {
        if (data != null && data.length != 0) {
            var dgData = dgDetail.getDatagridData();
            for (var i = 0; i < data.length; i++) {
                var id = data[i].id;
                for (var j = 0; j < dgData.length; j++) {
                    var partId = dgDetail.getValueByKey(j, "partId");
                    if (id == partId) {
                        //校验重复
                        ReactAPI.showMessage(
                            "w",
                            ReactAPI.international.getText(
                                "material.custom.CannotBeReferencedRepeatedly",
                                "" + (i + 1)
                            )
                        );
                        return false;
                    }
                }

                var wareCode = data[i].inSingle.ware.code;
                for (var t = 0; t < data.length; t++) {
                    var wareCodet = data[t].inSingle.ware.code;
                    if (wareCodet != wareCode) {
                        //所选仓库不同，无法同时参照！
                        ReactAPI.showMessage(
                            "w",
                            ReactAPI.international.getText(
                                "material.custom.wareDifferent",
                                "" + (t + 1),
                                "" + (i + 1)
                            )
                        );
                        return false;
                    }
                }
                var ware = ReactAPI.getComponentAPI("Reference")
                    .APIs("otherInSingle.ware.code")
                    .getValue()[0];
                if (undefined != ware && wareCode != ware.code) {
                    //所选仓库不同，无法同时参照！
                    ReactAPI.showMessage(
                        "w",
                        ReactAPI.international.getText("material.custom.wareisNotDifferent")
                    );
                    return false;
                }
            }
            for (var j = 0; j < data.length; j++) {
                var rowIndex = dgDetail.addLine().rowIndex;
                dgDetail.setValueByKey(rowIndex, "good", data[j].good);
                dgDetail.setValueByKey(rowIndex, "redPartID", data[j].id);
                dgDetail.setValueByKey(rowIndex, "batchText", data[j].batchText);
                dgDetail.setValueByKey(
                    rowIndex,
                    "inQuantity",
                    data[j].inQuantity - data[j].redNumber
                );
                dgDetail.setValueByKey(
                    rowIndex,
                    "appliQuanlity",
                    data[j].inQuantity - data[j].redNumber
                );
                dgDetail.setValueByKey(rowIndex, "placeSet", data[j].placeSet);
                dgDetail.setValueByKey(rowIndex, "productionDate", data[j].productionDate);
                dgDetail.setDatagridCellAttr(rowIndex, "good.name", {readonly: true});
                dgDetail.setDatagridCellAttr(rowIndex, "placeSet.name", {readonly: true});
            }
            ReactAPI.getComponentAPI("Reference")
                .APIs("otherInSingle.ware.name")
                .setReadonly(false);
            ReactAPI.getComponentAPI("Reference")
                .APIs("otherInSingle.ware.name")
                .setValue(data[0].inSingle.ware);
        } else {
            //请至少选中一行
            ReactAPI.showMessage(
                "w",
                ReactAPI.international.getText("material.custom.randon1574406106043")
            );
            return false;
        }
        ReactAPI.showMessage(
            "s",
            ReactAPI.international.getText("foundation.common.tips.addsuccessfully")
        );
    };
}

function vendorChangeCallback(value) {
    function vendorChangeFun() {
        vendorCache = value
        let selectedVendor = value[0];
        vendorId = selectedVendor.id;
        var result = ReactAPI.request({
            url: "/msService/material/entity/getEntityList",
            type: "get",
            async: false,
            data: {
                moduleName: "BaseSet",
                entityName: "BaseSetSupplierMater",
                conditions: "valid=1,cooperator.id=" + vendorId,
                includes: "id,material,packageWeight"
            }
        })
        if (result && result.code == 200 && result.data) {
            let suppliers = result.data;
            if (suppliers && suppliers.length > 0) {
                vendorMaterials = suppliers;
                vendorMaterials.forEach(v => {
                    vendorMaterialsMap[v.material.id] = v.packageWeight;
                })
            } else {
                // 清除供应商下物料缓存
                vendorMaterials = [];
                vendorMaterialsMap = {};
            }
        }
        return true;
    }

    if (value && value.length > 0) {
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
                    vendorChangeFun()
                    return true;
                },
                onCancel: () => {
                    ReactAPI.closeConfirm();
                    if (vendorCache && vendorCache[vendorCache.length - 1]) {
                        vendorRf.setValue(vendorCache[vendorCache.length - 1])
                    } else {
                        vendorRf.setValue({})
                    }
                    return true;
                }
            });
        } else {
            vendorChangeFun()
        }
    }
}


/**
 * 传入对象返回url参数
 * @param {Object} data {a:1}
 * @returns {string}
 */
function getParam(data) {
    let url = '';
    for (var k in data) {
        let value = data[k] !== undefined ? data[k] : '';
        url += `&${k}=${encodeURIComponent(value)}`
    }
    return url ? url.substring(1) : ''
}

function beforeClearVendor(value) {
    var length = dgDetail.getDatagridData().length;
    if (length == 0) {
        // 清除供应商id
        vendorId = null;
        vendorMaterials = [];
        vendorMaterialsMap = {};
        return true;
    }

    var id = null;
    var name = null;
    var code = null;
    if (undefined != vendorRf.getValue()[0]) {
        id = vendorRf.getValue()[0].id;
        name = vendorRf.getValue()[0].name;
        code = vendorRf.getValue()[0].code;
    }

    ReactAPI.openConfirm({
        //"清除后将清空表体数据，是否继续？",
        message: ReactAPI.international.getText("material.custom.randonAfterclearingBody"),
        okText: ReactAPI.international.getText("attendence.attStaff.isInstitutionYes"),//是
        cancelText: ReactAPI.international.getText("attendence.attStaff.isInstitutionNo"),//否
        onOk: () => {
            ReactAPI.closeConfirm();
            // 清除供应商id
            vendorId = null;
            vendorMaterials = [];
            vendorMaterialsMap = {};
            vendorCache = null
            // 清除表头供应商
            vendorRf.removeValue();
            // 清除表头供应商时清空表体
            dgDetail.deleteLine();
            return true;
        },
        onCancel: () => {
            ReactAPI.closeConfirm();
            if (null != id) {
                vendorRf.setValue({
                    code: code,
                    id: id,
                    name: name,
                });
            }
            return false;
        }
    });
    return false;
}

/**
 * 按照件数和单件量计算申请入库数量
 * @param value
 * @param rowIndex 行号
 */
function renderAppliQuanlityOnPackageNumberChange(value, rowIndex) {
    var itemQty = dgDetail.getValueByKey(rowIndex, "packageWeight");
    if (value && itemQty) {
        dgDetail.setValueByKey(rowIndex, "appliQuanlity", value * itemQty)
    } else if (parseFloat(value) === 0) {
        dgDetail.setValueByKey(rowIndex, "appliQuanlity", 0)
    }
}

/**
 * 根据申请入库数量计算件数
 * @param value
 * @param rowIndex 行号
 */
function renderPackageNumberOnAppliQuanlityChange(value, rowIndex) {
    var itemQty = dgDetail.getValueByKey(rowIndex, "packageWeight");
    if (value && itemQty) {
        dgDetail.setValueByKey(rowIndex, "packageNumber", value / itemQty)
    } else if (parseFloat(value) === 0) {
        dgDetail.setValueByKey(rowIndex, "packageNumber", 0)
    }
}

/**
 * 清除表头仓库
 * @param afterClearWare
 * @returns {boolean}
 */
function afterClearWare(afterClearWare) {
    var length = dgDetail.getDatagridData().length;
    if (length == 0) {
        //清空仓库缓存
        wareCache = null;
        return true;
    }
    var id = null;
    var name = null;
    var code = null;
    if (undefined != rfWarehouse.getValue()[0]) {
        id = rfWarehouse.getValue()[0].id;
        name = rfWarehouse.getValue()[0].name;
        code = rfWarehouse.getValue()[0].code;
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
            rfWarehouse.removeValue();
            // 清除表头供应商时清空表体
            dgDetail.deleteLine();
            //清空仓库缓存
            wareCache = null;
            return true;
        },
        onCancel: function onCancel() {
            ReactAPI.closeConfirm();
            if (null != id) {
                rfWarehouse.setValue({
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


