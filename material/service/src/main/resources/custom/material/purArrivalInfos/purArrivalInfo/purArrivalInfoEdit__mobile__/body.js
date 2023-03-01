//-----采购到货_移动视图-----
const dgDetailName = "material_1.0.0_purArrivalInfos_purArrivalInfoEdit__mobile__dg1662376522863";

var dgDetail;       // dataDrid
var rfVendor;       // 供应商
var rfVendorCode;   // 供应商编码
var scRedBlue;      // 红蓝字
var rfPurPerson;    // 收货人
var rfPurDepart;    // 收货部门
var tempVendor;     // 供应商副本 同步保存供应商,用于取消修改供应商时回填

// ---------------------- 初始化 start ----------------------

function onLoad() {
    dataInit();
    btnInit();

    var rb = scRedBlue.getValue().value;
    console.log("rb: " + rb);
    // if (rb == "BaseSet_redBlue/red") {
    //     ReactAPI.setHeadBtnAttr('redWriteOff', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: false });
    //     ReactAPI.setHeadBtnAttr('refPurchase', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: true });
    // } else {
    //     ReactAPI.setHeadBtnAttr('redWriteOff', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: true });
    //     ReactAPI.setHeadBtnAttr('refPurchase', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: false });
    // }
}

function dataInit() {
    dgDetail = ReactAPI.getComponentAPI("Datagrid").APIs(dgDetailName);
    rfVendor = ReactAPI.getComponentAPI("Reference").APIs("purArrivalInfo.vendor.name");
    rfVendorCode = ReactAPI.getComponentAPI("Reference").APIs("purArrivalInfo.vendor.code");
    scRedBlue = ReactAPI.getComponentAPI("SystemCode").APIs("purArrivalInfo.redBlue");
    rfPurPerson = ReactAPI.getComponentAPI("Reference").APIs("purArrivalInfo.purePerson.name");
    rfPurDepart = ReactAPI.getComponentAPI("Reference").APIs("purArrivalInfo.purchDept.name");

    // 隐藏表头字段
    rfVendorCode.hide().row();

    dataInit = () => { };
}

function btnInit() {
    // 隐藏表头的[参照采购订单]按钮名称
    $("#refPurchase .btn-tit").hide();
}

// ---------------------- 初始化 end ----------------------

// ---------------------- 收货人事件 start ----------------------

// 清空收货部门
function removeDeptName() {
    rfPurDepart.removeValue();
}

// 收货人回调事件
function callbackPurPerson(obj) {
    // 移动视图2.0中 平台提供的callback事件存在问题
    // 改成在onchange中调用 效果一样
    if (!obj[0] || !obj[0].department[0]) return;
    var dept = obj[0].department[0];
    if (dept && dept.id && dept.name) {
        rfPurDepart.setValue({
            id: dept.id,
            name: dept.name
        })
    }
}

// ---------------------- 收货人事件 end ----------------------

// ---------------------- 供应商事件 start ----------------------

// 清空表体提示
function clearDgTips(obj) {
    // 移动视图2.0调用1.0版本的参照视图时,如果已经选择过对象,会有数据无法覆盖原字段的bug(返回了两个值)
    // 需要手动赋值 by wcy 2022-09-07

    // 如果已选供应商, 在供应商发送改变时给出提示信息

    // 如果选择了与之前相同的供应商 不弹出提示信息
    // (有新供应商) && (无原供应商 || 新供应商code == 原供应商code)
    if ((obj.length && obj[obj.length - 1]) && (!rfVendor.getValue()[0] || rfVendor.getValue()[0].code == obj[obj.length - 1].code)) {
        rfVendor.setValue(obj[obj.length - 1]);
        tempVendor = obj[obj.length - 1];
        return;
    }


    // 弹出确认框
    ReactAPI.openConfirm({
        message: "切换供应商会同时清空表体数据,是否确认?",
        buttons: [
            {
                name: "是",
                type: "primary",
                onClick: function () {
                    // 同步更新供应商副本
                    rfVendor.setValue(obj[obj.length - 1]);
                    tempVendor = obj[obj.length - 1];
                    // 清空表体
                    removeAllLine();
                },
            },
            {
                name: "否",
                onClick: function () {
                    // 复原供应商
                    rfVendor.setValue(tempVendor);
                },
            }
        ],
    });
}

// 清空表体
function removeAllLine() {
    dgDetail.deleteLine();
}

// ---------------------- 供应商事件 end ----------------------


// ---------------------- 参照采购订单按钮 start ----------------------

