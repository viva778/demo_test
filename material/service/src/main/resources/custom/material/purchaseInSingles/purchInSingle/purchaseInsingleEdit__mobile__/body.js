// ----采购入库 移动视图----
const dgDetailName = "material_1.0.0_purchaseInSingles_purchaseInsingleEdit__mobile__dg1665625109086";
// const dgContainerName = "material_1.0.0_purchaseInSingles_containerEdit__mobile__dg1667303999536";
const dgContainerName = "material_1.0.0_purchaseInSingles_purchaseInsingleEdit__mobile__dg1667440759359";
const dgContainerCode = "dg1667440759359"
//引入datagrid.js
//是否生成任务（开启PRO并且开启货位
var dgDetail;//dataGrid
var dgContainer;
var integrateWmsPro;
var enablePlace;
//是否生成任务（开启PRO并且开启货位
var generateTask;
var ctDetails = [];
var cbEnablePlace;
var scRedBlue;
var rfDept;
var rfWarehouse;
var elDgDetail;
var elDgContainer;

var flagCtdRenderOver;
var flagDetailRenderOver;

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
                    ReactAPI.getComponentAPI("Datagrid").APIs(dgName).setRequired(param.key, true)
                    // datagrid.dom.set_header_color(dgName, param.key, "#b30303");
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
                    // datagrid.dom.remove_header_color(dgName, param.key);
                    ReactAPI.getComponentAPI("Datagrid").APIs(dgName).setRequired(param.key, false)
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
function dataInit() {
    dgDetail = ReactAPI.getComponentAPI("Datagrid").APIs(dgDetailName);
    dgContainer = ReactAPI.getComponentAPI("Datagrid").APIs(dgContainerName);
    ReactAPI.getSystemConfig({
        moduleCode: "material",
        key: "material.wmspro"
    }, res => {
        integrateWmsPro = (res.data["material.wmspro"] == true);
    });
    enablePlace = ReactAPI.getComponentAPI("Boolean").APIs("purchInSingle.wareId.storesetState").getValue();
    rfWarehouse = ReactAPI.getComponentAPI("Reference").APIs("purchInSingle.wareId.name");
    scRedBlue = ReactAPI.getComponentAPI("SystemCode").APIs("purchInSingle.redBlue");
    generateTask = (integrateWmsPro && enablePlace);
    elDgDetail = $("div[keyname='" + dgDetailName + "']").parents(".layout-comp-wrap")[0];
    elDgContainer = $("div[keyname='" + dgContainerName + "']").parents(".layout-comp-wrap")[0];
    $("div[data-code='" + dgContainerName + "']").parent().hide()
    dataInit = () => {
    }
}

function onLoad() {
    dataInit();

    //隐藏隐藏字段
    ReactAPI.getComponentAPI("Label").APIs("purchInSingle.purArrivalNo").hide().row();
    ReactAPI.getComponentAPI("Label").APIs("purchInSingle.wareId.storesetState").hide().row();
    ReactAPI.getComponentAPI("Label").APIs("purchInSingle.wareId.id").hide().row();
    ReactAPI.getComponentAPI("Label").APIs("purchInSingle.wareId.code").hide().row();
    ReactAPI.getComponentAPI("Label").APIs("purchInSingle.vendor.code").hide().row();
    ReactAPI.getComponentAPI("Label").APIs("purchInSingle.srcId").hide().row();
    // 赋值业务类型为采购入库事务
    // ReactAPI.getComponentAPI("Reference").APIs("purchInSingle.serviceTypeId.serviceTypeExplain").setValue({
    //     id: 1007,
    //     serviceTypeCode: "purchaseStorageIn",
    //     serviceTypeExplain: getIntlValue("material.custom.PurchaseStockTransaction")
    // });
    // var inCome = ReactAPI.getComponentAPI("Reference").APIs("purchInSingle.inCome.reasonExplain").getValue()[0];
    // if (undefined == inCome) {
    //     ReactAPI.getComponentAPI("Reference").APIs("purchInSingle.inCome.reasonExplain").setValue({ id: 1016, reasonExplain: getIntlValue("material.custom.PurchaseWarehousing") });
    // }
    // var srcId = ReactAPI.getComponentAPI("InputNumber").APIs("purchInSingle.srcId").getValue();
    // // 若采购到货单ID不为空, 表示单据由采购到货单下推生成, 隐藏增行按钮
    // if (srcId) {
    //     $("#btn-addRow").hide();
    // }
    // var value = scRedBlue.getValue().value;

    // if (value == "BaseSet_redBlue/red") {
    //     ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: false });
    // } else {
    //     ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: true });
    // }
}




