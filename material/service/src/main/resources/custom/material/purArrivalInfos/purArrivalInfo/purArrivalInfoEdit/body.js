//-----采购到货-----

//引入datagrid.js dateutil.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");
loader.import("dateutil");

const dgDetailName = "material_1.0.0_purArrivalInfos_purArrivalInfoEditdg1574067447595";

var rfVendor;
var dgDetail;
var scRedBlue;

function dataInit() {
    rfVendor = ReactAPI.getComponentAPI("Reference").APIs("purArrivalInfo.vendor.name");
    dgDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgDetailName);
    scRedBlue = ReactAPI.getComponentAPI("SystemCode").APIs("purArrivalInfo.redBlue");
    dataInit = () => {
    };
}

function onLoad() {
    dataInit();

    var rb = scRedBlue.getValue().value;
    if (rb == "BaseSet_redBlue/red") {
        ReactAPI.setHeadBtnAttr('redWriteOff', {icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: false});
        ReactAPI.setHeadBtnAttr('refPurchase', {icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: true});
    } else {
        ReactAPI.setHeadBtnAttr('redWriteOff', {icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: true});
        ReactAPI.setHeadBtnAttr('refPurchase', {icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: false});
    }
}

function ptInit() {
    dataInit();
    //设置校验
    //物品启用按件管理，数量只能为1件
    datagrid.validator.add(dgDetailName, "arrivalQuan", rowData => {
        var batchType = rowData.good && rowData.good.isBatch && rowData.good.isBatch.id;
        if (batchType == "BaseSet_isBatch/piece") {
            return rowData.arrivalQuan == 1;
        } else {
            return true;
        }
    }, rowIndex => {
        return ReactAPI.international.getText(
            "material.custom.can.only.beOne",
            String(rowIndex)
        );
    });
    //生产日期与供应商检验日期不能同时为空！
    datagrid.validator.add(dgDetailName, "produceDate", rowData => {
        return rowData.produceDate || rowData.supplierCheckDate;
    }, rowIndex => {
        return ReactAPI.international.getText(
            "material.custom.randon1576041697719",
            String(rowIndex)
        );
    });
}

function ptRenderOver() {
    //设置条件只读（由于是固定的，所以不需要反复刷新
    //批号不启用只读
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "batch", rowData => {
        var batchType = rowData.good && rowData.good.isBatch && rowData.good.isBatch.id;
        return batchType != "BaseSet_isBatch/batch" && batchType != "BaseSet_isBatch/piece";
    });
    //按件时数量只读为1
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "arrivalQuan", rowData => {
        var batchType = rowData.good && rowData.good.isBatch && rowData.good.isBatch.id;
        return batchType == "BaseSet_isBatch/piece";
    });
    //仓库不存在,或未启用货位,货位只读
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "storeSet.name", rowData => {
        return !rowData.ware || !rowData.ware.storesetState;
    });
    //设置点击超链接事件
    datagrid.bindEvent.onClick(dgDetailName, "purchaseNo", (rowIndex, rowData) => {
        onclickpurch(rowData);
    });
    var dgData = dgDetail.getDatagridData();
    if (dgData.length) {
        dgData.forEach(rowData => rowData.edited = true);
        dgDetail.setDatagridData(dgData);
    }
}


