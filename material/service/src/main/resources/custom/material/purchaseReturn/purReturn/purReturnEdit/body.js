//-----采购退货出库-----

//引入datagrid.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");
loader.import("material");

const dgDetailName = "material_1.0.0_purchaseReturn_purReturnEditdg1574990545044";
var dgDetail;

//获取是否启用wmsPro模块,并设成全局常量
var integrateWmsPro;
var generateTask;
var enablePlace;
var rfWarehouse;


function dataInit() {
    dgDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgDetailName);
    rfWarehouse = ReactAPI.getComponentAPI("Reference").APIs("purReturn.warehouse.name");

    enablePlace = ReactAPI.getComponentAPI("Checkbox").APIs("purReturn.warehouse.storesetState").getValue().value;
    integrateWmsPro = (ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.wmspro",
    })["material.wmspro"] == 'true');
    generateTask = enablePlace && integrateWmsPro;
    dataInit = () => { }
}

function onLoad() {
    dataInit();
    // 赋值业务类型为采购退货事务
    ReactAPI.getComponentAPI("Reference").APIs("purReturn.serviceType.serviceTypeExplain").setValue({
        id: 1012,
        serviceTypeCode: "returnedPurchaseOut",
        serviceTypeExplain: ReactAPI.international.getText("material.custom.PurchaseReturnAffair")
    }
    );
    var outCome = ReactAPI.getComponentAPI("Reference").APIs("purReturn.outType.reasonExplain").getValue()[0];
    if (!outCome) {
        ReactAPI.getComponentAPI("Reference").APIs("purReturn.outType.reasonExplain").setValue({ id: 1021, reasonExplain: ReactAPI.international.getText("material.custom.PurchaseReturn") });
    }

    ReactAPI.setHeadBtnAttr('refPurchaseInPart', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference' });

    //设置校验
    datagrid.validator.add(dgDetailName, "applyNum", rowData => {
        //申请数量+已退货数量<=实际入库数量
        var applyQuan = rowData.applyNum;
        var rtnQuan = (rowData.purchaseInSingleDetail && rowData.purchaseInSingleDetail.returnNum) || 0;
        var inQuan = (rowData.purchaseInSingleDetail && rowData.purchaseInSingleDetail.inQuantity) || 0;
        return !inQuan || applyQuan + rtnQuan <= inQuan;
    }, (rowIndex, title, rowData) => {
        var rtnQuan = (rowData.purchaseInSingleDetail && rowData.purchaseInSingleDetail.returnNum) || 0;
        var inQuan = rowData.purchaseInSingleDetail.inQuantity;
        //分成三种情况
        //1.退货量为0，提示申请退货量不能大于入库量
        if (!rtnQuan) {
            return ReactAPI.international.getText(
                "material.datagrid.validator.less_equal_constraint",
                String(rowIndex), title, datagrid.dom.get_header_text(dgDetailName, "purchaseInSingleDetail.inQuantity")
            );
        }
        //2.退货量>=入库量，提示退货已完成，不能继续退货
        if (rtnQuan >= inQuan) {
            return ReactAPI.international.getText(
                //第{0}行，退货已完成，不允许继续退货
                "material.return.description.overhead",
                String(rowIndex)
            );
        }
        //3.申请量>剩余量=入库量-退货量，提示已退货量为{1}，申请退货量应在{2}及以下
        return ReactAPI.international.getText(
            //第{0}行，已退货量为{1}，申请退货量应在{2}及以下
            "material.purchase.return.amount_check",
            String(rowIndex), String(rtnQuan), String(inQuan - rtnQuan)
        );
    });
}


function refreshRequired() {
    //不生成上下架，则可用量必填
    if (!generateTask) {
        datagrid.validator.required.setColRequired(dgDetailName, [{
            key: "stockOnHand.availiQuantity",
            type: "object"
        }]);
    } else {
        datagrid.validator.required.removeColRequired(dgDetailName, [{
            key: "stockOnHand.availiQuantity"
        }]);
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
                    ReactAPI.getComponentAPI("Reference").APIs("purReturn.warehouse.name").setValue(warehouseBeforeChange);
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
                    ReactAPI.getComponentAPI("Reference").APIs("purReturn.warehouse.name").setValue(warehouseBeforeChange);
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
    }
}


function ptBtnBizDetail() {
    //拼接查询条件
    var keys = ["bizType"];
    var conditions = ["multiSelect=true", "bizType=purchaseStorageIn"];
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
        callback: (data, event) => biz_detail_callback(data, event),
        isRef: true,
        onOk: (data, event) => biz_detail_callback(data, event),
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
                }
            }).filter(v => v);
            if (notFoundHints.length) {
                event.ReactAPI.showMessage('w', notFoundHints.join("<br>"));
                return false;
            }
            //查询对应采购入库单表体
            var ids = data.map(rowData => rowData.tableBodyID).filter(val => val).join(",");
            var id$srcDetail = {};
            if (ids) {
                var result = ReactAPI.request({
                    type: "get",
                    url: "/msService/material/entity/getEntityList",
                    data: {
                        moduleName: "material",
                        entityName: "MaterialPurchInPart",
                        ids: ids,
                        includes: "id,returnNum,inQuantity"
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
                    stockOnHand: stocks[idx],
                    product: rowData.good,
                    purchaseInSingle: rowData.tableHeadID ? {
                        id: parseInt(rowData.tableHeadID)
                    } : null,
                    purchaseInSingleDetail: srcDetail
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
        } else {
            //请至少选中一行
            event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1574406106043"));
            return false;
        }
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
                    product: rowData
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

function ptocgStock(value) {
    if (value && value[0] && value[0].ware) {
        var warehouse = rfWarehouse.getValue()[0];
        if (!warehouse || !warehouse.id) {
            rfWarehouse.setValue(value[0].ware);
            refreshPlaceEnable(value[0].ware.storesetState);
        }
    }
}


function ptInit() {
    dataInit();
    refreshRequired();
    //设置校验
    datagrid.validator.add(dgDetailName, "applyNum", rowData => {
        //按件时
        if (rowData.product && rowData.product.isBatch && rowData.product.isBatch.id == "BaseSet_isBatch/piece") {
            //如果存在现存量，则数量必须为1
            if (rowData.stockOnHand && rowData.stockOnHand.id) {
                return rowData.applyNum == 1;
            } else {
                //如果不存在现存量，则申请量只能为整数
                return rowData.applyNum % 1 == 0;
            }
        } else {
            return true;
        }
    }, (rowIndex, titile, rowData) => {
        if (rowData.stockOnHand && rowData.stockOnHand.id) {
            //物品批号{0}已开启按件管理，数量只能为1。
            return ReactAPI.international.getText(
                "material.validator.by_piece_quantity_check",
                "[" + rowData.product.name + "/" + rowData.stockOnHand.batchText + "]"
            );
        } else {
            //物品批号{0}已开启按件管理，数量只能为整数。
            return ReactAPI.international.getText(
                "material.validator.by_piece_int_quantity_check",
                "[" + rowData.product.name + "]"
            );
        }
    });
}

function ptRenderOver() {
    //设置现存量只读条件（由于是固定的，所以不需要反复刷新
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "stockOnHand.availiQuantity", rowData => {
        //来自上游参照时只读
        return rowData.purchaseInSingleDetail;
    });
}

function ocgStaff(value) {
    if (value && value[0]) {
        ReactAPI.getComponentAPI("Reference").APIs("purReturn.returnDepartment.name").setValue(value[0].department[0]);
    } else {
        ReactAPI.getComponentAPI("Reference").APIs("purReturn.returnDepartment.name").removeValue();
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
        //校验退货总量
        var id$rtn_info = {};
        dgData.forEach(rowData => {
            var srcDetail = rowData.purchaseInSingleDetail
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
                        in_num: srcDetail.inQuantity || 0
                    };
                }
            }
        });
        //进行校验
        var hint = Object.values(id$rtn_info).filter(info => {
            return info.apply_num + info.rtn_num > info.in_num;
        }).map(info => ReactAPI.international.getText(
            "material.purchase.return.amount_check",
            String(info.row_idx.map(idx => idx + 1).join(",")), String(info.rtn_num), String(info.in_num - info.rtn_num)
        )).join("<br>");
        if (hint) {
            ReactAPI.showMessage('w', hint);
            return false;
        }
    }
}