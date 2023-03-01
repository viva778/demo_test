//-----其他入库移动视图-----

window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");
loader.import("utility");

//获取是否启用wmsPro模块,并设成全局常量
var integrateWmsPro;
var enablePlace;
//是否生成任务（开启PRO并且开启货位
var generateTask;

//定义“其他入库单编辑”页面上面的表格对象，全局共用（在ptInit中初始化）
var dgDetail;
var cbEnablePlace;
var cbCheckRequire;

var rfServiceType;
var rfReasonExplain;
var rfWarehouse;
var scRedBlue;

// 供应商
var vendorRf
var vendorId
// 供应商下对应供应商物料
var vendorMaterials = []
var vendorMaterialsMap = {}
// 卡片收起时的影藏项
var rollKeys = ["good.specifications", "good.model", "good.isBatch", "batchText", "genPrintInfo", "checkResult", "inMemo"]

//发起请检
var checkOption;

var wareCache
var vendorCache


const dgDetailName = "material_1.0.0_otherInSingle_inSingleEdit__mobile__dg1667525755014";

/**
 * 获取国际化值
 * @param key 国际化key
 */
function getIntlValue(key) {
    var intlValue;
    ReactAPI.request(
        {
            type: "get",
            data: {},
            url: "/inter-api/i18n/v1/internationalConvert?key=" + key,
            async: false
        },
        function (res) {
            intlValue = res && res.data;
        }
    );
    return intlValue;
}

Date.prototype.Format = function (fmt) {
    var o = {
        "M+": this.getMonth() + 1, // 月份
        "d+": this.getDate(), // 日
        "h+": this.getHours(), // 小时
        "m+": this.getMinutes(), // 分
        "s+": this.getSeconds(), // 秒
        "q+": Math.floor((this.getMonth() + 3) / 3), // 季度
        "S": this.getMilliseconds() // 毫秒
    };
    if (/(y+)/.test(fmt))
        fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o)
        if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
    return fmt;
}

function dataInit() {


    cbEnablePlace = ReactAPI.getComponentAPI("Boolean")
        .APIs("otherInSingle.ware.storesetState");
    enablePlace = cbEnablePlace.getValue();
    integrateWmsPro = (getSystemConfigInMobile({
        moduleCode: "material",
        key: "material.wmspro",
    })["material.wmspro"] == 'true');
    generateTask = (integrateWmsPro && enablePlace);

    rfWarehouse = ReactAPI.getComponentAPI("Reference")
        .APIs("otherInSingle.ware.name");

    ReactAPI.getComponentAPI().Reference.APIs("otherInSingle.ware.name")
        .APIAfterCallback(function (data) {
            ocgWarehouse(data);
        })

    rfServiceType = ReactAPI.getComponentAPI("Reference")
        .APIs("otherInSingle.serviceTypeID.serviceTypeExplain");

    rfReasonExplain = ReactAPI.getComponentAPI("Reference")
        .APIs("otherInSingle.inCome.reasonExplain");
    scRedBlue = ReactAPI.getComponentAPI("SystemCode")
        .APIs("otherInSingle.redBlue");

    dgDetail = ReactAPI.getComponentAPI("Datagrid").APIs(
        "material_1.0.0_otherInSingle_inSingleEdit__mobile__dg1667525755014"
    );
    vendorRf = ReactAPI.getComponentAPI("Reference")
        .APIs("otherInSingle.vendor.name");
    cbCheckRequire = ReactAPI.getComponentAPI("Boolean")
        .APIs("otherInSingle.inspectRequired");
    checkOption = getSystemConfigInMobile({
        moduleCode: "material",
        key: "material.otherInCheckOptions",
    })["material.otherInCheckOptions"];
    // 入库日期默认当前
    //设置仓库缓存
    wareCache = rfWarehouse.getValue()
    dataInit = () => {
    };
}


/**
 * 其他入库单编辑界面onLoad脚本,初始化部分默认值以及根据红蓝字设置功能按钮是否可用
 * @author  dfx
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by dfx
 */
