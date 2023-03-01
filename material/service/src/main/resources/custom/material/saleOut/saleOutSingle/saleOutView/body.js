function onclick(srcID) {

    var tableInfo = '';

    if (srcID) {
        var result = ReactAPI.request({
            url: "/msService/material/saleOut/saleOutSingle/getTableInfo",
            type: "get",
            data: {
                "srcID": srcID,
                "tableType": "salDelivery"
            },
            async: false
        });
        if (result.code == 200) {
            let data = result.data;
            tableInfo = data.result;
            if (result != null && data && data.tableInfoId) {
                tableInfo = data.tableInfoId;

            }
        }
    }

    if (tableInfo) {
        // 销售发货单实体的entityCode
        var entityCode = "material_1.0.0_salDelivery";
        // 销售发货单实体的查看视图URL
        var url = "/msService/material/salDelivery/salDeliveryInf/salDeiliView";
        // 当前页面的URL
        var currentPageURL = window.location.href;
        // 菜单操作编码
        var operateCode = "material_1.0.0_saleOut_saleOutList_self";
        var pcMap = ReactAPI.getPowerCode(operateCode);
        var pc = pcMap[operateCode];
        url += "?tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + srcID + "&__pc__=" + pc;
        window.open(url);
    }
}


var dg
var dgApi
function dginit(){
dgApi="material_1.0.0_saleOut_saleOutViewdg1574228691804"
dg=ReactAPI.getComponentAPI("SupDataGrid")
  .APIs(dgApi)
}

function onload(){
dginit();
var status=ReactAPI.getFormData().status;
if(status!=99){
 ReactAPI.getComponentAPI("SupDataGrid")
  .APIs(dgApi)
  .setBtnHidden(["outBoundView"]);
}
else{
//通过表体id查询入库通知的信息
$.ajax({
    url:"/msService/WMSPRO/outBoundList/outBoundList/findIdBySrcPartId?srcPartId="+dgData[0].id,
    type: 'get',
    async: false,
    success: function (res) {
        id=res.data
    }
})
//隐藏无下游出库通知的单据
if(!id){
 ReactAPI.getComponentAPI("SupDataGrid")
  .APIs(dgApi)
  .setBtnHidden(["outBoundView"]);
}
}

}


  function outBoundDetailView(){
  dgData=dg.getSelecteds();
  if(!dgData.length || dgData.length==0){

        ReactAPI.showMessage("w", ReactAPI.international.getText("请至少选择一行！" ));
        return false;
  }
var id
//通过表体id查询出库通知的信息
$.ajax({
     url:"/msService/WMSPRO/outBoundList/outBoundList/findIdBySrcPartId?srcPartId="+dgData[0].id,
    type: 'get',
    async: false,
    success: function (res) {
        id=res.data
    }
})
//入库通知打开
ReactAPI.createDialog("newDialog", {
    title: ReactAPI.international.getText("入库明细"),
    url: "/msService/WMSPRO/outBoundList/outBoundList/outBoundDetailView?id=" + id,
    size: 5,
    isRef: false

})
}

//表体加载判定
function outBoundPtInit(){
//通过表体id查询入库通知的信息
$.ajax({
    url:"/msService/WMSPRO/outBoundList/outBoundList/findIdBySrcPartId?srcPartId="+dg.getDatagridData()[0].id,
    type: 'get',
    async: false,
    success: function (res) {
        id=res.data
    }
})
//隐藏无下游入库通知的单据
if(!id){
 ReactAPI.getComponentAPI("SupDataGrid")
  .APIs(dgApi)
  .setBtnHidden(["outBoundView"]);
}
}


