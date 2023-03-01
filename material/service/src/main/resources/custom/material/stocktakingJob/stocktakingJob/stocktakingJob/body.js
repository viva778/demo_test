//-----盘点任务增强-----
//引入datagrid.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");

const dgListName = "material_1.0.0_stocktakingJob_stocktakingJobdg1662085520156";
const dgDetailName = "material_1.0.0_stocktakingJob_stocktakingJobdg1662085567350";
const dgStockRecordName = "material_1.0.0_stocktakingJob_stocktakingJobdg1662085580011";

var dgList;
var dgDetail;
var dgStockRecord;
var vPlaceEnable;
var vFullOrSpot;
var vShowOrNot;
var vMaterialIds;
var vPlaceModelIds = [];
var isView;
var jobId;
var vListSelectRowIdx = -1;
var vDetailSelectRowIdxs = [];
var searchPanel;

var disableSaveLoadingAutoClose = false;

function dataInit() {
    dgList = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgListName);
    dgDetail = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgDetailName);
    dgStockRecord = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgStockRecordName);
    searchPanel = ReactAPI.getComponentAPI().SearchPanel.APIs("material_1.0.0_stocktakingJob_StocktakingJob_column-2_sp");
    dataInit = () => {
    }
}


//--------------------------------------------------------------------------------------ptinit--------------------------------------------------------------------------------------BEGIN
function ptListInit() {
    dataInit();
    //绑定点击事件
    dgList.setClickEvt(function () {
        listOnClick();
    });
}


function ptDetailInit() {
    dataInit();
    //设置图标
    dgDetail.setBtnImg("submit", "sup-btn-own-tj");
    //首次进入隐藏按钮
    dgDetail.setBtnHidden(["btn-submit"]);
    //绑定点击事件
    dgDetail.setClickEvt(function () {
        detailOnClick();
    });
    dgDetail.setCheckBoxClickEvt(function () {
        detailOnClick();
    });
}


function ptSRInit() {
    dataInit();
    //首次进入隐藏按钮
    dgStockRecord.setBtnHidden(["btn-add", "btn-delete", "btn-save"]);
    //设置图标
    dgStockRecord.setBtnImg("save", "sup-btn-own-bc");
    //------------------------------------设置校验------------------------------------
    //第{0}行，按件管理物品数量有误！
    datagrid.validator.add(dgStockRecordName, "quantityByCount", rowData => {
        var batchType = rowData.material && rowData.material.isBatch && rowData.material.isBatch.id;
        if (batchType == "BaseSet_isBatch/piece") {
            return !rowData.quantityByCount || rowData.quantityByCount == 1;
        } else {
            return true;
        }
    }, rowIndex => {
        return ReactAPI.international.getText(
            "material.custom.can.only.beOne",
            String(rowIndex)
        );
    });
    //第{0}行，盘点数量不能小于0
    datagrid.validator.add(dgStockRecordName, "quantityByCount", rowData => {
        if (typeof rowData.quantityByCount == "number" && rowData.quantityByCount < 0) {
            return false;
        } else {
            return true;
        }
    }, (rowIndex, title) => {
        //第{0}行，{1}不能小于{2}
        return ReactAPI.international.getText(
            "material.validator.cannot_be_less_than",
            String(rowIndex), title, "0"
        );
    });
    //设置条件必填 开启批号的物料批次信息必填
    datagrid.validator.required.setRequiredByCondition(dgStockRecordName, [{
        key: "batchInfo.originBatchNum",
        type: "object"
    }], rowData => {
        return rowData.material && rowData.material.isBatch && rowData.material.isBatch.id != "BaseSet_isBatch/nobatch";
    });
}

//--------------------------------------------------------------------------------------ptinit--------------------------------------------------------------------------------------END