function onLoad() {


    dataInit();
    // var serviceType = {
    //     serviceTypeCode: "otherStorageIn",
    //     id: 1000,
    //     serviceTypeExplain: getIntlValue1(
    //         "material.custom.OtherWarehousingTransactions"
    //     ),
    // };
    var serviceType = {
        serviceTypeCode: "otherStorageIn",
        id: 1000,
        serviceTypeExplain: "其他入库事务",
    };
    // var reasonExplain = {
    //     id: 1025,
    //     reasonExplain: getIntlValue(
    //         "material.custom.ConventionalMaterials"
    //     ),
    // };
    var reasonExplain = {
        id: 1025,
        reasonExplain: "常规物料入库",
    };
    //根据配置项设置是否需要请检
    cbCheckRequire.setValue(checkOption && checkOption != "no");
    //业务类型赋值
    setTimeout(() => {
        ReactAPI.getComponentAPI("Reference")
            .APIs("otherInSingle.serviceTypeID.serviceTypeExplain").setValue(serviceType);
    })


    var inCome = ReactAPI.getComponentAPI("Reference")
        .APIs("otherInSingle.inCome.reasonExplain").getValue();
    if (!inCome[0]) {
        setTimeout(() => {
            ReactAPI.getComponentAPI("Reference")
                .APIs("otherInSingle.inCome.reasonExplain").setValue(reasonExplain);
        })
    }
    //初始日期赋值
    setTimeout(() => {
        ReactAPI.getComponentAPI("DatePicker").APIs("otherInSingle.inStorageDate").setValue(new Date().Format("yyyy-MM-dd"))
    })

    // var redBlueValue = scRedBlue.getValue().value;
    scRedBlue.setValue("BaseSet_redBlue/blue")

    initVendor();

    refreshRequired();

    // 绑定入库数量onchange
    $('.sup-datagrid-con').on('change', 'div[data-key=\'appliQuanlity\'] input', function (e) {

        let appliQuanlity = $(this).val();
        let packageWeight = $(this).parents('.sup-datagrid-list').find('div[data-key=\'packageWeight\'] input').val();

        if (appliQuanlity && packageWeight && parseFloat(packageWeight)) {
            let packageNumber = (parseFloat(appliQuanlity) / parseFloat(packageWeight)).toFixed(2);
            let rowIndex = $(this).parents('.sup-datagrid-item').attr('data-rowindex');
            const rowData = ReactAPI.getComponentAPI("Datagrid").APIs(
                "material_1.0.0_otherInSingle_inSingleEdit__mobile__dg1667525755014"
            ).getRows(rowIndex)[0];
            rowData.packageNumber = packageNumber
            ReactAPI.getComponentAPI("Datagrid").APIs(
                "material_1.0.0_otherInSingle_inSingleEdit__mobile__dg1667525755014"
            ).setRowData(Number(rowIndex), rowData)
            // $(this).parents('.sup-datagrid-list').find('div[data-key=\'packageNumber\'] input').val(packageNumber);
        }

    })

    // 绑定包数onchange
    $('.sup-datagrid-con').on('change', 'div[data-key=\'packageNumber\'] input', function (e) {
        let packageNumber = $(this).val();
        let packageWeight = $(this).parents('.sup-datagrid-list').find('div[data-key=\'packageWeight\'] input').val();
        if (packageNumber && packageWeight && parseFloat(packageWeight)) {
            let appliQuanlity = (parseFloat(packageNumber) * parseFloat(packageWeight)).toFixed(2);
            let rowIndex = $(this).parents('.sup-datagrid-item').attr('data-rowindex');
            const rowData = ReactAPI.getComponentAPI("Datagrid").APIs(
                "material_1.0.0_otherInSingle_inSingleEdit__mobile__dg1667525755014"
            ).getRows(rowIndex)[0];
            rowData.appliQuanlity = appliQuanlity
            ReactAPI.getComponentAPI("Datagrid").APIs(
                "material_1.0.0_otherInSingle_inSingleEdit__mobile__dg1667525755014"
            ).setRowData(Number(rowIndex), rowData)

        }

    })

}

function initVendor() {
    if (vendorRf && vendorRf.getValue().length > 0) {
        vendorId = vendorRf.getValue()[0].id;
        vendorCache = vendorRf.getValue()
        ReactAPI.request({
            url: "/msService/material/entity/getEntityList",
            type: "get",
            async: false,
            data: {
                moduleName: "BaseSet",
                entityName: "BaseSetSupplierMater",
                conditions: "valid=1,cooperator.id=" + vendorId,
                includes: "id,material,packageWeight"
            }
        }, function (result) {
            if (result && result.code == 200 && result.data) {
                let suppliers = result.data;
                if (suppliers && suppliers.length > 0) {
                    vendorMaterials = suppliers;
                    vendorMaterials.forEach(v => {
                        vendorMaterialsMap[v.material.id] = v.packageWeight;
                    })
                }
            }
        })


    }
}

/**
 * 表体初始化时，处理相应样式
 * @author  dfx
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by dfx
 */