function vendorCallBack(value) {
    if (value && value.length > 1) {
        setTimeout(function () { ReactAPI.getComponentAPI("Reference").APIs("purchInSingle.vendor.name").setValue(value[1]) })

    }
}

function oacWarehouse() {
    //清空表体货位
    var dgData = dgDetail.getDatagridData();
    dgData.forEach(rowData => delete rowData.placeSet);
    dgDetail.setDatagridData(dgData);
}

function cbWarehouse(value) {
    ReactAPI.request({
        type: "get",
        async: false,
        data: {
            'wareCode': value[0].code
        },
        url: "/msService/material/foreign/foreign/getWareByCode"
    },
        function (res) {
            result = res && res.data;
            return result
        })
    setTimeout(function () { ReactAPI.getComponentAPI("Reference").APIs("purchInSingle.wareId.name").setValue(value[1]) })
    const newPlaceState = result.storesetState;
    if (newPlaceState != enablePlace) {
        enablePlace = newPlaceState;
        generateTask = (integrateWmsPro && enablePlace);
        refreshRequired();
        //设置货位只读
        if (enablePlace) {
            datagrid.readonly.setColReadonly(dgDetailName, "placeSet.name");
        } else {
            datagrid.readonly.removeColReadonly(dgDetailName, "placeSet.name");
        }
    }
    //清空货位
    datagrid.clearColValue(dgDetail, "placeSet");
}

function oacStaff() {
    rfDept.removeValue();
}

function cbStaff(value) {
    var dept = value[0].department;
    rfDept.setValue(dept);
}



function ocgRedBlue(value) {
    var before = scRedBlue.getValue().value;

    if (value == "BaseSet_redBlue/red") {
        if (dgDetail.getDatagridData().length != 0) {
            ReactAPI.openConfirm({
                message: getIntlValue("material.custom.clearTheTableBodyAtTheSameTime"),//切换红蓝字会同时清空表体！
                okText: getIntlValue("ec.common.confirm"),//确定
                cancelText: getIntlValue("foundation.signature.cancel"),//取消
                onOk: () => {
                    ReactAPI.closeConfirm();
                    // 清空表体
                    dgDetail.deleteLine();
                    ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: false });
                    $("#btn-addRow").hide();
                    return false;
                },
                onCancel: () => {
                    scRedBlue.setValue(before);
                    ReactAPI.closeConfirm();
                    return false;
                }
            });
        } else {
            $("#btn-addRow").hide();
            ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: false });
        }

    } else {
        if (dgDetail.getDatagridData().length != 0) {
            ReactAPI.openConfirm({
                message: getIntlValue("material.custom.clearTheTableBodyAtTheSameTime"),//切换红蓝字会同时清空表体！
                okText: getIntlValue("ec.common.confirm"),//确定
                cancelText: getIntlValue("foundation.signature.cancel"),//取消
                onOk: () => {
                    ReactAPI.closeConfirm();
                    // 清空表体
                    dgDetail.deleteLine();
                    $("#btn-addRow").show();
                    ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: true });
                    return false;
                },
                onCancel: () => {
                    scRedBlue.setValue(before);
                    ReactAPI.closeConfirm();
                    return false;
                }
            });
        } else {
            $("#btn-addRow").show();
            ReactAPI.setHeadBtnAttr('redRef', { icon: 'sup-btn-icon sup-btn-eighteen-dt-op-reference', isHide: true });
        }
    }
}




