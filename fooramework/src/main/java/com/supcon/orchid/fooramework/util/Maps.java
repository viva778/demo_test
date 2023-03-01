package com.supcon.orchid.fooramework.util;


import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.data.util.CastUtils.cast;

public class Maps {
    @SuppressWarnings("unchecked")
    public static <K, V>  Map<K,V> immutable(Object... keyValuePairs){
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        for(int i=1;i<keyValuePairs.length;i+=2){
            if(keyValuePairs[i]!=null&&keyValuePairs[i-1]!=null){
                builder.put((K) keyValuePairs[i-1],(V) keyValuePairs[i]);
            }
        }
        return builder.build();
    }

    /**
     * 反转
     */
    public static <K, V>  Map<V,K> inverse(Map<K,V> map){
        return map.entrySet().stream().filter(entry->entry.getValue()!=null).collect(Collectors.toMap(
                Map.Entry::getValue,
                Map.Entry::getKey
        ));
    }

    public static <V> Map<String,V> toLowerCaseKey(Map<String,V> map){
        return map.entrySet().stream().collect(Collectors.toMap(
                entry->entry.getKey().toLowerCase(),
                Map.Entry::getValue
        ));
    }

    public static <V> Map<String,V> toUpperCaseKey(Map<String,V> map){
        return map.entrySet().stream().collect(Collectors.toMap(
                entry->entry.getKey().toUpperCase(),
                Map.Entry::getValue
        ));
    }

    @SuppressWarnings("unchecked")
    public static <X> X getByRegex(Map<String,?> map,String regex){
        return (X) map
                .entrySet()
                .stream()
                .filter(entry->entry.getKey().matches(regex))
                .findAny()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    public static String getKeyByRegex(Map<String,?> map,String regex){
        return map
                .keySet()
                .stream()
                .filter(key->key.matches(regex))
                .findAny()
                .orElse(null);
    }

    public static <X> X getValueSplitByDot(Map<String,?> map,String key){
        String[] keys = key.split("\\.");
        Map<String,?> innerMap = map;
        for(int i=0;i<keys.length-1;i++){
            innerMap = cast(innerMap.get(keys[i]));
            if(innerMap==null){
                return null;
            }
        }
        return cast(innerMap.get(keys[keys.length-1]));
    }

    public static void setValueSplitByDot(Map<String,?> map,String key,Object value){
        String[] keys = key.split("\\.");
        Map<String,?> innerMap = map;
        for(int i=0;i<keys.length-1;i++){
            Map<String,?> tmpMap = cast(innerMap.get(keys[i]));
            if(tmpMap==null){
                tmpMap = new HashMap<>();
                innerMap.put(keys[i],cast(tmpMap));
            }
            innerMap = tmpMap;
        }
        innerMap.put(keys[keys.length-1],cast(value));
    }


    /**
     * 获取交集
     */
    public static <K, V>  Map<K,V> intersect(Map<K,V> mapA,Map<K,V> mapB){
        Map<K,V> intersection = new HashMap<>(mapA);
        intersection.entrySet().removeIf(entry->!mapB.containsKey(entry.getKey()));
        return intersection;
    }



}
