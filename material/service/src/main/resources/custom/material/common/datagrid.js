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
            ReactAPI.showMessage("w", ReactAPI.international.getText("SupDatagrid.button.error"));//请选择一条记录进行操作！
        }
    },
    attr_suffix: "_attr",
    /**
     * 设置行属性
     * @param dgName
     * @param rowIndex
     * @param attr
     */
    setRowAttr: function (dgName, rowIndex, attr) {
        //首先获取header名称
        var dataKeys = datagrid.dom.get_data_keys(dgName);
        var dg = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgName);
        var rowsData = dg.getRows(String(rowIndex));
        rowsData.forEach(rowData => {
            dataKeys.forEach(dataKey => {
                var attrKey = dataKey + datagrid.attr_suffix;
                rowData[attrKey] = rowData[attrKey] || {};
                Object.keys(attr).forEach(key => {
                    rowData[attrKey][key] = attr[key];
                })
            });
            dg.setRowData(rowData.rowIndex, rowData);
        });
    },
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
            var dg = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgName);
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
            var dg = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgName);
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
            var dg = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgName);
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
            $("div[keyname='" + dgName + "'] div[data-key='" + key + "'] div[role='cell']").eq(rowIndex).addClass("checked-fail");
        },
        remove_check_fail: function (dgName, key, rowIndex) {
            $("div[keyname='" + dgName + "'] div[data-key='" + key + "'] div[role='cell']").eq(rowIndex).removeClass("checked-fail");
        },
        get_header_text: function (dgName, key) {
            return $("div[keyname='" + dgName + "'] div[data-key='" + key + "'] .header-cell").text();
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
        },
        get_data_keys: function (dgName) {
            return Object.values($("div[keyname='" + dgName + "'] .header-column").parent()).map(val => $(val).attr('data-key')).filter(val => val);
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
            var dg = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgName);
            var dgData = dg.getDatagridData();
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
                    hints.push(ReactAPI.international.getText("material.datagrid.cellRequired", title, rowData.rowIndex + 1));//xxxx 第N行数据不能为空
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
                ReactAPI.showMessage("f", hints.join('<br>'));
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
            var dg = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgName);
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