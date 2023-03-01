
function wareAlarm_RenderOverEvent() {
	
    var refresh = localStorage.getItem("refresh");
	if(refresh == "true"){
        localStorage.setItem("refresh","false");
        return false;
	}
	var dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_socketSet_wareAlarm_socketSetPart_sdg");
	var length = dataGrid.getDatagridData().length;
	if (length == 0) {
		return false;
	}

	for (var i = 0; i < length; i++) {
		var wareId = dataGrid.getValueByKey(i, 'socketSetInfo.ware.id');
		var goodId = dataGrid.getValueByKey(i, 'good.id');
		var partId = dataGrid.getValueByKey(i, 'id');
		//获取物品的现存量
		$.ajax({
			url: "/msService/material/socketSet/socketSetPart/findStandQuality",
			type: 'post',
			async: false,
			data: {
				"ware": wareId,
				"good": goodId,
				"partId": partId
			},
			success: function (msg) {
		
			}
		});

	}
        localStorage.setItem("refresh","true");
  
       	ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_socketSet_wareAlarm_socketSetPart_sdg").refreshDataByRequst({
		type: "post",
		url: "/msService/material/socketSet/socketSetPart/wareAlarm-query",
		param: {}
	});

}

