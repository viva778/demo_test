//-----下发复盘-----
const dgDistName = "material_1.0.0_stocktaking_stocktakingDistdg1662517393459";
const dgStockViewName = "material_1.0.0_stocktaking_stocktakingDistdg1662517393567";

var dgDist;
var dgStockView;
var rfWarehouse;
var rfMaterials;
var vMaterialIds;
var vSelectDistModelIds = [];
var vWareModelIds = [];
var firstStaffDist = [];
var enable_place;

function dataInit() {
    dgDist = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgDistName);
    dgStockView = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgStockViewName);
    rfWarehouse = ReactAPI.getComponentAPI().Reference.APIs("stocktaking.warehouse.name");
    rfMaterials = ReactAPI.getComponentAPI().Reference.APIs("stocktaking.stocktakingMaterial");
    enable_place = ReactAPI.getComponentAPI().Checkbox.APIs("stocktaking.warehouse.storesetState").getValue().value;
    dataInit = () => { };
}

function onLoad() {
    dataInit();
    vMaterialIds = rfMaterials.getValue().map(val => val.id);
}

function ptDistInit() {
    dataInit();
    //设置图标
    dgDist.setBtnImg("btn-distribute", "sup-btn-own-wtdb");
    //绑定点击事件
    dgDist.setClickEvt(function () {
        distLineOnClick();
    });
    //绑定多选事件
    dgDist.setCheckBoxClickEvt(function () {
        distLineOnClick();
    });
}

function ptDistRenderOver() {

}


//分配任务点击，刷新右侧
function distLineOnClick() {
    setTimeout(() => {
        //判断本次选择和上次选择是否一致，不一致则刷新
        var flush = false;
        var selRows = dgDist.getSelecteds();
        if (vSelectDistModelIds.length != selRows.length) {
            //先根据长度判断
            flush = true;
        } else {
            //再根据全包含判断
            selRows.map(val => val.target.id).forEach(id => {
                if (!flush) {
                    if (!vSelectDistModelIds.includes(id)) {
                        flush = true;
                    }
                }
            })
        }
        if (flush) {
            vSelectDistModelIds = selRows.map(val => val.target.id);
            refreshStockView();
        }
    });
}

function ptBtnDistribute() {
    if (!dgDist.getSelecteds().length) {
        //请至少选择一行数据！
        ReactAPI.showMessage('w', ReactAPI.international.getText("ec.business.SelectRow"));
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
            debugger
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
    if (!vWareModelIds.length) {
        dgStockView.deleteLine();
    } else {
        //刷新
        refreshStockView();
    }
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

function refreshStockView() {
    var warehouse = rfWarehouse.getValue()[0];
    if (warehouse && warehouse.id) {
        const customCondition = {
            materialIds: vMaterialIds.join(","),
            hasStock: true
        };
        if (enable_place) {
            customCondition.placeModelIds = vSelectDistModelIds.length ? vSelectDistModelIds.join(",") : vWareModelIds.join(",");
        } else if (vWareModelIds.length) {
            customCondition.wareId = warehouse.id;
        }
        dgStockView.refreshDataByRequst({
            type: "POST",
            url: "/msService/material/stocktaking/stStockView/data-dg1660640892409?datagridCode=material_1.0.0_stocktaking_stocktakingApplydg1660640892409&id=-1",
            param: {
                customCondition: customCondition
            }
        });
    }
}

function toStringCondition(condition) {
    var valid_keys = Object.keys(condition).filter(key => condition[key]);
    if (valid_keys.length) {
        return "customConditionKey=" + valid_keys.join(",") + "&" + valid_keys.map(key => key + "=" + condition[key]).join("&");
    }
    return ""
}


function onSave() {
    try {
        var dgData = dgDist.getDatagridData()
        if (!dgData.length) {
            //表体数据不能为空！
            ReactAPI.showMessage("w", ReactAPI.international.getText("material.custom.randon1573634425210"));
            return false;
        }
        //将数据塞到全局变量等待上级页面读取
        window._saveData = dgData.map(val => new Object({
            target: val.target,
            distributionStaffmultiselectIDs: val.distributionStaffmultiselectIDs
        }));
    } finally {
        return false;
    }
}