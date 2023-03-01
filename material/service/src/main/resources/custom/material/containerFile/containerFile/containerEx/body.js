//-----容器台账-----
//引入datagrid.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");

const dgListName = "material_1.0.0_containerFile_containerExdg1663553401070";
const dgDetailName = "material_1.0.0_containerFile_containerExdg1663553435814";
var dgList;
var dgDetail;
var new_data_key = [];
var currentContainerId;

function dataInit() {
    dgList = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgListName);
    dgDetail = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgDetailName);
    dataInit = () => {
    }
}


function ptListInit() {
    dataInit();
    //绑定点击事件
    dgList.setClickEvt((e, rowData) => {
        listOnClick(rowData);
    });
}

function ptListRenderOver() {
    new_data_key = [];
    dgDetail.deleteLine();
    dgDetail.setBtnHidden(["btn-confirm", "btn-arrivalDetail", "btn-delete", "btn-uninstall"]);
    currentContainerId = undefined;
}

function ptDetailInit() {
    dataInit();
    dgDetail.setBtnImg("confirm", "sup-btn-own-pljd");
    //首次进入隐藏按钮
    dgDetail.setBtnHidden(["btn-confirm", "btn-arrivalDetail", "btn-delete", "btn-uninstall"]);
    //设置按钮样式
    dgDetail.setBtnImg("btn-uninstall", "sup-btn-own-qzdc");
    //增加校验
    datagrid.validator.add(dgDetailName, "materQty", rowData => {
        if (rowData.materQty) {
            return true;
        } else {
            return false;
        }
    }, (rowIndex) => {
        //第{0}行,数量不能为空
        return ReactAPI.international.getText(
            "material.datagrid.validator.no_materQty",
            String(rowIndex)
        );

    });
    datagrid.validator.add(dgDetailName, "materQty", rowData => {
        if (typeof rowData.materQty === "number" && rowData.materQty <= 0) {
            return false;
        } else {
            return true;
        }
    }, (rowIndex, title) => {
        //第{0}行,「{1}」必须大于等于「{2}」
        return ReactAPI.international.getText(
            "material.datagrid.validator.more_constraint",
            String(rowIndex), title, "0"
        );

    });


}

function ptDetailRenderOver() {
    //设置只读条件
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "materQty", rowData => {
        return rowData.id;
    });
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "materUnit.name", rowData => {
        return rowData.id;
    });
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "unloadOver", rowData => {
        return rowData.id;
    });
    new_data_key = [];
}

function listOnClick(rowData) {
    currentContainerId = rowData && rowData.id;
    //根据启用状态判断是否需要隐藏绑定按钮
    if (rowData.useState && rowData.useState.id == "material_ruleState/started") {
        dgDetail.setBtnHidden([]);
    } else {
        dgDetail.setBtnHidden(["btn-confirm", "btn-arrivalDetail", "btn-delete", "btn-uninstall"]);
    }
    refreshDetail();
}


function refreshDetail() {
    //刷新明细
    dgDetail.refreshDataByRequst({
        type: "POST",
        url: "/msService/material/containerFile/containerFile/data-dg1663553435814?datagridCode=material_1.0.0_containerFile_containerExdg1663553435814&id=" + currentContainerId
    });
}

function getBatchRefParams() {
    var selRow = dgDetail.getSelecteds()[0];
    var materialId = selRow && selRow.materInfo && selRow.materInfo.id;
    if (materialId) {
        return "materialId=" + materialId;
    } else {
        return "";
    }
}


