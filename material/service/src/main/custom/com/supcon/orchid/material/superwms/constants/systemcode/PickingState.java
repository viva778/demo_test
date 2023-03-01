package com.supcon.orchid.material.superwms.constants.systemcode;

public class PickingState {

    /**
     * 待下架
     */
    public static final String TO_PICK = "material_pickingState/waitPicking";

    /**
     * 拣货中
     */
    public static final String PICKING = "material_pickingState/picking";

    /**
     * 待发货
     */
    public static final String TO_DELIVER = "material_pickingState/stayDelivery";

    /**
     * 完成
     */
    public static final String COMPLETE = "material_pickingState/complete";

    /**
     * 已取消
     */
    public static final String CANCEL = "material_pickingState/cancelPicking";
}