//--------------------------------------------------------------------------------------renderover--------------------------------------------------------------------------------------BEGIN
function ptSRRenderOver() {
    try {
        //设置明盘暗盘隐藏
        if (vShowOrNot == 'material_stockTakingWay/implicitWay') {
            dgStockRecord.setColumnsHideOrShow("quantityOnBook", false);
            dgStockRecord.setColumnsHideOrShow("quantityOffset", false);
        } else {
            dgStockRecord.setColumnsHideOrShow("quantityOnBook", true);
            dgStockRecord.setColumnsHideOrShow("quantityOffset", true);
        }
        //设置只读条件
        datagrid.readonly.setReadonlyByCondition(dgStockRecordName, "batchInfo.originBatchNum", rowData => {
            //批号 账面量>0时只读
            return rowData.quantityOnBook > 0;
        });
        datagrid.readonly.setReadonlyByCondition(dgStockRecordName, "material.code", rowData => {
            //物料 账面量>0时或批号存在时只读
            return rowData.quantityOnBook > 0 || rowData.batchInfo;
        });
        var dgData = dgStockRecord.getDatagridData();
        dgData.forEach(val => {
            //计算盈亏量
            if (typeof val.quantityByCount == "number") {
                val.quantityOffset = val.quantityByCount - val.quantityOnBook;
                //设置颜色
                var color = getQuantityColor(val.quantityOnBook, val.quantityByCount);
                val.quantityOffset_attr = {
                    style: {
                        color: color
                    }
                }
            }
        });
        dgStockRecord.setDatagridData(dgData);
        if (checkStockRecordWhenRenderOVer) {
            checkDgStockRecord();
        }
    } finally {
        checkStockRecordWhenRenderOVer = false;
    }
}

function ptDetailRenderOver() {
    vPlaceModelIds = [];
}

function ptListRenderOver() {
    //初始化变量
    vListSelectRowIdx = -1;
    vDetailSelectRowIdxs = [];
    //取消必填
    datagrid.validator.required.removeColRequired(dgStockRecordName, [{
        key: "quantityByCount",
        type: "plain"
    }]);
    datagrid.validator.required.removeColRequired(dgStockRecordName, [{
        key: "material.code",
        type: "object"
    }]);
    //清空表体
    dgStockRecord.deleteLine();
    dgDetail.deleteLine();
    vPlaceModelIds = [];
    clearChanged();
    //隐藏图标
    dgStockRecord.setBtnHidden(["btn-add", "btn-delete", "btn-save"]);
    dgDetail.setBtnHidden(["btn-submit"]);
}

//--------------------------------------------------------------------------------------renderover--------------------------------------------------------------------------------------END


//--------------------------------------------------------------------------------------onclick--------------------------------------------------------------------------------------BEGIN
function listOnClick() {
    //确认点击项是否发生变化
    setTimeout(() => {
        var selRow = dgList.getSelecteds()[0];
        if (selRow.rowIndex != vListSelectRowIdx) {
            disableSaveLoadingAutoClose = false;
            confirmChanged().then(() => {
                //刷新全局变量
                vListSelectRowIdx = selRow.rowIndex;
                vPlaceEnable = selRow.stocktaking.warehouse.storesetState;
                vFullOrSpot = selRow.stocktaking.fullOrSpot.id;
                vShowOrNot = selRow.stocktaking.showStockOrNot.id;
                var sMaterialIds = selRow.material_1_0_0_stocktakingJob_stocktakingJobdg1662085520156_LISTPT_ASSO_b32189ae_4293_486d_8ed9_2040e3bea8d3;
                vMaterialIds = sMaterialIds ? sMaterialIds.split(",").map(val => parseInt(val)) : [];
                isView = (selRow.jobState.id != "material_stocktakingOperateState/edit");
                jobId = selRow.id;
                //刷新明细表体
                dgDetail.setSelecteds('-1');
                vPlaceModelIds = [];
                dgDetail.refreshDataByRequst({
                    type: "POST",
                    url: "/msService/material/stocktakingJob/stocktakingJob/data-dg1662085567350?datagridCode=material_1.0.0_stocktakingJob_stocktakingJobdg1662085567350&id=" + jobId,
                    param: {
                        customCondition: {
                            staffId: ReactAPI.getUserInfo().staff.id
                        }
                    },
                });

                //根据是否已完成盘点，设置下方表体按钮显示
                if (isView) {
                    dgStockRecord.setBtnHidden(["btn-add", "btn-delete", "btn-save"]);
                    dgDetail.setBtnHidden(["btn-submit"]);
                    //设置dg只读
                    datagrid.readonly.setDgReadonly(dgStockRecordName, true);
                } else {
                    dgStockRecord.setBtnHidden([]);
                    dgDetail.setBtnHidden([]);
                    //设置dg不只读
                    datagrid.readonly.setDgReadonly(dgStockRecordName, false);
                }

                //根据全盘抽盘更新现存量盘点必填
                if (!isView && vFullOrSpot == "material_invRange/allRange") {
                    //设置必填
                    datagrid.validator.required.setColRequired(dgStockRecordName, [{
                        key: "quantityByCount",
                    }]);
                    datagrid.validator.required.setColRequired(dgStockRecordName, [{
                        key: "material.code",
                        type: "object"
                    }]);
                } else {
                    //取消必填
                    datagrid.validator.required.removeColRequired(dgStockRecordName, [{
                        key: "quantityByCount",
                    }]);
                    datagrid.validator.required.removeColRequired(dgStockRecordName, [{
                        key: "material.code",
                        type: "object"
                    }]);
                }
                //刷新记录表体
                refreshDgStockRecord();
            }).error(() => {
                //取消选择
                dgList.setSelecteds(String(vListSelectRowIdx));
            });
        }
    })
}

