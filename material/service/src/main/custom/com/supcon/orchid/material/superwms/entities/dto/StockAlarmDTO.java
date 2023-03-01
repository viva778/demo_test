package com.supcon.orchid.material.superwms.entities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockAlarmDTO {
    @JsonProperty("isNo")
    private Boolean activate = false;

    @JsonProperty("Onhand")
    private BigDecimal stockQuan;

    @JsonProperty("UpAlarm")
    private BigDecimal upAlarm;

    @JsonProperty("DownAlarm")
    private BigDecimal downAlarm;
}
