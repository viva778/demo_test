function movePickSite(movePsNodeId, movToNodeId) {

	//后台请求数据，并进行处理
	$.ajax({
		type: 'post',
		url: "/msService/material/warehouse/warehouse/moveResult",

		data: { movePsNodeId:movePsNodeId, movToNodeId:movToNodeId },
		async: false,
		success: function(res) {
			ReactAPI.closeConfirm();
			//刷新树
			parent.ReactAPI.getComponentAPI("NavTree").APIs("material_1.0.0_wareModel_wareLayout_wareModel_nt").refreshTreeNode(-1);
			//刷新列表
			parent.ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_wareModel_wareLayout_wareModel_sdg").refreshDataByRequst();
            
			setTimeout(function(){
				parent.ReactAPI.showMessage("s", ReactAPI.international.getText("EditView.notice.operate.success"));
                ReactAPI.destroyDialog("wareTreeMoveDialog");
			},800);
			parent.ReactAPI.destroyDialog("wareTreeMoveDialog");
            parent.ReactAPI.destroyDialog("pickSIteTreeMoveDialog");
			
          parent.ReactAPI.destroyDialog("pickSIteTreeMoveDialog");
		},
		error: function(res) {
			//ReactAPI.closeConfirm();
			var resultInfo = JSON.parse(res.responseText);
			ReactAPI.showMessage("f", resultInfo.msg, ReactAPI.international.getText("Notification.message.title.info"));
            
			return false;
		}
	});
}

