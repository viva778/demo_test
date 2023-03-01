//-----销售退货入库-----

//引入datagrid.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");

//获取是否启用wmsPro模块,并设成全局常量
var integrateWmsPro;
var enablePlace;
//是否生成任务（开启PRO并且开启货位
var generateTask;
var dgDetail;
var rfWare;
var cbPlaceEnableState;
//获取是否启用wmsPro模块,并设成全局常量
var wareCache;


const dgDetailName = "material_1.0.0_saleReturn_saleReturnEditdg1574665641534";


function dataInit() {
    rfWare = ReactAPI.getComponentAPI().Reference.APIs("saleReturn.ware.name");
    dgDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgDetailName);
    cbPlaceEnableState = ReactAPI.getComponentAPI("Checkbox").APIs("saleReturn.ware.storesetState");
    enablePlace = cbPlaceEnableState.getValue().value;
    integrateWmsPro = (ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.wmspro",
    })["material.wmspro"] == 'true');
    generateTask = cbPlaceEnableState.getValue().value && integrateWmsPro;
    //设置仓库缓存
    wareCache = rfWare.getValue()
    dataInit = () => {
    };
}


function onLoad() {
    dataInit();
    ReactAPI.getComponentAPI("Reference").APIs("saleReturn.serviceType.serviceTypeExplain").setValue({
        serviceTypeCode: "saleReturnIn",
        id: 1011,
        serviceTypeExplain: ReactAPI.international.getText("material.custom.SalesReturnAffair")
    });
    var storageType = ReactAPI.getComponentAPI("Reference").APIs("saleReturn.storageType.reasonExplain").getValue()[0];
    if (!storageType) {
        ReactAPI.getComponentAPI("Reference").APIs("saleReturn.storageType.reasonExplain").setValue({
            id: 1020,
            reasonExplain: ReactAPI.international.getText("material.custom.SalesReturn")
        });
    }

    ReactAPI.setHeadBtnAttr('saleRef', {icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference'});
}


function ocgWarehouse(value) {
    function wareChange() {
        wareCache = value
        refreshPlaceEnable(value[0].storesetState);
        //清空货位
        datagrid.clearColValue(dgDetail, "placeSet");
        //删除参照出库的数据
        var deleteRows = dgDetail.getDatagridData().filter(rowData => rowData.saleOutDetail).map(rowData => rowData.rowIndex).join(",");
        if (deleteRows) {
            dgDetail.deleteLine(deleteRows);
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
                    // 清除表头时清空表体
                    dgDetail.deleteLine();
                    wareChange()
                },
                onCancel: () => {
                    ReactAPI.closeConfirm();
                    if (wareCache && wareCache[0]) {
                        rfWare.setValue(wareCache[0])
                    } else {
                        rfWare.setValue({})
                    }
                    return false;
                }
            });
        } else {
            wareChange()
        }
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


function ptInit() {
    dataInit();
    refreshRequired();
    //设置校验
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
    datagrid.validator.add(dgDetailName, "applyNum", rowData => {
        //申请数量+已退货数量<=实际出库数量
        var applyQuan = rowData.applyNum;
        var rtnQuan = (rowData.saleOutDetail && rowData.saleOutDetail.returnNum) || 0;
        var outQuan = (rowData.saleOutDetail && rowData.saleOutDetail.outQuantity) || 0;
        return !outQuan || applyQuan + rtnQuan <= outQuan;
    }, (rowIndex, title, rowData) => {
        var rtnQuan = (rowData.saleOutDetail && rowData.saleOutDetail.returnNum) || 0;
        var outQuan = (rowData.saleOutDetail && rowData.saleOutDetail.outQuantity) || 0;
        //分成三种情况
        //1.退货量为0，提示申请退货量不能大于出库量
        if (!rtnQuan) {
            return ReactAPI.international.getText(
                "material.datagrid.validator.less_equal_constraint",
                String(rowIndex), title, datagrid.dom.get_header_text(dgDetailName, "saleOutDetail.outQuantity")
            );
        }
        //2.退货量>=出库量，提示退货已完成，不能继续退货
        if (rtnQuan >= outQuan) {
            return ReactAPI.international.getText(
                //第{0}行，退货已完成，不允许继续退货
                "material.return.description.overhead",
                String(rowIndex)
            );
        }
        //3.申请量>剩余量=出库量-退货量，提示已退货量为{1}，申请退货量应在{2}及以下
        return ReactAPI.international.getText(
            //第{0}行，已退货量为{1}，申请退货量应在{2}及以下
            "material.purchase.return.amount_check",
            String(rowIndex), String(rtnQuan), String(outQuan - rtnQuan)
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
        var disableBatch = batchType != "BaseSet_isBatch/batch" && batchType != "BaseSet_isBatch/piece"
        var saleOutDetail = rowData.saleOutDetail;
        //不启用批次或来自上游参照
        return disableBatch || saleOutDetail;
    });
    //设置客商只读条件
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "customer.name", rowData => {
        //来自上游参照
        return rowData.saleOutDetail;
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

function ptBtnBizDetail() {
    //拼接查询条件
    var keys = ["bizType"];
    var conditions = ["multiSelect=true", "bizType=saleStorageOut"];
    var warehouse = rfWare.getValue()[0];
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
            //校验仓库一致
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
            //查询对应销售出库单表体
            var ids = data.map(rowData => rowData.tableBodyID).filter(val => val).join(",");
            var id$srcDetail = {};
            if (ids) {
                var result = ReactAPI.request({
                    type: "get",
                    url: "/msService/material/entity/getEntityList",
                    data: {
                        moduleName: "material",
                        entityName: "MaterialSaleOutDetail",
                        ids: ids,
                        includes: "id,returnNum,outQuantity,customer.id,customer.name"
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
            dgDetail.addLine(data.map(rowData => {
                var srcDetail = rowData.tableBodyID && id$srcDetail[parseInt(rowData.tableBodyID)];
                var newLine = {
                    good: rowData.good,
                    batchText: rowData.batchText,
                    placeSet: rowData.placeSet,
                    saleOutDetail: srcDetail,
                    customer: srcDetail && srcDetail.customer
                };
                //将之前设置的属性附加上去
                datagrid.appendRowAttr(dgDetailName, newLine);
                return newLine;
            }), true);
            //回填表头货位
            var warehouse = rfWare.getValue()[0];
            if (!warehouse || !warehouse.id) {
                rfWare.setValue(lastWarehouse);
                wareCache = [lastWarehouse]
                refreshPlaceEnable(lastWarehouse.storesetState);
            }
            ReactAPI.destroyDialog("biz_detail_ref");
        } else {
            //请至少选中一行
            event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1574406106043"));
            return false;
        }
    }
}

//查询仓库下的库存对应的所有物料
function getMaterialsByWare() {
    let warehouseArr = rfWare.getValue();
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
        callback: (data, event) => {
            material_callback(data, event)
        },
        isRef: true, // 是否开启参照
        onOk: (data, event) => {
            material_callback(data, event)
        },
        onCancel: () => {
            ReactAPI.destroyDialog("material_ref");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
    });

    var material_callback = (data, event) => {
        if (data && data.length) {
            dgDetail.addLine(data.map(material => {
                var newLine = {
                    good: material
                };
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

function onSave() {
    var type = ReactAPI.getOperateType();
    if ("submit" == type) {
        var dgData = dgDetail.getDatagridData();
        var warehouse = rfWare.getValue()[0];
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

        //校验退货总量
        var id$rtn_info = {};
        dgData.forEach(rowData => {
            var srcDetail = rowData.saleOutDetail
            if (srcDetail) {
                var rtn_info = id$rtn_info[srcDetail.id];
                if (rtn_info) {
                    rtn_info.apply_num += rowData.applyNum;
                    rtn_info.row_idx.push(rowData.rowIndex);
                } else {
                    id$rtn_info[srcDetail.id] = {
                        row_idx: [rowData.rowIndex],
                        apply_num: rowData.applyNum || 0,
                        rtn_num: srcDetail.returnNum || 0,
                        out_num: srcDetail.outQuantity || 0
                    };
                }
            }
        });
        //进行校验
        var hint = Object.values(id$rtn_info).filter(info => {
            return info.apply_num + info.rtn_num > info.out_num;
        }).map(info => ReactAPI.international.getText(
            "material.purchase.return.amount_check",
            String(info.row_idx.map(idx => idx + 1).join(",")), String(info.rtn_num), String(info.out_num - info.rtn_num)
        )).join("<br>");
        if (hint) {
            ReactAPI.showMessage('w', hint);
            return false;
        }

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

function onBeforeClear(deleteObj) {
    localStorage.setItem("clearFlag", 0);
    var dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_saleReturn_saleReturnEditdg1574665641534");
    if (dataGrid.getDatagridData().length != 0){
        ReactAPI.openConfirm({
            message: ReactAPI.international.getText("material.custom.randonAfterclearingBody"),//同时清空表体内容
            okText: ReactAPI.international.getText("ec.common.confirm"),//确定
            cancelText: ReactAPI.international.getText("foundation.signature.cancel"),//取消
            onOk: () => {
                ReactAPI.closeConfirm();
                ReactAPI.getComponentAPI("Reference").APIs("saleReturn.ware.name").removeValue();
                //ReactAPI.getComponentAPI("Reference").APIs("saleReturn.customer.code").removeValue();
                dgDetail.deleteLine();
                //ReactAPI.getComponentAPI("Input").APIs("saleReturn.srcTableNos").setValue(null);
                wareCache = null
                return false;
            },
            onCancel: () => {
                ReactAPI.closeConfirm();
                ReactAPI.getComponentAPI("Reference").APIs("saleReturn.ware.name").setValue(deleteObj[0]);
                return false;
            }
        });
    }else {
        //清空仓库缓存
        wareCache = null;
    }


}

function wareCallback(deleteObj){
    var clearFlag = localStorage.getItem("clearFlag");
    if(clearFlag == 0){
        localStorage.setItem("clearFlag", 1);
        return false;
    }
    // var dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_saleReturn_saleReturnEditdg1574665641534");
    //ReactAPI.getComponentAPI("Reference").APIs("saleReturn.customer.code").removeValue();
    // dgDetail.deleteLine();
    //ReactAPI.getComponentAPI("Input").APIs("saleReturn.srcTableNos").setValue(null);
}
