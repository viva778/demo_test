package com.supcon.orchid.material.superwms.constants.systemcode;

public class WareType {
    /**
     * 原料仓
     */
    public static final String WARE_RAW_MATERIAL = "material_wareType/wareYL";
    /**
     * 产品仓
     */
    public static final String WARE_PRODUCT = "material_wareType/wareCP";
    /**
     * 货区
     */
    public static final String CARGO_AREA = "material_wareType/cargoArea";
    /**
     * 货位
     */
    public static final String CARGO_PLACE = "material_wareType/storeSet";

    /**
     * 是否区域类型
     */
    public static boolean isArea(String code){
        return CARGO_AREA.equals(code);
    }

    /**
     * 是否仓库类型
     */
    public static boolean isWarehouse(String code){
        return WARE_RAW_MATERIAL.equals(code)||WARE_PRODUCT.equals(code);
    }

    /**
     * 是否货位类型
     */
    public static boolean isPlace(String code){
        return CARGO_PLACE.equals(code);
    }
}
