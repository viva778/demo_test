//-----其他入库-----
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




//------------------------datagrid start----------------------
const datagrid = {
    /**
     * 获取选中行数据
     * @param {*} dg
     * @returns 选中行数据
     */
    getSelectedData: function (dg) {
        var data = dg.getSelecteds();
        if (data && data[0]) {
            return data[0];
        } else {
            ReactAPI.showMessage("w", getIntlValue("SupDatagrid.button.error"));//请选择一条记录进行操作！
        }
    },
    attr_suffix: "_attr",

    //-------------------------------------只读相关-----------------------------START
    readonly: {
        readonly_keys: {},
        rw_keys: {},
        readonly_conditions: {},
        /**
         * 设置表只读
         * @param dg
         * @param keys
         */
        setDgReadonly: function (dgName, readonly) {
            var id = "style" + dgName.replace(".", "_");
            var dom = document.getElementById(id);
            if (!readonly) {
                if (dom) {
                    dom.parentElement.removeChild(dom);
                    //移除字段头颜色
                    datagrid.dom.remove_header_color(dgName);
                }
            } else {
                if (!dom) {
                    var styleEl = document.createElement('style');
                    styleEl.type = 'text/css';
                    styleEl.id = id;
                    //设置只读块透明
                    const disable_cell_transparent = "div[keyname='" + dgName + "'] .disable-cell{background:transparent;}";
                    //设置鼠标不能键入
                    const disable_mouse = "div[keyname='" + dgName + "'] .sup-datagrid-cell{pointer-events:none;}";
                    //字段头标黑
                    datagrid.dom.set_header_color(dgName, undefined, "black");
                    styleEl.innerHTML = disable_cell_transparent + disable_mouse;
                    document.head.appendChild(styleEl);
                }
            }
        },
        /**
         * 设置列只读（不包含后续增行数据
         * @param dg
         * @param keys
         */
        setColReadonly: function (dgName, keys) {
            var dg = ReactAPI.getComponentAPI("Datagrid").APIs(dgName);
            var dgData = dg.getDatagridData();
            var key_array = typeof keys == "string" ? [keys] : keys;
            this.readonly_keys[dgName] = this.readonly_keys[dgName] || new Set();
            this.rw_keys[dgName] = this.rw_keys[dgName] || new Set();
            key_array.forEach(key => {
                this.readonly_keys[dgName].add(key);
                this.rw_keys[dgName].delete(key);
            });
            dgData.forEach(rowData => {
                key_array.forEach(key => {
                    var attr_key = key + datagrid.attr_suffix;
                    rowData[attr_key] = rowData[attr_key] || {};
                    rowData[attr_key].readonly = true;
                });
            });
            dg.setDatagridData(dgData);
        },
        /**
         * 移除列只读（不包含后续新增数据
         * @param dg
         * @param keys
         */
        removeColReadonly: function (dgName, keys) {
            var dg = ReactAPI.getComponentAPI("Datagrid").APIs(dgName);
            var dgData = dg.getDatagridData();
            var key_array = typeof keys == "string" ? [keys] : keys;
            this.readonly_keys[dgName] = this.readonly_keys[dgName] || new Set();
            this.rw_keys[dgName] = this.rw_keys[dgName] || new Set();
            key_array.forEach(key => {
                this.readonly_keys[dgName].delete(key);
                this.rw_keys[dgName].add(key);
            });
            dgData.forEach(rowData => {
                key_array.forEach(key => {
                    var attr_key = key + datagrid.attr_suffix;
                    rowData[attr_key] = rowData[attr_key] || {};
                    if (!rowData[attr_key].conditionCnt) {
                        //排除条件只读
                        rowData[attr_key].readonly = false;
                    }
                });
            });
            dg.setDatagridData(dgData);
        },
        /**
         * 设置只读条件
         * @param dgName DG名称
         * @param key 列KEY
         * @param condition 条件函数 boolean(rowData) =>...
         */
        setReadonlyByCondition: function (dgName, key, condition) {
            var dg = ReactAPI.getComponentAPI("Datagrid").APIs(dgName);
            this.readonly_conditions[dgName] = this.readonly_conditions[dgName] || {};
            this.readonly_conditions[dgName][key] = condition;
            var dgData = dg.getDatagridData();
            var attr_key = key + datagrid.attr_suffix;
            dgData.forEach(rowData => {
                if (condition(rowData)) {
                    rowData[attr_key] = rowData[attr_key] || {};
                    rowData[attr_key].readonly = true;
                    rowData[attr_key].conditionCnt = (rowData[attr_key].conditionCnt || 0) + 1;
                } else {
                    if (rowData[attr_key]) {
                        rowData[attr_key].readonly = false;
                        rowData[attr_key].conditionCnt = (rowData[attr_key].conditionCnt || 1) - 1;
                    }
                }
            });
            dg.setDatagridData(dgData);
        },
        /**
         * 移除只读条件
         * @param dgName DG名称
         * @param key 列名
         */
        removeReadonlyCondition: function (dgName, key) {
            var dg = ReactAPI.getComponentAPI("Datagrid").APIs(dgName);
            if (this.readonly_conditions[dgName]) {
                delete this.readonly_conditions[dgName][key];
                var dgData = dg.getDatagridData();
                var attr_key = key + datagrid.attr_suffix;
                dgData.forEach(rowData => {
                    if (rowData[attr_key] && rowData[attr_key].conditionCnt) {
                        rowData[attr_key].conditionCnt--;
                    }
                });
            }
        },
        appendRowWithReadonlyAttr: function (dgName, rowData) {
            this.readonly_keys[dgName] && this.readonly_keys[dgName].forEach(key => {
                var attr_key = key + datagrid.attr_suffix;
                rowData[attr_key] = rowData[attr_key] || {};
                rowData[attr_key].readonly = true;
            });
            this.rw_keys[dgName] && this.rw_keys[dgName].forEach(key => {
                var attr_key = key + datagrid.attr_suffix;
                rowData[attr_key] = rowData[attr_key] || {};
                rowData[attr_key].readonly = false;
            });
            this.readonly_conditions[dgName] && Object.keys(this.readonly_conditions[dgName]).forEach(key => {
                var condition = this.readonly_conditions[dgName][key];
                var attr_key = key + datagrid.attr_suffix;
                if (condition(rowData)) {
                    rowData[attr_key] = rowData[attr_key] || {};
                    rowData[attr_key].readonly = true;
                    rowData[attr_key].conditionCnt = (rowData[attr_key].conditionCnt || 0) + 1;
                } else {
                    if (rowData[attr_key]) {
                        rowData[attr_key].readonly = false;
                        rowData[attr_key].conditionCnt = (rowData[attr_key].conditionCnt || 1) - 1;
                    }
                }
            });
        }
    },
    //-------------------------------------只读相关-----------------------------END


    //-------------------------------------DOM相关-----------------------------START
    dom: {
        check_fail: function (dgName, key, rowIndex) {
            $("div[data-code='" + dgName + "'] div[data-key='" + key + "'] div[role='cell']").eq(rowIndex).addClass("checked-fail");
        },
        remove_check_fail: function (dgName, key, rowIndex) {
            $("div[data-code='" + dgName + "'] div[data-key='" + key + "'] div[role='cell']").eq(rowIndex).removeClass("checked-fail");
        },
        get_header_text: function (dgName, key) {
            return $("div[data-key='" + key + "'] .label-text ").text();
        },
        /**
         * 设置字段头颜色
         * @param dgName
         * @param key
         * @param color
         */
        set_header_color: function (dgName, key, color) {
            //如果存在key，就只设置一个，否则所有字段头都设置
            var id;
            if (key) {
                id = "style_header" + (dgName + key).replace(".", "_");
            } else {
                id = "style_header" + dgName.replace(".", "_");
            }
            //清除下级设置
            $("[id^='" + id + "']").each((idx, dom) => {
                dom.parentElement.removeChild(dom);
            });

            var styleEl = document.createElement('style');
            styleEl.type = 'text/css';
            styleEl.id = id;
            var color_head;
            //字段颜色标注
            if (key) {
                color_head = "div[keyname='" + dgName + "'] .header-cell[data-key='" + key + "']{color:" + color + ";}";
            } else {
                color_head = "div[keyname='" + dgName + "'] .header-cell{color:" + color + ";}";
            }
            styleEl.innerHTML = color_head;
            document.head.appendChild(styleEl);
        },
        remove_header_color: function (dgName, key) {
            //清除样式
            var id;
            if (key) {
                id = "style_header" + (dgName + key).replace(".", "_");
            } else {
                id = "style_header" + dgName.replace(".", "_");
            }
            var dom = document.getElementById(id);
            if (dom) {
                dom.parentElement.removeChild(dom);
            }
        }
    },
    //-------------------------------------DOM相关-----------------------------END


    //-------------------------------------校验相关-----------------------------START
    validator: {
        validator_checker: {},
        add: function (dgName, key, validator, message_getter) {
            this.validator_checker[dgName] = this.validator_checker[dgName] || [];
            this.validator_checker[dgName].push({
                key: key,
                validator: validator,
                message_getter: message_getter
            });
        },
        remove: function (dgName, key) {
            if (this.validator_checker[dgName]) {
                this.validator_checker[dgName] = this.validator_checker[dgName].filter(ck => ck.key != key);
            }
        },
        /**
         * 触发数据校验
         * @param {*} dg
         * @returns 校验成功/失败
         */
        check: function (dgName) {
            var dg = ReactAPI.getComponentAPI("Datagrid").APIs(dgName);
            var dgData = dg.getDatagridData();
            dgData.forEach((e, i) => {
                e.rowIndex = i
            })
            var hints = [];
            var restoreList = {};
            var failList = {};
            function check_required(key, rowData, requiredCheck) {
                //对值进行校验
                if (!requiredCheck(rowData, key)) {
                    //校验失败
                    //1.设置失败样式
                    failList[key] = failList[key] || [];
                    failList[key].push(rowData.rowIndex);
                    datagrid.dom.check_fail(dgName, key, rowData.rowIndex);
                    //2.追加提示信息
                    var title = datagrid.dom.get_header_text(dgName, key);
                    hints.push(getIntlValue("material.datagrid.cellRequired").format(title, rowData.rowIndex + 1));//xxxx 第N行数据不能为空
                } else {
                    //删除失败样式
                    restoreList[key] = restoreList[key] || [];
                    restoreList[key].push(rowData.rowIndex);
                }
            }
            //校验单元格
            dgData.forEach(rowData => Object.keys(rowData).filter(key => key.endsWith(datagrid.attr_suffix)).forEach(attr_key => {
                //获取之前设置的校验函数
                var requiredCheck = rowData[attr_key].requiredCheck;
                if (requiredCheck) {
                    //去掉后缀_attr得到key
                    var key = attr_key.substr(0, attr_key.length - datagrid.attr_suffix.length);
                    check_required(key, rowData, requiredCheck);
                } else {
                    //删除失败样式
                    restoreList[key] = restoreList[key] || [];
                    restoreList[key].push(rowData.rowIndex);
                }
            }));
            //校验列(必填)
            this.required.required_check_map[dgName] && Object.keys(this.required.required_check_map[dgName]).forEach(key => {
                var requiredCheck = this.required.required_check_map[dgName][key];
                if (requiredCheck) {
                    dgData.forEach(rowData => check_required(key, rowData, requiredCheck));
                }
            });
            //校验列(其他)
            this.validator_checker[dgName] && this.validator_checker[dgName].forEach(checker => {
                var key = checker.key;
                var validator = checker.validator;
                var messager = checker.message_getter;
                dgData.forEach(rowData => {
                    if (!validator(rowData)) {
                        failList[key] = failList[key] || [];
                        failList[key].push(rowData.rowIndex);
                        var title = datagrid.dom.get_header_text(dgName, key);
                        hints.push(messager(rowData.rowIndex + 1, title, rowData));//自定义提示
                    } else {
                        //删除失败样式
                        restoreList[key] = restoreList[key] || [];
                        restoreList[key].push(rowData.rowIndex);

                    }
                })
            })
            Object.keys(restoreList).forEach(key => {
                var idxList = restoreList[key];
                idxList.forEach(idx => {
                    datagrid.dom.remove_check_fail(dgName, key, idx);
                });
            })
            Object.keys(failList).forEach(key => {
                var idxList = failList[key];
                idxList.forEach(idx => {
                    datagrid.dom.check_fail(dgName, key, idx);
                });
            })
            dg.setDatagridData(dgData);
            if (hints.length) {
                ReactAPI.showMessage("f", hints.join(','));
                return false;
            } else {
                return true;
            }
        },
        //-------------------------------------必填校验-----------------------------START
        required: {
            required_check_map: {},
            required_conditions: {},
            /**
             * 设置dg列必填
             * @param {*} dg
             * @param {*} params [{key:?,type:?}]
             */
            setColRequired: function (dgName, params) {
                //设置必填校验
                this.required_check_map[dgName] = this.required_check_map[dgName] || {};
                params.forEach(param => {
                    this.required_check_map[dgName][param.key] = this.fieldType$requiredCheck[param.type];
                    //设置必填样式
                    datagrid.dom.set_header_color(dgName, param.key, "#b30303");
                });
            },

            /**
             * 移除dg列必填
             * @param {*} dg
             * @param {*} params [{key:?}]
             */
            removeColRequired: function (dgName, params) {
                //移除必填校验
                this.required_check_map[dgName] = this.required_check_map[dgName] || {};
                params.forEach(param => {
                    this.required_check_map[dgName][param.key] = undefined;
                    //移除必填样式
                    datagrid.dom.remove_header_color(dgName, param.key);
                });
            },
            /**
             * 设置单元格必填条件
             * @param dgName dg名称
             * @param param {key:?,type:?}
             * @param condition 必填判断条件
             */
            setRequiredByCondition: function (dgName, params, condition) {
                //设置必填校验
                this.required_check_map[dgName] = this.required_check_map[dgName] || {};
                params.forEach(param => {
                    this.required_check_map[dgName][param.key] = (rowData, key) => {
                        if (condition(rowData)) {
                            return this.fieldType$requiredCheck[param.type](rowData, key);
                        } else {
                            return true;
                        }
                    }
                });
            },
            /**
             * 设置dg单元格必填
             * @param {*} dg
             * @param {*} params [{key:?,rowIndex:?,type:?}]
             */
            setCellRequired: function (dg, params) {
                params.forEach(param => {
                    var rowData = dg.getRows('' + param.rowIndex)[0];
                    var attr_key = param.key + datagrid.attr_suffix;
                    rowData[attr_key] = rowData[attr_key] || {};
                    rowData[attr_key].requiredCheck = this.fieldType$requiredCheck[param.type];
                    dg.setRowData(param.rowIndex, rowData);
                });
            },
            /**
             * 移除dg单元格必填
             * @param {*} dg
             * @param {*} params [{key:?,rowIndex:?}]
             */
            removeCellRequired: function (dg, params) {
                params.forEach(param => {
                    var rowData = dg.getRows('' + param.rowIndex)[0];
                    var attr_key = param.key + datagrid.attr_suffix;
                    rowData[attr_key] && delete rowData[attr_key].requiredCheck;
                    dg.setRowData(param.rowIndex, rowData);
                });
            },
            fieldType$requiredCheck: {
                "plain": function (rowData, key) {
                    return rowData[key] || (rowData[key] !== undefined && rowData[key] !== null && rowData[key] !== "");
                },
                "object": function (rowData, key) {
                    var dotIndex = key.indexOf(".");
                    var origin_key;
                    if (dotIndex > 0) {
                        origin_key = key.substr(0, dotIndex);
                    } else {
                        origin_key = key;
                    }
                    return rowData[origin_key] && rowData[origin_key].id;
                },
                "file": function (rowData, key) {
                    return rowData[key] && rowData[key].length;
                }
            },
            clearCellRequired: function (rowData, key) {
                var attr_key = key + datagrid.attr_suffix;
                rowData[attr_key] && delete rowData[attr_key].requiredCheck;
            }
        }
        //-------------------------------------必填校验-----------------------------END
    },
    //-------------------------------------校验相关-----------------------------END
    /**
     * 清除列值
     * @param dg
     * @param keys
     */
    clearColValue: function (dg, keys) {
        var keys_array = typeof keys == "string" ? [keys] : keys;
        var dgData = dg.getDatagridData();
        dgData.forEach(rowData => {
            keys_array.forEach(key => {
                var index = key.indexOf(".");
                var attr = index > 0 ? key.substr(0, index) : key;
                delete rowData[attr];
            });
        });
        dg.setDatagridData(dgData);
    },

    //-------------------------------------绑定事件-----------------------------START
    bindEvent: {
        bind_event_map: {},
        onClick: function (dgName, key, event) {
            this.bind_event_map[dgName] = this.bind_event_map[dgName] || {};
            this.bind_event_map[dgName][key] = event;
            var dg = ReactAPI.getComponentAPI("Datagrid").APIs(dgName);
            var dgData = dg.getDatagridData();
            dgData.forEach(rowData => {
                var attr_key = key + datagrid.attr_suffix;
                rowData[attr_key] = rowData[attr_key] || {};
                rowData[attr_key].bindEvent = rowData[attr_key].bindEvent || {};
                rowData[attr_key].bindEvent.onClick = function () {
                    event(rowData.rowIndex, rowData);
                };
            });
            dg.setDatagridData(dgData);
        },
        appendRowWithEventAttr: function (dgName, rowData) {
            this.bind_event_map[dgName] && Object.keys(this.bind_event_map[dgName]).forEach(key => {
                var attr_key = key + datagrid.attr_suffix;
                rowData[attr_key] = rowData[attr_key] || {};
                rowData[attr_key].bindEvent = rowData[attr_key].bindEvent || {};
                rowData[attr_key].bindEvent.onClick = function () {
                    datagrid.bindEvent.bind_event_map[dgName][key](rowData.rowIndex, rowData);
                };
            })
        }
    },
    //-------------------------------------绑定事件-----------------------------END
    /**
     * 追加rowData属性，包括之前设置的只读和事件
     * @param dgName
     * @param rowData
     */
    appendRowAttr: function (dgName, rowData) {
        this.readonly.appendRowWithReadonlyAttr(dgName, rowData);
        this.bindEvent.appendRowWithEventAttr(dgName, rowData);
    }
}