function ptInit() {
    dataInit();
    //设置复制行按钮样式
    $("#btn-copy i").attr("class", "sup-btn-icon sup-btn-own-fzh");
    //设置参照按钮
    dgDetail.setBtnImg("btn-goodRef", "sup-btn-eighteen-dt-op-reference");
    refreshRequired();
    //设置校验
    datagrid.validator.add(dgDetailName, "appliQuanlity", rowData => {
        //物品启用按件管理，入库数量只能为1件
        var batchType = rowData.good && rowData.good.isBatch && rowData.good.isBatch.id;
        if (batchType == "BaseSet_isBatch/piece") {
            return rowData.appliQuanlity == 1;
        } else {
            return true;
        }
    }, rowIndex => {
        return getIntlValue(
            "material.custom.can.only.beOne",
            String(rowIndex)
        );
    });
}

/**
 * 表体请求数据时处理信息
 * @author  dfx
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by dfx
 */
function ptRenderOver() {

    refreshReadonly();

    // 设置批号，包数只读
    refreshReadOnlyByIndex();

    refreshRequired();
}

// 设置批号，包数readOnly
function refreshReadOnlyByIndex() {
    let rowDatas = dgDetail.getDatagridData()
    if (rowDatas && rowDatas.length > 0) {
        dgDetail.setCellsAttr(rowDatas.map((r, index) => {
            var batchType = r.good && r.good.isBatch && r.good.isBatch.id;
            return {
                row: index,
                keyToAttr: {
                    batchText: {readonly: !(batchType && batchType != "BaseSet_isBatch/batch" && batchType != "BaseSet_isBatch/piece")},
                    packageNumber: {readonly: r.packageWeight == undefined || r.packageWeight == null}
                }
            }
        }))
    }
}

/**
 * 刷新字段必填、只读
 */
function refreshRequired() {
    //如果不生成入库任务，且启用货位，则货位必填
    var dgData = dgDetail.getDatagridData();
    if (dgData.length > 0) {
        var placeSetRequired = !generateTask && enablePlace ? true : false;
        dgDetail.setCellRequired(
            dgData.map((d, index) => {
                return {row: index, keys: ["placeSet.name"], required: placeSetRequired};
            })
        )
    }
}


function refreshReadonly() {
    //如果未开启货位，货位只读
    let dgData = dgDetail.getDatagridData();
    if (dgData.length > 0) {
        dgDetail.setCellsAttr(dgData.map((d, index) => {
            return {
                row: index,
                keyToAttr: {
                    "placeSet.name": {readonly: !enablePlace}
                }
            }
        }))
    }
}

var materialsIds

//查询仓库下的库存对应的所有物料
function getMaterialsByWare() {
    let warehouseArr = rfWarehouse.getValue();
    if (warehouseArr.length > 0) {
        var warehouseId = warehouseArr[0].id;
        var response
        $.ajax({
            type: "get",
            url: "/msService/material/wareModel/findGoodIdByWare",
            async: false,
            data: {
                wareId: warehouseId
            },
            success: function success(res) {
                response = JSON.parse(res)
            }
        });

        if (response.code === 200) {
            let data = response.data;
            if (data && data.length > 0) {
                return data;
            } else {
                return null;
            }
        } else {
            ReactAPI.showMessage("w", response.message);
        }
    }
    return false;
}

/**
 * 表体“参照”物料按钮事件
 * @author  dfx
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by dfx
 */
function goodRefFnc() {
    var url = "/msService/BaseSet/material/material/materialRef?clientType=mobile&multiSelect=true";
    var materialIds = getMaterialsByWare()
    if (materialIds && materialIds.length > 0) {
        if (vendorId) {
            url += "&cooperateId=" + vendorId + "&cappMaterialIds=" + materialIds + "&customConditionKey=cooperateId,cappMaterialIds";
        } else {
            url += "&cappMaterialIds=" + materialIds + "&customConditionKey=cappMaterialIds"
        }
    } else {
        if (vendorId) {
            url += "&cooperateId=" + vendorId + "&customConditionKey=cooperateId";
        }
    }
    ReactAPI.openReference({
        id: "newDialog",
        title: getIntlValue(
            "BaseSet.viewtitle.randon1569570764419"
        ), //物料参照
        type: "Other",
        displayfield: "bm1", // 显示字段
        url: url,
        isRef: true, // 是否开启参照
        onOk: (data) => {
            material_callback(data);
        },
        onCancel: () => {
            ReactAPI.destroyDialog("newDialog");
        },
    });
    var material_callback = (data) => {
        if (data && data.length) {
            let list = []
            data.forEach(material => {
                var newLine = {
                    good: material,
                    genPrintInfo: true,
                    packageWeight: null
                };
                // 物料对应的包重添加上
                if (typeof (vendorMaterialsMap) !== "undefined" && vendorMaterialsMap[material.id]) {
                    newLine['packageWeight'] = vendorMaterialsMap[material.id]
                }
                list.push(newLine)
            })
            ReactAPI.getComponentAPI("Datagrid").APIs(
                "material_1.0.0_otherInSingle_inSingleEdit__mobile__dg1667525755014"
            ).addLine(list, true);
            refreshRequired();
            refreshReadOnlyByIndex();
            refreshReadonly();
        } else {
            // //请至少选中一行
            // ReactAPI.showMessage(
            //     "w",
            //     getIntlValue("material.custom.randon1574406106043")
            // );
            return false;
        }
        ReactAPI.showMessage(
            "s",
            getIntlValue("foundation.common.tips.addsuccessfully")
        );
    };
}


