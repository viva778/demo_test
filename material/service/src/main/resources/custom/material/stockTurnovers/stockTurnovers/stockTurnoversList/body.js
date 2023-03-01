
var SearchPanel_Key = "material_1.0.0_stockTurnovers_stockTurnoversList_stockTurnovers_sp";

// 初始化查询条件
function initSearchPanel() {

    // 查询条件必填校验
    ReactAPI.getComponentAPI("SearchPanel").APIs(SearchPanel_Key).beforeSearch(function (data) {

        var wareName = data.ware_name;
        console.log(wareName);

        var startDate = data.startDate;
        console.log(startDate);

        if ((wareName == null || wareName.length == 0) || startDate == null) {

            var tip = null;
            if (wareName == null || wareName.length == 0) {
                //仓库名称不能为空
                tip = ReactAPI.international.getText("material.custom.warehouseNameNotNull");
            }
            if (startDate == null) {

                if (tip == null) {

                    //tip = "起止时间不能为空";
                    tip = ReactAPI.international.getText("material.custom.startAndEndTimeNotNull");
                }
                else {
                    //tip = "仓库名称、起止时间不能为空";
                    tip = ReactAPI.international.getText("material.custom.warehouseNameAndTimeNotNull");
                }
            }
            // 查询条件"值类型"不能为空！
            ReactAPI.showMessage("w", tip);
            return false;
        }

        var myDate = new Date(); //判断结束时间不能超过今天
        var tomorrow = myDate.setHours(0, 0, 0, 0); //当前日期的开始时间

        var endDate = data.startDate[1]._d;

        if (endDate > tomorrow) {
            //结束时间不能超过今天 
            ReactAPI.showMessage("w", ReactAPI.international.getText("material.custom.endTimeNotExceedToday"));
            return false;
        }

        return true;
    });
}