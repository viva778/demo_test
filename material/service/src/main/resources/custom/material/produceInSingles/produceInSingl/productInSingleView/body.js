function onclickDirectiveNo(tableNo) {
    var tableInfo = ""; 
	var id = '';
    if (tableNo) {
		$.ajax({
			url: "/msService/material/foreign/foreign/findTableInfoId",
			type: "GET",
			async: false,
			data: { 
				tableNo: tableNo,
                deal: "directiveNo"
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

        // 实体的entityCode
        var entityCode = "WOM_1.0.0_produceTask";
        // 实体的查看视图URL
        var url = "/msService/WOM/produceTask/produceTask/makeTaskView";
        // 当前页面的URL
        var currentPageURL = window.location.href;
        // 菜单操作编码
        var operateCode = "WOM_1.0.0_produceTask_makeTaskList_self";
        var pcMap = ReactAPI.getPowerCode(operateCode);
        var pc = pcMap[operateCode];
        url += "?tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + id + "&__pc__=" + pc;
        window.open(url);
    }
}


function onclickTaskReportNo(tableNo) {
    var tableInfo = ""; 
	var id = '';
    if (tableNo) {
		$.ajax({
			url: "/msService/material/foreign/foreign/findTableInfoId",
			type: "GET",
			async: false,
			data: { 
				tableNo: tableNo,
                deal: "taskReportNo"
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

        // 实体的entityCode
        var entityCode = "WOM_1.0.0_produceTask";
        // 实体的查看视图URL
        var url = "/msService/WOM/produceTask/produceTask/makeTaskView";
        // 当前页面的URL
        var currentPageURL = window.location.href;
        // 菜单操作编码
        var operateCode = "WOM_1.0.0_produceTask_makeTaskList_self";
        var pcMap = ReactAPI.getPowerCode(operateCode);
        var pc = pcMap[operateCode];
        url += "?tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + id + "&__pc__=" + pc;
        window.open(url);
    }
}

function renderOverIn(){
var a=ReactAPI.getComponentAPI("SupDataGrid")
  .APIs("material_1.0.0_produceInSingles_productInSingleViewdg1573805176867")
  .getDatagridData();
  for(var i=0;i<a.length;i++){
	if(a[i].good.isBatch.id=='BaseSet_isBatch/nobatch'){
		ReactAPI.getComponentAPI("SupDataGrid")
  .APIs("material_1.0.0_produceInSingles_productInSingleViewdg1573805176867")
  .setValueByKey(i, 'batchText','');
	}
  }
  }
var dg
function dginit(){
  
dgApis="material_1.0.0_produceInSingles_productInSingleViewdg1573805176867"
dg=ReactAPI.getComponentAPI("SupDataGrid")
  .APIs(dgApis)

}
function onload(){
  
dginit();
var status=ReactAPI.getFormData().status;
if(status!=99){
 ReactAPI.getComponentAPI("SupDataGrid")
  .APIs(dgApis)
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
//入库通知打开
ReactAPI.createDialog("newDialog", {
    title: ReactAPI.international.getText("入库明细"),
    url: "/msService/WMSPRO/inBoundList/inBoundList/inBoundDetailView?id=" + id,
    size: 5,
    isRef: false

})
}

//表体加载判定
function inBoundPtInit(){
//通过表体id查询入库通知的信息
$.ajax({
    url:"/msService/WMSPRO/inBoundList/inBoundList/findIdBySrcPartId?srcPartId="+dg.getDatagridData()[0].id,
    type: 'get',
    async: false,
    success: function (res) {
        id=res.data
    }
})
//隐藏无下游入库通知的单据
if(!id){
 ReactAPI.getComponentAPI("SupDataGrid")
  .APIs(dgApis)
  .setBtnHidden(["inBoundView"]);
}
}