/**
 * 根据是否有包重数据，设置包数是否只读
 */
function setPackageNumberReadOnly() {
    let dgData = dgDetail.getDatagridData();
    if (dgData.length > 0) {
        dgDetail.setCellsAttr(dgData.map((d, index) => {
            return {
                row: index,
                keyToAttr: {
                    packageNumber: {readonly: d.packageWeight ? false : true}
                }
            }
        }))
    }
}


/**
 * 表体“复制”物料按钮事件
 * @author  dfx
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by dfx
 */
function copyRowFnc() {
    // 选中行对象
    var selRows = dgDetail.getSelecteds();
    if (selRows.length == 0) {
        // 请至少选择一条数据！
        ReactAPI.showMessage(
            "w",
            getIntlValue("material.custom.randon1574406106043")
        );
        return;
    }
    dgDetail.addLine(selRows.map(rowData => {
        var copy = $.extend(true, {}, rowData);
        delete copy.id;
        delete copy.version;
        delete copy.sort;
        delete copy.currClickColKey;
        delete copy.edited;
        delete copy.key;
        delete copy.rowIndex;
        delete copy.batchText;
        return copy;
    }), true);

    refreshRequired();
}


var ignoreConfirmFlag = false;

/**
 * 其他入库单编辑界面onsave脚本
 * @author  dfx
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by dfx
 *   2.TODO:如果是为了检测是否超储问题,建议优化为后台处理，并只做提醒，不做限制 modify by dfx
 */
function onSave() {

    var type = ReactAPI.getOperateType();

    var dgData = dgDetail.getDatagridData();

    for (let i = 0; i < dgData.length; i++) {
        if (parseFloat(dgData[i].appliQuanlity) < 0) {
            ReactAPI.showMessage(
                "w",
                getIntlValue("material.custom.random1667903495079")
            );
            return false;
        }
    }

    if ("submit" == type) {
        var warehouse = rfWarehouse.getValue()[0];
        if (!dgData.length) {
            ReactAPI.showMessage(
                "w",
                getIntlValue("material.custom.randon1573634425210")
            ); //	表体数据不能为空！
            return false;
        }

        var check_result = datagrid.validator.check(dgDetailName);
        if (!check_result) {
            return false;
        }

        //校验超储,归并同物料
        if (!ignoreConfirmFlag) {
            var checked_idx = new Set();
            for (var i = 0; i < dgData.length; i++) {
                if (!checked_idx.has(i)) {
                    var material = dgData[i].good;
                    //统计物料数量
                    var quan = dgData[i].appliQuanlity;
                    for (var j = i + 1; j < dgData.length; j++) {
                        if (!checked_idx.has(j)) {
                            if (material.id == dgData[j].good.id) {
                                checked_idx.add(j);
                                quan += dgData[k].appliQuanlity;
                            }
                        }
                    }
                    //统计完成，进行校验
                    if (!check_material_limit(warehouse, material, quan)) {
                        return false;
                    }
                }
            }
        }
    }
    return true;
}

/**
 * 校验申请入库数量为正数
 */
function checkAppliQuanlity() {

}

