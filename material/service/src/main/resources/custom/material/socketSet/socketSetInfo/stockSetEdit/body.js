var validAlarm = (obj, nRow, key) =>{
	var dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs('material_1.0.0_socketSet_stockSetEditdg1573107110496');
	var upAlarm = dataGrid.getValueByKey(nRow, 'upAlarm');
	var lowAlarm = dataGrid.getValueByKey(nRow, 'downAlarm');
	var safetyAlarm = dataGrid.getValueByKey(nRow, 'safetyAlarm');
	if(upAlarm != "" && lowAlarm != ""){
		//低库存大于最大库存
		if(upAlarm < lowAlarm){
			dataGrid.setValueByKey(nRow, key, '');
		}
	}

	if(safetyAlarm != "" && lowAlarm != ""){
		//低库存大于安全库存
		if(safetyAlarm < lowAlarm){
			dataGrid.setValueByKey(nRow, key, '');
		}
	}
	if(safetyAlarm != "" && upAlarm != ""){
		//安全库存大于最高库存
		if(upAlarm < safetyAlarm){
			dataGrid.setValueByKey(nRow, key, '');
		}
	}
}