//分配任务点击，刷新右侧
function detailOnClick() {
    if (vPlaceEnable) {
        setTimeout(() => {
            //判断本次选择和上次选择是否一致，不一致则刷新
            var flush;
            var refresh = false;
            var selRows = dgDetail.getSelecteds();
            if (vPlaceModelIds.length != selRows.length) {
                //先根据长度判断
                flush = true;
                var total = dgDetail.getDatagridData().length;
                //0->全部或 全部->0不需要刷新
                if (total == selRows.length && vPlaceModelIds.length == 0) {
                    refresh = false;
                } else if (selRows.length == 0 && vPlaceModelIds.length == total) {
                    refresh = false;
                } else {
                    refresh = true;
                }
            } else {
                //再根据全包含判断
                flush = false;
                selRows.map(val => val.distribution.target.id).forEach(id => {
                    if (!flush) {
                        if (!vPlaceModelIds.includes(id)) {
                            refresh = true;
                            flush = true;
                        }
                    }
                })
            }
            if (flush) {
                disableSaveLoadingAutoClose = false;
                confirmChanged().then(() => {
                    vPlaceModelIds = selRows.map(val => val.distribution.target.id);
                    if (enableStockRecordCheck) {
                        enableStockRecordCheck = false;
                        checkStockRecord(refresh);
                    }
                    if (refresh) {
                        refreshDgStockRecord();
                    }
                    vDetailSelectRowIdxs = selRows.map(rowData => rowData.rowIndex);
                }).error(() => {
                    //取消选择
                    dgDetail.setSelecteds(vDetailSelectRowIdxs.join(",") || "-1");
                });
            } else {
                if (enableStockRecordCheck) {
                    enableStockRecordCheck = false;
                    checkStockRecord(false);
                }
            }
        });
    }
}

//--------------------------------------------------------------------------------------onclick--------------------------------------------------------------------------------------END
var enableStockRecordCheck;

function setupDetailSelectWithCheck() {
    enableStockRecordCheck = true;
    detailOnClick();
}

var checkStockRecordWhenRenderOVer = false;

function checkStockRecord(refresh) {
    if (refresh) {
        checkStockRecordWhenRenderOVer = true;
    } else {
        checkDgStockRecord();
    }
}

function refreshDgStockRecord() {
    dgStockRecord.refreshDataByRequst({
        type: "POST",
        url: "/msService/material/stocktakingJob/stocktakingJob/data-dg1662085580011?datagridCode=material_1.0.0_stocktakingJob_stocktakingJobdg1662085580011&id=" + jobId + "&isView=" + isView,
        param: {
            customCondition: {
                placeModelIds: vPlaceModelIds.join(","),
                // placeModelIds:null,
                staffId: ReactAPI.getUserInfo().staff.id
            }
        }
    });
}

var changed_rows = new Set();
var deleteIds = [];

function clearChanged() {
    changed_rows.clear();
    deleteIds = [];
}

function isChanged() {
    return changed_rows.size || deleteIds.length;
}

function confirmChanged() {
    var promise = {
        then: (callback) => {
            promise._then = callback;
            return promise;
        },
        error: (callback) => {
            promise._error = callback;
            return promise;
        },
        _then: () => {
        },
        _error: () => {
        }
    };
    if (!isChanged()) {
        setTimeout(() => {
            promise._then();
        });
    } else {
        ReactAPI.openConfirm({
            message: ReactAPI.international.getText("material.stocktaking.content_changed"),
            onOk: () => {
                ReactAPI.closeConfirm();
                saveChanged(promise._then, promise._error);
            },
            onCancel: () => {
                //关闭提示框
                ReactAPI.closeConfirm();
                promise._then();
                clearChanged();
            }
        });
    }
    return promise;
}

