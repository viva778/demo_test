//----采购入库----

//引入datagrid.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");
loader.import("utility");

//获取是否启用wmsPro模块,并设成全局常量
var integrateWmsPro;
var enablePlace;
//是否生成任务（开启PRO并且开启货位
var generateTask;
var dgDetail;
var dgContainer;
const dgDetailName = "material_1.0.0_purchaseInSingles_purchaseInsingleEditdg1574672991669";
const dgContainerName = "material_1.0.0_purchaseInSingles_purchaseInsingleEditdg1663745892717";
const dgContainerCode = "dg1663745892717";

var cbEnablePlace;
var scRedBlue;
var rfDept;
var rfWarehouse;
var elDgDetail;
var elDgContainer;

var flagCtdRenderOver;
var flagDetailRenderOver;


//表格放大
function elBigup(el1, el2) {
    el1.style.width = "100%";
    el2.style.width = "0%";
    el2.style.display = "none";
}

function elRestore(el1, el2) {
    el1.style.width = "62%";
    el2.style.width = "38%";
    el1.style.display = "";
    el2.style.display = "";
}

function dataInit() {
    integrateWmsPro = (ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.wmspro"
    })["material.wmspro"] == 'true');
    cbEnablePlace = ReactAPI.getComponentAPI("Checkbox")
        .APIs("purchInSingle.wareId.storesetState");
    enablePlace = cbEnablePlace.getValue().value;
    generateTask = (integrateWmsPro && enablePlace);

    dgDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgDetailName);
    dgContainer = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgContainerName);

    rfDept = ReactAPI.getComponentAPI("Reference").APIs("purchInSingle.inDepart.name");
    rfWarehouse = ReactAPI.getComponentAPI("Reference").APIs("purchInSingle.wareId.name");
    scRedBlue = ReactAPI.getComponentAPI("SystemCode").APIs("purchInSingle.redBlue");
    elDgDetail = $("div[keyname='" + dgDetailName + "']").parents(".layout-comp-wrap")[0];
    elDgContainer = $("div[keyname='" + dgContainerName + "']").parents(".layout-comp-wrap")[0];
}

function ptBtnContainer() {
    elRestore(elDgDetail, elDgContainer);
}

function ptBtnCopy() {
    // 选中行对象
    var selRows = dgDetail.getSelecteds();
    if (selRows.length == 0) {
        // 请选择一条记录进行操作
        ReactAPI.showMessage('w', ReactAPI.international.getText("SupDatagrid.button.error"));
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
        copy.uuid = uuid();
        //将之前的只读属性附加上去
        datagrid.appendRowAttr(dgDetailName, copy);
        return copy;
    }), true);
}

function ptBtnWeighingDiff() {
    //计算磅差
    var selRow = dgDetail.getSelecteds()[0];
    if (!selRow) {
        //请选择一条记录进行操作
        ReactAPI.showMessage('w', ReactAPI.international.getText("SupDatagrid.button.error"));
        return;
    }
    if (typeof selRow.weighing1 != "number") {
        //一次过磅数据不存在！
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.container.weighing1_data_not_exist"));
        return;
    }
    if (typeof selRow.weighing2 != "number") {
        //二次过磅数据不存在！
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.container.weighing2_data_not_exist"));
        return;
    }
    //将差值回填到申请数量
    dgDetail.setRowData(selRow.rowIndex, {
        applyQuantity: selRow.weighing1 - selRow.weighing2
    });
}

function ptInit() {
    dataInit();
    //设置图标
    dgDetail.setBtnImg("copy", "sup-btn-own-fzh");
    dgDetail.setBtnImg("weighingDiff", "sup-btn-insert");
    dgDetail.setBtnImg("container", "sup-btn-own-shouyang");

    refreshRequired();
    //设置校验
    datagrid.validator.add(dgDetailName, "applyQuantity", rowData => {
        //物品启用按件管理，入库数量只能为1件
        var batchType = rowData.good && rowData.good.isBatch && rowData.good.isBatch.id;
        if (batchType == "BaseSet_isBatch/piece") {
            return rowData.applyQuantity == 1;
        } else {
            return true;
        }
    }, rowIndex => {
        return ReactAPI.international.getText(
            "material.custom.can.only.beOne",
            String(rowIndex)
        );
    });
    //绑定点击事件
    dgDetail.setClickEvt(function (e, data) {
        showContainerDetail(data);
    });
    dgDetail.setCheckBoxClickEvt(function (e, data) {
        setTimeout(() => {
            var selData = dgDetail.getSelecteds();
            if (selData.length == 1) {
                showContainerDetail(selData[0]);
            } else {
                dgContainer.setDatagridData([]);
            }
        })
    });
}

