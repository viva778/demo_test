package com.supcon.orchid.material.superwms.entities.dto;

import lombok.Data;

/**
 * 检验结果回填传参DTO
 * @author ：dfx
 */
@Data
public class QCSCheckResultDTO {

    private Long tableInfoId;
    /**
     * 检验结果
     */
    private String checkResult;
    /**
     * 上游单据id
     */
    private Long srcId;

}
