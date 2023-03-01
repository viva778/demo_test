//-----废料入库-----

//引入datagrid.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");

const dgDetailName = "material_1.0.0_wasteInSingle_hazardousInEditdg1632377232470";
const dgCheckDetailName = "material_1.0.0_wasteInSingle_hazardousInEditdg1632377232551";
var dgDetail;
var dgCheckDetail;
var rfWarehouse;
var cbStoreState;
var rfServiceTypeExplain;
var rfStorageType;
//废料出库单不生成任务,此变量暂时不用
var generateTask;
var wareCache;

const default_storage_type = {
    "id": 1036,
    "reasonCode": "hazardousIn",
    "reasonExplain": "危废入库"
};

function dataInit() {
    cbEnablePlace = ReactAPI.getComponentAPI("Checkbox")
        .APIs("wasteInSingle.warehourse.storesetState");
    enablePlace = cbEnablePlace.getValue().value;
    integrateWmsPro = (ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.wmspro",
    })["material.wmspro"] == 'true');
    generateTask = (integrateWmsPro && enablePlace);

    dgDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgDetailName);
    dgCheckDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgCheckDetailName);
    rfWarehouse = ReactAPI.getComponentAPI("Reference").APIs("wasteInSingle.warehourse.name");
    rfServiceTypeExplain = ReactAPI.getComponentAPI("Reference").APIs("wasteInSingle.serviceTypeID.serviceTypeExplain");
    rfStorageType = ReactAPI.getComponentAPI("Reference").APIs("wasteInSingle.storageType.reasonExplain");

    cbStoreState = ReactAPI.getComponentAPI("Checkbox").APIs("wasteInSingle.warehourse.storesetState");
    //设置仓库缓存
    wareCache = rfWarehouse.getValue()
}

function onLoad() {
    dataInit();
    rfServiceTypeExplain.setValue({serviceTypeCode: "wasteIn", id: 1015, serviceTypeExplain: "废物入库事务"});
    var storageType = rfStorageType.getValue()[0];
    if (!storageType || !storageType.id) {
        rfStorageType.setValue(default_storage_type);
    }
}

function ptRenderOver() {
    refreshReadonly();
}

