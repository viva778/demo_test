package com.supcon.orchid.material.superwms.config;

import com.supcon.supfusion.systemconfig.api.tenantconfig.annotation.ClassSystemConfigAnno;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ClassSystemConfigAnno
@Data
public class MaterialSystemConfig {


    /**
     * 启用上下架
     */
    @Value("${material/material.wmspro:}")
    private Boolean enableTask;

    /**
     * 采购到货生成请检单
     */
    @Value("${material/material.purArrivalInfos:}")
    private Boolean generateCheckRequest;


    /**
     * 仓库物料存储检查类型
     */
    @Value("${material/material.stockEnable:}")
    private String storageCheckType;


    /**
     * 批次唯一规则
     */
    @Value("${material/material.batchUniqueRule:}")
    public String batchUniqueRule;

    /**
     * 质检采购入库生成
     */
    @Value("${material/material.arrivalCheck:}")
    public Boolean arrivalCheck;

    /**
     * 启用打印机
     */
    @Value("${material/material.enablePrinter:}")
    public Boolean enablePrinter;

    /**
     * 其他入库单请检设置
     */
    @Value("${material/material.otherInCheckOptions:}")
    public String otherInCheckOptions;
}
