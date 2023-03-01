//-----盘点策略编辑-----
const dgDistName = "material_1.0.0_stocktakingStrategy_stsEditdg1663052282229";

var dgDist;
var rfWarehouse;
var rfMaterials;
var lbMaterials;
var lbTakingWay;
var scTakingWay;
var cbTargetMaterial;
var vWareModelIds;
var vMaterialIds;
var vSelectDistModelIds = [];
var firstStaffDist

function dataInit() {
    dgDist = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgDistName);
    rfWarehouse = ReactAPI.getComponentAPI().Reference.APIs("stStrategy.warehouse.name");
    rfMaterials = ReactAPI.getComponentAPI().Reference.APIs("stStrategy.stocktakingStrategyMaterial");
    lbMaterials = ReactAPI.getComponentAPI().Label.APIs("stStrategy-stocktakingStrategyMaterial-label");
    lbTakingWay = ReactAPI.getComponentAPI().Label.APIs("stStrategy.takingWay");
    scTakingWay = ReactAPI.getComponentAPI().SystemCode.APIs("stStrategy.takingWay");
    cbTargetMaterial = ReactAPI.getComponentAPI().Checkbox.APIs("stStrategy.targetMaterial");
    dataInit = () => { };
}

function ptInit() {
    dataInit();
    // 设置 加载清单图标
    dgDist.setBtnImg("btn-load", "sup-btn-own-sx");
    // 设置 批量指派图标
    dgDist.setBtnImg("btn-distribute", "sup-btn-own-wtdb");
}


function onLoad() {
    dataInit();
    refreshTakingWay();

    vMaterialIds = rfMaterials.getValue().map(val => val.id);
    refreshMaterialReference();
    //监听盘点仓库状态
    new MutationObserver(() => {
        refreshTakingWay();
    }).observe($("#stStrategy_warehouse_storesetState .ant-checkbox")[0], {
        subtree: true,
        attributeOldValue: true,
        characterDataOldValue: true
    });
    //当盘点方式为动碰盘点时，会载入自上次盘点开始后动碰的货位，若未发生动碰，则不会产生盘点单
    lbTakingWay.showTip(ReactAPI.international.getText("material.stocktaking.move_check_hint"));
}

function ptRenderOver() {
    vWareModelIds = dgDist.getDatagridData().map(val => val.target && val.target.id).filter(val => val);
    firstStaffDist = dgDist.getDatagridData();
}


function ptBtnLoad() {
    var warehouse = rfWarehouse.getValue()[0];
    if (!warehouse || !warehouse.id) {
        //先选择仓库
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.stocktaking.choose_warehouse_first"));
        return false;
    }
    var takingWay = scTakingWay.getValue() && scTakingWay.getValue().value;
    if (!takingWay) {
        //请先选择盘点方式
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.stocktaking.choose_taking_way_first"));
        return false;
    }
    if (enable_place) {
        var result = ReactAPI.request({
            url: "/msService/material/wareModel/getPlaceModelsByWarehouse",
            type: "get",
            async: false,
            data: {
                "warehouseId": warehouse.id,
                "includes": "id,name,wareType.id,wareType.value"
            }
        });
        if (result.code != 200) {
            ReactAPI.showMessage('f', result.message);
            return false;
        }
        addWareModels = result.data;
    } else {
        var result = ReactAPI.request({
            url: "/msService/material/entity/getEntityList",
            type: "get",
            async: false,
            data: {
                moduleName: "material",
                entityName: "MaterialWareModel",
                conditions: "code='" + warehouse.code + "',layNo=1",
                includes: "id,name,wareType.id,wareType.value"
            }
        });
        if (result.code != 200) {
            ReactAPI.showMessage('f', result.message);
            return false;
        }
        addWareModels = result.data;
    }
    if (addWareModels.length) {
        //对已存在数据进行过滤
        var newTargetLineList = addWareModels.filter(val => !vWareModelIds.includes(val.id)).map(val => new Object({
            target: val
        }));
        if (newTargetLineList.length) {
            dgDist.addLine(newTargetLineList, true);
            newTargetLineList.forEach(val => vWareModelIds.push(val.target.id));
        }
    } else {
        //无更多数据
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.no_more_data"));
        return false;
    }

}


