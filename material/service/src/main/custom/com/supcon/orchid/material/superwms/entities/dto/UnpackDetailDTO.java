package com.supcon.orchid.material.superwms.entities.dto;

import com.supcon.orchid.material.entities.MaterialQrDetailInfo;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 拆包明细
 */
@Data
public class UnpackDetailDTO {

    private Long unpackDetailId;

    private String dataKey;

    private String batchNum;

    private BigDecimal quantity;

    private MaterialQrDetailInfo barCode;

}