function saveChanged(onSuccess, onError) {
    //触发保存接口
    var savedData = dgStockRecord.getDatagridData().filter(rowData => changed_rows.has(rowData.key)).map(rowData => new Object({
        id: rowData.id,
        stocktakingJob: {
            id: jobId
        },
        material: rowData.material,
        batchInfo: rowData.batchInfo,
        place: rowData.place,
        quantityByCount: rowData.quantityByCount,
        quantityOnBook: rowData.quantityOnBook,
        quantityOffset: rowData.quantityOffset,
        valid: 1,
        stockKey: getStockKey(rowData)
    }));
    deleteIds.forEach(id => {
        savedData.push({
            id: id,
            valid: 0
        });
    })
    ReactAPI.openLoading();
    ReactAPI.request({
        url: "/msService/material/stocktaking/saveStockRecord",
        type: "post",
        async: true,
        data: savedData
    }, res => {
        if (res.code != 200) {
            ReactAPI.showMessage('f', res.message);
            while ($("div[id='com_self-loading']")[0]) {
                ReactAPI.closeLoading();
            }
            if (typeof onError == "function") {
                onError();
            }
        } else {
            if (typeof onSuccess == "function") {
                onSuccess();
            }
            if (!disableSaveLoadingAutoClose) {
                ReactAPI.openLoading(res.message, "2", true);
                setTimeout(() => {
                    while ($("div[id='com_self-loading']")[0]) {
                        ReactAPI.closeLoading();
                    }
                }, 500);
            }
        }
    });
    clearChanged();
}


//--------------------------------------------------------------------------------------btn--------------------------------------------------------------------------------------BEGIN
function ptBtnDetailSubmit() {

    function afterConfirm() {
        //确认后执行
        var refreshOnFail;

        function afterSave() {
            //保存后执行
            if (!vPlaceEnable) {
                //未开启货位就先校验
                if (!checkDgStockRecord()) {
                    while ($("div[id='com_self-loading']")[0]) {
                        ReactAPI.closeLoading();
                    }
                    if (refreshOnFail) {
                        refreshDgStockRecord();
                    }
                    return false;
                }
            }
            ReactAPI.openLoading("提交中...");
            //查询当前人未完成盘点的货位，如果完成就提交
            ReactAPI.request({
                url: "/msService/material/stocktaking/getUndoneTargetCodesAndSubmitIfDone",
                type: "get",
                async: true,
                data: {
                    stocktakingJobId: jobId,
                    staffId: ReactAPI.getUserInfo().staff.id
                }
            }, res => {
                if (res.code != 200) {
                    ReactAPI.showMessage('f', res.message);
                    while ($("div[id='com_self-loading']")[0]) {
                        ReactAPI.closeLoading();
                    }
                    if (refreshOnFail) {
                        refreshDgStockRecord();
                    }
                    return false;
                }
                if (res.data && res.data.length) {
                    //存在未完成盘点
                    var undoneRowIdx = dgDetail.getDatagridData().filter(val => res.data.includes(val.distribution.target.onlyCode)).map(val => val.rowIndex).join(",");
                    var curRowIdx = dgDetail.getSelecteds().map(val => val.rowIndex).join(",");
                    if (undoneRowIdx != curRowIdx) {
                        dgDetail.setSelecteds(undoneRowIdx);
                        setupDetailSelectWithCheck();
                    } else {
                        checkStockRecord(refreshOnFail);
                        if (refreshOnFail) {
                            refreshDgStockRecord();
                        }
                    }
                    while ($("div[id='com_self-loading']")[0]) {
                        ReactAPI.closeLoading();
                    }
                } else {
                    //刷新上表体
                    searchPanel.updateSearch();
                    //盘点完成并已提交
                    ReactAPI.openLoading(res.message, "2", true);
                    setTimeout(() => {
                        while ($("div[id='com_self-loading']")[0]) {
                            ReactAPI.closeLoading();
                        }
                    }, 500);
                }
            });
        }

        //未保存数据时先保存，然后
        if (isChanged()) {
            disableSaveLoadingAutoClose = true;
            //触发保存然后回刷表体
            saveChanged(() => {
                //保存过后需要刷新，否则新增数据保存的id会丢失，如果再次修改会导致问题
                refreshOnFail = true;
                afterSave();
            });
        } else {
            refreshOnFail = false;
            afterSave();
        }
    }

    //如果是抽盘，提交前确认
    if (vFullOrSpot != "material_invRange/allRange") {
        //先确认
        ReactAPI.openConfirm({
            message: ReactAPI.international.getText("Confirm.process.message"),
            onOk: () => {
                ReactAPI.closeConfirm();
                setTimeout(() => {
                    afterConfirm();
                })
            },
            onCancel: () => {
                ReactAPI.closeConfirm();
            }
        });
    } else {
        afterConfirm();
    }
}


