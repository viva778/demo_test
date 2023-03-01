//======================================================================================
//自定义代码
//======================================================================================

/**
 * 将采购订单的单据编号字段显示为超链接的样式, 点击后跳转到制定的采购订单的查看视图
 */
function renderPurchaseOrderNo() {
  	debugger
    var dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_purArrivalInfos_purArrivalInfoViewdg1574068127680");
    var length = dataGrid.getDatagridData().length;
    for (var i = 0; i < length; i++) {
        var purchaseNo = dataGrid.getValueByKey(i, "purchaseNo");
        var purchaseId = dataGrid.getValueByKey(i, "purchaseId");
        var html = "<a onclick=\"openPurchaseOrder(" + purchaseId + ")\"><span onmouseover=\"this.style.color='#f00'\" onmouseout=\"this.style.color='#3366CC'\" style='text-decoration:underline;color:#3366CC;cursor:pointer'>" + purchaseNo + "</span></a>";
        $("div[id='material_1.0.0_purArrivalInfos_purArrivalInfoViewdg1574068127680_row" + i + "'] div[data-key='purchaseNo']").children().html(html);
    }

}

/**
 * 打开采购订单查看视图
 * @param {采购订单表体ID} srcID 
 */
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
		// 采购订单实体的entityCode
		var entityCode = "material_1.0.0_purchaseInfos";
		// 查看视图URL
		var url = "/msService/material/purchaseInfos/purchaseInfo/purchaseView";
		// 当前页面的URL
		var currentPageURL = window.location.href;
		// 菜单操作编码
		var operateCode = "material_1.0.0_purchaseInfos_purchaseList_self";
		var pcMap = ReactAPI.getPowerCode(operateCode);
		var pc = pcMap[operateCode];
		url += "?tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + tableId + "&__pc__=" + pc;
		window.open(url);
	}
}