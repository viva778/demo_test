package com.supcon.orchid.material.superwms.util.adptor;

import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BizTypeContext {

    /**
     * 类型编码
     */
    private String bizType;

    /**
     * 前缀
     */
    private String prefix;

    /**
     * 单据CLASS对象
     */
    private Class<? extends AbstractEcFullEntity> tableClass;

    /**
     * 表体关联表头列名
     */
    private String associatedColumn;

    /**
     * 表体CLASS对象
     */
    private Class<? extends AbstractEcPartEntity> detailClass;


    /**
     * 申请数量列名
     */
    private String applyQuantityColumn;

    /**
     * 实际数量列名
     */
    private String quantityColumn;

    private String processKey;

    /**
     * 编辑视图编码
     */
    private String editCode;

    /**
     * 列表视图编码
     */
    private String listCode;

    /**
     * 查看视图编码
     */
    private String viewCode;

    /**
     * 对应基础单据类型
     */
    private String baseSourceType;

    /**
     * 生成二维码类型
     */
    private String qrTypeCode;

    private InTypeContext inContext;

    private OutTypeContext outContext;

    public BizTypeContext(String bizType, String prefix, Class<? extends AbstractEcFullEntity> tableClass, String associatedColumn, Class<? extends AbstractEcPartEntity> detailClass, String applyQuantityColumn, String quantityColumn, String processKey, String editCode, String listCode, String viewCode, String baseSourceType, String qrTypeCode, InTypeContext inContext) {
        this.bizType = bizType;
        this.prefix = prefix;
        this.tableClass = tableClass;
        this.associatedColumn = associatedColumn;
        this.detailClass = detailClass;
        this.applyQuantityColumn = applyQuantityColumn;
        this.quantityColumn = quantityColumn;
        this.processKey = processKey;
        this.editCode = editCode;
        this.listCode = listCode;
        this.viewCode = viewCode;
        this.baseSourceType = baseSourceType;
        this.qrTypeCode = qrTypeCode;
        this.inContext = inContext;
    }

    public BizTypeContext(String bizType, String prefix, Class<? extends AbstractEcFullEntity> tableClass, String associatedColumn, Class<? extends AbstractEcPartEntity> detailClass, String applyQuantityColumn, String quantityColumn, String processKey, String editCode, String listCode, String viewCode, String baseSourceType, String qrTypeCode, OutTypeContext outContext) {
        this.bizType = bizType;
        this.prefix = prefix;
        this.tableClass = tableClass;
        this.associatedColumn = associatedColumn;
        this.detailClass = detailClass;
        this.applyQuantityColumn = applyQuantityColumn;
        this.quantityColumn = quantityColumn;
        this.processKey = processKey;
        this.editCode = editCode;
        this.listCode = listCode;
        this.viewCode = viewCode;
        this.baseSourceType = baseSourceType;
        this.qrTypeCode = qrTypeCode;
        this.outContext = outContext;
    }
}
