package com.supcon.orchid.material.superwms.constants.systemcode;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

public class BaseTimeUnit {
    public static TemporalUnit getTemporalUnit(String systemCode){
        switch (systemCode.substring(systemCode.lastIndexOf("/")+1)){
            case "day":
                return ChronoUnit.DAYS;
            case "month":
                return ChronoUnit.MONTHS;
            case "year":
                return ChronoUnit.YEARS;
        }
        return ChronoUnit.DAYS;
    }
}
