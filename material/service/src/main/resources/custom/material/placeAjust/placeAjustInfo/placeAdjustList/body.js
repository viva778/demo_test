var dgList;

function dataInit() {
    dgList = ReactAPI.getComponentAPI().SupDataGrid.APIs("material_1.0.0_placeAjust_placeAdjustList_placeAjustInfo_sdg");
    dataInit = () => { };
}

function onLoad() {
    dataInit();
}

function ptInit() {
    dataInit();
    dgList.setBtnImg("overall", "sup-btn-own-qianyi");
    dgList.setBtnImg("tankAdjust", "sup-btn-own-dcstxx");
    dgList.setBtnImg("tonTankAdjust", "sup-btn-own-db");
}


//由整体调整单回调
function overallCallback(fromPlaceId, toPlaceId) {
    ReactAPI.destroyDialog("overallAdj");
    ReactAPI.openLoading("处理中");
    ReactAPI.request({
        type: "post",
        url: "/msService/HfWareCustom/placeAdjust/createPlaceAdjustByPlaceId?" + ["fromPlaceId=" + fromPlaceId, "toPlaceId=" + toPlaceId].join("&"),
        async: true
    }, result => {
        ReactAPI.closeLoading();
        if (result && result.code == 200) {
            ReactAPI.showMessage('s', "操作成功");
            ReactAPI.getComponentAPI("SearchPanel").APIs("material_1.0.0_placeAjust_placeAdjustList_placeAjustInfo_sp").updateSearch();
        } else {
            ReactAPI.showMessage('f', result.message);
        }
    });
}

function ptBtnOverall() {
    ReactAPI.createDialog("overallAdj", {
        title: "货位整体调整",
        url: "/msService/material/placeAjust/placeOvrAdj/placeOvrAdjEdit",
        size: 1,
        buttons: [
            {
                text: "提交",
                type: "primary",
                onClick: function (event) {
                    event.ReactAPI.submitFormData("submit", function (res) {
                        if (res && res.code == 200) {
                            ReactAPI.destroyDialog("overallAdj");
                        }
                    });
                }
            }
        ]
    });
}

function ptBtnTankAdjust() {
    ReactAPI.createDialog("tankAdj", {
        title: "储罐调整",
        url: "/msService/material/placeAjust/tankAdjust/tankAdjustEdit",
        size: 5,
        buttons: [
            {
                text: "提交",
                type: "primary",
                onClick: function (event) {
                    event.ReactAPI.submitFormData("submit", function (res) {
                        if (res && res.code == 200) {
                            ReactAPI.destroyDialog("tankAdj");
                        }
                    });
                }
            }
        ]
    });
}

function ptBtnTonTankAdjust() {
    ReactAPI.createDialog("tonTankAdj", {
        title: "单吨罐调整",
        url: "/msService/material/placeAjust/tankAdjust/tonTankAdjEdit",
        size: 5,
        buttons: [
            {
                text: "提交",
                type: "primary",
                onClick: function (event) {
                    event.ReactAPI.submitFormData("submit", function (res) {
                        if (res && res.code == 200) {
                            ReactAPI.destroyDialog("tonTankAdj");
                        }
                    });
                }
            }
        ]
    });
}