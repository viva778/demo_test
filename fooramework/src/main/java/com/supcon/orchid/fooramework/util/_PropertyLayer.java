package com.supcon.orchid.fooramework.util;

import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.support.Wrapper;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class _PropertyLayer {
    /**
     * 获取层级信息，返回直接属性和下级属性列表
     *  例：
     *  -->[a,b,c,d.a,d.b,e.a]
     *  <--Pair.of(
     *      [a,b,c],
     *      {d:[a,b],e:[a]}
     *     )
     * @param includes 包含属性
     * @return Pair<直接属性,下级属性列表>
     */
    static Pair<String[], Map<String, List<String>>> info(String[] includes){
        Wrapper<Map<String, List<String>>> wInner = new Wrapper<>();
        String[] remain = Stream.of(includes).filter(include->{
            int iDot = include.indexOf(".");
            if(iDot==-1){
                return true;
            } else {
                if(wInner.get()==null){
                    wInner.set(new LinkedHashMap<>());
                }
                Map<String, List<String>> remain_map = wInner.get();
                String propertyName = include.substring(0,iDot);
                remain_map.computeIfAbsent(propertyName,k->new LinkedList<>()).add(include.substring(iDot+1));
                return false;
            }
        }).toArray(String[]::new);
        return Pair.of(remain,wInner.get());
    }
}
