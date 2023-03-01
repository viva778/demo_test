// ----采购订单 移动视图----
const dgDetailName = "material_1.0.0_purchaseInfos_purchaseEdit__mobile__dg1662345827692";

var dgDetail;       // dataGrid
var vendorName;     // 供应商
var vendorCode;     // 供应商编码
var purPersonName;  // 采购员
var purDepartName;  // 采购部门

var tempVendor;     // 供应商副本 同步保存供应商,用于取消修改供应商时回填

// ---------------------- 初始化 start ----------------------

function dataInit() {
    dgDetail = ReactAPI.getComponentAPI("Datagrid").APIs(dgDetailName);
    vendorName = ReactAPI.getComponentAPI("Reference").APIs("purchaseInfo.vendor.name");
    vendorCode = ReactAPI.getComponentAPI("Input").APIs("purchaseInfo.vendor.code");
    purPersonName = ReactAPI.getComponentAPI("Reference").APIs("purchaseInfo.purchPerson.name");
    purDepartName = ReactAPI.getComponentAPI("Reference").APIs("purchaseInfo.purchDepart.name");

    // 隐藏表头字段
    vendorCode.hide().row();
    purDepartName.hide().row();
}

function btnInit() {
    // 隐藏表头的[物料参照]按钮名称
    $("#goodRef .btn-tit").hide();
}

// ---------------------- 初始化 end ----------------------



// ---------------------- 供应商事件 start ----------------------

// 清空表体提示
function clearDgTips(obj) {
    // 移动视图2.0调用1.0版本的参照视图时,如果已经选择过对象,会有数据无法覆盖原字段的bug(返回了两个值)
    // 需要手动赋值 by wcy 2022-09-07

    // 如果已选供应商, 在供应商发送改变时给出提示信息

    // 未选择供应商或选择了相同的供应商时 不弹出对话框
    if (!vendorName.getValue()[0] || vendorName.getValue()[0].code == obj[obj.length - 1].code) {
        vendorName.setValue(obj[obj.length - 1]);
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
                    vendorName.setValue(obj[obj.length - 1]);
                    tempVendor = obj[obj.length - 1];
                    // 清空表体
                    removeAllLine();
                },
            },
            {
                name: "否",
                onClick: function () {
                    // 复原供应商
                    vendorName.setValue(tempVendor);
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



// ---------------------- 采购员事件 start ----------------------

// 清空采购部门
function removeDeptName() {
    purDepartName.removeValue();
}

// 采购员回调事件
function callbackPurPerson(obj) {
    // 移动视图2.0中 平台提供的callback事件存在问题
    // 改成在onchange中调用 效果一样
    if (!obj[0] || !obj[0].department[0]) return;
    var dept = obj[0].department[0];
    if (dept && dept.id && dept.name) {
        purDepartName.setValue({
            id: dept.id,
            name: dept.name
        })
    }
}

// ---------------------- 采购员事件 end ----------------------



// 打开物料参照
function goodRef() {
    // debugger
    // 物料参照视图url
    var url = "/msService/BaseSet/material/material/materialRef?clientType=mobile&multiSelect=1&crossCompanyFlag=false&isRefer=true";
    // 供应商
    var vendor = vendorName.getValue()[0];

    // 若供应商未选择 提示"请先选择供应商"
    if (vendor && vendor.id) {
        url += "&customConditionKey=cooperateId&cooperateId=" + vendor.id;
        console.log("url: " + url);
    } else {
        // 请先选择供应商
        // ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.PleaseSelectTheSupplierFirst"));
        ReactAPI.showMessage('w', "请先选择供应商");
        return false;
    }

    // 打开业务参照
    ReactAPI.openReference({
        id: "matReference",
        url: url,
        type: "Other",
        displayfield: "test", // 显示字段
        onOk: (data, event) => {
            partCallback(data, event);
        },
        onCancel: (data, event) => {
            ReactAPI.closeReference("matReference");
        }
    });

    var partCallback = (data, event) => {
        if (data != null && data.length != 0) {
            var dgData = dgDetail.getDatagridData();
            for (var i = 0; i < data.length; i++) {
                console.log(data[i]);
                var code = data[i].code;
                for (var j = 0; j < dgData.length; j++) {
                    var goodCode = dgDetail.getValueByKey(j, "good.code");
                    if (code == goodCode) {
                        // 校验重复 
                        // ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.CannotBeReferencedRepeatedly", "" + (i + 1)));
                        ReactAPI.showMessage('w', "第" + (i + 1) + "行,不允许重复参照!");
                        return false;
                    }
                }
                // 增行操作
                dgDetail.addLine([{ good: data[i] }]);
            }

        } else {
            // 请至少选中一行
            // ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1574406106043"));
            ReactAPI.showMessage('w', "请至少选中一行");
            return false;
        }
        // 添加成功
        // ReactAPI.showMessage('s', ReactAPI.international.getText("foundation.common.tips.addsuccessfully"));
        ReactAPI.showMessage('s', "添加成功!");
    }
}