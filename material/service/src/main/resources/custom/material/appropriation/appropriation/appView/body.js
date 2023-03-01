
var dg
var dgApi
function dginit(){
dgApi="material_1.0.0_appropriation_appViewdg1574663298940"
dg=ReactAPI.getComponentAPI("SupDataGrid")
  .APIs(dgApi)
}

function onLoad(){
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
  .setBtnHidden(["outBoundView","inBoundView"]);
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
function inOutBoundPtInit(){
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
    debugger
 ReactAPI.getComponentAPI("SupDataGrid")
  .APIs(dgApi)
  .setBtnHidden(["outBoundView"]);
}
var inId
var dgData=dg.getDatagridData();
var inBoundBtn=false;
for(var i=0;i<dgData.length;i++){
    $.ajax({
        url:"/msService/WMSPRO/inBoundList/inBoundList/findIdBySrcPartId?srcPartId="+dg.getDatagridData()[0].id,
        type: 'get',
        async: false,
        success: function (res) {
            inId=res.data
        }
    })

    if(inId){
        inBoundBtn=true;
       }
}

if(!inBoundBtn && !id){
    debugger
    ReactAPI.getComponentAPI("SupDataGrid")
    .APIs(dgApi)
    .setBtnHidden(["inBoundView","outBoundView"]);
}
else if(!id){
    ReactAPI.getComponentAPI("SupDataGrid")
    .APIs(dgApi)
    .setBtnHidden(["outBoundView"]);
}
else if(!inBoundBtn ){
    ReactAPI.getComponentAPI("SupDataGrid")
    .APIs(dgApi)
    .setBtnHidden(["inBoundView"]);
}

}


function inBoundDetailView(){
    dgData=dg.getSelecteds();
    if(!dgData.length || dgData.length==0){

          ReactAPI.showMessage("w", ReactAPI.international.getText("请至少选择一行！" ));
          return false;
    }
  var id
  //通过表体id查询出库通知的信息
  $.ajax({
       url:"/msService/WMSPRO/inBoundList/inBoundList/findIdBySrcPartId?srcPartId="+dgData[0].id,
      type: 'get',
      async: false,
      success: function (res) {
          id=res.data
      }
  })
  if(!id){
    ReactAPI.showMessage("w", ReactAPI.international.getText("该行无入库任务！" ));
    return false;
  }
  //入库通知打开
  ReactAPI.createDialog("newDialog", {
      title: ReactAPI.international.getText("入库明细"),
      url: "/msService/WMSPRO/inBoundList/inBoundList/inBoundDetailView?id=" + id,
      size: 5,
      isRef: false

  })
  }