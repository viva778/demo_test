package com.supcon.orchid.material.superwms.entities.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QCSCheckRequestDTO {

    /**
     * 创建部门ID
     */
    private Long createDeptId;

    /**
     * 创建岗位ID
     */
    private Long createPositionId;

    /**
     * 创建人员ID
     */
    private Long createStaffId;

    /**
     * 供应商ID
     */
    private Long VendorId;

    /**
     * 表头ID
     */
    private Long sourceTableId;

    /**
     * 表体ID
     */
    private Long sourceId;

    /**
     * 来源单据编号
     */
    private String sourcTableNo;

    /**
     * 物品Id
     */
    private Long prodId;

    /**
     * 批号
     */
    private String batchCode;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 来源类型
     */
    private String sourceType = "QCS_sourceType/purchArrival";

    /**
     * 单据类型
     */
    private String tableType = "purch";

    /**
     * 业务类型
     */
    private String busiType = "material";


}
