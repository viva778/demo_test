var dg;
var sp;
var nt;

function ptInit() {
    //1.数据初始化
    dg = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_wareModel_wareLayout_wareModel_sdg");
    sp = ReactAPI.getComponentAPI("SearchPanel").APIs("material_1.0.0_wareModel_wareLayout_wareModel_sp");
    nt = ReactAPI.getComponentAPI("NavTree").APIs("material_1.0.0_wareModel_wareLayout_wareModel_nt");
    //2.修改按钮样式
    dg.setBtnImg("btn-wareOpen", "sup-btn-own-qiyong");
    dg.setBtnImg("btn-disable", "sup-btn-own-ty");
    dg.setBtnImg("btn-batchEdit", "sup-btn-own-stfz")
    dg.setBtnImg("btn-synchronous", "sup-btn-own-tb");
    dg.setBtnImg("exportTag", "sup-btn-own-qzdc");
    //3.设置增加按钮校验
    dg.setBeforeInBtn("add", function () {
        var selNode = nt.getSelectedTreeNode();
        if (selNode && selNode.wareType.code == "storeSet") {
            ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.notAllowedToAddMore"));
            return false;
        }
    });
}




/**
 * 设置上下限
 */
function ptBtnSetStoreLimit() {
    //1.获取选中行
    if (dg.getSelecteds().length != 1) {
        //请选中一行
        ReactAPI.showMessage('w', ReactAPI.international.getText("ec.entity.wf.choice"));
        return false;
    }
    var selData = dg.getSelecteds()[0];

    localStorage.setItem("model.code", selData.code);
    localStorage.setItem("model.name", selData.name);
    localStorage.setItem("model.id", selData.id);
    //2.打开仓库库存设置页面
    ReactAPI.createDialog("wareAuthSetDailog", {
        //物料库存设置
        title: ReactAPI.international.getText("material.viewtitle.randon1639809513857"),
        size: 5,
        url: "/msService/material/wareModel/wareModel/storeSetEdit",
        onOk: function (event) {
            event.ReactAPI.submitFormData("save");
            var saveData = event.ReactAPI.getSaveData();
            if (saveData) {
                //处理中
                event.ReactAPI.openLoading(ReactAPI.international.getText("EditView.notice.processing"), '1', true);
                //编辑保存数据为指定仓库
                var partDeleteDg = saveData.dgDeletedIds.dg1639810510486;
                var partDg = saveData.dgList.dg1639810510486;
                if (partDg) {
                    partDg = JSON.parse(partDg);
                    for (var i = 0; i < partDg.length; i++) {
                        partDg[i].wareModel = partDg[i].wareModel || {};
                        partDg[i].wareModel.id = selData.id;
                    }
                    partDg = JSON.stringify(partDg);
                }
                //自定义保存
                var res = ReactAPI.request({
                    url: "/msService/material/wareModel/WareModel/saveScoketSetPart",
                    type: "post",
                    async: false,
                    data: { partDeleteDg: partDeleteDg, dgPartList: partDg },
                });
                //成功后关闭
                event.ReactAPI.closeLoading();
                __result_processer(res, () => {
                    event.ReactAPI.openLoading(ReactAPI.international.getText("EditView.notice.operate.success"), '2', true);
                    setTimeout(function () {
                        event.ReactAPI.closeLoading();
                        //关闭编辑页面
                        ReactAPI.destroyDialog("wareAuthSetDailog");
                    }, 1000);
                }, undefined, event.ReactAPI);
            }
        },
        okText: ReactAPI.international.getText("Button.text.save"),
        onCancel: function () {
            ReactAPI.destroyDialog("wareAuthSetDailog");
        }
    });
}


/**
 * 设置入口
 */
function ptBtnSetEntrance() {
    //1.排除情况
    var selRows = dg.getSelecteds();
    if (selRows.length == 0) {
        //请选择一条记录进行操作！
        ReactAPI.showMessage("w", ReactAPI.international.getText("SupDatagrid.button.error"));
        return false;
    } else if (selRows.length > 1) {
        //每个仓库只能设置一个出口货位！
        ReactAPI.showMessage("w", ReactAPI.international.getText("material.custom.EachWarehouseCanOnlySetUpOneExportSpace"));
        return false;
    }
    var selData = selRows[0];
    if (selData.exportTag) {
        //第{0}行，已启用，无需再次启用！
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.noNeedToEnableAgain", "" + (selData.rowIndex + 1)));
        return false;
    }
    if (selData.wareType.id != 'material_wareType/storeSet') {
        //选中的不是货位
        ReactAPI.showMessage("w", ReactAPI.international.getText("material.custom.random1626229033172"));
        return false;
    }

    //2.调用接口
    var res = ReactAPI.request({
        url: "/msService/material/wareModel/wareModel/changeExportTag",
        type: 'get',
        async: false,
        data: { "storeSetId": selData.id },
    });
    __result_processer(res, () => {
        ReactAPI.showMessage("s", ReactAPI.international.getText("EditView.notice.operate.success"));
        sp.updateSearch();
    });
}