// ---------------------- 初始化 end ----------------------


/**
 * 复制行
 */
function ptBtnCopy() {
    // 选中行对象
    var selRows = dgDetail.getSelecteds();
    if (selRows.length === 0) {
        // 请选择一条记录进行操作
        var errMessage = getIntlValue("SupDatagrid.button.error");
        ReactAPI.showMessage('w', errMessage);
        return;
    }
    var newRows = [];
    selRows.forEach(rowData => {
        // var copy = $.extend(true, {}, rowData);
        var copy = rowData;
        delete copy.id;
        delete copy.version;
        delete copy.sort;
        delete copy.currClickColKey;
        delete copy.edited;
        delete copy.key;
        delete copy.rowIndex;
        copy.uuid = uuid();
        //将之前的只读属性附加上去
        datagrid.appendRowAttr(dgDetailName, copy);
        newRows.push(copy);
    });
    dgDetail.addLine(newRows);
    dgDetail.setSelecteds("0");
}


function ptInit() {
    dataInit();
    //设置图标
    // dgDetail.setBtnImg("copy", "sup-btn-own-fzh");
    // dgDetail.setBtnImg("weighingDiff", "sup-btn-insert");
    // dgDetail.setBtnImg("container", "sup-btn-own-shouyang");

    refreshRequired();
    //设置校验
    datagrid.validator.add(dgDetailName, "applyQuantity", rowData => {
        //物品启用按件管理，入库数量只能为1件
        var batchType = rowData.good && rowData.good.isBatch && rowData.good.isBatch.id;
        if (batchType == "BaseSet_isBatch/piece") {
            return rowData.applyQuantity == 1;
        } else {
            return true;
        }
    }, rowIndex => {
        return getIntlValue("material.custom.can.only.beOne")(
        ).format(String(rowIndex)
        )
    });
}


/**
 * 刷新字段必填、只读
 */
function refreshRequired() {
    //如果不生成入库任务，且启用货位，则货位必填
    //如果未开启货位，货位只读

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

var deleteContainerIds = [];

function ptBtnDelete() {
    var selRows = dgDetail.getSelecteds();
    if (!selRows[0]) {
        ReactAPI.showMessage('w', getIntlValue("Reference.confirm.tip.message"));
        return false;
    }
    var srcId$cnt = {};
    var map = new Map();
    dgDetail.getDatagridData().forEach((rowData, index) => {
        map.set(rowData.uuid, index);
        if (rowData.srcPartId) {
            var value = srcId$cnt[rowData.srcPartId];
            srcId$cnt[rowData.srcPartId] = (value || 0) + 1;
        }
    });
    var deleteRows = [];
    var errorRows = [];
    for (const rowData of selRows) {
        if (rowData.srcPartId) {
            if (srcId$cnt[rowData.srcPartId] > 1) {
                deleteRows.push(rowData);
                srcId$cnt[rowData.srcPartId]--;
            } else {
                //仅存的一条上游单据下推的数据不能删除
                errorRows.push(rowData.rowIndex + 1);
            }
        } else {
            deleteRows.push(rowData);
        }
    }
    if (errorRows.length) {
        //上游单据，不能删除
        ReactAPI.showMessage('w', getIntlValue("material.custom.randon1581399401875").format(errorRows.join(",")));
    } else if (deleteRows.length) {
        //删除对应uuid的数据
        var uuidList = deleteRows.map(val => val.uuid);
        var deleteContainerList = ctDetails.filter(ctDetail => uuidList.includes(ctDetail.purchaseInDetailUuid));
        ctDetails = ctDetails.filter(ctDetail => !uuidList.includes(ctDetail.purchaseInDetailUuid));
        dgDetail.deleteLine(deleteRows.map(rowData => map.get(rowData.uuid)).join(","));
        deleteContainerList.map(val => val.id).filter(val => val).forEach(id => deleteContainerIds.push(id));
    }
}

function ptRenderOver() {
    refreshReadonly();
    //设置批号只读条件（由于是固定的，所以不需要反复刷新
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "batch", rowData => {
        var batchType = rowData.good && rowData.good.isBatch && rowData.good.isBatch.id;
        return batchType != "BaseSet_isBatch/batch" && batchType != "BaseSet_isBatch/piece";
    });
    //如果存在采购订单，则将编号设为只读
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "purOrderNo", rowData => {
        return rowData.purchaseId;
    });
    //设置点击事件
    datagrid.bindEvent.onClick(dgDetailName, "purOrderNo", (rowIndex, rowData) => {
        onclickpurch(rowData);
    });
    //如果uuid为空，设置uuid
    var dgData = dgDetail.getDatagridData();
    var generated;
    dgData.forEach(rowData => {
        if (!rowData.uuid) {
            rowData.uuid = uuid();
            generated = true;
        }
    });
    if (generated) {
        dgDetail.setDatagridData(dgData);
    }
    flagDetailRenderOver = true;
    if (flagCtdRenderOver) {
        ptAllRenderOver();
    }
}