//------------------------datagrid end----------------------



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

//发起请检
var checkOption;

const dgDetailName = "material_1.0.0_otherInSingle_inSingleEdit__mobile__dg1667525755014";

function dataInit() {
    enablePlace = ReactAPI.getComponentAPI("Boolean").APIs("otherInSingle.ware.storesetState").getValue();
    ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.wmspro"
    }, res => {
        integrateWmsPro = (res.data["material.wmspro"] == true);
    });
    generateTask = (integrateWmsPro && enablePlace);
    rfWarehouse = ReactAPI.getComponentAPI("Reference")
        .APIs("otherInSingle.ware.name");

    rfServiceType = ReactAPI.getComponentAPI("Reference")
        .APIs("otherInSingle.serviceTypeID.serviceTypeExplain");

    rfReasonExplain = ReactAPI.getComponentAPI("Reference")
        .APIs("otherInSingle.inCome.reasonExplain");


    scRedBlue = ReactAPI.getComponentAPI("SystemCode")
        .APIs("otherInSingle.redBlue");


    dgDetail = ReactAPI.getComponentAPI("Datagrid").APIs(
        dgDetailName
    );
    vendorRf = ReactAPI.getComponentAPI("Reference")
        .APIs("otherInSingle.vendor.name");
    cbCheckRequire = ReactAPI.getComponentAPI("Boolean").APIs("otherInSingle.inspectRequired");
    ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.otherInCheckOptions"
    }, res => {
        checkOption = (res.data["material.otherInCheckOptions"]);
    });

    // checkOption = ReactAPI.getSystemConfig({
    //     moduleCode: "material",
    //     key: "material.otherInCheckOptions",
    // })["material.otherInCheckOptions"];
    dataInit = () => { };
}