function ptBtnSRAddLine() {
    var newLine;
    if (vPlaceEnable) {
        var selDetails = dgDetail.getSelecteds();
        if (selDetails.length != 1) {
            //请选择一条盘点任务明细
            ReactAPI.showMessage('w', ReactAPI.international.getText("material.stocktaking.choose_job_detail_first"));
            return;
        }
        var target = selDetails[0].distribution.target;
        //根据建模查找货位
        var result = ReactAPI.request({
            url: "/msService/material/entity/getEntityList",
            type: "get",
            async: false,
            data: {
                moduleName: "BaseSet",
                entityName: "BaseSetStoreSet",
                conditions: "onlyCode='" + target.onlyCode + "'",
                includes: "id,name,code,onlyCode"
            }
        });
        if (result.code != 200) {
            ReactAPI.showMessage('f', result.message);
            return false;
        }
        if (!result.data.length) {
            //找不到对应基础货位！
            ReactAPI.showMessage('f', ReactAPI.international.getText("material.stocktaking.cannot_find_place_in_base"));
            return false;
        }
        var place = result.data[0];
        //设置新增数据
        newLine = {
            place: place,
            quantityOnBook: 0
        };

    } else {
        //设置新增数据
        newLine = {
            quantityOnBook: 0
        };
    }
    //将之前设置的属性附加上去
    datagrid.appendRowAttr(dgStockRecordName, newLine);
    //增行后添加到变化表
    var addResult = dgStockRecord.addLine([newLine], true)[0];
    changed_rows.add(addResult.key);
}

function ptBtnSRDeleteLine() {
    var selRows = dgStockRecord.getSelecteds();
    if (!selRows.length) {
        //请至少选择一行数据！
        ReactAPI.showMessage('w', ReactAPI.international.getText("ec.business.SelectRow"));
        return false;
    }
    for (const selRow of selRows) {
        if (selRow.quantityOnBook > 0) {
            //不能删除账面量大于0的数据！
            ReactAPI.showMessage('w', ReactAPI.international.getText("material.stocktaking.cannot_delete_data_with_quan"));
            return false;
        }
    }
    //取消注册后删行
    selRows.forEach(rowData => {
        changed_rows.delete(rowData.key);
        if (rowData.id) {
            deleteIds.push(rowData.id);
        }
    });
    dgStockRecord.deleteLine(selRows.map(val => val.rowIndex).join(","));
}


function ptBtnSRSave() {
    if (isChanged()) {
        disableSaveLoadingAutoClose = false;
        //触发保存然后回刷表体
        saveChanged(() => {
            refreshDgStockRecord();
        });
    } else {
        //盘点数据未发生变化
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.stocktaking.content_not_changed"));
    }
}

//--------------------------------------------------------------------------------------btn--------------------------------------------------------------------------------------END

function ptocgBatchInfo(val, rowIdx) {
    var rowData = dgStockRecord.getRows(String(rowIdx))[0];
    if (val && val[0]) {
        rowData.batchInfo = val[0];
        rowData.material = val[0].materialId;
    } else {
        rowData.batchInfo = undefined;
    }
    //将之前设置的属性附加上去
    datagrid.appendRowAttr(dgStockRecordName, rowData);
    dgStockRecord.setRowData(rowIdx, rowData);
    changed_rows.add(rowData.key);
}

function ptocgMaterial(val, rowIdx) {
    var rowData = dgStockRecord.getRows(String(rowIdx))[0];
    if (val && val[0]) {
        if (vMaterialIds.length) {
            if (!vMaterialIds.includes(val[0].id)) {
                dgStockRecord.setRowData(rowIdx, {
                    material: null
                });
                setTimeout(() => {
                    $("div[keyname='" + dgStockRecordName + "']").click();
                    //超出盘点物料范围
                    ReactAPI.showMessage('w', ReactAPI.international.getText("material.stocktaking.exceed_stocktaking_material_scope"));
                });
            }
        }
    }
    changed_rows.add(rowData.key);
}