function check_material_limit(warehouse, material, quan) {
    var result = ReactAPI.request({
        type: "get",
        url: "/msService/material/socketSet/socketSetInfo/findSocketSet",
        async: false,
        data: {
            wareId: String(warehouse.id),
            goodId: String(material.id),
            direction: "directionReceive",
            num: String(quan),
        },
    });
    if (result.code == 200) {
        let data = result.data;
        if (data.isNo) {
            ReactAPI.openConfirm({
                //<b>【{0}】</b>中<b>【{1}】</b>的最高库存为<b>{2}</b>，现存量<b>{3}</b>。<br><b>【{4}】</b>入库后库存数量将超过最大库存！
                message: getIntlValue(
                    "material.custom.SocketSet.confirm",
                    warehouse.name,
                    material.name,
                    String(data.UpAlarm),
                    String(data.Onhand),
                    material.name
                ),
                okText: getIntlValue(
                    "attendence.attStaff.isInstitutionYes"
                ), //是
                cancelText: getIntlValue(
                    "attendence.attStaff.isInstitutionNo"
                ), //否
                onOk: () => {
                    ReactAPI.closeConfirm();
                    ignoreConfirmFlag = true;
                    ReactAPI.submitFormData("submit");
                    ignoreConfirmFlag = false;
                    return true;
                },
                onCancel: () => {
                    ReactAPI.closeConfirm();
                    return false;
                },
            });
            return false;
        }
        return true;
    } else {
        ReactAPI.showMessage("w", result.message);
        return false;
    }
}

/**
 * 其他入库单编辑界面表头红蓝字变化时
 * @author  dfx
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by dfx
 */
function redBlueOnchangeFnc(value) {
    var preValue = ReactAPI.getComponentAPI("SystemCode")
        .APIs("otherInSingle.redBlue")
        .getValue().value;

    if (value == "BaseSet_redBlue/red") {
        if (dgDetail.getDatagridData().length != 0) {
            ReactAPI.openConfirm({
                message: getIntlValue(
                    "material.custom.clearTheTableBodyAtTheSameTime"
                ), //切换红蓝字会同时清空表体！
                okText: getIntlValue("ec.common.confirm"), //确定
                cancelText: getIntlValue(
                    "foundation.signature.cancel"
                ), //取消
                onOk: () => {
                    ReactAPI.closeConfirm();
                    // 清空表体
                    dgDetail.deleteLine();
                    ReactAPI.setHeadBtnAttr("redRef", {
                        icon: "sup-btn-icon sup-btn-eighteen-dt-op-reference",
                        isHide: false,
                    });
                    refreshReadonly();
                    $("#btn-goodRef").hide();
                    return false;
                },
                onCancel: () => {
                    ReactAPI.getComponentAPI("SystemCode")
                        .APIs("otherInSingle.redBlue")
                        .setValue(preValue);
                    ReactAPI.closeConfirm();
                    return false;
                },
            });
        } else {
            ReactAPI.setHeadBtnAttr("redRef", {
                icon: "sup-btn-icon sup-btn-eighteen-dt-op-reference",
                isHide: false,
            });
            $("#btn-goodRef").hide();
        }
    } else {
        if (dgDetail.getDatagridData().length != 0) {
            ReactAPI.openConfirm({
                message: getIntlValue(
                    "material.custom.clearTheTableBodyAtTheSameTime"
                ), //切换红蓝字会同时清空表体！
                okText: getIntlValue("ec.common.confirm"), //确定
                cancelText: getIntlValue(
                    "foundation.signature.cancel"
                ), //取消
                onOk: () => {
                    ReactAPI.closeConfirm();
                    // 清空表体
                    dgDetail.deleteLine();
                    ReactAPI.setHeadBtnAttr("redRef", {
                        icon: "sup-btn-icon sup-btn-eighteen-dt-op-reference",
                        isHide: true,
                    });
                    $("#btn-goodRef").show();
                    refreshReadonly();
                    return false;
                },
                onCancel: () => {
                    ReactAPI.getComponentAPI("SystemCode")
                        .APIs("otherInSingle.redBlue")
                        .setValue(preValue);
                    ReactAPI.closeConfirm();
                    return false;
                },
            });
        } else {
            ReactAPI.getComponentAPI("Reference")
                .APIs("otherInSingle.ware.code")
                .setReadonly(false);
            ReactAPI.setHeadBtnAttr("redRef", {
                icon: "sup-btn-icon sup-btn-eighteen-dt-op-reference",
                isHide: true,
            });
            $("#btn-goodRef").show();
        }
    }
}

/**
 * 其他入库单编辑界面表头仓库变化方法
 * @author  dfx
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by dfx
 */
