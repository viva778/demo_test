//-----拆包列表-----
const dgListName = "material_1.0.0_unpack_unpackList_unpackInfo_sdg";

var dgList;

function dataInit() {
    dgList = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgListName);
    dataInit = () => { }
}

function ptInit() {
    dataInit();
    dgList.setBtnImg("pack", "sup-btn-zidong-chaifen");
    dgList.setBtnImg("batch", "sup-btn-own-fp");
}

function ptDblClick(event, row) {
    var isSplitBatch = (row.splitType && row.splitType.id) == "material_splitType/batch";
    var url = (isSplitBatch ? "/msService/material/unpack/unpackInfo/unpackBatchView" : "/msService/material/unpack/unpackInfo/unpackView") + "?id=" + row.id;
    var title = ReactAPI.international.getText(isSplitBatch ? "material.viewtitle.randon1668148803231" : "material.viewdisplayName.randon1667443345702");
    ReactAPI.createDialog("unpack_view", {
        title: title,
        size: 5,
        url: url,
        buttons: []
    });
}

function ptBtnBatch() {
    //弹出窗口拆批
    ReactAPI.createDialog("split_batch", {
        title: ReactAPI.international.getText("material.viewtitle.randon1668061870677"),//拆批
        size: 5,
        url: "/msService/material/unpack/unpackInfo/unpackBatchEdit",
        buttons: [
            {
                text: ReactAPI.international.getText("Button.text.save"),
                type: "primary",
                onClick: function (event) {
                    event.ReactAPI.submitFormData("save", function (res) {
                        if (res && res.code == 200) {
                            ReactAPI.destroyDialog("split_batch");
                        }
                    });
                }
            },
            {
                text: ReactAPI.international.getText("Button.text.cancel"),
                type: "cancel",
                onClick: function () {
                    ReactAPI.destroyDialog("split_batch");
                }
            },
        ]
    });
}

function ptBtnPack() {
    //弹出窗口拆包
    ReactAPI.createDialog("unpack", {
        title: ReactAPI.international.getText("material.viewtitle.randon1667441073276"),//拆包
        size: 5,
        url: "/msService/material/unpack/unpackInfo/unpackEdit",
        buttons: [
            {
                text: ReactAPI.international.getText("Button.text.save"),
                type: "primary",
                onClick: function (event) {
                    event.ReactAPI.submitFormData("save", function (res) {
                        if (res && res.code == 200) {
                            ReactAPI.destroyDialog("unpack");
                        }
                    });
                }
            },
            {
                text: ReactAPI.international.getText("Button.text.cancel"),
                type: "cancel",
                onClick: function () {
                    ReactAPI.destroyDialog("unpack");
                }
            },
        ]
    });
}