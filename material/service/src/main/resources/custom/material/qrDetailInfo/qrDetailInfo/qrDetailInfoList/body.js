//======================================================================================
//自定义代码
//======================================================================================

/**
 * 打开界面时根据策略状态,加不同底色
 * @author yaoyao
 * @date 2021-09-07
 */
function ChangeColor(){
    var dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs("material_1.0.0_inventoryMethod_invMtdList_inventoryMtd_sdg");
    var length = dataGrid.getDatagridData().length;
    var invStatus = null;
    for (var i = 0; i < length; i++) {
        invStatus = dataGrid.getValueByKey(i, "invStatus").id;
        //策略状态为“启用”时
        if("material_ruleState/started" == invStatus){
            dataGrid.setDatagridCellAttr(i, "invStatus.value",{ className: "sup-datagrid-cell startStatus" });
        }
        //策略状态为"停用"时
        else if("material_ruleState/stopped" == invStatus){
            dataGrid.setDatagridCellAttr(i, "invStatus.value", { className: "sup-datagrid-cell stopStatus" });
        }
	}
}

/**
 * 打开界面时根据策略状态,加不同底色
 * @author yaoyao
 * @date 2021-09-07
 */
function openSplitEdit(){

}
