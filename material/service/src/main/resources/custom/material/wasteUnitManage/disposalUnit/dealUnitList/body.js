function hyperlink(nRow) {
	
	var disposalUnit = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_wasteUnitManage_dealUnitList_disposalUnit_sdg").getValueByKey(nRow, "id");
	operateCode = "material_1.0.0_wasteUnitManage_rateLis";
	pcMap = ReactAPI.getPowerCode(operateCode);
	pc = pcMap[operateCode];
	var url = "/msService/material/wasteUnitManage/rateDetail/rateList?disposalUnitId=" + disposalUnit+"&customConditionKey=disposalUnitId";
  
  
    ReactAPI.createDialog("newDialog", {
        title: ReactAPI.international.getText("material.custom.random1634613424619"), //物料参照
        url: url,
        size: 5,
        callback: (data, event) => {
            partCallback(data, event);
        },
        isRef: true, // 是否开启参照
        onOk: (data, event) => {
            partCallback(data, event);
        },
        onCancel: (data, event) => {
            ReactAPI.destroyDialog("newDialog");
        }
    });
//  window.open(url);
}