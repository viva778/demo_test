package com.supcon.orchid.fooramework.util;


import com.supcon.orchid.fooramework.support.Pair;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Elements {


    /**
     * 分流
     *  将集合根据谓语分成成立和不成立两组
     */
    public static <T, A, R> Pair<R,R> shunt(Collection<T> collection, Predicate<T> predicate, Collector<? super T, A, R> collector){
        return Pair.of(
                collection.stream().filter(predicate).collect(collector),
                collection.stream().filter(predicate.negate()).collect(collector)
        );
    }


    /**
     * 取子数组
     * @param array 数组
     * @param start 起始下标
     * @param end 结束下标
     */
    @SuppressWarnings("unchecked")
    public static <A> A subArray(A array,int start,int end){
        A subArray = (A) Array.newInstance(array.getClass().getComponentType(),end-start);
        System.arraycopy(array,start,subArray,0,end-start);
        return subArray;
    }

    /**
     * 取重复元素
     * @param iterable iterable
     */
    public static <T> Set<T> repeat(Iterable<T> iterable){
        return _Repeat((Consumer<Consumer<? super T>>)iterable::forEach);
    }

    /**
     * 取重复元素
     * @param stream stream
     */
    public static <T> Set<T> repeat(Stream<T> stream){
        return _Repeat((Consumer<Consumer<? super T>>)stream::forEach);
    }

    /**
     * 取重复元素
     * @param array array
     */
    public static <T> Set<T> repeat(T[] array){
        return _Repeat(consumer -> {
            for (T item : array) {
                consumer.accept(item);
            }
        });
    }

    private static <T> Set<T> _Repeat(Consumer<Consumer<? super T>> forEach){
        Set<T> repeat = new LinkedHashSet<>();
        Set<T> exist = new HashSet<>();
        forEach.accept(item->{
            if(!exist.add(item)){
                repeat.add(item);
            }
        });
        return repeat;
    }

    /**
     * 取交集
     */
    public static <X> Set<X> intersect(Collection<X> s1, Collection<X> s2){
        Set<X> intersection = new HashSet<>(s1);
        intersection.removeIf(v->!s2.contains(v));
        return intersection;
    }

    /**
     * 取交集
     */
    public static <X> Set<X> intersect(X[] s1, X[] s2){
        Set<X> intersection = Stream.of(s1).collect(Collectors.toSet());
        Set<X> s2Set = Stream.of(s2).collect(Collectors.toSet());
        intersection.removeIf(v->!s2Set.contains(v));
        return intersection;
    }

    /**
     * 取差集
     */
    public static <X> Set<X> difference(Collection<X> s1, Collection<X> s2){
        Set<X> difference = new HashSet<>(s1);
        difference.removeAll(s2);
        return difference;
    }

    /**
     * 取差集
     */
    public static <X> Set<X> difference(X[] s1, X[] s2){
        Set<X> difference = Stream.of(s1).collect(Collectors.toSet());
        for (X x : s2) {
            difference.remove(x);
        }
        return difference;
    }


    /**
     * 转数组（将其余数组集合对象平面化
     * @param generator 数组生成器
     * @param converter 类型转换器
     * @param items 转换元素
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(IntFunction<T[]> generator, Function<Object,T> converter, Object... items){
        Object[] temp_arr = new Object[items.length];
        int total_length = 0;
        for (int i = 0; i < items.length; i++) {
            Object item = items[i];
            if(item instanceof Collection){
                temp_arr[i] = ((Collection<?>)item).toArray();
            } else if(item instanceof Stream){
                temp_arr[i] = ((Stream<?>)item).toArray();
            } else {
                temp_arr[i] = item;
            }
            if(temp_arr[i]!=null&&temp_arr[i].getClass().isArray()){
                total_length += ((Object[])temp_arr[i]).length;
            } else {
                total_length += 1;
            }
        }
        T[] result = generator.apply(total_length);
        int copy_cursor = 0;
        for (Object item : temp_arr) {
            if(item!=null&&item.getClass().isArray()){
                Object[] arr = (Object[]) item;
                if(arr.length>0){
                    if(converter!=null){
                        for (Object ele : arr) {
                            result[copy_cursor++] = converter.apply(ele);
                        }
                    } else {
                        System.arraycopy((T[])arr,0,result,copy_cursor,arr.length);
                        copy_cursor += arr.length;
                    }
                }
            } else {
                result[copy_cursor++] = converter!=null?converter.apply(item):(T) item;
            }
        }
        return result;
    }

    /**
     * 转数组（将其余数组集合对象平面化
     * @param generator 数组生成器
     * @param items 转换元素
     */
    public static <T> T[] toArray(IntFunction<T[]> generator, Object... items){
        return toArray(generator,null,items);
    }

    /**
     * 转数组（将其余数组集合对象平面化
     */
    public static Object[] toArray(Object... items){
        return toArray(Object[]::new,items);
    }


}
