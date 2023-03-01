//-----盘点查看-----

const dgStatusName = "material_1.0.0_stocktaking_stocktakingViewdg1660700053214";

var dgStatus;
var urlDgStatus;

function dataInit() {
	dgStatus = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgStatusName);
	urlDgStatus = "/msService/material/stocktaking/stRecordStatus/data-dg1660700053214?datagridCode=material_1.0.0_stocktaking_stocktakingViewdg1660700053214&id=" + ReactAPI.getFormData().id + "&groupType=material";
	dataInit = () => { }
}

function ptInit() {
	dataInit();
    setStatusBtnImg();
	//增加groupType参数
	dgStatus.setRequestUrl(urlDgStatus);
}

function setStatusBtnImg() {
    // 设置 查看图标
    dgStatus.setBtnImg("btn-record", "sup-btn-own-ck");
}


function ptRenderOver() {
	var dgData = dgStatus.getDatagridData();
	dgData.forEach(rowData => {
		//设置颜色
		var color = getQuantityColor(rowData.quantityOnBook, rowData.quantityByCount);
		rowData.quantityOffset_attr = {
			style: {
				color: color
			}
		}
		if (rowData.dispute) {
			rowData.row_attr = {
				background: rgbToHex([220, 211, 251])
			}
		}
	});
	dgStatus.setDatagridData(dgData);
}


function ptBtnRecord() {
	var selRow = dgStatus.getSelecteds()[0];
	//打开操作框
	ReactAPI.createDialog("record_list", {
		title: ReactAPI.international.getText("material.viewdisplayName.randon1661218109528"), //盘点任务
		url: "/msService/material/stocktakingJob/stocktakingJob/stockRecordListView?" + toStringCondition({
			stocktakingId: ReactAPI.getFormData().id,
			materialId: selRow && selRow.material && selRow.material.id,
			just4view: true
		}),
		width: '1500px',
		height: '800px',
		buttons: [],
		onClose: function () {
			if (ReactAPI.getIframeWindow("record_list").rechecked) {
				//重新刷新表体
				dgStatus.refreshDataByRequst({
					type: "POST",
					url: urlDgStatus
				});
			}
			ReactAPI.destroyDialog("record_list");
		}
	});
}

function toStringCondition(condition) {
	var valid_keys = Object.keys(condition).filter(key => condition[key]);
	if (valid_keys.length) {
		return "customConditionKey=" + valid_keys.join(",") + "&" + valid_keys.map(key => key + "=" + condition[key]).join("&");
	}
	return ""
}


const base_col = 0x50;
const minimum_col = 0xA0;
const maximum_col = 0xFF;

function rgbToHex(rgb) {
	return '#' + ((parseInt(rgb[0]) << 16) + (parseInt(rgb[1]) << 8) + parseInt(rgb[2])).toString(16);
}

function getQuantityColor(quantityOnBook, quantityByCount) {
	var quantityOffset = quantityByCount - quantityOnBook;
	var color;
	if (quantityOffset > 0) {
		//越盈越蓝，2倍封顶
		var rate = 2 - quantityByCount / quantityOnBook;
		if (rate < 0) {
			color = rgbToHex([base_col, base_col, maximum_col]);
		} else {
			color = rgbToHex([base_col, base_col, maximum_col * (1 - rate) + minimum_col * rate]);
		}
	} else if (quantityOffset < 0) {
		//越亏越红，2倍封顶
		var rate = 2 - quantityOnBook / quantityByCount;
		if (rate < 0) {
			color = rgbToHex([maximum_col, base_col, base_col]);
		} else {
			color = rgbToHex([maximum_col * (1 - rate) + minimum_col * rate, base_col, base_col]);
		}
	} else {
		color = "#111";
	}
	return color;
}