/**
 * 其他入库单编辑界面onLoad脚本,初始化部分默认值以及根据红蓝字设置功能按钮是否可用
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
 */
function onLoad() {
    dataInit();
    ReactAPI.getComponentAPI("Label").APIs("otherInSingle.inspectRequired").hide().row();
    ReactAPI.getComponentAPI("Label").APIs("otherInSingle.ware.code").hide().row();
    var serviceType = {
        serviceTypeCode: "otherStorageIn",
        id: 1000,
        serviceTypeExplain: getIntlValue(
            "material.custom.OtherWarehousingTransactions"
        ),
    };
    var reasonExplain = {
        id: 1025,
        reasonExplain: getIntlValue(
            "material.custom.ConventionalMaterials"
        ),
    };
    //根据配置项设置是否需要请检
    cbCheckRequire.setValue(checkOption && checkOption != "no");

    //业务类型赋值
    rfServiceType.setValue(serviceType);

    var inCome = rfReasonExplain.getValue();
    if (!inCome[0]) {
        rfReasonExplain.setValue(reasonExplain);
    }

    var redBlueValue = scRedBlue.getValue().value;

    //根据红蓝字设置隐藏和显示功能按钮
    if (redBlueValue == "BaseSet_redBlue/red") {
        ReactAPI.setHeadBtnAttr("redRef", {
            icon: "sup-btn-icon sup-btn-eighteen-dt-op-reference",
            isHide: false,
        });
        $("#btn-goodRef").hide();
    } else {
        ReactAPI.setHeadBtnAttr("redRef", {
            icon: "sup-btn-icon sup-btn-eighteen-dt-op-reference",
            isHide: true,
        });
        $("#btn-goodRef").show();
    }
}