function ocgWarehouse(value) {
    var length = dgDetail.getDatagridData().length;
    if (length > 0) {
        ReactAPI.openConfirm({
            //"修改后将清空表体数据，是否继续？",
            message: getIntlValue("material.custom.randonAfterModifyBody"),
            okText: getIntlValue("attendence.attStaff.isInstitutionYes"),//是
            cancelText: getIntlValue("attendence.attStaff.isInstitutionNo"),//否
            onOk: () => {
                ReactAPI.closeConfirm();
                // 清除表头时清空表体
                dgDetail.deleteLine();
                if (value && value.length > 0) {
                    var newEnablePlace = value && value[value.length - 1] && value[value.length - 1].storesetState;
                    if (newEnablePlace != enablePlace) {
                        enablePlace = newEnablePlace;
                        generateTask = (integrateWmsPro && enablePlace);
                        setTimeout(() => {
                            // 更新启用货位字段
                            rfWarehouse.setValue(value[value.length - 1])
                        })
                    }
                    if (enablePlace == false) {
                        //dgDetail, "placeSet.name"
                        const data = dgDetail.getDatagridData();
                        data.forEach(e => {
                            e["placeSet"] = null
                        })
                        dgDetail.setDatagridData(data)
                    }
                    wareCache = [value[value.length - 1]]
                    setTimeout(() => {
                        // 更新仓库数据
                        rfWarehouse.setValue(value[value.length - 1])
                        refreshRequired();
                        refreshReadonly();
                    })
                }
            },
            onCancel: () => {
                ReactAPI.closeConfirm();
                if (wareCache && wareCache[wareCache.length - 1]) {
                    setTimeout(() => {
                        // 更新仓库数据
                        setTimeout(() => {
                            // 更新仓库数据
                            rfWarehouse.setValue(wareCache[wareCache.length - 1])
                            refreshRequired();
                            refreshReadonly();
                        })
                    })
                } else {
                    setTimeout(() => {
                        // 更新仓库数据
                        rfWarehouse.removeValue()
                        refreshRequired();
                        refreshReadonly();
                    })
                }
                return false;
            }
        });
    } else {
        //清空表体
        dgDetail.deleteLine()
        enablePlace = false;

        if (value && value.length > 0) {
            wareCache = [value[value.length - 1]]
            setTimeout(() => {
                // 更新仓库数据
                rfWarehouse.setValue(value[value.length - 1])
                refreshRequired();
                refreshReadonly();
            })
        } else {
            wareCache = null
            setTimeout(() => {
                // 更新仓库数据
                refreshRequired();
                refreshReadonly();
            })
        }

    }


}

/**
 * 其他入库单编辑界面表头,参照红字冲销按钮
 * @author  dfx
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by dfx
 *   2.TODO: 待优化
 */
function redRefFnc() {
    ReactAPI.createDialog("newDialog", {
        title: getIntlValue(
            "material.custom.RedInkOffsetReference"
        ), //红字冲销参照
        url: "/msService/material/otherInSingle/inSingleDetail/inSinglePartRef",
        size: 5,
        callback: (data, event) => {
            partCallback(data, event);
        },
        isRef: true, // 是否开启参照
        onOk: (data, event) => {
            partCallback(data, event);
        },
        onCancel: (data, event) => {
            ReactAPI.destroyDialog("newDialog");
        },
        okText: getIntlValue("Button.text.select"), // 选择
        cancelText: getIntlValue("Button.text.close"), // 关闭
    });

    var partCallback = (data, event) => {
        if (data != null && data.length != 0) {
            var dgData = dgDetail.getDatagridData();
            for (var i = 0; i < data.length; i++) {
                var id = data[i].id;
                for (var j = 0; j < dgData.length; j++) {
                    var partId = dgDetail.getValueByKey(j, "partId");
                    if (id == partId) {
                        //校验重复
                        ReactAPI.showMessage(
                            "w",
                            getIntlValue(
                                "material.custom.CannotBeReferencedRepeatedly",
                                "" + (i + 1)
                            )
                        );
                        return false;
                    }
                }

                var wareCode = data[i].inSingle.ware.code;
                for (var t = 0; t < data.length; t++) {
                    var wareCodet = data[t].inSingle.ware.code;
                    if (wareCodet != wareCode) {
                        //所选仓库不同，无法同时参照！
                        ReactAPI.showMessage(
                            "w",
                            getIntlValue(
                                "material.custom.wareDifferent",
                                "" + (t + 1),
                                "" + (i + 1)
                            )
                        );
                        return false;
                    }
                }
                var ware = ReactAPI.getComponentAPI("Reference")
                    .APIs("otherInSingle.ware.code")
                    .getValue()[0];
                if (undefined != ware && wareCode != ware.code) {
                    //所选仓库不同，无法同时参照！
                    ReactAPI.showMessage(
                        "w",
                        getIntlValue("material.custom.wareisNotDifferent")
                    );
                    return false;
                }
            }
            for (var j = 0; j < data.length; j++) {
                var rowIndex = dgDetail.addLine().rowIndex;
                dgDetail.setValueByKey(rowIndex, "good", data[j].good);
                dgDetail.setValueByKey(rowIndex, "redPartID", data[j].id);
                dgDetail.setValueByKey(rowIndex, "batchText", data[j].batchText);
                dgDetail.setValueByKey(
                    rowIndex,
                    "inQuantity",
                    data[j].inQuantity - data[j].redNumber
                );
                dgDetail.setValueByKey(
                    rowIndex,
                    "appliQuanlity",
                    data[j].inQuantity - data[j].redNumber
                );
                dgDetail.setValueByKey(rowIndex, "placeSet", data[j].placeSet);
                dgDetail.setValueByKey(rowIndex, "productionDate", data[j].productionDate);
                dgDetail.setDatagridCellAttr(rowIndex, "good.name", {readonly: true});
                dgDetail.setDatagridCellAttr(rowIndex, "placeSet.name", {readonly: true});
            }
            ReactAPI.getComponentAPI("Reference")
                .APIs("otherInSingle.ware.name")
                .setReadonly(false);
            ReactAPI.getComponentAPI("Reference")
                .APIs("otherInSingle.ware.name")
                .setValue(data[0].inSingle.ware);
        } else {
            //请至少选中一行
            ReactAPI.showMessage(
                "w",
                getIntlValue("material.custom.randon1574406106043")
            );
            return false;
        }
        ReactAPI.showMessage(
            "s",
            getIntlValue("foundation.common.tips.addsuccessfully")
        );
    };
}

