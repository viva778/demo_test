package com.supcon.orchid.material.superwms.util.builder;

import com.supcon.orchid.material.superwms.entities.dto.QCSCheckRequestDTO;

import java.math.BigDecimal;

public final class QCSCheckRequestDTOBuilder {
    private Long createDeptId;
    private Long createPositionId;
    private Long createStaffId;
    private Long VendorId;
    private Long sourceTableId;
    private Long sourceId;
    private String sourcTableNo;
    private Long prodId;
    private String batchCode;
    private BigDecimal quantity;
    private String sourceType = "QCS_sourceType/purchArrival";
    private String tableType = "purch";
    private String busiType = "material";

    private QCSCheckRequestDTOBuilder() {
    }

    public static QCSCheckRequestDTOBuilder aQCSCheckRequestDTO() {
        return new QCSCheckRequestDTOBuilder();
    }

    public QCSCheckRequestDTOBuilder withCreateDeptId(Long createDeptId) {
        this.createDeptId = createDeptId;
        return this;
    }

    public QCSCheckRequestDTOBuilder withCreatePositionId(Long createPositionId) {
        this.createPositionId = createPositionId;
        return this;
    }

    public QCSCheckRequestDTOBuilder withCreateStaffId(Long createStaffId) {
        this.createStaffId = createStaffId;
        return this;
    }

    public QCSCheckRequestDTOBuilder withVendorId(Long VendorId) {
        this.VendorId = VendorId;
        return this;
    }

    public QCSCheckRequestDTOBuilder withSourceTableId(Long sourceTableId) {
        this.sourceTableId = sourceTableId;
        return this;
    }

    public QCSCheckRequestDTOBuilder withSourceId(Long sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    public QCSCheckRequestDTOBuilder withSourcTableNo(String sourcTableNo) {
        this.sourcTableNo = sourcTableNo;
        return this;
    }

    public QCSCheckRequestDTOBuilder withProdId(Long prodId) {
        this.prodId = prodId;
        return this;
    }

    public QCSCheckRequestDTOBuilder withBatchCode(String batchCode) {
        this.batchCode = batchCode;
        return this;
    }

    public QCSCheckRequestDTOBuilder withQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        return this;
    }

    public QCSCheckRequestDTOBuilder withSourceType(String sourceType) {
        this.sourceType = sourceType;
        return this;
    }

    public QCSCheckRequestDTOBuilder withTableType(String tableType) {
        this.tableType = tableType;
        return this;
    }

    public QCSCheckRequestDTOBuilder withBusiType(String busiType) {
        this.busiType = busiType;
        return this;
    }

    public QCSCheckRequestDTO build() {
        QCSCheckRequestDTO qCSCheckRequestDTO = new QCSCheckRequestDTO();
        qCSCheckRequestDTO.setCreateDeptId(createDeptId);
        qCSCheckRequestDTO.setCreatePositionId(createPositionId);
        qCSCheckRequestDTO.setCreateStaffId(createStaffId);
        qCSCheckRequestDTO.setVendorId(VendorId);
        qCSCheckRequestDTO.setSourceTableId(sourceTableId);
        qCSCheckRequestDTO.setSourceId(sourceId);
        qCSCheckRequestDTO.setSourcTableNo(sourcTableNo);
        qCSCheckRequestDTO.setProdId(prodId);
        qCSCheckRequestDTO.setBatchCode(batchCode);
        qCSCheckRequestDTO.setQuantity(quantity);
        qCSCheckRequestDTO.setSourceType(sourceType);
        qCSCheckRequestDTO.setTableType(tableType);
        qCSCheckRequestDTO.setBusiType(busiType);
        return qCSCheckRequestDTO;
    }
}
