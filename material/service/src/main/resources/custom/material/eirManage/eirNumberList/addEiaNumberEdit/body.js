//======================================================================================
//自定义代码
//======================================================================================

/**
 * 打开界面时根据策略状态,加不同底色
 * @author yaoyao
 * @date 2021-12-17
 */
function eiaEditRefer_method() {
  var editPage_dg = ReactAPI.getComponentAPI("SupDataGrid").APIs(
    "material_1.0.0_eirManage_addEiaNumberEditdg1632882390131"
  );
  var editPage_dg_maters = {};

  for (let i = 0; i < editPage_dg.getDatagridData().length; i++) {
    let dg_mater = editPage_dg.getDatagridData()[i].wasteGood;
    editPage_dg_maters[dg_mater.id] = true;
  }

  ReactAPI.createDialog("newDialog", {
    title: ReactAPI.international.getText(
      "material.buttonPropertyshowName.randon1592465499893.flag"
    ), //物料参照
    url: "/msService/BaseSet/material/material/materialRef?multiSelect=true",
    size: 5,
    isRef: true, // 是否开启参照
    onOk: (data, event) => {
      eiaEditReferConfirm_method(data, editPage_dg, editPage_dg_maters, event);
      //callback(data, event);
      ReactAPI.showMessage(
        "s",
        ReactAPI.international.getText("foundation.common.tips.addsuccessfully")
      );
    },
    onCancel: (data, event) => {
      ReactAPI.destroyDialog("newDialog");
    },
    okText: ReactAPI.international.getText("Button.text.select"), // 选择
    cancelText: ReactAPI.international.getText("Button.text.close") // 关闭
  });
}

/**
 * 参照回调确认方法
 * @author yaoyao
 * @date 2021-12-17
 */
function eiaEditReferConfirm_method(data, editPage_dg, editPage_dg_maters, event) {
  //debugger;
  //0. 选中后将选中数据回传,否则提示需要选中数据
  if (data.length != 0) {
    var company = ReactAPI.getComponentAPI("Reference")
      .APIs("eirNumberList.toCompany.name")
      .getValue()[0];

    var eia_year = ReactAPI.getComponentAPI("DatePicker")
      .APIs("eirNumberList.eiaYear")
      .getValue();

    for (var i = 0; i < data.length; i++) {
      var goodId = data[i].id;
      var goodName = data[i].name;
      var materialClassId = data[i].materialClass.id;
      var materialName = data[i].materialClass.name;

      //1.判断是否在父页面是否已经存在，已存在的不用重复参照
      if (editPage_dg_maters[goodId]) {
        continue;
      }

      //2.如果不存在父页面则在父页面新增行，并赋值
      editPage_dg.addLine();
      var lenNum = editPage_dg.getDatagridData().length - 1;
      editPage_dg.setValueByKey(lenNum, "wasteGood", {
        id: goodId,
        name: goodName,
        materialClass: { id: materialClassId, name: materialName }
      });
      if (company != undefined) {
        editPage_dg.setValueByKey(lenNum, "companyInfo", company.fullName);
      }
      if (eia_year != undefined) {
        editPage_dg.setValueByKey(lenNum, "eiaYear", eia_year);
      }
    }

    //3. 全部选中行处理完成后，关闭弹出层，在原页面上提上操作成功
    ReactAPI.destroyDialog("newDialog");
  } else {
    //4.未选择时，提示用户“请至少选中一行”
    event.ReactAPI.showMessage(
      "w",
      ReactAPI.international.getText("material.custom.randon1574406106043")
    );
  }
}
