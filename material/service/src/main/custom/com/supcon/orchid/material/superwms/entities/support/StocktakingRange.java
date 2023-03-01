package com.supcon.orchid.material.superwms.entities.support;

import lombok.Data;

import java.util.Set;

@Data
public class StocktakingRange {
    private Long stocktakingId;

    private Long warehouseId;

    private Set<Long> placeIds;

    private Set<Long> materialIds;
}
