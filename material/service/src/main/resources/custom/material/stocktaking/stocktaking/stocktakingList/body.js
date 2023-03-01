//-----盘点单列表-----

const dgListName = "material_1.0.0_stocktaking_stocktakingList_stocktaking_sdg";
var dgList;

function dataInit() {
    dgList = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgListName);
    dataInit = () => { }
}

function ptInit() {
    dataInit();
    dgList.setBtnImg("end", "sup-btn-own-zz");
}

function ptBtnEnd() {
    var selRow = dgList.getSelecteds()[0];
    if (!selRow) {
        ReactAPI.showMessage('w', ReactAPI.international.getText("ec.common.checkselected"));
        return false;
    }
    if (selRow.status == 99) {
        //盘点已结束
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.stocktaking.stocktaking_job_finished_already"));
        return false;
    }
    // 自定义按钮
    ReactAPI.openConfirm({
        //此操作会将已盘点的数据直接生效，确认操作吗
        message: ReactAPI.international.getText("material.stocktaking.quick_end_confirm"),
        buttons: [
            {
                operatetype: "yes",
                //确定
                text: ReactAPI.international.getText("foundation.common.currency.ok"),
                type: "primary",
                onClick: function () {
                    //调用接口
                    ReactAPI.closeConfirm();
                    ReactAPI.openLoading();
                    ReactAPI.request({
                        url: "/msService/material/stocktaking/quickEnd?stocktakingId=" + selRow.id,
                        type: "post",
                        async: true
                    }, result => {
                        ReactAPI.closeLoading();
                        if (result.code != 200) {
                            ReactAPI.showMessage('f', result.message);
                            return false;
                        }
                        ReactAPI.getComponentAPI().SearchPanel.APIs("material_1.0.0_stocktaking_stocktakingList_stocktaking_sp").updateSearch();
                    });

                }
            },
            {
                operatetype: "no",
                //取消
                text: ReactAPI.international.getText("foundation.common.cancel"),
                type: "primary",
                onClick: function () {
                    ReactAPI.closeConfirm();
                }
            }
        ]
    });
}