function btnOrderReference() {
    // debugger
    // 采购订单参照视图url
    var url = "/msService/material/purchaseInfos/purchasePart/purchasePartRef?clientType=mobile&multiSelect=1&crossCompanyFlag=false&isRefer=true";
    // 供应商
    var vendor = rfVendor.getValue()[0];
    if (vendor && vendor.id) {
        url += "&customConditionKey=vendorId&vendorId=" + vendor.id;
    }

    // 打开业务参照
    ReactAPI.openReference({
        id: "purPartReference",
        url: url,
        type: "Other",
        displayfield: "test", // 显示字段
        onOk: (data, event) => {
            console.log("onOk success");
            order_callback(data, event);
        },
        onCancel: (data, event) => {
            console.log("onCancel success");
            ReactAPI.closeReference("purPartReference");
        }
    });

    function order_callback(selData, event) {
        // 选择数量校验
        if (!selData.length) {
            //请至少选择一条数据！
            ReactAPI.showMessage('w', "请至少选择一条数据！");
            return;
        }

        // 校验供应商唯一
        var vendorCnt = new Set(selData.map(rowData => rowData.purchaseInfo && rowData.purchaseInfo.vendor && rowData.purchaseInfo.vendor.id).filter(v => v)).size;
        if (vendorCnt > 1) {
            // 选中采购订单的供应商不同，请重新选择！
            ReactAPI.showMessage('w', "选中采购订单的供应商不同，请重新选择！");
            return false;
        }

        // 设置表头的供应商信息
        rfVendor.setValue(selData[0].purchaseInfo.vendor);
        dgDetail.addLine(selData.map(rowData => {
            //未到货量 = 订单数量 - 已到货数量
            var remainNum = Math.max(0, Number(rowData.purchQuantity) - Number(rowData.arrivalNum))
            var isByPiece = (rowData.good.isBatch && rowData.good.isBatch.id) == "BaseSet_isBatch/piece";
            var newLine = {
                good: rowData.good,
                // 采购订单单据编号
                purchaseNo: rowData.purchaseInfo.tableNo,
                // 订单数量
                orderNum: Number(rowData.purchQuantity),
                // 未到货量
                remainNum: remainNum,
                // 到货数量
                arrivalQuan: isByPiece ? 1 : remainNum,
                // 采购员
                keepUser: rowData.purchaseInfo.purchPerson,
                // 订单明细id
                purchaseId: String(rowData.id),
                // 供应商id
                vendorId: rowData.purchaseInfo.vendor.id,
                // 待打印
                genPrintInfo: true
            };
            return newLine;
        }), true);
        ///////////////////////
        ///需要设置到货数量只读///
        ///////////////////////

        // 填写仓库之前 货位只读

        // 按件物料 到货数量为1且只读

        // 不启用批次物料 批号只读

        ReactAPI.closeReference("purPartReference");
        ReactAPI.showMessage('s', "添加成功");
    }
}


// ---------------------- 参照采购订单按钮 end ----------------------


// ---------------------- 表体-仓库事件 start ----------------------


function ptocgWarehouse(newVal, oldVal, rowIndex) {
    var warehouse = newVal && newVal[0];
    console.log(newVal);
    console.log(oldVal);
    console.log(rowIndex);

    //清空货位
    dgDetail.setCellValueByKey(rowIndex, 'storeSet.name', null);
    // 根据货位状态改变只读
    // 当前版本属性设置存在问题 等待后续版本解决 by wcy 2022-09-09

    // dgDetail.setDatagridCellAttr(rowIndex, "storeSet.name", {
    //     readonly: !(warehouse && warehouse.storesetState)
    // });

}

// ---------------------- 表体-仓库事件 end ----------------------


// ---------------------- onSave start ----------------------

function onSave() {
    // 单据提交时判断表体是否为空
    if (ReactAPI.getOperateType() == 'submit') {
        var dgData = dgDetail.getDatagridData();
        if (!dgData.length) {
            ReactAPI.showMessage("w", "表体数据不能为空!");
            return false;
        }

        // var check_result = datagrid.validator.check(dgDetailName);
        // if (!check_result) {
        //     return false;
        // }

        for (var i = 0; i < dgData.length; i++) {
            // 供应商检验日期 或 生产日期 必须填写一个
            if (dgData[i] && !dgData[i].supplierCheckDate && !dgData[i].produceDate) {
                ReactAPI.showMessage("w", "第" + (i + 1) + "行, 供应商检验日期和生产日期不能同时为空!");
                return false;
            }
        }
    }
}

// ---------------------- onSave end ----------------------

// ---------------------- 参照物料按钮 start ----------------------
function btnGoodRef() {

    var vendor = rfVendor.getValue()[0];
    var ids;
    if (vendor) {
        var result = ReactAPI.request({
            url: "/msService/material/purArrival/purArrival/findWarewithVendor?vendor=" + vendor.id,
            type: "get",

            async: false
        },
            function (res) {

                ids = res.data
            }
        );

    }
    else {
        ReactAPI.showMessage('w', "请先选择供应商！");
        return false;
    }


    var url = "/msService/BaseSet/material/material/materialRef?multiSelect=true&clientType=mobile&multiSelect=1&crossCompanyFlag=false&isRefer=true";
    // 供应商

    if (ids) {
        url += "&customConditionKey=cappMaterialIds&cappMaterialIds=" + ids;
    }
    ReactAPI.openReference({
        id: "purPartReference",
        url: url,
        type: "Other",
        displayfield: "test", // 显示字段
        onOk: (data, event) => {
            console.log("onOk success");
            order_callback(data, event);
        },
        onCancel: (data, event) => {
            console.log("onCancel success");
            ReactAPI.closeReference("purPartReference");
        }
    });



    function order_callback(selData, event) {
        // 选择数量校验
        if (!selData.length) {
            //请至少选择一条数据！
            ReactAPI.showMessage('w', "请至少选择一条数据！");
            return;
        }

        dgDetail.addLine(selData.map(rowData => {
            var isByPiece = (rowData.isBatch && rowData.isBatch.id) == "BaseSet_isBatch/piece";
            var newLine = {
                good: rowData,
                // 到货数量
                arrivalQuan: isByPiece ? 1 : null,
                // 供应商id
                vendorId: vendor.id,
                // 待打印
                genPrintInfo: true
            };
            return newLine;
        }), true);
        ///////////////////////
        ///需要设置到货数量只读///
        ///////////////////////

        // 填写仓库之前 货位只读

        // 按件物料 到货数量为1且只读

        // 不启用批次物料 批号只读

        ReactAPI.closeReference("purPartReference");
        ReactAPI.showMessage('s', "添加成功");
    }



}