function ptocgCountQuan(val, rowIdx) {
    var rowData = dgStockRecord.getRows(String(rowIdx))[0];
    if (typeof val == "number" && val >= 0) {
        //根据偏移量设置颜色
        var color = getQuantityColor(rowData.quantityOnBook, val);
        dgStockRecord.setRowData(rowIdx, {
            quantityOffset: val - rowData.quantityOnBook,
            quantityOffset_attr: {
                style: {
                    color: color
                }
            }
        });
    } else {
        dgStockRecord.setRowData(rowIdx, {
            quantityOffset: null,
            quantityOffset_attr: {}
        });
    }
    changed_rows.add(rowData.key);
}

const base_col = 0x50;
const minimum_col = 0xA0;
const maximum_col = 0xFF;

function rgbToHex(rgb) {
    return '#' + ((parseInt(rgb[0]) << 16) + (parseInt(rgb[1]) << 8) + parseInt(rgb[2])).toString(16);
}

function getQuantityColor(quantityOnBook, quantityByCount) {
    var quantityOffset = quantityByCount - quantityOnBook;
    var color;
    if (quantityOffset > 0) {
        //越盈越蓝，2倍封顶
        var rate = 2 - quantityByCount / quantityOnBook;
        if (rate < 0) {
            color = rgbToHex([base_col, base_col, maximum_col]);
        } else {
            color = rgbToHex([base_col, base_col, maximum_col * (1 - rate) + minimum_col * rate]);
        }
    } else if (quantityOffset < 0) {
        //越亏越红，2倍封顶
        var rate = 2 - quantityOnBook / quantityByCount;
        if (rate < 0) {
            color = rgbToHex([maximum_col, base_col, base_col]);
        } else {
            color = rgbToHex([maximum_col * (1 - rate) + minimum_col * rate, base_col, base_col]);
        }
    } else {
        color = "#111";
    }
    return color;
}


function getBatchRefParams() {
    var selRow = dgStockRecord.getSelecteds()[0];
    if (selRow && selRow.material && selRow.material.id) {
        return "materialId=" + selRow.material.id;
    } else {
        if (vMaterialIds.length) {
            return "materialIds=" + vMaterialIds.join(",");
        }
        return "";
    }
}

function getMaterialRefParams() {
    if (vMaterialIds.length) {
        return "targetIds=" + vMaterialIds.join(",");
    }
    return "";
}


function checkDgStockRecord() {
    //只在提交时校验
    var check_result = datagrid.validator.check(dgStockRecordName);
    if (!check_result) {
        return false;
    }
    //如果抽盘，则至少填一项的盘点数量
    if (vFullOrSpot != "material_invRange/allRange") {
        var dgData = dgStockRecord.getDatagridData();
        if (dgData.filter(val => typeof val.quantityByCount == "number").length == 0) {
            //请填写盘点数量
            ReactAPI.showMessage('w', ReactAPI.international.getText("material.stocktaking.fill_count_quan"));
            return false;
        }
    }
    //校验现存量重复
    var repeatRows = findRepeatRows();
    if (repeatRows) {
        //第{0}行，数据重复
        ReactAPI.showMessage('w', ReactAPI.international.getText("material.custom.data_repeat", repeatRows));
        return false;
    }
    return true;
}


function findRepeatRows() {
    var keyRowsMap = new Map();
    var dgData = dgStockRecord.getDatagridData();
    dgData.forEach(record => {
        var key = getStockKey(record);
        if (keyRowsMap.has(key)) {
            keyRowsMap.get(key).push(record.rowIndex + 1);
        } else {
            keyRowsMap.set(key, [record.rowIndex + 1]);
        }
    });
    return Array.from(keyRowsMap.values()).filter(arr => arr.length > 1).map(arr => arr.join(",")).join("; ");
}

function getStockKey(stockRecord) {
    //物料id、批次id、货位id合成
    return [stockRecord.material && stockRecord.material.id, stockRecord.batchInfo && stockRecord.batchInfo.id, stockRecord.place && stockRecord.place.id].join(",");
}