function ptRenderOver() {
    refreshReadonly();
    //设置批号只读条件（由于是固定的，所以不需要反复刷新
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "batch", rowData => {
        var batchType = rowData.good && rowData.good.isBatch && rowData.good.isBatch.id;
        return batchType != "BaseSet_isBatch/batch" && batchType != "BaseSet_isBatch/piece";
    });
    //如果存在采购订单，则将编号设为只读
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "purOrderNo", rowData => {
        return rowData.purchaseId;
    });
    //设置点击事件
    datagrid.bindEvent.onClick(dgDetailName, "purOrderNo", (rowIndex, rowData) => {
        onclickpurch(rowData);
    });
    //如果uuid为空，设置uuid
    var dgData = dgDetail.getDatagridData();
    var generated;
    dgData.forEach(rowData => {
        if (!rowData.uuid) {
            rowData.uuid = uuid();
            generated = true;
        }
    });
    if (generated) {
        dgDetail.setDatagridData(dgData);
    }
    flagDetailRenderOver = true;
    if (flagCtdRenderOver) {
        ptAllRenderOver();
    }
    setOpenButtenClick()
}


var deleteContainerIds = [];

function ptBtnDelete() {
    var selRows = dgDetail.getSelecteds();
    if (!selRows[0]) {
        ReactAPI.showMessage('w', ReactAPI.international.getText("Reference.confirm.tip.message"));
        return false;
    }

    var srcId$cnt = {};
    dgDetail.getDatagridData().forEach(rowData => {
        if (rowData.srcPartId) {
            var value = srcId$cnt[rowData.srcPartId];
            srcId$cnt[rowData.srcPartId] = (value || 0) + 1;
        }
    });

    var deleteRows = [];
    var errorRows = [];
    for (const rowData of selRows) {
        if (rowData.srcPartId) {
            if (srcId$cnt[rowData.srcPartId] > 1) {
                deleteRows.push(rowData);
                srcId$cnt[rowData.srcPartId]--;
            } else {
                //仅存的一条上游单据下推的数据不能删除
                errorRows.push(rowData.rowIndex + 1);
            }
        } else {
            deleteRows.push(rowData);
        }
    }
    if (errorRows.length) {
        //上游单据，不能删除
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1581399401875", errorRows.join(",")));
        return false;
    } else if (deleteRows.length) {
        //删除对应uuid的数据
        var uuidList = deleteRows.map(val => val.uuid);
        var deleteContainerList = ctDetails.filter(ctDetail => uuidList.includes(ctDetail.purchaseInDetailUuid));
        ctDetails = ctDetails.filter(ctDetail => !uuidList.includes(ctDetail.purchaseInDetailUuid));
        dgDetail.deleteLine(deleteRows.map(rowData => rowData.rowIndex).join(","));
        deleteContainerList.map(val => val.id).filter(val => val).forEach(id => deleteContainerIds.push(id));
    }
}

function oacWarehouse() {
    //清空表体货位
    var dgData = dgDetail.getDatagridData();
    dgData.forEach(rowData => delete rowData.placeSet);
    dgDetail.setDatagridData(dgData);
}

function cbWarehouse(value) {
    var newPlaceState = ReactAPI.request({
        type: "get",
        async: false,
        data: {
            'wareCode': value[0].code
        },
        url: "/msService/material/foreign/foreign/getWareByCode"
    }).data.storesetState;
    if (newPlaceState != enablePlace) {
        enablePlace = newPlaceState;
        generateTask = (integrateWmsPro && enablePlace);
        refreshRequired();
        //设置货位只读
        if (enablePlace) {
            datagrid.readonly.setColReadonly(dgDetailName, "placeSet.name");
        } else {
            datagrid.readonly.removeColReadonly(dgDetailName, "placeSet.name");
        }
    }
    //清空货位
    datagrid.clearColValue(dgDetail, "placeSet");
}