function ptBtnArrivalDetail() {
    //打开采购到货明细参照
    ReactAPI.createDialog("arrival_detail_ref", {
        title: ReactAPI.international.getText("material.viewdisplayName.randon1593502505901"), //采购到货明细
        url: "/msService/material/purArrivalInfos/purArrivalPart/purArrivalPartRef",
        size: 5,
        callback: function callback(data, event) {
            ad_callback(data, event);
        },
        isRef: true, // 是否开启参照
        onOk: function onOk(data, event) {
            ad_callback(data, event);
        },
        onCancel: function onCancel(data, event) {
            ReactAPI.destroyDialog("arrival_detail_ref");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
    });

    function ad_callback(selData, event) {
        if (!selData.length) {
            //请至少选择一条数据！
            event.ReactAPI.showMessage('w', ReactAPI.international.getText("ec.ec_view_select_property.selectNullData"));
            return;
        }
        //判断重复
        var dgData = dgDetail.getDatagridData();
        var batchKeys = dgData.map(rowData => rowData.materInfo.id + "@" + (rowData.materBatch && rowData.materBatch.batchNum || "") + "@" + (rowData.storeset ? rowData.storeset.code : ""));
        var conflictRows = selData.filter(ad => {
            var batchKey = ad.good.id + "@" + (ad.batch || "") + "@" + (ad.ware && ad.ware.code || "");
            return batchKeys.includes(batchKey);
        }).map(rowData => rowData.rowIndex + 1).join(",");
        if (conflictRows) {
            //第{0}行，数据重复
            event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.data_repeat", conflictRows));
            return false;
        }
        var unloadOvers = selData.filter(e => {
            return e.unloadOver === true
        });
        if (unloadOvers && unloadOvers.length > 0) {
            //增加是否卸载完毕提示
            ReactAPI.openConfirm({
                message: ReactAPI.international.getText("material.container.confirmed_add_unloadover"),
                buttons: [
                    {
                        operatetype: "yes",
                        text: ReactAPI.international.getText("ec.common.confirm"),//确定
                        type: "primary",
                        onClick: function () {
                            addContainerDetails(selData)
                            ReactAPI.closeConfirm();
                        }
                    },
                    {
                        operatetype: "cancel",
                        text: ReactAPI.international.getText("ec.common.cancel"),
                        onClick: function () {
                            ReactAPI.closeConfirm();
                        }
                    }
                ]
            });
        } else {
            addContainerDetails(selData)
        }
    }
}


function addContainerDetails(selData) {
    //根据物料和批号查找批次信息
    var result = ReactAPI.request({
        url: "/msService/material/entity/getEntityList",
        type: "get",
        async: false,
        data: {
            moduleName: "material",
            entityName: "MaterialBatchInfo",
            conditions: selData.map(ad => "materialId.id=" + ad.good.id + "&batchNum=" + (ad.batch || "null")).join("|"),
            includes: "id,batchNum,materialId.id"
        }
    });
    if (result.code != 200) {
        event.ReactAPI.showMessage('f', result.message);
        return;
    }
    var newData = dgDetail.addLine(selData.map(ad => {
        //找到对应批次信息
        var theBatchInfo;
        for (const batchInfo of result.data) {
            if (ad.good.id == batchInfo.materialId.id) {
                if (ad.batch == batchInfo.batchNum || !ad.batch && !batchInfo.batchNum) {
                    theBatchInfo = batchInfo;
                    break;
                }
            }
        }
        //创建对象
        var rowData = {
            materInfo: ad.good,
            materBatch: theBatchInfo || null,
            materUnit: ad.good.purchaseUnit,
            materQty: ad.arrivalQuan,
            arrivalPart: ad.id,
            unloadOver: ad.unloadOver,
            storeset: ad.ware
        };
        //按件时数量只读
        if (ad.good.isBatch && ad.good.isBatch.id == "BaseSet_isBatch/piece") {
            rowData.materQty_attr = {
                readonly: true
            }
        }
        //不允许更改卸货完毕
        if (ad.unloadOver) {
            rowData.unloadOver_attr = {
                readonly: true
            }
        }
        return rowData;
    }), true);
    newData.forEach(rowData => {
        new_data_key.push(rowData.key);
    });
    ReactAPI.destroyDialog("arrival_detail_ref");
}

function ptBtnConfirm() {
    if (!new_data_key.length) {
        //装载数据未发生变化！
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.container.data_aint_changed"));
        return false;
    }
    //进行数据校验
    if (!datagrid.validator.check(dgDetailName)) {
        return false;
    }
    var dgData = dgDetail.getDatagridData();
    var changedData = dgData.filter(rowData => new_data_key.includes(rowData.key)).map(rowData => new Object({
        container: {
            id: currentContainerId
        },
        materInfo: rowData.materInfo,
        materBatch: rowData.materBatch,
        materUnit: rowData.materUnit,
        materQty: rowData.materQty,
        unloadOver: rowData.unloadOver,
        storeset: rowData.storeset,
        arrivalPart: {
            id: rowData.arrivalPart
        }

    }));
    //调用保存
    ReactAPI.openLoading();
    ReactAPI.request({
        url: "/msService/material/container/checkoutContainerDetails",
        type: "post",
        async: true,
        data: changedData
    }, res => {
        if (res.code != 200) {
            ReactAPI.showMessage('f', res.message);
            ReactAPI.closeLoading();
        } else {
            new_data_key = [];
            ReactAPI.openLoading(res.message, "2", true);
            refreshDetail();
            setTimeout(() => {
                ReactAPI.closeLoading();
            }, 500);
        }
    });
}

function ptBtnDelete() {
    var selData = dgDetail.getSelecteds();
    if (!selData.length) {
        //请至少选择一条数据！
        ReactAPI.showMessage('w', ReactAPI.international.getText("ec.ec_view_select_property.selectNullData"));
        return false;
    }
    //数据不存在id才能删
    var dataWithId = selData.filter(rowData => rowData.id);
    if (dataWithId.length) {
        //已确认数据不允许删除！
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.container.checked_data_cannot_be_deleted"));
        return false;
    }
    dgDetail.deleteLine(selData.map(rowData => rowData.rowIndex).join(","));
}

function uninstall() {
    var selData = dgDetail.getSelecteds();
    if (!selData.length) {
        //请至少选择一条数据！
        ReactAPI.showMessage('w', ReactAPI.international.getText("ec.ec_view_select_property.selectNullData"));
        return false;
    }
    //数据存在id才能删
    var dataNotWithId = selData.filter(rowData => !rowData.id);
    if (dataNotWithId.length) {
        //当前到货明细未确认无需卸载！
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.container.Unconfirmed_data_cannot_be_deleted"));
        return false;
    }
    //确认要卸载当前到货明细吗？
    ReactAPI.openConfirm({
        message: ReactAPI.international.getText("material.container.confirmed_uninstall"),
        buttons: [
            {
                operatetype: "yes",
                text: ReactAPI.international.getText("ec.common.confirm"),//确定
                type: "primary",
                onClick: function () {
                    //校验是否存在非生效状态的采购入库单绑定过此容器
                    //根据物料和查询采购入库单容器明细
                    var deleteData = selData[0];
                    var detail = ReactAPI.request({
                        url: "/msService/material/container/queryDetailsInPurchaseInSingles",
                        type: "post",
                        async: false,
                        data: deleteData

                    });
                    if (detail.code != 200) {
                        ReactAPI.showMessage('f', detail.message);
                        return;
                    }
                    if (detail.data != null && detail.data.length > 0) {
                        //  XX容器已被（XXX采购入库单）绑定，确定要卸载当前到货明细吗？
                        let title = detail.data.map(rowData => rowData.tableNo).join(",")
                        ReactAPI.openConfirm({
                            message: ReactAPI.international.getText("material.container.container_is_bound", title),
                            buttons: [
                                {
                                    operatetype: "yes",
                                    text: ReactAPI.international.getText("ec.common.confirm"),//确定
                                    type: "primary",
                                    onClick: function () {
                                        uninstallDelete(selData)
                                        ReactAPI.closeConfirm();
                                    }
                                },
                                {
                                    operatetype: "cancel",
                                    text: ReactAPI.international.getText("ec.common.cancel"),
                                    onClick: function () {
                                        ReactAPI.closeConfirm();
                                    }
                                }
                            ]
                        });
                    } else {
                        uninstallDelete(selData)
                    }
                    ReactAPI.closeConfirm();

                }
            },
            {
                operatetype: "cancel",
                text: ReactAPI.international.getText("ec.common.cancel"),
                onClick: function () {
                    ReactAPI.closeConfirm();
                }
            }
        ]
    });


}

function uninstallDelete(selData) {
    // dgDetail.deleteLine(selData.map(rowData => rowData.rowIndex).join(","));
    let map = selData.map(rowData => rowData.id);
    //调用删除
    ReactAPI.openLoading();
    ReactAPI.request({
        url: "/msService/material/container/deletePurchaseInContainerDetails",
        type: "post",
        async: true,
        data: map
    }, res => {
        if (res.code != 200) {
            ReactAPI.showMessage('f', res.message);
            ReactAPI.closeLoading();
        } else {
            new_data_key = [];
            ReactAPI.openLoading(res.message, "2", true);
            refreshDetail();
            setTimeout(() => {
                ReactAPI.closeLoading();
            }, 500);
        }
    });
}

