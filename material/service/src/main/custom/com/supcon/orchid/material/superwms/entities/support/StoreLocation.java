package com.supcon.orchid.material.superwms.entities.support;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoreLocation {
    private Integer row;
    private Integer column;
    private Integer layer;
}
