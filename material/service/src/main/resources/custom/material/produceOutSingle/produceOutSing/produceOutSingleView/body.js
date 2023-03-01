function onclickSrcNo(tableNo) {
    var tableInfo = ""; 
	var id = '';
    if (tableNo) {
		$.ajax({
			url: "/msService/material/foreign/foreign/findTableInfoId",
			type: "GET",
			async: false,
			data: { 
				tableNo: tableNo,
                deal: "putInMaterial"
			},
			success: function (res) {
				var data = res.data;
				if (res != null && data && data.tableInfoId) {
					tableInfo = data.tableInfoId;
					id = data.tableId;
				}
			}

		});
	}else{
		return false;
	}
	
     if (tableInfo) {

        // 用料单实体的entityCode
        var entityCode = "WOM_1.0.0_putInMaterial";
        // 用料单实体的查看视图URL
        var url = "/msService/WOM/putInMaterial/putInMaterial/putinView";
        // 当前页面的URL
        var currentPageURL = window.location.href;
        // 菜单操作编码
        var operateCode = "WOM_1.0.0_putInMaterial_putinList_self";
        var pcMap = ReactAPI.getPowerCode(operateCode);
        var pc = pcMap[operateCode];
        url += "?tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + id + "&__pc__=" + pc;
        window.open(url);
    }
}


function onclickBatchNum(id) {
     if (id) {
      ReactAPI.createDialog("newDialog", {
		title: '生产批次详情', 
		url: "/msService/WOM/produceTask/matConsumRecod/matConsumeRecordView?id="+id,
		size: 5,
		callback: (data, event) => {
			partCallback(data, event);
		},
		isRef: true, // 是否开启参照
		
		onCancel: (data, event) => {	
              ReactAPI.destroyDialog("newDialog");
		},
		cancelText:'关闭'
	});
    }
}




var dg
var dgApi
function dginit(){
dgApi="material_1.0.0_produceOutSingle_produceOutSingleViewdg1574143418999"
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


