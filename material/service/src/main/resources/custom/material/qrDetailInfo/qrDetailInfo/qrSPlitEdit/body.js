//======================================================================================
//自定义代码
//======================================================================================

/**
 * 拆包编辑界面，打开时执行js
 * @author yaoyao
 * @changeLog
 *   1. 新增 2022-03-25 by yaoyao
 **/
function splitEditOnload() {
  var urlData = ReactAPI.getParamsInRequestUrl();

  var wareObj = {
    id: urlData.warehouseId,
    code: urlData.warehouseCode,
    name: urlData.warehouseName,
  };
  var materObj = {
    id: urlData.materialId,
    code: urlData.materialCode,
    name: urlData.materialName,
  };

  if(urlData.batchText != undefined && urlData.batchText != "null"){
    ReactAPI.getComponentAPI("Input")
      .APIs("qrDetailInfo.batchText")
      .setValue(urlData.batchText);
  }
  ReactAPI.getComponentAPI("InputNumber")
    .APIs("qrDetailInfo.availableQty")
    .setValue(Number(urlData.availableQty));
  ReactAPI.getComponentAPI("Input")
    .APIs("qrDetailInfo.sequenceCode")
    .setValue(urlData.sequenceCode);
  ReactAPI.getComponentAPI("InputNumber")
    .APIs("qrDetailInfo.splitLeftNum")
    .setValue(Number(urlData.availableQty));
  if(wareObj.id != undefined && wareObj.id != "null"){
    ReactAPI.getComponentAPI("Reference")
      .APIs("qrDetailInfo.warehouse.name")
      .setValue(wareObj);
  }
  ReactAPI.getComponentAPI("Reference")
    .APIs("qrDetailInfo.material.name")
    .setValue(materObj);
}

/**
 * 拆包明细数量变化时检测值是否合理
 * @author yaoyao
 * @changeLog
 *  1. 新增 2022-03-25 by yaoyao
 */
function detaillChangeQty() {
  var allNum = ReactAPI.getComponentAPI("InputNumber")
    .APIs("qrDetailInfo.availableQty")
    .getValue();
  var gridTable = ReactAPI.getComponentAPI("SupDataGrid").APIs(
    "material_1.0.0_qrDetailInfo_qrSPlitEditdg1647748494109"
  );
  var tableData = gridTable.getDatagridData();
  var tableLength = tableData.length;
  var splitAllNum = 0;

  for (let i = 0; i < tableLength; i++) {
    let tempRow = tableData[i];
    let tempQty = tempRow.newQuantity;
    if(tempQty != undefined){
      splitAllNum += Number(tempQty);
    }
  }

  if (allNum < splitAllNum) {
    ReactAPI.showMessage("w", "明细总量超过了可拆总量，请核实数据!");
    return false;
  }

  ReactAPI.getComponentAPI("InputNumber")
    .APIs("qrDetailInfo.splitLeftNum")
    .setValue(allNum - splitAllNum);
}

/**
 * 拆包按钮，增行赋值
 * @author yaoyao
 * @changeLog
 *  1. 新增 2022-03-25 by yaoyao
 */
function splitAddRow() {
  var urlData = ReactAPI.getParamsInRequestUrl();
  var gridTable = ReactAPI.getComponentAPI("SupDataGrid").APIs(
    "material_1.0.0_qrDetailInfo_qrSPlitEditdg1647748494109"
  );
  var newData = {
    batchText: ReactAPI.getComponentAPI("Input")
      .APIs("qrDetailInfo.batchText")
      .getValue(),
    materialCode: ReactAPI.getComponentAPI("Reference")
      .APIs("qrDetailInfo.material.name")
      .getValue()[0].code,
    materialId: ReactAPI.getComponentAPI("Reference")
      .APIs("qrDetailInfo.material.name")
      .getValue()[0].id,
    materialName: ReactAPI.getComponentAPI("Reference")
      .APIs("qrDetailInfo.material.name")
      .getValue()[0].name,
    qrDetail: { id: urlData.qrId },
  };
  var dataArray = [newData];
  gridTable.addLine(dataArray, true);
}

/**
 * 通过标准数据服务获取电子秤读数
 * @author yaoyao
 * @changeLog
 *  1. 新增 2022-04-01 by yaoyao
 */
function getValueByEquipCode(){
    var gridTable = ReactAPI.getComponentAPI("SupDataGrid").APIs(
        "material_1.0.0_qrDetailInfo_qrSPlitEditdg1647748494109"
    );

    var selectedData = gridTable.getSelecteds();

    if(selectedData.length == 0 ){
        ReactAPI.showMessage("w", "请选择一行记录进行操作!");
        return false;
    }

    if(!selectedData[0].equipTag){
        ReactAPI.showMessage("w", "请先选择电子秤设备!");
        return false;
    }

    var result = ReactAPI.request({
        url: "/msService/material/qrSplitEdit/getNumByEquipCode",
        type: "GET",
        data: {equipCode: selectedData[0].equipTag.code},
        async: false,
    });

    if(result.code == 200 && result.data && result.data.ReadTagsSyncResult.length > 0) {
        gridTable.setValueByKey(gridTable.getSelecteds()[0].rowIndex, "newQuantity", result.data.ReadTagsSyncResult[0].Value);
        detaillChangeQty();
    } else {
        ReactAPI.showMessage("w", "获取电子秤读数失败，请重试!");
    }
}