function ptAllRenderOver() {
    //入库数量只读条件，在存在容器明细时只读
    datagrid.readonly.setReadonlyByCondition(dgDetailName, "applyQuantity", rowData => {
        return ctDetails.filter(val => val.purchaseInDetailUuid == rowData.uuid).length;
    });
}


function onclickpurch() {
    let rowData = dgDetail.getSelecteds()[0];
    var srcID = rowData.purchaseId;
    if (srcID) {
        var tableInfo = '';
        var tableId = '';
        ReactAPI.request({
            url: "/msService/material/purchaseInSingles/purchInSingle/findSrcTableInfoId",
            type: "get",
            data: {
                "srcID": srcID,
                "tableType": "MaterialPurchasePart"
            },
            async: false

        },
            function (res) {
                result = res && res.data;
                return result
            }
        );
        if (result) {
            {
                tableInfo = result.tableInfoId;
                tableId = result.tableId;
            }
        }
    }

    if (tableInfo) {
        // 采购入库单实体的entityCode
        var entityCode = "material_1.0.0_purchaseInfos";
        // 采购入库单实体的查看视图URL
        var url = "/msService/material/purchaseInfos/purchaseInfo/purchaseView";
        // 当前页面的URL
        var currentPageURL = window.location.href;
        // 菜单操作编码
        var operateCode = "material_1.0.0_purchaseInfos_purchaseList_self";
        // var pc = pcMap[operateCode];
        url += "?clientType=mobile&tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + tableId;
        window.open(url);
    }
}

/**
 * 获取采购到货单的tableInfoId并打开查看视图
 */
function getPurArrial() {
    var tableInfo = '';
    var srcID = ReactAPI.getComponentAPI("InputNumber").APIs("purchInSingle.srcId").getValue();
    if (srcID) {
        ReactAPI.request({
            type: "get",
            data: {
                "srcID": srcID,
                "tableType": "MaterialPurArrivalInfo"
            },
            url: "/msService/material/purchaseInSingles/purchInSingle/findSrcTableInfoId",
            async: false
        },
            function (msg) {
                if (msg != null && msg.data && msg.data.tableInfoId) {
                    tableInfo = msg.data.tableInfoId;
                }
            }
        );
    }
    if (tableInfo) {
        // 采购到货单实体的entityCode
        var entityCode = "material_1.0.0_purArrivalInfos";
        // 采购到货单实体的查看视图URL
        var url = "/msService/material/purArrivalInfos/purArrivalInfo/purArrivalInfoView";
        // 当前页面的URL
        var currentPageURL = window.location.href;
        // 菜单操作编码
        // var operateCode = "material_1.0.0_purArrivalInfos_purArrivalInfoList_self";
        // var pcMap = ReactAPI.getPowerCode(operateCode);
        // var pc = pcMap[operateCode];
        url += "?clientType=mobile&tableInfoId=" + tableInfo + "&entityCode=" + entityCode + "&id=" + srcID;
        window.open(url);
    }
}



