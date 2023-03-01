// ----采购入库 移动视图----
const dgDetailName = "material_1.0.0_purchaseInSingles_purchaseInsingleView__mobile__dg1665628609124";
const dgContainerName = "material_1.0.0_purchaseInSingles_purchaseInsingleView__mobile__dg1667464208060";
var dgDetail;//dataGrid
var dgContainer;
// ---------------------- 初始化 start ----------------------
// ---------------------- 通用方法 start ----------------------

/**
 * 获取国际化值
 * @param key 国际化key
 */
function getIntlValue(key) {
    var intlValue;
    var result = ReactAPI.request(
        {
            type: "get",
            data: {},
            url: "/inter-api/i18n/v1/internationalConvert?key=" + key,
            async: false
        },
        function (res) {
            intlValue = res && res.data;
            return intlValue
        }
    );
    return intlValue.replace('</b>', '').replace('<b>', "").replace('<br/>', "");
}


//占位符匹配
String.prototype.format = function () {
    if (arguments.length === 0) return this;
    for (var s = this, i = 0; i < arguments.length; i++)
        s = s.replace(new RegExp("\\{" + i + "\\}", "g"), arguments[i]);
    return s;
};


function uuid() {
    function S4() {
        return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
    }

    return (S4() + S4() + "-" + S4() + "-" + S4() + "-" + S4() + "-" + S4() + S4() + S4());
}

// ---------------------- 通用方法 end ----------------------

function onLoad() {
    dataInit();
    ReactAPI.getComponentAPI("Label").APIs("purchInSingle.purArrivalNo").hide().row();
    ReactAPI.getComponentAPI("Label").APIs("purchInSingle.wareId.storesetState").hide().row();
    ReactAPI.getComponentAPI("Label").APIs("purchInSingle.srcId").hide().row();
    ReactAPI.getComponentAPI("Label").APIs("purchInSingle.wareId.storesetState").hide().row();

}



function dataInit() {
    dgDetail = ReactAPI.getComponentAPI("Datagrid").APIs(dgDetailName);
    dgContainer = ReactAPI.getComponentAPI("Datagrid").APIs(dgContainerName);
    ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.wmspro"
    }, res => {
        integrateWmsPro = (res.data["material.wmspro"] == true);
    });
    generateTask = (integrateWmsPro && enablePlace);
    enablePlace = ReactAPI.getComponentAPI("Boolean").APIs("purchInSingle.wareId.storesetState").getValue();
    rfWarehouse = ReactAPI.getComponentAPI("Reference").APIs("purchInSingle.wareId.name");
    scRedBlue = ReactAPI.getComponentAPI("SystemCode").APIs("purchInSingle.redBlue");
    elDgDetail = $("div[keyname='" + dgDetailName + "']").parents(".layout-comp-wrap")[0];
    elDgContainer = $("div[keyname='" + dgContainerName + "']").parents(".layout-comp-wrap")[0];
    $("div[data-code='" + dgContainerName + "']").parent().hide()
    dataInit = () => {
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
        url += "?clientType=mobile&tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + srcID;
        window.open(url);
    }
}

var ctDetails = [];

function ptBtnContainerBind() {
    var selDetails = dgDetail.getSelecteds();
    if (selDetails.length != 1) {
        //请选择一条入库单明细！
        ReactAPI.showMessage('w', getIntlValue("material.inbound.select_detail_before_do_some_things"));
        return false;
    }
    //todo test
    ctDetails = dgContainer.getDatagridData()
    ReactAPI.createDialog({
        id: "newDialog",
        title: "托盘",
        url: "/msService/material/purchaseInSingles/purchInPart/containerView?clientType=mobile",
        isRef: true,
        initData: {
            purchInPart: {
                good: selDetails[0].good,
                batch: selDetails[0].batch,
                uuid: selDetails[0].uuid,
                dataList: ctDetails.filter(e => e.purchaseInDetail.id == selDetails[0].id),
                readOnly: true
            }
        }, // 页面初始值
        footer: [
        ]
    });
}


function onSave() {
    var type = ReactAPI.getOperateType();
    if ("submit" == type) {
        ReactAPI.setSaveData({
            // dgList: {
            //     dg1667464208060: JSON.stringify(ctDetails.map(ctd => new Object({
            //         id: ctd.id,
            //         container: {
            //             id: ctd.container.id
            //         },
            //         material: {
            //             id: ctd.material.id
            //         },
            //         purchaseInDetailUuid: ctd.purchaseInDetailUuid,
            //         quantity: ctd.quantity,
            //         sort: sort++
            //     })))
            // }
            dgList:null
        });
    }

}


