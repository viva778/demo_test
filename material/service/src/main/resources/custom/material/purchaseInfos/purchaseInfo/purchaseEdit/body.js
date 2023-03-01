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