function oacStaff() {
    rfDept.removeValue();
}

function cbStaff(value) {
    var dept = value[0].department;
    rfDept.setValue(dept);
}

function onclickpurch(rowData) {
    var srcID = rowData.purchaseId;
    if (srcID) {
        var tableInfo = '';
        var tableId = '';
        var result = ReactAPI.request({
            url: "/msService/material/purchaseInSingles/purchInSingle/findSrcTableInfoId",
            type: "get",
            data: {
                "srcID": srcID,
                "tableType": "MaterialPurchasePart"
            },
            async: false
        });
        if (result.code == 200) {
            let data = result.data;
            tableInfo = data.result;
            if (result != null && data && data.tableInfoId) {
                tableInfo = data.tableInfoId;
                tableId = data.tableId;
            }
        }
    }

    if (tableInfo) {
        // 采购入库单实体的entityCode
        var entityCode = "material_1.0.0_purchaseInfos";
        // 采购入库单实体的查看视图URL
        var url = "/msService/material/purchaseInfos/purchaseInfo/purchaseView";
        // 当前页面的URL
        var currentPageURL = window.location.href;
        // 菜单操作编码
        var operateCode = "material_1.0.0_purchaseInfos_purchaseList_self";
        var pcMap = ReactAPI.getPowerCode(operateCode);
        var pc = pcMap[operateCode];
        url += "?tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + tableId + "&__pc__=" + pc;
        window.open(url);
    }
}


function ocgRedBlue(value) {
    var before = scRedBlue.getValue().value;

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
                    ReactAPI.setHeadBtnAttr('redRef', {
                        icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference',
                        isHide: false
                    });
                    $("#btn-addRow").hide();
                    return false;
                },
                onCancel: () => {
                    scRedBlue.setValue(before);
                    ReactAPI.closeConfirm();
                    return false;
                }
            });
        } else {
            $("#btn-addRow").hide();
            ReactAPI.setHeadBtnAttr('redRef', {icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: false});
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
                    $("#btn-addRow").show();
                    ReactAPI.setHeadBtnAttr('redRef', {
                        icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference',
                        isHide: true
                    });
                    return false;
                },
                onCancel: () => {
                    scRedBlue.setValue(before);
                    ReactAPI.closeConfirm();
                    return false;
                }
            });
        } else {
            $("#btn-addRow").show();
            ReactAPI.setHeadBtnAttr('redRef', {icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: true});
        }
    }
}

