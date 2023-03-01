//-----采购入库查看-----

const dgDetailName = "material_1.0.0_purchaseInSingles_purchaseInsingleViewdg1574673357433";
const dgContainerName = "material_1.0.0_purchaseInSingles_purchaseInsingleViewdg1667445538944";
var dgDetail;
var dgContainer;
var elDgDetail;
var elDgContainer;

function dataInit() {
    elDgDetail = $("div[keyname='" + dgDetailName + "']").parents(".layout-comp-wrap")[0];
    elDgContainer = $("div[keyname='" + dgContainerName + "']").parents(".layout-comp-wrap")[0];
    dataInit = () => {
    };
}


function onLoad() {
    dataInit();
    //添加采购到货单超链接
    var tableNo = ReactAPI.getComponentAPI("Input").APIs("purchInSingle.purArrivalNo").getValue();
    var srcID = ReactAPI.getComponentAPI("InputNumber").APIs("purchInSingle.srcId").getValue();
    ReactAPI.getComponentAPI("Input").APIs("purchInSingle.purArrivalNo").replace("<div class='supplant-readonly-wrap supplant-comp readonly'>" + tableNo + "</div>", {
        onClick: function (e) {
            getPurArrial(srcID);
        },
    });

}


function ptInit() {
    dgDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgDetailName);
    dataInit();
    if (dgContainer) {
        elBigup(elDgDetail, elDgContainer);
    }
    var status = ReactAPI.getFormData().status;
    if (status != 99) {
        dgDetail.setBtnHidden(["inBoundView"]);
    }


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

function ptContainerInit() {
    dgContainer = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgContainerName);
    dataInit();
    if (dgDetail) {
        elBigup(elDgDetail, elDgContainer);
    }
    // 兼容提交保存平台前端代码错误
    setTimeout(function () {
        var pt =
            ReactAPI.getComponentAPI("SupDataGrid")[
                "material_1.0.0_purchaseInSingles_purchaseInsingleViewdg1667445538944"
                ];
        if (!pt.btnWidths) {
            pt.btnWidths = [];
        }
    });
}

function showContainerDetail(rowData) {
    var ctds = ctDetails.filter(val => val.purchaseInDetailUuid == rowData.uuid);
    if (ctds.length) {
        elRestore(elDgDetail, elDgContainer);
    }
    dgContainer.setDatagridData(ctds);
}

var ctDetails = [];

function ptContainerRenderOver() {
    //首次进入缓存数据
    var dgData = dgContainer.getDatagridData();
    if (dgData.length) {
        elRestore(elDgDetail, elDgContainer);
        dgData.forEach(rowData => {
            ctDetails.push(rowData);
        });
        dgContainer.setDatagridData([]);
    }
}

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
        // 菜单操作编码
        var operateCode = "material_1.0.0_purArrivalInfos_purArrivalInfoList_self";
        var pcMap = ReactAPI.getPowerCode(operateCode);
        var pc = pcMap[operateCode];
        url += "?tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + srcID + "&__pc__=" + pc;
        window.open(url);
    }
}

/**
 * 获取URL中指定参数的值
 *
 * @param {*} url
 * @param {*} name
 */
var getParam = function (url, name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    var r = url.match(reg);
    if (r != null) return unescape(decodeURI(r[2]));
    return "";
};


function onclickpurch(srcID) {

    var tableInfo = '';
    var tableId = '';
    if (srcID) {
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
        // 菜单操作编码
        var operateCode = "material_1.0.0_purchaseInfos_purchaseList_self";
        var pcMap = ReactAPI.getPowerCode(operateCode);
        var pc = pcMap[operateCode];
        url += "?tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + tableId + "&__pc__=" + pc;
        window.open(url);
    }
}

function ptRenderOver() {
    var dgData = dgDetail.getDatagridData();
    if (dgData.length) {
        //绑定点击事件
        dgData.forEach(rowData => {
            if (rowData.purchaseId) {
                dgDetail.setRowData(rowData.rowIndex, {
                    purOrderNo_attr: {
                        bindEvent: {
                            onClick: function (e) {
                                onclickpurch(srcID);
                            }
                        }
                    }
                })
            }
        });
        //通过表体id查询入库通知的信息
        var id;
        $.ajax({
            url: "/msService/WMSPRO/inBoundList/inBoundList/findIdBySrcPartId?srcPartId=" + dgData[0].id,
            type: 'get',
            async: false,
            success: function (res) {
                id = res.data
            }
        })
        //隐藏无下游入库通知的单据
        if (!id) {
            dgDetail.setBtnHidden(["inBoundView"]);
        }
    } else {
        dgDetail.setBtnHidden(["inBoundView"]);
    }
}


function inBoundDetailView() {
    dgData = dgDetail.getSelecteds();
    if (!dgData.length || dgData.length == 0) {

        ReactAPI.showMessage("w", ReactAPI.international.getText("请至少选择一行！"));
        return false;
    }
    var id
    //通过表体id查询出库通知的信息
    $.ajax({
        url: "/msService/WMSPRO/inBoundList/inBoundList/findIdBySrcPartId?srcPartId=" + dgData[0].id,
        type: 'get',
        async: false,
        success: function (res) {
            id = res.data
        }
    })
    //入库通知打开
    ReactAPI.createDialog("newDialog", {
        title: ReactAPI.international.getText("入库明细"),
        url: "/msService/WMSPRO/inBoundList/inBoundList/inBoundDetailView?id=" + id,
        size: 5,
        isRef: false
    })
}


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