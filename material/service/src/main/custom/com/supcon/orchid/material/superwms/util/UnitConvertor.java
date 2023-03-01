package com.supcon.orchid.material.superwms.util;

import com.supcon.orchid.BaseSet.entities.BaseSetUnit;
import com.supcon.orchid.fooramework.util.Dbs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class UnitConvertor {
    public static BigDecimal convert(String fromUnit,String toUnit,BigDecimal value){
        if(fromUnit.equals(toUnit)){
            return value;
        } else {
            Map<String,BigDecimal> code$rate = Dbs.binaryMap(
                    "SELECT CODE,EXCHANGE_RATE FROM "+ BaseSetUnit.TABLE_NAME+" WHERE VALID=1 AND (CODE=? OR CODE=?)",
                    String.class,BigDecimal.class,
                    fromUnit,toUnit
            );
            BigDecimal fromRate = code$rate.get(fromUnit);
            BigDecimal toRate = code$rate.get(toUnit);
            if(fromRate==null||toRate==null){
                return value;
            }
            return value.multiply(toRate).divide(fromRate, RoundingMode.CEILING);
        }
    }
}
