//-----处置单位编辑-----
//引入datagrid.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");

const dgListName = "material_1.0.0_wasteUnitManage_dealUnitEditdg1631941868127";
const dgOtherName = "material_1.0.0_wasteUnitManage_dealUnitEditdg1632272652884";
var dgList;
var dgOther;
var new_data_key = [];
var currentContainerId;


function ptListInit() {
    dgList = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgListName)
    dgList.setBtnImg("btn-ref", "sup-btn-eighteen-dt-op-reference");
}

function ptOhterListInit() {
    dgOther = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgOtherName)
    dgOther.setBtnImg("btn-ref", "sup-btn-eighteen-dt-op-reference");
}


/**
 * 处置信息物料参照
 */
function openMaterialReference() {
    ReactAPI.createDialog("newDialog", {
        title: ReactAPI.international.getText("material.buttonPropertyshowName.randon1592465499893.flag"),//物料参照
        url: "/msService/BaseSet/material/material/materialRef?multiSelect=true",
        size: 5,
        callback: (data, event) => {
            callback(data, event);
        },
        isRef: true, // 是否开启参照
        onOk: (data, event) => {
            callback(data, event, true);
        },
        onCancel: (data, event) => {
            ReactAPI.destroyDialog("newDialog");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
    });

    var callback = (selData, event, close) => {
        if (!selData || selData.length === 0) {
            //请至少选中一行
            ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1574406106043"));
            return false;
        }
        var dgData = dgList.getDatagridData();
        var batchKeys = dgData.map(rowData => rowData.waste.id);
        var conflictRows = selData.filter(ad => {
            var goodId = ad.id;
            return batchKeys.includes(goodId);
        }).map(rowData => rowData.rowIndex + 1).join(",");
        if (conflictRows) {
            //第{0}行，数据重复
            ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1574404333334", "" + conflictRows));
            return false;
        }
        selData.forEach(ad => {
            dgList.addLine();
            var lenNum = dgList.getDatagridData().length - 1;
            dgList.setValueByKey(lenNum, "waste", {
                id: ad.id,
                name: ad.name,
                materialClass: {id: ad.materialClass.id, name: ad.materialClass.name},
                storeUnit: ad.mainUnit
            });
        })
        if (close) {
            ReactAPI.destroyDialog("newDialog");
        }
        ReactAPI.showMessage('s', ReactAPI.international.getText("foundation.common.tips.addsuccessfully"));
    }
}


/**
 * 其他处置信息物料参照
 */
function openMaterialRefOther() {
    ReactAPI.createDialog("newDialog", {
        title: ReactAPI.international.getText("material.buttonPropertyshowName.randon1592465499893.flag"),//物料参照
        url: "/msService/BaseSet/material/material/materialRef?multiSelect=true",
        size: 5,
        callback: (data, event) => {
            callback(data, event);
        },
        isRef: true, // 是否开启参照
        onOk: (data, event) => {
            callback(data, event, true);
        },
        onCancel: (data, event) => {
            ReactAPI.destroyDialog("newDialog");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
    });

    var callback = (selData, event, close) => {
        if (!selData || selData.length === 0) {
            //请至少选中一行
            ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1574406106043"));
            return false;
        }
        var dgData = dgOther.getDatagridData();
        var batchKeys = dgData.map(rowData => rowData.otherDetail.id);
        var conflictRows = selData.filter(ad => {
            var goodId = ad.id;
            return batchKeys.includes(goodId);
        }).map(rowData => rowData.rowIndex + 1).join(",");
        if (conflictRows) {
            //第{0}行，数据重复
            ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1574404333334", "" + conflictRows));
            return false;
        }
        selData.forEach(ad => {
            dgOther.addLine();
            var lenNum = dgOther.getDatagridData().length - 1;
            dgOther.setValueByKey(lenNum, "otherDetail", {
                id: ad.id,
                name: ad.name,
                materialClass: {id: ad.materialClass.id, name: ad.materialClass.name},
                storeUnit: ad.mainUnit
            });
        })
        if (close) {
            ReactAPI.destroyDialog("newDialog");
        }
        ReactAPI.showMessage('s', ReactAPI.international.getText("foundation.common.tips.addsuccessfully"));
    }


}

