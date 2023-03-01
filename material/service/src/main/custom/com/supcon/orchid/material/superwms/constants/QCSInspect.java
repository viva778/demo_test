package com.supcon.orchid.material.superwms.constants;

public interface QCSInspect {
    String QCS_MODULE_NAME = "QCS";
    // 发起请检url
    String CREATE_INSPECT_URL = "/public/QCS/inspect/inspect/createInspect";
    // 删除请检url
    String DELETE_INSPECT_URL = "/public/QCS/inspect/inspect/deleteTaskPending";
    // 业务类型
    String BUSINESS_TYPE_OTHER = "other";
    String BUSINESS_TYPE_MATERIAL = "material";

    // 单据类型
    String TABLE_TYPE_OTHER = "other";
    String TABLE_TYPE_MATERIAL = "purch";
}