/**
 * 表体初始化时，处理相应样式
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
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
            "material.custom.can.only.beOne").format(
                String(rowIndex)
            );
    });
}

/**
 * 表体请求数据时处理信息
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
 */
function ptRenderOver() {
    refreshReadonly();
    //设置批号只读条件（由于是固定的，所以不需要反复刷新
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "batchText", rowData => {
        var batchType = rowData.good && rowData.good.isBatch && rowData.good.isBatch.id;
        return batchType != "BaseSet_isBatch/batch" && batchType != "BaseSet_isBatch/piece";
    });
    //根据物料是否“质检”,设置表体的"检验结论"字段是否只读
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "checkResult.value", rowData => {
        var isCheck = rowData.good && rowData.good.isCheck;
        return !isCheck;
    });
}

/**
 * 刷新字段必填、只读
 */
function refreshRequired() {
    //如果不生成入库任务，且启用货位，则货位必填
    if (!generateTask && enablePlace) {
        datagrid.validator.required.setColRequired(dgDetailName, [{
            key: "placeSet.name",
            type: "object"
        }]);
    } else {
        datagrid.validator.required.removeColRequired(dgDetailName, [{
            key: "placeSet.name"
        }]);
    }
}

function refreshReadonly() {
    //如果未开启货位，货位只读
    //如果红字，货位只读，批号只读
    var readonly_keys = [];
    var rw_keys = [];
    var isRed = scRedBlue.getValue().value == "BaseSet_redBlue/red";
    if (!enablePlace || isRed) {
        readonly_keys.push("placeSet.name");
    } else {
        rw_keys.push("placeSet.name");
    }
    if (isRed) {
        readonly_keys.push("batchText");
    } else {
        rw_keys.push("batchText");
    }
    if (readonly_keys.length) {
        datagrid.clearColValue(dgDetail, readonly_keys);
        datagrid.readonly.setColReadonly(dgDetailName, readonly_keys);
    }
    if (rw_keys.length) {
        datagrid.readonly.removeColReadonly(dgDetailName, rw_keys);
    }
}