function ptInit() {
    dataInit();
    refreshRequired();
    datagrid.validator.add(dgDetailName, "applyNum", rowData => {
        //物品启用按件管理，入库数量只能为1件
        var batchType = rowData.good && rowData.good.isBatch && rowData.good.isBatch.id;
        if (batchType == "BaseSet_isBatch/piece") {
            return rowData.applyNum == 1;
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


const storage_type_map = {
    "hazardousIn": "material_wasteChecktype/HazardousWaste",
    "recyclableWasteIn": "material_wasteChecktype/RecyclableMaterial",
    "generalWasteIn": "material_wasteChecktype/GeneralWaste",
    "liveWasteIn": "material_wasteChecktype/LivingGarbage"
}


function ptCheckRenderOver() {
    var dgData = dgCheckDetail.getDatagridData();
    if (!dgData.length) {
        refreshCheckDetail(rfStorageType.getValue()[0] || default_storage_type);
    }
}

function ocgStorageType(value) {
    var dgData = dgCheckDetail.getDatagridData();
    if (dgData.length) {
        dgCheckDetail.deleteLine(dgData.map(rowData => rowData.rowIndex).join(","));
    }
    if (value && value[0]) {
        refreshCheckDetail(value[0]);
    }
}

function refreshCheckDetail(storageType) {
    var type = storage_type_map[storageType.reasonCode];
    if (type) {
        ReactAPI.request({
            url: "/msService/material/wasteCheckManage/wasteCheck/findByType",
            type: 'get',
            async: true,
            data: {
                type: type
            }
        }, res => {
            if (res.code != 200) {
                ReactAPI.showMessage('w', res.message);
                return false;
            }
            ;
            dgCheckDetail.addLine(res.data.map(ckDetail => new Object({
                checkReg: ckDetail
            })), true);
        })
    }
}


function ocgWarehouse(value) {
    var clear_list = [];

    function wareChange() {
        wareCache = value
        var cargoEnabled = value[0].storesetState;
        if (cargoEnabled != enablePlace) {
            enablePlace = cargoEnabled;
            generateTask = (integrateWmsPro && enablePlace);
            refreshRequired();
            refreshReadonly();
        }
        //刷新表体现存量汇总数据
        var dgData = dgDetail.getDatagridData();
        if (dgData.length) {
            var result = ReactAPI.request({
                type: "get",
                url: "/msService/material/waste/detail/wasteRefreshStockByWarehouse",
                async: false,
                data: {
                    warehouseId: value[0].id,
                    materialIds: dgData.map(rowData => rowData.good.id).join(",")
                }
            });
            if (result.code != 200) {
                ReactAPI.showMessage('w', result.message);
                return false;
            }
            dgData.forEach(rowData => {
                var res = result.data.find(v => v.good.id == rowData.good.id);
                rowData.nowQuanlity = res && res.nowQuanlity;
            });
            dgDetail.setDatagridData(dgData, true);
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
                    return;
                },
                onCancel: () => {
                    ReactAPI.closeConfirm();
                    if (wareCache && wareCache[0]) {
                        rfWarehouse.setValue(wareCache[0])
                    }else {
                        rfWarehouse.setValue({})
                    }
                    return false;
                }
            });
        } else {
            wareChange()
        }
    } else {
        clear_list.push("nowQuanlity");
    }
    //清空货位,以及没有仓库时清空现存量汇总
    clear_list.push("placeSet");
    datagrid.clearColValue(dgDetail, clear_list);
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

function ptBtnMaterial() {
    var url = "/msService/BaseSet/material/material/materialRefLayout?multiSelect=true";
    let materialIds = getMaterialsByWare()
    if (materialIds && materialIds.length > 0) {
        url += "&cappMaterialIds=" + materialIds + "&customConditionKey=cappMaterialIds"
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
        if (data && data.length) {
            //查找创建废料入库相关信息
            var warehourse = rfWarehouse.getValue()[0];
            var result = ReactAPI.request({
                type: "get",
                url: "/msService/material/waste/detail/wasteCreateDetailsByWarehouseAndMaterials",
                async: false,
                data: {
                    warehouseId: (warehourse && warehourse.id) || "",
                    materialIds: data.map(material => material.id).join(",")
                }
            });
            if (result.code != 200) {
                event.ReactAPI.showMessage('w', result.message);
                return false;
            }
            dgDetail.addLine(data.map(material => {
                var newLine = result.data.find(v => v.good.id == material.id) || {};
                newLine.good = material;
                //将之前设置的属性附加上去
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
        ReactAPI.destroyDialog("material_ref");
        ReactAPI.showMessage(
            "s",
            ReactAPI.international.getText("foundation.common.tips.addsuccessfully")
        );
    };
}


function refreshReadonly() {
    //如果未开启货位，货位只读
    //如果红字，货位只读，批号只读
    var readonly_keys = [];
    var rw_keys = [];
    if (!enablePlace) {
        readonly_keys.push("placeSet.name");
    } else {
        rw_keys.push("placeSet.name");
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
    //!generateTask &&
    if (enablePlace) {
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

var ignoreConfirmFlag = false;

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

        var warehouse = rfWarehouse.getValue()[0];

        //校验超储,归并同物料
        if (!ignoreConfirmFlag) {
            var checked_idx = new Set();
            for (var i = 0; i < dgData.length; i++) {
                if (!checked_idx.has(i)) {
                    var material = dgData[i].good;
                    //统计物料数量
                    var quan = dgData[i].applyNum;
                    for (var j = i + 1; j < dgData.length; j++) {
                        if (!checked_idx.has(j)) {
                            if (material.id == dgData[j].good.id) {
                                checked_idx.add(j);
                                quan += dgData[k].applyNum;
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




