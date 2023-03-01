package com.supcon.orchid.fooramework.util;

import com.supcon.orchid.fooramework.support.Pair;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalUnit;
import java.util.Calendar;
import java.util.Date;

public class Dates {

    private static final ZoneId zoneId = ZoneId.systemDefault();

    private static final long MILS_OF_DAY = 24*3600*1000L;

    public static LocalDate getLocalDate(Date date) {
        return date.toInstant().atZone(zoneId).toLocalDate();
    }

    public static LocalDateTime getLocalDateTime(Date date) {
        return date.toInstant().atZone(zoneId).toLocalDateTime();
    }

    /**
     * 偏移时间
     * @param from 起始时间
     * @param offset 偏移
     * @param unit 时间单位
     * @return 偏移后时间
     */
    public static Date offset(Date from, long offset, TemporalUnit unit){
        LocalDateTime ldFrom = getLocalDateTime(from);
        if(offset>=0){
            return Date.from(ldFrom.plus(offset,unit).atZone(zoneId).toInstant());
        } else {
            return Date.from(ldFrom.minus(-offset,unit).atZone(zoneId).toInstant());
        }
    }

    /**
     * 获取时间偏移
     * @param from  起始时间
     * @param to 结束时间
     * @param unit 时间单位
     * @return 时间偏移
     */
    public static long getOffset(Date from, Date to, TemporalUnit unit){
        if(to.before(from)){
            return -1;
        } else {
            LocalDate ldFrom = getLocalDate(from);
            LocalDate ldTo = getLocalDate(to);
            return ldFrom.until(ldTo, unit);
        }
    }


    /**
     * 获取年时间范围
     * @param year 年份
     * @return Pair: {first:起始时间, second:结束时间}
     */
    public static Pair<Date,Date> getYearRange(Integer year){
        Calendar cal = Calendar.getInstance();
        // 这里首先获取到今年的年份，然后在这个基准年份上进行驱动计算。
        int yearVal = year!=null?year:cal.get(Calendar.YEAR);
        // 设置时间归零。
        cal.clear();
        // 设置时间从指定的年份开始。
        cal.set(Calendar.YEAR, yearVal);
        Date start = cal.getTime();
        cal.set(Calendar.YEAR, yearVal+1);
        cal.set(Calendar.DAY_OF_MONTH, 0);
        Date end = cal.getTime();
        return Pair.of(start,end);
    }

    /**
     * 获取日时间范围
     * @param date date
     * @return Pair: {first:起始时间, second:结束时间}
     */
    public static Pair<Date,Date> getDayRange(Date date){
        Date cleanDate = getDateWithoutTime(date!=null?date:new Date());
        return Pair.of(cleanDate,new Date(cleanDate.getTime()+MILS_OF_DAY));
    }

    public static Date getDateWithoutTime(Date date){
        LocalDate localDate = getLocalDate(date);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, localDate.getYear());
        cal.set(Calendar.MONTH, localDate.getMonthValue()-1);
        cal.set(Calendar.DAY_OF_MONTH, localDate.getDayOfMonth());
        return cal.getTime();
    }

    public static int getYear(Date date){
        return getLocalDate(date).getYear();
    }

    public static int getMonth(Date date){
        return getLocalDate(date).getMonthValue();
    }

    public static int getDay(Date date){
        return getLocalDate(date).getDayOfMonth();
    }

    /**
     * 获取今年时间范围
     * @return Pair: {first:起始时间, second:结束时间}
     */
    public static Pair<Date,Date> getYearRange(){
        return getYearRange(null);
    }

    /**
     * 获取今日时间范围
     * @return Pair: {first:起始时间, second:结束时间}
     */
    public static Pair<Date,Date> getDayRange(){
        return getDayRange(null);
    }

    public static void main(String[] args) {
        Pair<Date, Date> dayRange = getDayRange();
        System.out.println(dayRange.getFirst().toLocaleString());
    }
}
