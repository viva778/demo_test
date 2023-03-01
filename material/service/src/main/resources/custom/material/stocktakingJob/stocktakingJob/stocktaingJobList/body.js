//-----盘点任务列表-----

const dgListName = "material_1.0.0_stocktakingJob_stocktaingJobList_stocktakingJob_sdg";
var dgList;

function dataInit() {
    dgList = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgListName);
    dataInit = () => { }
}

function ptInit() {
    dataInit();
    dgList.setBtnImg("doJob", "sup-btn-own-bgyt");
}


function ptDblClick(e, selRow) {
    if (selRow.jobState.id == "material_stocktakingOperateState/edit") {
        ptBtnDoJob(selRow);
    } else {
        ptBtnView(selRow);
    }
}

function ptBtnView(selRow) {
    selRow = selRow || dgList.getSelecteds()[0];
    if (!selRow) {
        ReactAPI.showMessage('w', ReactAPI.international.getText("ec.common.checkselected"));
        return false;
    }
    //打开操作框
    ReactAPI.createDialog("stocktaking_job", {
        title: ReactAPI.international.getText("material.stocktakingJob.StocktakingJob"), //盘点任务
        url: "/msService/material/stocktakingJob/stocktakingJob/stocktaingJobView?id=" + selRow.id + "&" + toStringCondition({
            staffId: ReactAPI.getUserInfo().staff.id,
            just4view: true
        }),
        width: '1500px',
        height: '800px',
        buttons: []
    });
}

function ptBtnDoJob(selRow) {
    selRow = selRow || dgList.getSelecteds()[0];
    if (!selRow) {
        ReactAPI.showMessage('w', ReactAPI.international.getText("ec.common.checkselected"));
        return false;
    }
    //判断数据状态
    if (selRow.jobState.id != "material_stocktakingOperateState/edit") {
        //只能盘点处于编辑状态的任务
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.stocktaking.job_state_constraint"));
        return false;
    }
    //打开操作框
    ReactAPI.createDialog("stocktaking_job", {
        title: ReactAPI.international.getText("material.stocktakingJob.StocktakingJob"), //盘点任务
        url: "/msService/material/stocktakingJob/stocktakingJob/stocktaingJobEdit?id=" + selRow.id + "&" + toStringCondition({
            staffId: ReactAPI.getUserInfo().staff.id,
        }),
        width: '1500px',
        height: '800px',
        buttons: [
            {
                text: ReactAPI.international.getText("ec.flow.submit"),
                type: "primary",
                onClick: function (event) {
                    event.resetDg();
                    event.ReactAPI.submitFormData("submit", function (res) {
                        if (res && res.code == 200) {
                            ReactAPI.destroyDialog("stocktaking_job");
                        }
                    });
                }
            },
            {
                text: ReactAPI.international.getText("Button.text.save"),
                type: "primary",
                onClick: function (event) {
                    event.resetDg();
                    event.ReactAPI.submitFormData("save", function (res) {
                        if (res && res.code == 200) {
                            ReactAPI.destroyDialog("stocktaking_job");
                        }
                    });
                }
            },
            {
                text: ReactAPI.international.getText("Button.text.cancel"),
                type: "cancel",
                onClick: function () {
                    ReactAPI.destroyDialog("stocktaking_job");
                }
            },

        ]
    });
}

function toStringCondition(condition) {
    var valid_keys = Object.keys(condition).filter(key => condition[key]);
    if (valid_keys.length) {
        return "customConditionKey=" + valid_keys.join(",") + "&" + valid_keys.map(key => key + "=" + condition[key]).join("&");
    }
    return ""
}