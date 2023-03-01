	var inventory = (tableId, ware) => {
  
        var inventoryId = tableId;
        var wareHouseId = ware[0].id;
        var result = ReactAPI.request({
            type: "get",
            data: {
                "inventoryId": inventoryId,
                "wareHouseId": wareHouseId
            },
            url: "/msService/material/inventory/inventory/wareHouseInventory",
            async: false
        });
        console.log(result);
        queryData();
      
    }
    
    var queryData = () => {
        // 获取请求 url 参数接口
        var urlParams = ReactAPI.getParamsInRequestUrl();
        // 重新刷新单元格数据
        ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_inventory_inventoryEditdg1575439236333").refreshDataByRequst({
            type: "post",
            url: "/msService/material/inventory/inventory/data-dg1575439236333?datagridCode=material_1.0.0_inventory_inventoryEditdg1575439236333&id=" + urlParams.id,
            param: {pageSize:200}
        });
      	
      
    }