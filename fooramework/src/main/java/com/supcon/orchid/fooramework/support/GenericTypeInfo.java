package com.supcon.orchid.fooramework.support;

import com.supcon.orchid.fooramework.util.Strings;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.LinkedList;
import java.util.List;

public class GenericTypeInfo {

    @Getter
    private Class<?> type;

    @Getter
    private GenericTypeInfo[] genericParametersInfo;


    private GenericTypeInfo() {
    }

    public GenericTypeInfo getComponentTypeInfo(){
        if(type.isArray()){
            GenericTypeInfo typeInfo = new GenericTypeInfo();
            typeInfo.type = this.type.getComponentType();
            typeInfo.genericParametersInfo = this.genericParametersInfo;
            return typeInfo;
        }
        return null;
    }

    @SneakyThrows
    public static GenericTypeInfo of(String signature){
        //外层
        int idxGenericStart = signature.indexOf("<");
        boolean isArray = signature.startsWith("[");
        int endIdx = idxGenericStart!=-1?idxGenericStart:isArray?signature.length():signature.length()-1;
        int startIdx = isArray?0:1;
        String outerClassName = signature.substring(startIdx,endIdx).replace("/",".");
        GenericTypeInfo outer = new GenericTypeInfo();
        outer.type = Class.forName(outerClassName);
        if(idxGenericStart!=-1){
            //内层
            String innerPattern = signature.substring(idxGenericStart+1,signature.length()-2);
            List<Pair<Integer,Integer>> innerPairIdx = Strings.indexOfPairs(innerPattern,Pair.of("<",">"));
            String[] innerClassesSignature;
            if(innerPairIdx.isEmpty()){
                //直接通过;分割 如 Laa;[Lbb;
                innerClassesSignature = innerPattern.split("(?<=;)");
            } else {
                //跳过尖括号后再分割 如 Laa<cc;dd;>;Lbb;
                List<String> innerClassesSignatureList = new LinkedList<>();
                int start_idx = 0;
                int cursor_idx = 0;
                for (int i = 0; i < innerPairIdx.size() + 1; i++) {
                    int start_cur,end_cur;
                    if(i!=innerPairIdx.size()){
                        Pair<Integer, Integer> pairIdx = innerPairIdx.get(i);
                        start_cur = pairIdx.getFirst();
                        end_cur = pairIdx.getSecond();
                    } else {
                        start_cur = innerPattern.length();
                        end_cur = innerPattern.length();
                    }
                    int nxt_split = innerPattern.indexOf(";",cursor_idx);
                    if(nxt_split<start_cur||nxt_split>=end_cur){
                        cursor_idx = nxt_split+1;
                        innerClassesSignatureList.add(innerPattern.substring(start_idx,cursor_idx));
                        start_idx = cursor_idx;
                    } else {
                        cursor_idx = end_cur;
                    }
                }
                innerClassesSignature = innerClassesSignatureList.toArray(new String[0]);
            }
            outer.genericParametersInfo = new GenericTypeInfo[innerClassesSignature.length];
            for (int i = 0; i < innerClassesSignature.length; i++) {
                //嵌套生成
                outer.genericParametersInfo[i] = of(innerClassesSignature[i]);
            }
        }
        return outer;
    }

    public static GenericTypeInfo of(Class<?> genericType){
        GenericTypeInfo info = new GenericTypeInfo();
        info.type = genericType;
        return info;
    }
}
