var dg
var dgApi
function dginit(){
dgApi="material_1.0.0_purchaseReturn_purReturnViewdg1574995261261"
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


