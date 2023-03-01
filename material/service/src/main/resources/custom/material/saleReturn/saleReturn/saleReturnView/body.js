const dgDetailName = "material_1.0.0_saleReturn_saleReturnViewdg1574666604280";
var dgDetail;

function dataInit() {
    dgDetail = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgDetailName);
    dataInit = () => { }
}

function ptRenderOver() {
    //通过表体id查询入库通知的信息
    $.ajax({
        url: "/msService/WMSPRO/inBoundList/inBoundList/findIdBySrcPartId?srcPartId=" + dgDetail.getDatagridData()[0].id,
        type: 'get',
        async: false,
        success: function (res) {
            //隐藏无下游入库通知的单据
            if (!res.data) {
                dgDetail.setBtnHidden(["inBoundView"]);
            }
        }
    })
}

function onclickSaleNo(tableNo) {
    var tableInfo = "";
    var id = '';
    //var row = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_produceInSingles_productInSingleList_produceInSingl_sdg").getSelecteds()[0].rowIndex;
    //var tableNo = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_produceInSingles_productInSingleList_produceInSingl_sdg").getValueByKey(row,"taskReportNo");
    if (tableNo) {
        $.ajax({
            url: "/msService/material/foreign/foreign/findTableInfoId",
            type: "GET",
            async: false,
            data: {
                tableNo: tableNo,
                deal: "saleNo"
            },
            success: function (res) {
                var data = res.data;
                if (res != null && data && data.tableInfoId) {
                    tableInfo = data.tableInfoId;
                    id = data.tableId;
                }
            }

        });
    } else {
        return false;
    }

    if (tableInfo) {
        // 销售出库单实体的entityCode
        var entityCode = "material_1.0.0_saleOut";
        // 销售出库单实体的查看视图URL
        var url = "/msService/material/saleOut/saleOutSingle/saleOutView";
        // 当前页面的URL
        var currentPageURL = window.location.href;
        // 菜单操作编码
        var operateCode = "material_1.0.0_saleOut_saleOutList_self";
        var pcMap = ReactAPI.getPowerCode(operateCode);
        var pc = pcMap[operateCode];
        //url += "?tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + id + "&__pc__=" + pc;
        url += "?tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + id;
        window.open(url);
    }
}

function onLoad() {

}

function ptInit() {
    dataInit();
    var status = ReactAPI.getFormData().status;
    if (status != 99) {
        dgDetail.setBtnHidden(["inBoundView"]);
    }
}


function inBoundDetailView() {
    var dgData = dgDetail.getSelecteds();
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