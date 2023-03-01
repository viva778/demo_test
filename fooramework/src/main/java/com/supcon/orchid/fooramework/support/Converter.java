package com.supcon.orchid.fooramework.support;


import com.supcon.orchid.fooramework.util.Maps;
import com.supcon.orchid.fooramework.util.Strings;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.sql.Clob;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import static org.springframework.data.util.CastUtils.cast;

/**
 * 基本类型转换器
 */
public class Converter {
    public static final Function<Object,Object> noneConvert = o->o;
    public static final Function<Object,Long> longConverter = Converter::longConverter;
    public static final Function<Object,Integer> integerConverter = Converter::integerConverter;
    public static final Function<Object,Date> dateConverter = Converter::dateConverter;
    public static final Function<Object, BigDecimal> decimalConverter = Converter::decimalConverter;
    public static final Function<Object, String> stringConverter = Converter::stringConverter;
    public static final Function<Object, Boolean> booleanConverter = Converter::booleanConverter;

    private static final Map<String,Function<Object,?>> CONVERTER_MAP = Maps.immutable(
            Long.class.getName(),longConverter,
            long.class.getName(),longConverter,
            Integer.class.getName(),integerConverter,
            int.class,integerConverter,
            BigDecimal.class.getName(),decimalConverter,
            String.class.getName(),stringConverter,
            Boolean.class.getName(),booleanConverter,
            boolean.class.getName(), (Function<Object, ?>) o -> Boolean.TRUE.equals(booleanConverter(o)),
            Date.class.getName(),dateConverter
    );

    public static <T> Function<Object,T> getConverter(Class<T> toClass){
        if(toClass!=null){
            return cast(CONVERTER_MAP.getOrDefault(toClass.getName(),noneConvert));
        }
        return cast(noneConvert);
    }

    public static Integer integerConverter(Object i){
        if(i instanceof Number){
            return ((Number)i).intValue();
        } else if(i instanceof CharSequence){
            String s = i.toString();
            if(Strings.isNumber(s)){
                return new BigDecimal(s).intValue();
            }
        }
        return null;
    }

    public static Long longConverter(Object l){
        if(l instanceof Number) {
            return ((Number)(l)).longValue();
        } else if(l instanceof CharSequence) {
            String s = l.toString();
            if(Strings.isNumber(s)){
                return new BigDecimal(s).longValue();
            }
        } else if(l instanceof Date) {
            return ((Date)l).getTime();
        }
        return null;
    }

    @SneakyThrows
    public static String stringConverter(Object s){
        if(s instanceof String) {
            return (String)s;
        } if(s instanceof Clob){
            BufferedReader br = new BufferedReader(((Clob) s).getCharacterStream());
            StringBuilder sb = new StringBuilder();
            for(String st = br.readLine();st!=null;st = br.readLine()){
                sb.append(st);
            }
            return sb.toString();
        } if(s!=null) {
            return s.toString();
        } else {
            return null;
        }
    }


    public static Date dateConverter(Object d){
        if(d instanceof Date){
            return (Date)d;
        } else if(d instanceof Number){
            return new Date(((Number)d).longValue());
        } else if(d instanceof CharSequence){
            return parseDate(d.toString());
        } else {
            return null;
        }
    }

    public static BigDecimal decimalConverter(Object d) {
        if(d instanceof BigDecimal) {
            return (BigDecimal)d;
        } if(d instanceof Number) {
            return new BigDecimal(d.toString());
        } else if(d instanceof CharSequence){
            String str = d.toString();
            if(Strings.isNumber(str)){
                return new BigDecimal(str);
            }
        }
        return null;
    }

    public static Boolean booleanConverter(Object b){
        if(b instanceof Boolean) {
            return (Boolean) b;
        } if(b instanceof Number) {
            return ((Number)b).longValue()!=0;
        } if(b instanceof CharSequence){
            switch (b.toString().toLowerCase()){
                case "true":
                case "1":
                    return true;
                case "false":
                case "0":
                    return false;
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    private static Date parseDate(String dateString){
        String cleanString = dateString.replaceAll("[^0-9|+]","");
        return parseDate(cleanString,"yyyyMMddHHmmssSSS");
    }

    @SneakyThrows
    private static Date parseDate(String dateString, String format){
        String[] arr = dateString.split("\\+");
        String subFormat;
        if(arr.length==2){
            subFormat = format.substring(0,arr[0].length()).concat("XXX");
        } else if(dateString.length()<=format.length()){
            subFormat = format.substring(0,dateString.length());
        } else {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(subFormat);
        return sdf.parse(dateString);
    }
}