function refreshReadonly() {
    //如果未开启货位，货位只读
    var readonly_keys = [];
    var rw_keys = [];
    var isRed = scRedBlue.getValue().value == "BaseSet_redBlue/red";
    if (!enablePlace || isRed) {
        readonly_keys.push("placeSet.name");
    } else {
        rw_keys.push("placeSet.name");
    }
    if (readonly_keys.length) {
        datagrid.clearColValue(dgDetail, readonly_keys);
        datagrid.readonly.setColReadonly(dgDetailName, readonly_keys);
    }
    if (rw_keys.length) {
        datagrid.readonly.removeColReadonly(dgDetailName, rw_keys);
    }
}

var ignoreConfirmFlag = false;

function onSave() {
    try {
        var type = ReactAPI.getOperateType();
        var dgData = dgDetail.getDatagridData();
        if (!dgData.length) {
            ReactAPI.showMessage(
                "w",
                getIntlValue("material.custom.randon1573634425210")
            ); //	表体数据不能为空！
            return false;
        }
        let checkFlag = false
        dgData.forEach(e => {
            if (e.arrivalQuantity < e.applyQuantity || e.arrivalQuantity <= 0 || e.applyQuantity <= 0) {
                checkFlag = true
            }
        })
        if (checkFlag) {
            ReactAPI.showMessage(
                "w",
                "申请入库数量不能大于到货数量且必须大于0!"
            );
            return false;
        }
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
                // ReactAPI.showMessage(
                //     "w",
                //     "数据有误"
                // ); //	数据有误
                return false;
            }

            //校验超储,归并同物料
            if (!ignoreConfirmFlag) {
                var checked_idx = new Set();
                for (var i = 0; i < dgData.length; i++) {
                    if (!checked_idx.has(i)) {
                        var material = dgData[i].good;
                        //统计物料数量
                        var quan = dgData[i].applyQuantity;
                        for (var j = i + 1; j < dgData.length; j++) {
                            if (!checked_idx.has(j)) {
                                if (material.id == dgData[j].good.id) {
                                    checked_idx.add(j);
                                    quan += dgData[k].applyQuantity;
                                }
                            }
                        }
                        //统计完成，进行校验
                        if (!check_material_limit(warehouse, material, quan)) {
                            getIntlValue("2")
                            return false;
                        }
                    }
                }
            }
        }
        //保存容器数据
        if (ctDetails.length) {
            var sort = 1;
            ReactAPI.setSaveData({
                dgList: {
                    [dgContainerCode]: JSON.stringify(ctDetails.map(ctd => new Object({
                        id: ctd.id,
                        container: {
                            id: ctd.container.id
                        },
                        material: {
                            id: ctd.material.id
                        },
                        purchaseInDetailUuid: ctd.purchaseInDetailUuid,
                        quantity: ctd.quantity,
                        sort: sort++
                    })))
                }
            });
        }
        //增加删除数据id
        if (deleteContainerIds.length) {
            var originDeleteIds = ReactAPI.getSaveData().dgDeletedIds[dgContainerCode];
            var deleteIds;
            if (originDeleteIds) {
                deleteIds = originDeleteIds + "," + deleteContainerIds.join(",");
            } else {
                deleteIds = deleteContainerIds.join(",");
            }
            ReactAPI.setSaveData({
                dgDeletedIds: {
                    [dgContainerCode]: deleteIds
                }
            })
        }
        return true;
    } catch (e) {
        console.log(e);
    }
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
// ------------------采购入库-移动视图-绑定托盘--------------------
var ctDetails = [];
function ptBtnContainerBind() {
    var selDetails = dgDetail.getSelecteds();
    if (selDetails.length != 1) {
        //请选择一条入库单明细！
        ReactAPI.showMessage('w', getIntlValue("material.inbound.select_detail_before_do_some_things"));
        return false;
    }
    ctDetails = dgContainer.getDatagridData()
    ReactAPI.createDialog({
        id: "newDialog",
        title: getIntlValue("material.viewtitle.randon1663744548655"), //托盘
        url: "/msService/material/purchaseInSingles/purchInPart/containerEdit?clientType=mobile",
        isRef: true,
        initData: {
            purchInPart: {
                good: selDetails[0].good,
                batch: selDetails[0].batch,
                uuid: selDetails[0].uuid,
                purArrivalInfo:selDetails[0].srcPartId,
                dataList: ctDetails.filter(e => e.purchaseInDetailUuid == selDetails[0].uuid)
            }
        }, // 页面初始值
        footer: [
            {
                name: getIntlValue("Button.text.save"),
                onClick: function (event) {
                    event.ReactAPI.submitFormData("save", function (res) {
                        debugger
                        var data = event.ReactAPI.getComponentAPI("Datagrid").APIs('material_1.0.0_purchaseInSingles_containerEdit__mobile__dg1667303999536').getDatagridData();
                        containerEditCallback(data)
                        ReactAPI.destroyDialog("newDialog");
                    });
                }
            },
            {
                name: getIntlValue("Button.text.cancel"),
                onClick: function (event) {
                    ReactAPI.destroyDialog("newDialog");
                }
            }
        ]
    });
}