function btnOrderReference() {
    var vendor = rfVendor.getValue()[0];
    var url = "/msService/material/purchaseInfos/purchasePart/purchasePartRef";
    if (vendor && vendor.id) {
        url = url + "?vendorId=" + vendor.id + "&customConditionKey=vendorId";
    }
    ReactAPI.createDialog("order_ref", {
        title: ReactAPI.international.getText("material.viewtitle.randon1574074721639"), // 采购订单参照
        url: url,
        size: 5,
        callback: (data, event) => {
            order_callback(data, event);
        },
        isRef: true, // 是否开启参照
        onOk: (data, event) => {
            order_callback(data, event);
        },
        onCancel: () => {
            ReactAPI.destroyDialog("order_ref");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
    });

    function order_callback(selData, event) {
        if (!selData.length) {
            //请至少选择一条数据！
            event.ReactAPI.showMessage('w', ReactAPI.international.getText("ec.ec_view_select_property.selectNullData"));
            return;
        }

        //校验供应商唯一
        var vendorCnt = new Set(selData.map(rowData => rowData.purchaseInfo && rowData.purchaseInfo.vendor && rowData.purchaseInfo.vendor.id).filter(v => v)).size;
        if (vendorCnt > 1) {
            // 选中采购订单的供应商不同，请重新选择！
            event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.purArrival.differentVendors"));
            return false;
        }
        // 设置表头的供应商信息
        rfVendor.setValue(selData[0].purchaseInfo.vendor);
        // 获取已填信息
        let wareMap = new Map()
        let dgData = dgDetail.getDatagridData();
        dgData.forEach(e => {
            let key = e.good.code
            if (!e.ware) {
                return
            }
            if (!wareMap.has(key) || e.ware.code < wareMap.get(key).ware.code || (e.storeSet && e.storeSet.code < wareMap.get(key).storeSet.code)) {
                wareMap.set(key, {
                    ware: e.ware,
                    storeSet: e.storeSet
                })
            }

        })
        let defaultRes = ReactAPI.request({
            url: "/msService/material/purArrival/purArrival/findDefaultWare",
            type: "post",
            async: false,
            data: selData.map(e => e.good.id)
        }).data;
        if (!defaultRes) {
            defaultRes = {}
        }
        dgDetail.addLine(selData.map(rowData => {
            //未到货量 = 订单数量 - 已到货数量
            var remainNum = Math.max(0, Number(rowData.purchQuantity) - Number(rowData.arrivalNum))
            var isByPiece = (rowData.good.isBatch && rowData.good.isBatch.id) == "BaseSet_isBatch/piece";
            var newLine = {
                good: rowData.good,
                //采购订单单据编号
                purchaseNo: rowData.purchaseInfo.tableNo,
                //订单数量
                orderNum: Number(rowData.purchQuantity),
                //未到货量
                remainNum: remainNum,
                //到货数量
                arrivalQuan: isByPiece ? 1 : remainNum,
                //采购员
                keepUser: rowData.purchaseInfo.purchPerson,
                //订单明细id
                purchaseId: String(rowData.id),
                //供应商id
                vendorId: rowData.purchaseInfo.vendor.id,
                //待打印
                genPrintInfo: true
            };
            //设置默认货位
            defaultWare(newLine, rowData, rowData.good, defaultRes[rowData.good.id + ""])
            //将之前设置的属性附加上去
            datagrid.appendRowAttr(dgDetailName, newLine);
            return newLine;
        }), true);
        ReactAPI.destroyDialog("order_ref");
        ReactAPI.showMessage('s', ReactAPI.international.getText("foundation.common.tips.addsuccessfully"));
    }
}

function onSave() {

    // 单据提交时判断表体是否为空
    if (ReactAPI.getOperateType() == 'submit') {
        var dgData = dgDetail.getDatagridData();
        if (!dgData.length) {
            ReactAPI.showMessage("w", ReactAPI.international.getText("material.custom.randon1573634425210"));
            return false;
        }

        var check_result = datagrid.validator.check(dgDetailName);
        if (!check_result) {
            return false;
        }
    }
}


function ptocgWarehouse(value, rowIndex) {
    var warehouse = value && value[0];
    //清空货位
    dgDetail.setValueByKey(rowIndex, 'storeSet', null);
    //根据货位状态改变只读
    dgDetail.setDatagridCellAttr(rowIndex, "storeSet.name", {
        readonly: !(warehouse && warehouse.storesetState)
    });
}

//生产日期变化,计算近效期 有效期
function ptocgProduceTime(value, rowIndex) {
    var dateValue = {};
    if (!value) {
        dateValue.approachTime = dateValue.validTime = null;
    } else {
        var rowData = dgDetail.getRows(String(rowIndex))[0];
        var validTerm = rowData.good.validPeriod;
        var validUnit = rowData.good.validUnit && rowData.good.validUnit.id;
        if (validTerm && validUnit) {
            //计算有效期
            var validTime = date_util.add(value, validTerm, validUnit.substr(validUnit.lastIndexOf("/") + 1)).getTime();
            dateValue.validTime = validTime;
            //计算近效期
            var approachTerm = rowData.good.approachDate;
            var approachUnit = rowData.good.approachUnit && rowData.good.approachUnit.id;
            if (approachTerm && approachUnit) {
                var approachTime = date_util.add(validTime, -approachTerm, approachUnit.substr(approachUnit.lastIndexOf("/") + 1)).getTime();
                dateValue.approachTime = approachTime;
            }
        }
    }
    dgDetail.setRowData(rowIndex, dateValue);
}


/**
 * 打开采购订单查看视图
 * @param {采购订单表体ID} srcID
 */
function onclickpurch(rowData) {
    if (rowData.purchaseId) {
        var result = ReactAPI.request({
            url: "/msService/material/purchaseInSingles/purchInSingle/findSrcTableInfoId",
            type: "get",
            data: {
                "srcID": rowData.purchaseId,
                "tableType": "MaterialPurchasePart"
            },
            async: false
        });
        if (result.code == 200 && result.data && result.data.tableInfoId) {
            var tableInfo = result.data.tableInfoId;
            var tableId = result.data.tableId;
            if (tableInfo) {
                // 采购订单实体的entityCode
                var entityCode = "material_1.0.0_purchaseInfos";
                // 查看视图URL
                var url = "/msService/material/purchaseInfos/purchaseInfo/purchaseView";
                // 菜单操作编码
                var operateCode = "material_1.0.0_purchaseInfos_purchaseList_self";
                var pcMap = ReactAPI.getPowerCode(operateCode);
                var pc = pcMap[operateCode];
                url += "?tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + tableId + "&__pc__=" + pc;
                window.open(url);
            }
        }
    }
}


function btnGoodRef() {
    //查找有没有供应商需要过滤
    var vendor = rfVendor.getValue()[0];
    var ids;
    if (vendor) {
        var result = ReactAPI.request({
            url: "/msService/material/purArrival/purArrival/findWarewithVendor?vendor=" + vendor.id,
            type: "get",

            async: false
        });
        ids = result.data;
    } else {
        ReactAPI.showMessage("w", ReactAPI.international.getText("material.custom.PleaseSelectTheSupplierFirst"));
        return false;
    }
    //如果有物料就只展现这些物料
    if (ids) {
        //打开参照
        ReactAPI.createDialog("materialRef", {
            title: ReactAPI.international.getText("BaseSet.viewtitle.randon1569570764419"), //物料参照
            url: "/msService/BaseSet/material/material/materialRefLayout?multiSelect=true&customConditionKey=cappMaterialIds&cappMaterialIds=" + ids,
            size: 5,
            callback: function callback(data, event) {
                partCallback(data, event);
            },
            isRef: true,
            onOk: function onOk(data, event) {
                partCallback(data, event);
            },
            onCancel: function onCancel() {
                ReactAPI.destroyDialog("materialRef");
            },
            okText: ReactAPI.international.getText("Button.text.select"), // 选择
            cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
        });

    } else {
        //打开参照
        ReactAPI.createDialog("materialRef", {
            title: ReactAPI.international.getText("BaseSet.viewtitle.randon1569570764419"), //物料参照
            url: "/msService/BaseSet/material/material/materialRefLayout?multiSelect=true",
            size: 5,
            callback: function callback(data, event) {
                partCallback(data, event);
            },
            isRef: true,
            onOk: function onOk(data, event) {
                partCallback(data, event);
            },
            onCancel: function onCancel() {
                ReactAPI.destroyDialog("materialRef");
            },
            okText: ReactAPI.international.getText("Button.text.select"), // 选择
            cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
        });
    }

}

function partCallback(selData, event) {
    if (!selData.length) {
        //请至少选择一条数据！
        event.ReactAPI.showMessage('w', ReactAPI.international.getText("ec.ec_view_select_property.selectNullData"));
        return false;
    }
    var vendorId;
    if (rfVendor.getValue()[0]) {
        vendorId = rfVendor.getValue()[0].id
    }
    let defaultRes = ReactAPI.request({
        url: "/msService/material/purArrival/purArrival/findDefaultWare",
        type: "post",
        async: false,
        data:
            selData.map(e => e.id)

    }).data;
    if (!defaultRes) {
        defaultRes = {}
    }
    //物料对应仓库 物料编码 -- 仓库实体
    dgDetail.addLine(selData.map(rowData => {
        var result = ReactAPI.request({
            url: "/msService/material/entity/getEntityList",
            type: "get",
            async: false,
            data: {
                moduleName: "BaseSet",
                entityName: "BaseSetMaterial",
                conditions: "id=" + rowData.id,
                includes: "id,name,code,isValidityManage,validPeriod,validUnit.id,isBatch,approachUnit.id,approachDate,defaultWarehouse,defaultStoreset"
            }
        }).data[0];
        var quantity;
        if (result.isBatch.id == 'BaseSet_isBatch/piece') {
            quantity = 1
        }
        var newLine = {
            good: result,
            vendorId: vendorId,
            genPrintInfo: true,
            arrivalQuan: quantity
        };

        //设置默认货位
        defaultWare(newLine, rowData, result, defaultRes[rowData.id + ""])
        //将之前设置的属性附加上去
        datagrid.appendRowAttr(dgDetailName, newLine);
        return newLine;
    }), true);
    ReactAPI.destroyDialog("materialRef");

}

function defaultWare(newLine, rowData, result, theDefaultRes) {

    if (theDefaultRes) {
        newLine.ware = theDefaultRes
    } else if (result.defaultWarehouse != null) {
        newLine.ware = result.defaultWarehouse
        if (result.defaultStoreset != null) {
            newLine.storeSet = result.defaultStoreset
        }
    }


}