/**
 * 数据同步
 */
function ptBtnSync() {
    ReactAPI.openLoading("同步中", '1', true);
    ReactAPI.request({
        url: "/msService/material/warehouse/warehouse/wareSynchronous",
        type: 'get',
        async: true,
    }, res => {
        __result_processer(res, () => {
            ReactAPI.showMessage("s", ReactAPI.international.getText("EditView.notice.operate.success"));
            ReactAPI.closeLoading();
            nt.refreshTreeNode(-1);
            sp.updateSearch();
        }, () => ReactAPI.closeLoading());
    });
}


/**
 * 批量新增
 */
function ptBtnBatchAddition() {
    var selNode = nt.getSelectedTreeNode();
    //排除情况
    if (!selNode) {
        ReactAPI.showMessage('w', ReactAPI.international.getText("TreeList.NavTree.chooseNode"));
        return false;
    }
    if (selNode.id == -1) {
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.pleaseReSelectTheNode"));
        return false;
    }
    if (!["cargoArea", "area"].includes(selNode.wareType.code)) {
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.pleaseReSelectTheNode"));
        return false;
    }
    localStorage.setItem("nodeId", selNode.id);

    //打开批量新增对话框
    ReactAPI.createDialog("BatchAddition", {
        title: ReactAPI.international.getText("material.viewtitle.randon1615971495579"),
        size: 4,
        url: "/msService/material/wareModel/quicklyCreate/storeSetFastEdit",
        buttons: [
            {
                text: ReactAPI.international.getText("Button.text.save"),
                type: "primary",
                onClick: function (event) {
                    //点击保存时提交
                    event.ReactAPI.submitFormData("save", function (res) {
                        __result_processer(res, () => {
                            ReactAPI.destroyDialog("BatchAddition");
                        }, undefined, event.ReactAPI);
                    });
                }
            },
            {
                text: ReactAPI.international.getText("Button.text.close"),
                type: "cancel",
                onClick: function () {
                    ReactAPI.destroyDialog("BatchAddition");
                }
            }
        ]
    });
}


/**
 * 启用
 */
function ptBtnEnable() {
    var selRows = dg.getSelecteds();
    var ids = selRows.map(data => data.id).filter(id => id).join(",");
    if (ids) {
        var res = ReactAPI.request({
            url: "/msService/material/wareModel/wareModel/enableByIdS",
            type: 'get',
            async: false,
            data: { "ids": ids, "enableFlag": "enable" },
        });
        __result_processer(res, () => {
            ReactAPI.showMessage("s", ReactAPI.international.getText("EditView.notice.operate.success"));
            sp.updateSearch();
        });
    } else {
        //请选择一条记录进行操作！
        ReactAPI.showMessage("w", ReactAPI.international.getText("SupDatagrid.button.error"));
    }
}

/**
 * 停用
 */
function ptBtnDisable() {
    var selRows = dg.getSelecteds();
    var ids = selRows.map(data => data.id).filter(id => id).join(",");
    if (ids) {
        var res = ReactAPI.request({
            url: "/msService/material/wareModel/wareModel/enableByIdS",
            type: 'get',
            async: false,
            data: { "ids": ids, "enableFlag": "stop" },
        })
        __result_processer(res, () => {
            ReactAPI.showMessage("s", ReactAPI.international.getText("EditView.notice.operate.success"));
            sp.updateSearch();
        });
    } else {
        //请选择一条记录进行操作！
        ReactAPI.showMessage("w", ReactAPI.international.getText("SupDatagrid.button.error"));
    }
}

/**
 * 处理返回值
 * @param res 返回结果
 * @param onSuccess 成功回调
 * @param API 返回信息的ReactAPI
 * @returns 是否成功
 */
function __result_processer(res, onSuccess, onError, API) {
    API = API || ReactAPI;
    if (!res || (res.code || res.status) == 200) {
        onSuccess && onSuccess();
        return true;
    } else if (res && res.message) {
        API.showMessage("f", res.message);
    } else {
        //异常管理员
        API.showMessage("f", ReactAPI.international.getText("ec.msModule.admin.exception"));
    }
    onError && onError();
    return false;
}