/**
 * 表体“参照”物料按钮事件
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
 */
function goodRefFnc() {
    var url = "/msService/BaseSet/material/material/materialRef?clientType=mobile&multiSelect=true";

    if (vendorId) {
        url += "&cooperateId=" + vendorId + "&customConditionKey=cooperateId";
    }

    ReactAPI.openReference({
        id: "newReference",
        title: getIntlValue(
            "BaseSet.viewtitle.randon1569570764419"
        ), //物料参照
        type: "Other",
        displayfield: "bm1", // 显示字段
        url: url,
        isRef: true, // 是否开启参照
        onOk: function (data) {
            material_callback(data)
        }
    });

    var material_callback = (data) => {
        if (data && data.length) {
            dgDetail.addLine(data.map(material => {
                var newLine = {
                    good: material,
                    genPrintInfo: true,
                    packageWeight: null
                };
                // 物料对应的包重添加上
                if (vendorMaterialsMap[material.id]) {
                    newLine['packageWeight'] = vendorMaterialsMap[material.id]
                }
                //将之前的属性附加上去
                datagrid.appendRowAttr(dgDetailName, newLine);
                return newLine;
            }), true);
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


/**
 * 表体“复制”物料按钮事件
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
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
    dgDetail.setSelecteds("0");
}


var ignoreConfirmFlag = false;

/**
 * 其他入库单编辑界面onsave脚本
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
 *   2.TODO:如果是为了检测是否超储问题,建议优化为后台处理，并只做提醒，不做限制 modify by yaoyao
 */
function onSave() {
    var type = ReactAPI.getOperateType();
    if ("submit" == type) {
        var dgData = dgDetail.getDatagridData();
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




function check_material_limit(warehouse, material, quan) {
    let flag = false;
    $.ajax({
        type: "get",
        url: "/msService/material/socketSet/socketSetInfo/findSocketSet",
        async: false,
        data: {
            wareId: String(warehouse.id),
            goodId: String(material.id),
            direction: "directionReceive",
            num: String(quan),
        },
        success: successCallback,
        // error: errorCallback
    });
    function successCallback(result) {
        if (result.code == 200) {
            let data = result.data;
            if (data.isNo) {
                ReactAPI.openConfirm({
                    //<b>【{0}】</b>中<b>【{1}】</b>的最高库存为<b>{2}</b>，现存量<b>{3}</b>。<br><b>【{4}】</b>入库后库存数量将超过最大库存！
                    message: getIntlValue(
                        "material.custom.SocketSet.confirm"
                    ).format(
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
                        flag = true;
                    },
                    onCancel: () => {
                        ReactAPI.closeConfirm();
                        flag = false;
                    },
                });
                flag = false;
            }
            flag = true;
        } else {
            ReactAPI.showMessage("w", result.message);
            flag = false;
        }
    }
    return flag;

}

/**
 * 其他入库单编辑界面表头红蓝字变化时
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
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
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
 */
function ocgWarehouse(value) {
    var newEnablePlace = value && value[0] && value[0].storesetState;
    if (newEnablePlace != enablePlace) {
        enablePlace = newEnablePlace;
        generateTask = (integrateWmsPro && enablePlace);
        refreshRequired();
        refreshReadonly();
    }
    //清空货位
    datagrid.clearColValue(dgDetail, "placeSet");
}

/**
 * 其他入库单编辑界面表头,参照红字冲销按钮
 * @author  yaoyao
 * @date  2022-05-25
 * @changeLog
 *   1.2022-05-25 代码分离,从原页面中将代码迁移到body.js modify by yaoyao
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
                                "material.custom.CannotBeReferencedRepeatedly").format(
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
                                "material.custom.wareDifferent").format(
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
                dgDetail.setCellValueByKey(rowIndex, "good", data[j].good);
                dgDetail.setCellValueByKey(rowIndex, "redPartID", data[j].id);
                dgDetail.setCellValueByKey(rowIndex, "batchText", data[j].batchText);
                dgDetail.setValuesetCellValueByKeyByKey(
                    rowIndex,
                    "inQuantity",
                    data[j].inQuantity - data[j].redNumber
                );
                dgDetail.setCellValueByKey(
                    rowIndex,
                    "appliQuanlity",
                    data[j].inQuantity - data[j].redNumber
                );
                dgDetail.setCellValueByKey(rowIndex, "placeSet", data[j].placeSet);
                dgDetail.setCellValueByKey(rowIndex, "productionDate", data[j].productionDate);
                dgDetail.setCellsAttr(rowIndex, "good.name", { readonly: true });
                dgDetail.setCellsAttr(rowIndex, "placeSet.name", { readonly: true });
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

function vendorChangeCallback(value) {

    var length = dgDetail.getDatagridData().length;
    if (length > 0) {
        ReactAPI.openConfirm({
            //"清除后将清空表体数据，是否继续？",
            message: getIntlValue("material.custom.randonAfterclearingBody"),
            okText: getIntlValue("attendence.attStaff.isInstitutionYes"),//是
            cancelText: getIntlValue("attendence.attStaff.isInstitutionNo"),//否
            onOk: () => {
                ReactAPI.closeConfirm();
                // 清除表头供应商时清空表体
                dgDetail.deleteLine();
            },
            onCancel: () => {
                ReactAPI.closeConfirm();
                return;
            }
        });
    }

    if (value && value.length > 0) {
        let selectedVendor = value[0];
        vendorId = selectedVendor.id;

        var result = ReactAPI.request({
            url: "/msService/material/entity/getEntityList",
            type: "get",
            async: false,
            data: {
                moduleName: "BaseSet",
                entityName: "BaseSetSupplierMater",
                conditions: "valid=1,cooperator.id=" + vendorId,
                includes: "id,material,packageWeight"
            }
        })

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

function beforeClearVendor(value) {
    var length = dgDetail.getDatagridData().length;
    if (length == 0) {
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
        //"清除后将清空表体数据，是否继续？",
        message: getIntlValue("material.custom.randonAfterclearingBody"),
        okText: getIntlValue("attendence.attStaff.isInstitutionYes"),//是
        cancelText: getIntlValue("attendence.attStaff.isInstitutionNo"),//否
        onOk: () => {
            ReactAPI.closeConfirm();
            // 清除表头供应商
            vendorRf.removeValue();
            // 清除表头供应商时清空表体
            dgDetail.deleteLine();
            // 清除供应商id
            vendorId = null;
            vendorMaterials = [];
            vendorMaterialsMap = {};
            return true;
        },
        onCancel: () => {
            ReactAPI.closeConfirm();
            if (null != id) {
                vendorRf.setValue({
                    code: code,
                    id: id,
                    name: name,
                });
            }
            return false;
        }
    });
    return false;
}

/**
 * 按照件数和单件量计算申请入库数量
 * @param value
 * @param rowIndex 行号
 */
function renderAppliQuanlityOnPackageNumberChange(value, rowIndex) {
    var itemQty = dgDetail.getValueByKey(rowIndex, "packageWeight");
    if (value && itemQty) {
        dgDetail.setValueByKey(rowIndex, "appliQuanlity", value * itemQty)
    } else if (parseFloat(value) === 0) {
        dgDetail.setValueByKey(rowIndex, "appliQuanlity", 0)
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
        dgDetail.setValueByKey(rowIndex, "packageNumber", value / itemQty)
    } else if (parseFloat(value) === 0) {
        dgDetail.setValueByKey(rowIndex, "packageNumber", 0)
    }
}


function vendorChangeCallback(value) {

    if (value && value.length > 0) {
        var length = dgDetail.getDatagridData().length;
        if (length > 0) {
            ReactAPI.openConfirm({
                //"清除后将清空表体数据，是否继续？",
                message: getIntlValue("material.custom.randonAfterclearingBody"),
                okText: getIntlValue("attendence.attStaff.isInstitutionYes"),//是
                cancelText: getIntlValue("attendence.attStaff.isInstitutionNo"),//否
                onOk: () => {
                    ReactAPI.closeConfirm();
                    // 清除表头供应商时清空表体
                    dgDetail.deleteLine();
                },
                onCancel: () => {
                    ReactAPI.closeConfirm();
                    return;
                }
            });
        }

        let selectedVendor = value[0];
        vendorId = selectedVendor.id;
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
        },
            function (res) {
                result = res && res.data;
                return result
            }
        )
        if (result) {
            let suppliers = result;
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

function beforeClearVendor(value) {
    var length = dgDetail.getDatagridData().length;
    if (length == 0) {
        // 清除供应商id
        vendorId = null;
        vendorMaterials = [];
        vendorMaterialsMap = {};
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
        //"清除后将清空表体数据，是否继续？",
        message: getIntlValue("material.custom.randonAfterclearingBody"),
        okText: getIntlValue("attendence.attStaff.isInstitutionYes"),//是
        cancelText: getIntlValue("attendence.attStaff.isInstitutionNo"),//否
        onOk: () => {
            ReactAPI.closeConfirm();
            // 清除供应商id
            vendorId = null;
            vendorMaterials = [];
            vendorMaterialsMap = {};
            // 清除表头供应商
            vendorRf.removeValue();
            // 清除表头供应商时清空表体
            dgDetail.deleteLine();
            return true;
        },
        onCancel: () => {
            ReactAPI.closeConfirm();
            if (null != id) {
                vendorRf.setValue({
                    code: code,
                    id: id,
                    name: name,
                });
            }
            return false;
        }
    });
    return false;
}

/**
 * 按照件数和单件量计算申请入库数量
 * @param value
 * @param rowIndex 行号
 */
function renderAppliQuanlityOnPackageNumberChange(value, rowIndex) {
  debugger
    var itemQty = dgDetail.getValueByKey(rowIndex, "packageWeight");
    if (value && itemQty) {
        dgDetail.setCellValueByKey(rowIndex, "appliQuanlity", value * itemQty)
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
    debugger
    var itemQty = dgDetail.getValueByKey(rowIndex, "packageWeight");
    if (value && itemQty) {
        dgDetail.setCellValueByKey(rowIndex, "packageNumber", value / itemQty)
    } else if (parseFloat(value) === 0) {
        dgDetail.setCellValueByKey(rowIndex, "packageNumber", 0)
    }
}
