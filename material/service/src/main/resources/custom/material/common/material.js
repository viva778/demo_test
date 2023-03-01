const material = {
    /**
     * 根据仓库下的库存物料，修改物料参照的url,进行过滤
     * @param {*} rfWarehouse 
     * @param {*} url 
     * @returns 
     */
    getMaterialsByWare: function (rfWarehouse, url) {
        var warehouseArr = rfWarehouse.getValue();
        if (warehouseArr.length > 0) {
            var warehouseId = warehouseArr[0].id;
            var fastQueryCond = {
                "viewCode": "material_1.0.0_standingcrop_onhandList",
                "modelAlias": "standingcrop",
                "condName": "fastCond",
                "remark": "fastCond",
                "subconds": [
                    {
                        "type": "2",
                        "joinInfo": "BASESET_WAREHOUSES,ID,MATER_STANDINGCROPS,WARE",
                        "subconds": [
                            {
                                "type": "0",
                                "columnName": "ID",
                                "dbColumnType": "LONG",
                                "operator": "=",
                                "paramStr": "?",
                                "value": warehouseId+""
                            }
                        ]
                    },
                    {
                        "type": "0",
                        "columnName": "IS_AVAILABLE",
                        "dbColumnType": "BOOLEAN",
                        "operator": "=",
                        "paramStr": "?",
                        "value": "1"
                    },
                    {
                        "type": "0",
                        "columnName": "AVAILI_QUANTITY",
                        "dbColumnType": "DECIMAL",
                        "operator": ">",
                        "paramStr": "?",
                        "value": "0"
                    }
                ]
            }
            var response = ReactAPI.request({
                type: "post",
                url: "/msService/material/standingcrop/standingcrop/onhandList-query",
                async: false,
                data: {
                    "classifyCodes": "",
                    "fastQueryCond": JSON.stringify(fastQueryCond),
                    "customCondition": {},
                    "permissionCode": "material_1.0.0_standingcrop_onhandList"
                },
            });
            if (response.code == 200) {
                let data = response.data;
                if (data && data.result && data.result.length > -1) {
                    var materialIds = data.result.length > 0 ? data.result.filter(val => val.onhand > 0).map(val => val.good.id) : [];
                    if (materialIds.length > 0) {
                        // 有库存，参照只显示当前仓库下的物料
                        materialIds = Array.from(new Set(materialIds)).join(",")//物料去重
                        url += ("&cappMaterialIds=" + materialIds + "&customConditionKey=cappMaterialIds")
                    } else {
                        // 没有库存提示，不再弹出参照
                        ReactAPI.showMessage("w", ReactAPI.international.getText("material.custom.random1675221540712"));
                        return false;
                    }
                }
            } else {
                ReactAPI.showMessage("w", response.message);
                return false;
            }
        }
        return url;
    }
}