function beforeClearVendor(value) {
    var length = dgDetail.getDatagridData().length;
    if (length == 0) {
        // 清除供应商id
        vendorId = null;
        vendorMaterials = [];
        vendorMaterialsMap = {};
        vendorCache = null
        return true;
    }
    var id = null;
    var name = null;
    var code = null;
    if (undefined != vendorRf.getValue()[0]) {
        id = vendorRf.getValue()[0].id;
        name = vendorRf.getValue()[0].name;
        code = vendorRf.getValue()[0].code;
    }

    ReactAPI.openConfirm({
        message: getIntlValue("material.custom.randonAfterModifyBody"),
        buttons: [
            {
                name: getIntlValue("attendence.attStaff.isInstitutionYes"),//是
                type: "primary",
                onClick: function () {
                    ReactAPI.closeConfirm();
                    // 清除表头供应商时清空表体
                    dgDetail.deleteLine();
                    //设置值
                    vendorCache = null
                },
            },
            {
                name: getIntlValue("attendence.attStaff.isInstitutionNo"),//否
                type: "primary",
                onClick: function () {
                    ReactAPI.closeConfirm();
                    if (null != id) {
                        vendorRf.setValue({
                            code: code,
                            id: id,
                            name: name,
                        });
                    }
                    return;
                },
            }
        ],
    });
}

var selectedVendorVar


function vendorChangeCallback(value) {
    if (value && value.length > 0) {
        if (value.length == 1) {
            selectedVendorVar = value[0]

        } else {
            selectedVendorVar = value[1]
        }
        var length = dgDetail.getDatagridData().length;
        if (length > 0) {
            ReactAPI.openConfirm({
                message: getIntlValue("material.custom.randonAfterModifyBody"),
                buttons: [
                    {
                        name: getIntlValue("attendence.attStaff.isInstitutionYes"),//是
                        type: "primary",
                        onClick: function () {
                            ReactAPI.closeConfirm();
                            // 清除表头供应商时清空表体
                            dgDetail.deleteLine();
                            //设置值
                            selectVendor()
                        },
                    },
                    {
                        name: getIntlValue("attendence.attStaff.isInstitutionNo"),//否
                        type: "primary",
                        onClick: function () {
                            ReactAPI.closeConfirm();
                            setTimeout(() => {
                                if (vendorCache) {
                                    ReactAPI.getComponentAPI("Reference").APIs("otherInSingle.vendor.name").setValue(vendorCache || null)
                                } else {
                                    ReactAPI.getComponentAPI("Reference").APIs("otherInSingle.vendor.name").removeValue()
                                }
                            })
                            return;
                        },
                    }
                ],
            });
        } else {
            selectVendor()
        }

        function selectVendor() {
            vendorId = selectedVendorVar.id;
            ReactAPI.request({
                url: "/msService/material/entity/getEntityList",
                type: "get",
                async: false,
                data: {
                    moduleName: "BaseSet",
                    entityName: "BaseSetSupplierMater",
                    conditions: "valid=1,cooperator.id=" + vendorId,
                    includes: "id,material,packageWeight"
                }
            }, function (result) {
                if (result && result.code == 200 && result.data) {
                    let suppliers = result.data;
                    if (suppliers && suppliers.length > 0) {
                        vendorMaterials = suppliers;
                        vendorMaterials.forEach(v => {
                            vendorMaterialsMap[v.material.id] = v.packageWeight;
                        })
                    } else {
                        // 清除供应商下物料缓存
                        vendorMaterials = [];
                        vendorMaterialsMap = {};
                    }
                }
            })
            //设置值
            setTimeout(() => {
                vendorCache = selectedVendorVar
                ReactAPI.getComponentAPI("Reference").APIs("otherInSingle.vendor.name").setValue(selectedVendorVar)
            })
        }


    }
}


