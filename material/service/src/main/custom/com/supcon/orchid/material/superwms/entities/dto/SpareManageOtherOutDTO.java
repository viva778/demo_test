package com.supcon.orchid.material.superwms.entities.dto;

import lombok.Data;

import java.util.List;

@Data
public class SpareManageOtherOutDTO {
    private Integer status;

    private List<SpareManageOtherOutDetailDTO> detailList;
}