function onLoad() {
    dataInit();
    // 赋值业务类型为采购入库事务
    ReactAPI.getComponentAPI("Reference").APIs("purchInSingle.serviceTypeId.serviceTypeExplain").setValue({
        id: 1007,
        serviceTypeCode: "purchaseStorageIn",
        serviceTypeExplain: ReactAPI.international.getText("material.custom.PurchaseStockTransaction")
    });
    var inCome = ReactAPI.getComponentAPI("Reference").APIs("purchInSingle.inCome.reasonExplain").getValue()[0];
    if (undefined == inCome) {
        ReactAPI.getComponentAPI("Reference").APIs("purchInSingle.inCome.reasonExplain").setValue({
            id: 1016,
            reasonExplain: ReactAPI.international.getText("material.custom.PurchaseWarehousing")
        });
    }
    var srcId = ReactAPI.getComponentAPI("InputNumber").APIs("purchInSingle.srcId").getValue();
    // 若采购到货单ID不为空, 表示单据由采购到货单下推生成, 隐藏增行按钮
    if (srcId) {
        $("#btn-addRow").hide();

    }
    var value = scRedBlue.getValue().value;

    if (value == "BaseSet_redBlue/red") {
        ReactAPI.setHeadBtnAttr('redRef', {icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: false});
    } else {
        ReactAPI.setHeadBtnAttr('redRef', {icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: true});
    }

    //添加采购到货单超链接
    var tableNo = ReactAPI.getComponentAPI("Input").APIs("purchInSingle.purArrivalNo").getValue();
    var srcID = ReactAPI.getComponentAPI("InputNumber").APIs("purchInSingle.srcId").getValue();
    ReactAPI.getComponentAPI("Input").APIs("purchInSingle.purArrivalNo").replace("<div class='supplant-readonly-wrap supplant-comp readonly'>" + tableNo + "</div>", {
        onClick: function (e) {
            getPurArrial(srcID);
        },
    });
    elBigup(elDgDetail, elDgContainer);
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
    var readonly_keys = [];
    var rw_keys = [];
    var isRed = scRedBlue.getValue().value == "BaseSet_redBlue/red";
    if (!enablePlace || isRed) {
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

var ignoreConfirmFlag = false;

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
            ReactAPI.international.getText("1")
            return false;
        }

        //校验超储,归并同物料
        if (!ignoreConfirmFlag) {
            var checked_idx = new Set();
            for (var i = 0; i < dgData.length; i++) {
                if (!checked_idx.has(i)) {
                    var material = dgData[i].good;
                    //统计物料数量
                    var quan = dgData[i].applyQuantity;
                    for (var j = i + 1; j < dgData.length; j++) {
                        if (!checked_idx.has(j)) {
                            if (material.id == dgData[j].good.id) {
                                checked_idx.add(j);
                                quan += dgData[k].applyQuantity;
                            }
                        }
                    }
                    //统计完成，进行校验
                    if (!check_material_limit(warehouse, material, quan)) {
                        ReactAPI.international.getText("2")
                        return false;
                    }
                }
            }
        }
    }
    //保存容器数据
    if (ctDetails.length) {
        var sort = 1;
        ReactAPI.setSaveData({
            dgList: {
                [dgContainerCode]: JSON.stringify(ctDetails.map(ctd => new Object({
                    id: ctd.id,
                    container: {
                        id: ctd.container.id
                    },
                    material: {
                        id: ctd.material.id
                    },
                    purchaseInDetailUuid: ctd.purchaseInDetailUuid,
                    quantity: ctd.quantity,
                    sort: sort++
                })))
            }
        });
    }
    //增加删除数据id
    if (deleteContainerIds.length) {
        var originDeleteIds = ReactAPI.getSaveData().dgDeletedIds[dgContainerCode];
        var deleteIds;
        if (originDeleteIds) {
            deleteIds = originDeleteIds + "," + deleteContainerIds.join(",");
        } else {
            deleteIds = deleteContainerIds.join(",");
        }
        ReactAPI.setSaveData({
            dgDeletedIds: {
                [dgContainerCode]: deleteIds
            }
        })
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
 * 获取采购到货单的tableInfoId并打开查看视图
 */
function getPurArrial() {
    var tableInfo = '';
    var srcID = ReactAPI.getComponentAPI("InputNumber").APIs("purchInSingle.srcId").getValue();
    if (srcID) {
        ReactAPI.request({
                type: "get",
                data: {
                    "srcID": srcID,
                    "tableType": "MaterialPurArrivalInfo"
                },
                url: "/msService/material/purchaseInSingles/purchInSingle/findSrcTableInfoId",
                async: false
            },
            function (msg) {
                if (msg != null && msg.data && msg.data.tableInfoId) {
                    tableInfo = msg.data.tableInfoId;
                }
            }
        );
    }
    if (tableInfo) {
        // 采购到货单实体的entityCode
        var entityCode = "material_1.0.0_purArrivalInfos";
        // 采购到货单实体的查看视图URL
        var url = "/msService/material/purArrivalInfos/purArrivalInfo/purArrivalInfoView";
        // 当前页面的URL
        var currentPageURL = window.location.href;
        // 菜单操作编码
        var operateCode = "material_1.0.0_purArrivalInfos_purArrivalInfoList_self";
        var pcMap = ReactAPI.getPowerCode(operateCode);
        var pc = pcMap[operateCode];
        url += "?tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + srcID + "&__pc__=" + pc;
        window.open(url);
    }
}

var ctDetails = [];

function ptBtnContainerRef() {
    var selDetails = dgDetail.getSelecteds();
    if (selDetails.length != 1) {
        //请选择一条入库单明细！
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.inbound.select_detail_before_do_some_things"));
        return false;
    }

    function container_detail_callback(data, event) {
        if (!data.length) {
            //请至少选择一条数据！
            event.ReactAPI.showMessage('w', ReactAPI.international.getText("ec.ec_view_select_property.selectNullData"));
            return false;
        }
        var addData = dgContainer.addLine(data.map(cd => new Object({
            container: cd.container,
            material: cd.materInfo,
            quantity: cd.materQty,
            purchaseInDetailUuid: selDetails[0].uuid
        })), true);
        addData.forEach(rowData => {
            ctDetails.push(rowData);
        })
        var totalQuan = dgContainer.getDatagridData().map(val => val.quantity).reduce((v1, v2) => v1 + v2);
        //设置申请数量只读，增加新增数量
        dgDetail.setRowData(selDetails[0].rowIndex, {
            "applyQuantity_attr": {
                readonly: true
            },
            "applyQuantity": totalQuan
        });
        ReactAPI.destroyDialog("container_details");
    }

    // var purArrivalInfo = ReactAPI.getComponentAPI("Input").APIs("purchInSingle.purArrivalNo").getValue()
    ReactAPI.createDialog("container_details", {
        title: ReactAPI.international.getText("material.containerFile.ContainerParts"), //容器明细
        url: "/msService/material/containerFile/containerParts/containerDetailRef?" + toStringCondition({
            materialId: selDetails[0].good.id,
            batchNum: selDetails[0].batch,
            purArrivalInfo: selDetails[0].srcPartId,
        }),
        size: 5,
        callback: function callback(data, event) {
            container_detail_callback(data, event);
        },
        isRef: true,
        onOk: function onOk(data, event) {
            container_detail_callback(data, event);
        },
        onCancel: function onCancel(data, event) {
            ReactAPI.destroyDialog("container_details");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
    });
}

function ptBtnContainerDetailDelete() {
    var selCtDetails = dgContainer.getSelecteds();
    if (!selCtDetails.length) {
        //请至少选择一条数据！
        ReactAPI.showMessage('w', ReactAPI.international.getText("ec.ec_view_select_property.selectNullData"));
        return false;
    }
    //删除明细缓存
    selCtDetails.forEach(ctDetail => {
        ctDetails = ctDetails.filter(val => val.container.id != ctDetail.container.id || val.material.id != ctDetail.material.id);
    });
    //删行
    dgContainer.deleteLine(selCtDetails.map(val => val.rowIndex).join(","));
    var selDetail = dgDetail.getSelecteds()[0];
    if (!dgContainer.getDatagridData().length) {
        //取消明细申请数量只读
        dgDetail.setRowData(selDetail.rowIndex, {
            "applyQuantity_attr": {
                readonly: false
            },
            "applyQuantity": 0
        })
    } else {
        var decQuan = selCtDetails.map(val => val.quantity).reduce((v1, v2) => v1 + v2);
        dgDetail.setRowData(selDetail.rowIndex, {
            "applyQuantity": selDetail.applyQuantity - decQuan
        });
    }
}


function ptContainerInit() {
    dataInit();
}

function ptContainerRenderOver() {
    //首次进入缓存数据
    var dgData = dgContainer.getDatagridData();
    if (dgData.length) {
        dgData.forEach(rowData => {
            ctDetails.push(rowData);
        });
        dgContainer.setDatagridData([]);
    }
    flagCtdRenderOver = true;
    if (flagDetailRenderOver) {
        ptAllRenderOver();
    }
}

function ptAllRenderOver() {
    //入库数量只读条件，在存在容器明细时只读
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "applyQuantity", rowData => {
        return ctDetails.filter(val => val.purchaseInDetailUuid == rowData.uuid).length;
    });
}

function showContainerDetail(rowData) {
    var ctds = ctDetails.filter(val => val.purchaseInDetailUuid == rowData.uuid);
    if (ctds.length) {
        elRestore(elDgDetail, elDgContainer);
    }
    dgContainer.setDatagridData(ctds);
}

function toStringCondition(condition) {
    var valid_keys = Object.keys(condition).filter(key => condition[key]);
    if (valid_keys.length) {
        return "customConditionKey=" + valid_keys.join(",") + "&" + valid_keys.map(key => key + "=" + condition[key]).join("&");
    }
    return ""
}