//托盘确定回调
function containerEditCallback(res) {
    // 当isRef为true时，res返回的是保存值，保存提交自己调接口
    // 校验会中断
    ctDetails = []
    //转为数字
    res.forEach(rowData => {
        rowData.quantity = Number(rowData.quantity)
        ctDetails.push(rowData);
    })
    dgContainer.setDatagridData(ctDetails)
    const data = res
    var selDetail = dgDetail.getSelecteds()[0];
    if (data && data.length > 0) {
        var totalQuan = Number(data.map(val => val.quantity).reduce((v1, v2) => Number(v1) + Number(v2)));
        selDetail["applyQuantity_attr"] = {
            readonly: true
        }
        selDetail["applyQuantity"] = totalQuan
        //设置申请数量只读，增加新增数量
        dgDetail.setRowData(selDetail.rowIndex, selDetail);
    } else {
        selDetail["applyQuantity_attr"] = {
            readonly: false
        }
        dgDetail.setRowData(selDetail.rowIndex, selDetail);
    }

}

//进入视图更新缓存数据
// function ptContainerRenderOver() {
//     //首次进入缓存数据
//     var dgData = dgContainer.getDatagridData();
//     if (dgData.length) {
//         dgData.forEach(rowData => {
//             ctDetails.push(rowData);
//         });
//         dgContainer.setDatagridData([]);
//     }
//     flagCtdRenderOver = true;
//     if (flagDetailRenderOver) {
//         ptAllRenderOver();
//     }
// }


function ptBtnWeighingDiff() {
    //计算磅差
    var selRow = dgDetail.getSelecteds()[0];
    if (!selRow) {
        //请选择一条记录进行操作
        ReactAPI.showMessage('w', getIntlValue("SupDatagrid.button.error"));
        return;
    }
    if (typeof selRow.weighing1 != "number") {
        //一次过磅数据不存在！
        ReactAPI.showMessage('w', getIntlValue("material.container.weighing1_data_not_exist"));
        return;
    }
    if (typeof selRow.weighing2 != "number") {
        //二次过磅数据不存在！
        ReactAPI.showMessage('w', getIntlValue("material.container.weighing2_data_not_exist"));
        return;
    }
    selRow.applyQuantity = selRow.weighing1 - selRow.weighing2
    //将差值回填到申请数量
    dgDetail.setRowData(selRow.rowIndex,
        selRow
    );
}




