//-----盘点任务-----
//引入datagrid.js
window.loader || loadScript("/msService/material/common/loader");
loader.import("datagrid");

const dgDetailName = "material_1.0.0_stocktakingJob_stocktaingJobEditdg1660874601546";
const dgStockRecordName = "material_1.0.0_stocktakingJob_stocktaingJobEditdg1660874616641";

var dgDetail;
var dgStockRecord;
var vPlaceEnable;
var vFullOrSpot;
var vShowOrNot;
var vMaterialIds;

function dataInit() {
    dgDetail = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgDetailName);
    dgStockRecord = ReactAPI.getComponentAPI().SupDataGrid.APIs(dgStockRecordName);
    vFullOrSpot = ReactAPI.getComponentAPI("SystemCode").APIs("stocktakingJob.stocktaking.fullOrSpot").getValue().value;
    vShowOrNot = ReactAPI.getComponentAPI("SystemCode").APIs("stocktakingJob.stocktaking.showStockOrNot").getValue().value;
    vMaterialIds = ReactAPI.getComponentAPI().Reference.APIs("stocktakingJob.stocktakingJobMaterial").getValue().map(val => parseInt(val.id));
    vPlaceEnable = ReactAPI.getComponentAPI("Checkbox").APIs("stocktakingJob.stocktaking.warehouse.storesetState").getValue().value;
    dataInit = () => { }
}


function onLoad() {
    dataInit();

    //绑定点击事件
    dgDetail.setClickEvt(function () {
        detailOnClick();
    });
    dgDetail.setCheckBoxClickEvt(function () {
        detailOnClick();
    });
}

function ptInit() {
    dataInit();
    //如果全盘则设置字段必填
    if (vFullOrSpot == "material_invRange/allRange") {
        //设置必填
        datagrid.validator.required.setColRequired(dgStockRecordName, [{
            key: "quantityByCount",
            type: "plain"
        }]);
    }
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
    //设置条件必填 开启批号的物料批次信息必填
    datagrid.validator.required.setRequiredByCondition(dgStockRecordName, [{
        key: "batchInfo.batchNum",
        type: "object"
    }], rowData => {
        return rowData.material && rowData.material.isBatch && rowData.material.isBatch.id != "BaseSet_isBatch/nobatch";
    });
}

function toStringCondition(condition) {
    var valid_keys = Object.keys(condition).filter(key => condition[key]);
    if (valid_keys.length) {
        return "customConditionKey=" + valid_keys.join(",") + "&" + valid_keys.map(key => key + "=" + condition[key]).join("&");
    }
    return ""
}

function ptBtnAddLine() {
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
                conditions: "onlyCode=" + target.onlyCode,
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
    //增行拿到结果后注册
    var addResult = dgStockRecord.addLine([newLine], true)[0];
    registerNewLine(addResult);
}

function ptBtnDeleteLine() {
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
        unregisterLine(rowData);
    });
    dgStockRecord.deleteLine(selRows.map(val => val.rowIndex).join(","));
}


function ptRenderOver() {
    //设置明盘暗盘隐藏
    if (vShowOrNot == 'material_stockTakingWay/implicitWay') {
        dgStockRecord.setColumnsHideOrShow("quantityOnBook");
        dgStockRecord.setColumnsHideOrShow("quantityOffset");
    }
    //设置只读条件
    datagrid.readonly.setReadonlyByCondition(dgStockRecordName, "batchInfo.batchNum", rowData => {
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
        //注册行
        registerNewLine(val);
    });
    dgStockRecord.setDatagridData(dgData);
}


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
    changed_row.add(rowIdx);

}
function ptocgMaterial(val, rowIdx) {
    if (val && val[0]) {
        if (vMaterialIds.length) {
            if (!vMaterialIds.includes(val[0].id)) {
                dgStockRecord.setRowData(rowIdx, {
                    material: null
                });
                $("div[keyname='" + dgStockRecordName + "']").click();
                //超出盘点物料范围
                ReactAPI.showMessage('w', ReactAPI.international.getText("material.stocktaking.exceed_stocktaking_material_scope"));
            }
        }
    }
    changed_row.add(rowIdx);
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

function ptocgCountQuan(val, rowIdx) {
    if (typeof val == "number" && val >= 0) {
        var rowData = dgStockRecord.getRows(String(rowIdx))[0];
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
            quantityOffset_attr: null
        });
    }
    changed_row.add(rowIdx);
}


function getBatchRefParams() {
    var selRow = dgStockRecord.getSelecteds()[0];
    if (selRow && selRow.material && selRow.material.id) {
        return "materialId=" + selRow.material.id;
    } else {
        var materials = ReactAPI.getComponentAPI().Reference.APIs("stocktakingJob.stocktakingJobMaterial").getValue();
        if (materials.length) {
            return "materialIds=" + materials.map(val => val.id).join(",");
        }
        return "";
    }
}


var vSelectDistModelCodes = new Set();
//分配任务点击，刷新右侧
function detailOnClick() {
    setTimeout(() => {
        //判断本次选择和上次选择是否一致，不一致则刷新
        var flush = false;
        var selRows = dgDetail.getSelecteds();
        if (vSelectDistModelCodes.size != selRows.length) {
            //先根据长度判断
            flush = true;
        } else {
            //再根据全包含判断
            selRows.map(val => val.distribution.target.onlyCode).forEach(onlyCode => {
                if (!flush) {
                    if (!vSelectDistModelCodes.has(onlyCode)) {
                        flush = true;
                    }
                }
            })
        }
        if (flush) {
            vSelectDistModelCodes = new Set(selRows.map(val => val.distribution.target.onlyCode));
            refreshStockRecord();
        }
    });
}

function refreshStockRecord() {
    flush_changed_data()
    //根据所选的货位编码过滤右侧数据
    if (vSelectDistModelCodes.size) {
        var remainList = Object.values(key$rec).filter(val => (!val.place || !val.place.id) || vSelectDistModelCodes.has(val.place.onlyCode));
        dgStockRecord.setDatagridData(remainList);
    } else {
        dgStockRecord.setDatagridData(Object.values(key$rec));
    }
}

//每次表体变化，标记变化行，等待重新注册
const changed_row = new Set();

const key$rec = {};

function flush_changed_data() {
    if (changed_row.size) {
        var changedRowsData = dgStockRecord.getRows(Array.from(changed_row).join(","));
        changedRowsData.forEach(rowData => {
            //重新计算stockKey
            rowData.stockKey = getStockKey(rowData);
            key$rec[rowData.key] = rowData;
        });
        changed_row.clear();
    }
}

function registerNewLine(rowData) {
    //存储记录
    key$rec[rowData.key] = rowData;
}

function unregisterLine(rowData) {
    //清空记录
    delete key$rec[rowData.key];
}


function resetDg() {
    //取消选中
    dgDetail.setSelecteds("-1");
    //恢复dg
    flush_changed_data();
    dgStockRecord.setDatagridData(Object.values(key$rec));
}

function onSave() {
    if (ReactAPI.getOperateType() == "submit") {
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
    }
}


function findRepeatRows() {
    var keyRowsMap = new Map();
    var dgData = dgStockRecord.getDatagridData();
    dgData.forEach(record => {
        var key = record.stockKey;
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