package com.supcon.orchid.fooramework.util;

import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.support.Wrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Strings {
    public static String upperCaseFirst(String origin){
        if(origin!=null&&origin.length()>0){
            return origin.substring(0,1).toUpperCase()+origin.substring(1);
        }
        return origin;
    }

    public static String lowerCaseFirst(String origin){
        if(origin!=null&&origin.length()>0){
            return origin.substring(0,1).toLowerCase()+origin.substring(1);
        }
        return origin;
    }

    /**
     * 转为大写+下划线
     * aaBb->AA_BB
     */
    public static String upperUnderLine(String origin){
        String[] peace = origin.split("");
        StringBuilder builder = new StringBuilder();
        //上一个是小写
        boolean lastIsLower = false;
        for (String s : peace) {
            String upper = s.toUpperCase();
            boolean isUpper = upper.equals(s);
            if(lastIsLower&&isUpper){
                builder.append("_");
            }
            builder.append(upper);
            lastIsLower = !isUpper;
        }
        return builder.toString();
    }

    private static final Set<String> camelIzeChars = Stream
            .of(".","_")
            .collect(Collectors.toSet());

    /**
     * 驼峰化
     * AA_BB&aa_bb ->aaBb
     */
    public static String camelIze(String origin){
        String[] peace = origin.split("");
        StringBuilder builder = new StringBuilder();
        int state = 0;//0正常 1下一个转大写
        for (String s : peace) {
            if(camelIzeChars.contains(s)){
                state = 1;
                continue;
            }
            if(state==1){
                builder.append(s.toUpperCase());
                state = 0;
            } else {
                builder.append(s.toLowerCase());
            }
        }
        return builder.toString();
    }
    /**
     * 获取唯一匹配项
     * @param patterns 字符对象集合
     * @param regex 正则表达式
     * @return 唯一匹配项
     */
    public static String match(Collection<String> patterns, String regex){
        Wrapper<Boolean> wFilterFound = new Wrapper<>(false);
        return patterns.stream()
                .filter(key-> {
                    if(key.matches(regex)){
                        Assert.isTrue(!wFilterFound.get(),"匹配到多个满足正则的条件！");
                        wFilterFound.set(true);
                        return true;
                    } else {
                        return false;
                    }
                })
                .findAny()
                .orElse(null);
    }


    public static boolean isNumber(String value){
        return value!=null&& Pattern.matches("^(-?\\d+)(\\.\\d+)?$",value);
    }

    public static boolean valid(Object origin){
        return origin instanceof String &&((String)origin).trim().length()!=0;
    }

    @SafeVarargs
    public static <X> String join_valid(String delimiter, X...elements){
        return join(delimiter, ArrayOperator.of(elements).filter(Strings::valid).get());
    }

    @SafeVarargs
    public static <X> String join(String delimiter,X...elements){
        return String.join(delimiter, Elements.toArray(String[]::new,String::valueOf, (Object[]) elements));
    }

    public static List<String> findPatternBetween(String origin, String header, String tail) {
        List<Snapshot> snapshots = getSnapshots(origin,Stream.of(header,tail).distinct().collect(Collectors.toList()));
        int state = -1; //-1 初始，0 找到起始
        int markIndex = -1; //如果找到起始字符串，则标记位置
        List<String> result = new LinkedList<>();
        for (Snapshot snapshot : snapshots) {
            switch (state){
                case -1:{
                    if(snapshot.pattern.equals(header)){
                        state = 0;
                        markIndex = snapshot.end;
                    }
                    break;
                }
                case 0:{
                    if(snapshot.pattern.equals(tail)){
                        //记录字符，恢复状态
                        result.add(origin.substring(markIndex,snapshot.start));
                        state = -1;
                    } else if(snapshot.pattern.equals(header)) {
                        //更新起始位置
                        markIndex = snapshot.end;
                    }
                }
            }
        }
        return result;
    }


    /**
     * 获取成对出现符号的下标
     * @param origin 原字符串
     * @param pairs 成对出现的字符串列表
     * @return 下标列表
     */
    @SafeVarargs
    public static List<Pair<Integer,Integer>> indexOfPairs(String origin, Pair<String,String>...pairs){
        Map<String,String> pairMap = Stream.of(pairs).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
        List<Snapshot> snapshots = getSnapshots(origin,Stream.of(pairs).flatMap(pair->Stream.of(pair.getFirst(),pair.getSecond())).collect(Collectors.toSet()));
        List<Pair<Integer,Integer>> result = new LinkedList<>();
        if(!snapshots.isEmpty()){
            Stack<Snapshot> headers = new Stack<>();
            Snapshot cur_header = null;
            for (Snapshot snapshot : snapshots) {
                if(cur_header!=null){
                    if(pairMap.get(cur_header.pattern).equals(snapshot.pattern)){
                        cur_header = headers.pop();
                        if(headers.isEmpty()){
                            result.add(Pair.of(cur_header.start,snapshot.end));
                            continue;
                        }
                    }
                }
                if(pairMap.containsKey(snapshot.pattern)){
                    cur_header = headers.push(snapshot);
                }
            }
        }
        return result;
    }

    /**
     * 简单的字符串替换，替换量少的时候使用
     * 多的时候时间复杂度比较高
     */
    public static String replace(String origin, Map<String, String> replaceMap){
        Wrapper<String> replaced = new Wrapper<>(origin);
        //从长到短排序后，进行替换
        replaceMap.keySet().stream().sorted((s1,s2)->s2.length()-s1.length()).forEach(pattern->{
            String str = replaced.get();
            replaced.set(str.replace(pattern,replaceMap.get(pattern)));
        });
        return replaced.get();
    }

    /**
     * 以一种稳定的算法方案，将map中的key在origin中找到，并替换成value
     * 时间复杂度为O(origin.len*replaceMap.size)
     * 但不建议替换项少的时候使用
     */
    public static String replace_stable(String origin, Map<String, String> replaceMap){
        List<Snapshot> snapshots = getSnapshots(origin,replaceMap.keySet());
        //进行替换
        StringBuilder builder = new StringBuilder();
        int replace_start,replace_end,move_index=0;

        for (Snapshot snapshot : snapshots) {
            replace_start = snapshot.start;
            replace_end = snapshot.end;
            if (move_index <= replace_start) {
                //把之前移动的末尾，到这次移动的开始，把字符串追加
                builder.append(origin, move_index, replace_start);
                move_index = replace_end;
                //追加这次替换的内容
                builder.append(replaceMap.get(snapshot.pattern));
            }
        }

        if(move_index<origin.length()){
            builder.append(origin.substring(move_index));
        }
        return builder.toString();
    }

    /**
     * 在origin中，查找patterns中字符串的快照
     * Snapshot中
     *  start 为字符串起始
     *  end 为字符串结束
     *  pattern 为字符串
     */
    public static List<Snapshot> getSnapshots(String origin, Collection<String> patterns){
        //用于保存快照，字符串 起始&结束 以及对应key
        List<Snapshot> snapshots = new ArrayList<>(patterns.size());
        char[] pOrigin = constCharArray(origin);

        //拿patterns中k的第v个字符与origin的index字符匹配
        Map<String,Integer> keyCnt = new HashMap<>();
        Map<String,char[]> pattern$pPattern = new HashMap<>();
        for (String pattern : patterns) {
            pattern$pPattern.put(pattern,constCharArray(pattern));
        }
        Map<String,int[]> pattern$marks = null;

        for(int originIndex = 0;originIndex<pOrigin.length;originIndex++){
            for(String pattern:patterns){
                char[] pPattern = pattern$pPattern.get(pattern);
                int patternIndex = keyCnt.computeIfAbsent(pattern,k->0);
                if(pOrigin[originIndex] == pPattern[patternIndex]){
                    patternIndex++;
                    if(patternIndex==pPattern.length){//如果完全匹配，保存快照，计数归0
                        snapshots.add(new Snapshot(originIndex-patternIndex+1,originIndex+1,pattern));
                        keyCnt.put(pattern,0);
                    } else {//匹配中途记录位置
                        keyCnt.put(pattern,patternIndex);
                    }
                } else {
                    if(patternIndex!=0){
                        //若匹配到一半失败了，则尝试回退匹配长度
                        int[] rewindMark = (pattern$marks!=null? pattern$marks:(pattern$marks=new HashMap<>()))
                                .computeIfAbsent(
                                        pattern,k->getRewindMark(pPattern)
                                );
                        //回退后匹配字符，如果相同则记录位置
                        int rewindTo = 0;
                        int rewindIndex = patternIndex-1;
                        do {
                            rewindIndex = rewindMark[rewindIndex];
                            if(pOrigin[originIndex] == pPattern[rewindIndex+1]){
                                rewindTo = rewindIndex+2;
                                break;
                            }
                        } while (rewindIndex>=0);
                        keyCnt.put(pattern,rewindTo);
                    }
                }
            }
        }
        snapshots.sort(Snapshot::compareTo);
        return snapshots;
    }

    private static int[] getRewindMark(char[] pPattern){
        int[] pcm = getPrefixCntMark(pPattern);
        int[] rewindMark = new int[pPattern.length];
        Arrays.fill(rewindMark,-1);

        for (int i = 1; i < pcm.length; i++) {
            if(pcm[i]!=0){
                for(int j=0;j<pcm[i];j++){
                    int targetIndex = i+j;
                    rewindMark[targetIndex] = Math.max(rewindMark[targetIndex],j);
                }
            }
        }
        return rewindMark;
    }


    /**
     * 得到字符与prefix匹配的长度
     */
    private static int[] getPrefixCntMark(char[] pPattern){
        int[] mark = new int[pPattern.length];
        Arrays.fill(mark,0);
        mark[0] = pPattern.length;
        Set<Integer> topIndex = new HashSet<>();
        List<Integer> removeList = new ArrayList<>();
        for(int i=1;i<pPattern.length;i++){
            //判断topIndex中，字符是否与当前字符相同
            for (Integer index : topIndex) {
                if(pPattern[i] == pPattern[mark[index]]){
                    //增加计数
                    mark[index]++;
                } else {
                    //记录移除列表
                    removeList.add(index);
                }
            }
            if(removeList.size()>0){
                //移除不匹配数据
                removeList.forEach(topIndex::remove);
                removeList.clear();
            }
            //若当前字符与首部字符相同，则将Index计入cnt
            if(pPattern[i] == pPattern[0]){
                mark[i] = 1;
                topIndex.add(i);
            }
        }
        return mark;
    }

    @AllArgsConstructor
    @Data
    public static class Snapshot implements Comparable<Snapshot>{
        Integer start;
        Integer end;
        String pattern;

        @Override
        public int compareTo(Snapshot that) {
            if(this.start.equals(that.start)){
                return that.end-this.end;//位置一样，长度越大排名越前
            }
            return this.start-that.start;//位置越前，排名越前
        }
    }

    private static final Field stringValueField = Reflects.getField(String.class,"value");

    private static char[] constCharArray(String str){
        return (char[]) Reflects.getValue(str,stringValueField);
    }
}