function ptBtnDelete() {
    var selRows = dgDist.getSelecteds();
    if (!selRows.length) {
        //请至少选择一行数据！
        ReactAPI.showMessage('w', ReactAPI.international.getText("ec.business.SelectRow"));
        return false;
    }
    //删行
    dgDist.deleteLine(selRows.map(val => val.rowIndex).join(","));
    //删变量
    var deleteIds = selRows.map(val => val.target.id);
    vWareModelIds = vWareModelIds.filter(id => !deleteIds.includes(id));
    //清空选择项
    vSelectDistModelIds = [];
}

function ptBtnReference() {
    var warehouse = rfWarehouse.getValue()[0];
    if (!warehouse || !warehouse.id) {
        ReactAPI.showMessage('w', "请先选择仓库");
        return false;
    }

    var url = "/msService/material/wareModel/wareModel/wareRefer?multiSelect=true&" + toStringCondition({
        warehouseId: warehouse.id
    });

    ReactAPI.createDialog("ware_model_ref", {
        title: "仓库建模",
        url: url,
        size: 5,
        callback: (data, event) => ware_model_callback(data, event),
        isRef: true, // 是否开启参照
        onOk: (data, event) => ware_model_callback(data, event),
        onCancel: () => {
            ReactAPI.destroyDialog("ware_model_ref");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
    });

    var ware_model_callback = (wareModels, event) => {
        if (!wareModels.length) {
            //请至少选择一行数据！
            event.ReactAPI.showMessage('w', ReactAPI.international.getText("ec.business.SelectRow"));
            return false;
        }

        var modelsToAdd;
        if (enable_place) {
            //获取非货位节点和货位节点
            modelsToAdd = [];
            var nplaceNodes = wareModels.filter(val => {
                if (val.wareType.id == "material_wareType/storeSet") {
                    modelsToAdd.push(val);
                    return false;
                } else {
                    return true;
                }
            }).map(val => val.id);
            //校验重复添加的直接节点
            var conflictRows = modelsToAdd.filter(val => vWareModelIds.includes(val.id)).map(val => val.rowIndex + 1).join(",");
            if (conflictRows) {
                //第{0}行，数据重复
                event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.data_repeat", conflictRows));
                return false;
            }
            //展开非货位节点
            if (nplaceNodes.length) {
                var result = ReactAPI.request({
                    url: "/msService/material/wareModel/getBulkEnabledPlaceModels",
                    type: "get",
                    async: false,
                    data: {
                        "wareModelIds": nplaceNodes.join(","),
                        "includes": "id,name,wareType.id,wareType.value"
                    }
                });
                if (result.code != 200) {
                    ReactAPI.showMessage('f', result.message);
                    return false;
                }
                var existIdsToAdd = modelsToAdd.map(val => val.id);
                result.data.filter(val => !existIdsToAdd.includes(val.id) && !vWareModelIds.includes(val.id)).forEach(wareModel => modelsToAdd.push(wareModel));
            }
        } else {
            //校验重复后添加
            var existIds = dgDist.getDatagridData().map(val => val.target.id);
            var conflictRows = wareModels.filter(val => existIds.includes(val.id)).map(val => val.rowIndex + 1).join(",");
            if (conflictRows) {
                //第{0}行，数据重复
                event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.data_repeat", conflictRows));
                return false;
            }
            modelsToAdd = wareModels;
        }
        if (modelsToAdd.length) {
            //添加到表体
            dgDist.addLine(modelsToAdd.map(val => new Object({
                target: val
            })), true);
            //添加到id列表
            modelsToAdd.forEach(wareModel => vWareModelIds.push(wareModel.id));
        }
        ReactAPI.destroyDialog("ware_model_ref");
    }
}



function ocgWarehouse(val) {
    //清空表体
    dgDist.deleteLine();
    vWareModelIds = [];
}

function ocgTakingWay(val) {
    //清空表体
    dgDist.deleteLine();
    vWareModelIds = [];
}

function ocgTargetMaterial(val) {
    refreshMaterialReference(val);
}

//刷新物料参照
function refreshMaterialReference(val) {
    if (val == undefined) {
        val = cbTargetMaterial.getValue().value;
    }
    rfMaterials.setReadonly(!val);
    lbMaterials.setNullableStyle(val);
    rfMaterials.setRequired(val);
    if (!val) {
        rfMaterials.setValue([]);
    }
}



//刷新盘点方式
var enable_place;
function refreshTakingWay(val) {
    var flush;
    if (enable_place == undefined) {
        flush = true;
    }
    if (val == undefined) {
        val = ReactAPI.getComponentAPI().Checkbox.APIs("stStrategy.warehouse.storesetState").getValue().value;
    }
    flush = flush || enable_place != val;
    if (flush) {
        enable_place = val;
    }
    if (flush) {
        if (enable_place) {
            //盘点方式取消只读
            scTakingWay.setReadonly(false);
        } else {
            //盘点方式只读，且设置静态
            scTakingWay.setReadonly(true);
            scTakingWay.setValue("material_invManner/staticInv");
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



function ptBtnDistribute(event) {
    if (dgDist.getSelecteds().length == 0) {
        //请至少选择一行数据！
        event.ReactAPI.showMessage('w', ReactAPI.international.getText("ec.business.SelectRow"));
        return false;
    }
    ReactAPI.createDialog("materDeptEditDailog", {
        //批量指派
        title: ReactAPI.international.getText("material.buttonPropertyshowName.randon1660640743465.flag"),
        size: 8,
        url: "/organization/#/reference?refKey=distributionStaffmultiselectNames&fromViewCode=material_1.0.0_stocktaking_stocktakingApply&closePage=false&crossCompanyFlag=true&multiSelect=true&openType=frame&type=staff",
        onOk: function (event) {
            var staffs = event.getSelectItem()
            var newStaff = [];
            var newStaffName = [];
            staffs.forEach(element => {
                newStaff.push(element.id)
                newStaffName.push(element.name)
            });
            staffDist = dgDist.getSelecteds()
            staffDist.forEach(element => {
                var oldElement = firstStaffDist[element.rowIndex]
                var oldStaff;
                if (oldElement && oldElement.distributionStaffmultiselectIDs) {
                    oldStaff = oldElement.distributionStaffmultiselectIDs.split(',')
                }
                var addStaff = []
                var deleteStaff = []
                newStaff.forEach(item => {
                    if (!oldStaff || !oldStaff.includes(item)) {
                        addStaff.push(item)
                    }
                })
                if (oldStaff) {
                    oldStaff.forEach(item => {
                        if (!newStaff.includes(item)) {
                            deleteStaff.push(item)
                        }
                    })
                }
                dgDist.setRowData(oldElement && oldElement.rowIndex || element.rowIndex, {
                    distributionStaffmultiselectIDs: newStaff.join(","),
                    distributionStaffmultiselectNames: newStaffName.join(","),
                    distributionStaffAddIds: addStaff.join(","),
                    distributionStaffDeleteIds: deleteStaff.join(",")
                });
            });

            ReactAPI.destroyDialog("materDeptEditDailog");
        },
        okText: ReactAPI.international.getText("Button.text.save"),
        onCancel: function () {
            ReactAPI.destroyDialog("materDeptEditDailog");
        }
    });
}


function onSave() {
    if (!dgDist.getDatagridData().length) {
        //表体数据不能为空！
        ReactAPI.showMessage("w", ReactAPI.international.getText("material.custom.randon1573634425210"));
        return false;
    }
}