//-----拆包列表-----
const dgListName = "material_1.0.0_unpack_unpackList__mobile___unpackInfo_sp";

var dgList;

// ---------------------- 通用方法 start ----------------------
/**
 * 获取国际化值
 * @param key 国际化key
 */
function getIntlValue(key) {
    var intlValue;
    var result = ReactAPI.request(
        {
            type: "get",
            data: {},
            url: "/inter-api/i18n/v1/internationalConvert?key=" + key,
            async: false
        },
        function (res) {
            intlValue = res && res.data;
            return intlValue
        }
    );
    return intlValue.replace('</b>', '').replace('<b>', "").replace('<br/>', "");
}

//占位符匹配
String.prototype.format = function () {
    if (arguments.length === 0) return this;
    for (var s = this, i = 0; i < arguments.length; i++)
        s = s.replace(new RegExp("\\{" + i + "\\}", "g"), arguments[i]);
    return s;
};

function uuid() {
    function S4() {
        return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
    }

    return (S4() + S4() + "-" + S4() + "-" + S4() + "-" + S4() + "-" + S4() + S4() + S4());
}

// ---------------------- 通用方法 end ----------------------


function onLoading() {
}

function dataInit() {
    ReactAPI.setClickEvt(function () {
        const rownIndex = $(event.target.closest('.list-cell')).index();
        ReactAPI.setSelected(rownIndex.toString());
        const data = ReactAPI.getSelected() && ReactAPI.getSelected() [0];
        if (!data) {
            return false;
        }
        var isSplitBatch = (data.splitType && data.splitType.id) == "material_splitType/batch";
        var url = (isSplitBatch ? "/msService/material/unpack/unpackInfo/unpackBatchView" : "/msService/material/unpack/unpackInfo/unpackView") + "?clientType=mobile&id=" + data.id;
        var title = getIntlValue(isSplitBatch ? "material.viewtitle.randon1668148803231" : "material.viewdisplayName.randon1667443345702");
        ReactAPI.createDialog({
            id: "unpackView",
            title: title,//拆包记录
            url: url,
            footer: [
                {
                    name: "返回",
                    onClick: function (event) {
                        ReactAPI.destroyDialog("unpackView");
                    }
                    // style: {color: "#fff", background: "#ff0000", 'display': 'none'}
                }]
        });
        return false;
    });
    dataInit = () => {
    }
}

function ptInit() {
    dataInit();
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