/**
 * 传入对象返回url参数
 * @param {Object} data {a:1}
 * @returns {string}
 */
function getParam(data) {
    let url = '';
    for (var k in data) {
        let value = data[k] !== undefined ? data[k] : '';
        url += `&${k}=${encodeURIComponent(value)}`
    }
    return url ? url.substring(1) : ''
}


/**
 * 按照件数和单件量计算申请入库数量
 * @param value
 * @param rowIndex 行号
 */
function renderAppliQuanlityOnPackageNumberChange(value, rowIndex) {
    var itemQty = dgDetail.getValueByKey(rowIndex, "packageWeight");
    if (value && itemQty) {
        dgDetail.setCellValueByKey(rowIndex, "appliQuanlity", (parseFloat(value) * itemQty).toFixed(2))
    } else if (parseFloat(value) === 0) {
        dgDetail.setCellValueByKey(rowIndex, "appliQuanlity", 0)
    }
}

/**
 * 根据申请入库数量计算件数
 * @param value
 * @param rowIndex 行号
 */
function renderPackageNumberOnAppliQuanlityChange(value, rowIndex) {
    var itemQty = dgDetail.getValueByKey(rowIndex, "packageWeight");
    if (value && itemQty) {
        dgDetail.setCellValueByKey(rowIndex, "packageNumber", (parseFloat(value) / itemQty).toFixed(2))
    } else if (parseFloat(value) === 0) {
        dgDetail.setCellValueByKey(rowIndex, "packageNumber", 0)
    }
}


// --------------------------与PC端代码差异相关-----------------------------------

/**
 * 移动端获取系统配置，实现PC端React.getSystemConfig相同功能
 */
function getSystemConfigInMobile(moduleAndKey) {
    var result = {}
    ReactAPI.request({
            url: "/inter-api/systemconfig/v1/config/catalog/by/module",
            type: "get",
            async: false,
            data: moduleAndKey
        },
        function (res) {
            if (res && res.data) {
                result = res.data
            }
        })

    return result;
}

function scannningClick() {
    console.log("扫码打开")

    function cbScanQrCode(qrData) {
        alert(qrData)
        let goodCode = qrData && qrData.code
        if (!goodCode) {
            alert("未获取到物料编码")
        }
        //请求对应物料信息
        ReactAPI.request({
            url: "/msService/material/entity/getEntityList",
            type: "get",
            async: false,
            data: {
                moduleName: "BaseSet",
                entityName: "BaseSetMaterial",
                conditions: "valid=1,code=" + goodCode,
            }
        }, function (result) {
            if (result && result.code == 200 && result.data) {
                let materialEntity = result.data;
                // callback(materialEntity)
            }
        })

        let callback = (data) => {
            //将物料信息回填到表体中
            wondows.alert("goodCode:" + goodCode)
            let list = []
            data.forEach(material => {
                var newLine = {
                    good: material,
                    genPrintInfo: true,
                    packageWeight: null
                };
                // 物料对应的包重添加上
                if (typeof (vendorMaterialsMap) !== "undefined" && vendorMaterialsMap[material.id]) {
                    newLine['packageWeight'] = vendorMaterialsMap[material.id]
                }
                list.push(newLine)
            })
            ReactAPI.getComponentAPI("Datagrid").APIs(
                "material_1.0.0_otherInSingle_inSingleEdit__mobile__dg1667525755014"
            ).addLine(list, true);
            refreshRequired();
            refreshReadOnlyByIndex();
            refreshReadonly();
        }
    }

    window.mobilejs.scanQRCode(cbScanQrCode);


}




