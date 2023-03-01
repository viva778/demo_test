//-----库存日结算-----
const primary_button_class = "_2HfKBYPk";
const normal_button_class = "_3aRgZuf5";
const dgName = "material_1.0.0_daySettlement_daySettlementList_daySettlement_sdg";
var dg;



function dataInit() {
    dg = ReactAPI.getComponentAPI("SupDataGrid").APIs(dgName);
    dataInit = () => { }
}


function ptInit() {
    dataInit();
}

function onLoad() {
    //隐藏原查询按钮
    var btnOriginQuery = $("button[data-id='query']");
    btnOriginQuery.hide();
    //添加仅查待办按钮，并绑定事件
    btnOriginQuery.parent().prepend(getPlaceButtonHtml());
    var btnPlace = $("#btn_place");
    btnPlace.click(() => {
        dg.setRequestUrl("/msService/material/daySettlement/daySettlement/daySettlementList-query?groupType=place");
        //显示货位列
        dg.setColumnsHideOrShow("placeSet.name", true);
        dgRefresh();
    });
    //添加查询按钮，并绑定事件
    btnOriginQuery.parent().prepend(getWarehouseButtonHtml());
    var btnWarehouse = $("#btn_warehouse");
    btnWarehouse.click(() => {
        dg.setRequestUrl("/msService/material/daySettlement/daySettlement/daySettlementList-query?groupType=warehouse");
        //隐藏货位列
        dg.setColumnsHideOrShow("placeSet.name", false);
        dgRefresh();
    });
}


function getWarehouseButtonHtml() {
    return '<button data-id="warehouse" id="btn_warehouse" type="button" class="ant-btn ' + primary_button_class + '"><span>' + ReactAPI.international.getText("material.stock_report.query_by_warehouse") + '</span></button>';
}

function getPlaceButtonHtml() {
    return '<button data-id="place" id="btn_place" type="button" class="ant-btn ' + primary_button_class + '" style="margin-left: 8px;" ant-click-animating-without-extra-node="false"><span>' + ReactAPI.international.getText("material.stock_report.query_by_place") + '</span></button>';
}

function dgRefresh() {
    ReactAPI.getComponentAPI("SearchPanel").APIs("material_1.0.0_daySettlement_daySettlementList_daySettlement_sp").updateSearch();
}