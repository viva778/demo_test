// ----绑定托盘 移动视图----
const dgContainerName = "material_1.0.0_purchaseInSingles_containerEdit__mobile__dg1667303999536";
var dgContainer;

// ---------------------- 初始化 start ----------------------
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
    console.log(intlValue)
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
var ctDetails = [];

// ------------------采购入库-移动视图-容器--------------------

var containerData;

function onLoad() {
    //初始化
    dgContainer = ReactAPI.getComponentAPI("Datagrid").APIs(dgContainerName);
    //获取上层传入的数据
    var dataString = window.sessionStorage.getItem('reference-data')
    containerData = JSON.parse(dataString)
    ReactAPI.getComponentAPI("Reference").APIs("purchInPart.good.code").setValue(containerData.purchInPart.good)
    ReactAPI.getComponentAPI("Input").APIs("purchInPart.batch").setValue(containerData.purchInPart.batch)
}

function getInitData() {
    if (containerData.purchInPart.dataList != [] && containerData.purchInPart.dataList.length > 0) {
        dgContainer.addLine(containerData.purchInPart.dataList, true)
    }

}

function ptBtnContainerRef() {
    ReactAPI.openReference({
        id: "newReference",
        url: "/msService/material/containerFile/containerParts/containerDetailRef?clientType=mobile&"
            + toStringCondition({
                materialId: containerData['purchInPart'].good.id,
                batchNum: containerData['purchInPart'].batch,
                purArrivalInfo:containerData['purchInPart'].purArrivalInfo
            }),
        type: "Other",
        displayfield: "bm1", // 显示字段
        onOk: function (data) {
            //todo dgContainer初始化 
            if (data.length > 0) {
                dgContainer.addLine(data.map(cd => new Object({
                    container: cd.container,
                    material: cd.materInfo,
                    quantity: cd.materQty,
                    purchaseInDetailUuid: containerData['purchInPart'].uuid
                })), true);
            }
            ReactAPI.closeReference("newReference")
        },
        onClose: function (data) {
            //销毁参照
            ReactAPI.closeReference("newReference")
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