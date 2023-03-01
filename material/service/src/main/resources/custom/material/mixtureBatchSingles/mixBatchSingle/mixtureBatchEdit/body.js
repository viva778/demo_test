function checkTime() {

	var date = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_mixtureBatchSingles_mixtureBatchEditdg1576128077743");
	var length = date.getDatagridData().length;
	var mixProductDate = null;//最小生产日期         
	var mixProduct = null;//最小生产日期--日期格式
	if (length > 0) {
		for (var i = 0; i < length; i++) {

			var mixProduct = date.getValueByKey(i, "onhand.materBatchInfo.productionDate");
			if (mixProduct != null) {
				//当生产日期不为空时，和最小生产日期对比
				if (mixProductDate != null) {
					if (mixProduct < mixProductDate) {
						mixProductDate = mixProduct;
					}
				} else {
					mixProductDate = mixProduct;
				}

			}

		}
		if (null == mixProductDate) {
			ReactAPI.getComponentAPI("DatePicker").APIs("mixBatchSingle.produceDate").setValue(null);
			return false;
		}
		var time = mixProductDate;
		var d = new Date(time);
		var month = "";
		if ((d.getMonth() + 1) < 10) {
			month = "0" + (d.getMonth() + 1);
		} else {
			month = d.getMonth() + 1;
		}
		var date = d.getFullYear() + '-' + month + '-' + d.getDate();

		ReactAPI.getComponentAPI("DatePicker").APIs("mixBatchSingle.produceDate").setValue(date);

		//若表体无数据时，生产日期设置为空
	} else if (length == 0) {
		ReactAPI.getComponentAPI("DatePicker").APIs("mixBatchSingle.produceDate").setValue(null);
	}


}

/**
 * 计算有效截止日期
 * @param {生产日期} produceDate 
 * @param {有效期} validNum 
 * @param {有效期单位} validUnit 
 * @param {类型} type 
 */
function generateValidTime(produceDate, goodId, type) {
    var keepDay = '';
    
    var result = ReactAPI.request({
        type: "get",
        data: {
            "produceDate": produceDate,
            "goodId": goodId,
            "type": type
        },
        url: "/msService/material/mixtureBatchSingles/mixtureBatchSingle/getApproachTime",
        async: false
    });
    console.log(result);

    if (result.data) {
        keepDay = result.data;
    }
    return keepDay;
}

//计算混批数量
function calculationNum() {

    var data = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_mixtureBatchSingles_mixtureBatchEditdg1576128077743");
    var sum = 0;

    var length = data.getDatagridData().length;
    if (length > 0) {
        for (var i = 0; i < length; i++) {
            sum = sum + data.getValueByKey(i, "quantity");
        }
        ReactAPI.getComponentAPI("InputNumber").APIs("mixBatchSingle.mixtureQuan").setValue(sum);
    } else {
        ReactAPI.getComponentAPI("InputNumber").APIs("mixBatchSingle.mixtureQuan").setValue(null);
    }

}
