var dgMaterial;
var cbSpecial;

function dataInit() {
    dgMaterial = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_containerFile_containerFileEditdg1630731123236");
    cbSpecial = ReactAPI.getComponentAPI("Checkbox").APIs("containerFile.special");
    dataInit = () => { }
}


function onLoad() {
    dataInit();
    if (ReactAPI.getParamsInRequestUrl().id != null && ReactAPI.getParamsInRequestUrl().id != "") {
        //修改时，修改编码为只读
        ReactAPI.getComponentAPI("Input").APIs("containerFile.code").setReadonly(true);
    }
    refreshSpecial(cbSpecial.getValue().value);

}


function onSave() {
    var special = cbSpecial.getValue().value;
    //必填校验
    if (special) {
        if (dgMaterial.getDatagridData().length == 0) {
            ReactAPI.showMessage("w", ReactAPI.international.getText("material.custom.random1630904672204"));
            return false;
        }
    }
}

function ocgSpecial(value) {
    refreshSpecial(value);
}

function refreshSpecial(value) {
    if (value) {
        $(".comp-dg-container").show();
    } else {
        $(".comp-dg-container").hide();
        ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_containerFile_containerFileEditdg1630731123236").deleteLine()
    }
}

/**
 * pt参照按钮
 */
function ptBtnGoodRef() {
    //打开参照
    ReactAPI.createDialog("materialRef", {
        title: ReactAPI.international.getText("BaseSet.viewtitle.randon1569570764419"), //物料参照
        url: "/msService/BaseSet/material/material/materialRefLayout?multiSelect=true",
        size: 5,
        callback: (data, event) => {
            callback(data, event);
        },
        isRef: true,
        onOk: (data, event) => {
            callback(data, event);
        },
        onCancel: () => {
            ReactAPI.destroyDialog("materialRef");
        },
        okText: ReactAPI.international.getText("Button.text.select"), // 选择
        cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
    });

    //添加选中数据，并设置属性
    var callback = (selRows, event) => {
        if (selRows && selRows[0]) {
            var currentIds = new Set(dgMaterial.getDatagridData().map(data => data.material && data.material.id));
            var existData = selRows.filter(selData => currentIds.has(selData.id));
            var rowsToAdd;
            if (existData.length > 0) {
                //进行报错
                event.ReactAPI.showMessage('w', "物料「" + existData.map(selData => selData.name).join(",") + "」被重复参照！");
                if (existData.length == selRows.length) {
                    return false;
                } else {
                    rowsToAdd = selRows.filter(selData => !currentIds.has(selData.id));
                }
            } else {
                rowsToAdd = selRows;
            }
            //添加数据
            rowsToAdd.forEach(selData => {
                var rowIndex = dgMaterial.addLine().rowIndex;
                dgMaterial.setRowData(rowIndex, {
                    material: selData,
                });
            });
            event.ReactAPI.showMessage('s', ReactAPI.international.getText("foundation.common.tips.addsuccessfully"));
            setTimeout(() => {
                ReactAPI.destroyDialog("materialRef");
            }, 1000);
        } else {
            //请至少选中一行
            event.ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.randon1574406106043"));
        }
    }
}