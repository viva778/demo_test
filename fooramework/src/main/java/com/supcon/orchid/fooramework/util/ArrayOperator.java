package com.supcon.orchid.fooramework.util;

import com.supcon.orchid.fooramework.support.Converter;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class ArrayOperator<T>{
    public static <T> ArrayOperator<T> of(T[] array){
        return new ArrayOperator<>(array);
    }

    private final Class<T> clazz;

    private T[] array;

    private int length;

    private boolean temp_array;

    private ArrayOperator(T[] array){
        if(array!=null){
            this.clazz = (Class<T>) array.getClass().getComponentType();
            this.array = array;
            this.length = array.length;
        } else {
            this.clazz = null;
            this.array = null;
            this.length = 0;
        }
    }

    public ArrayOperator<T> filter(Predicate<? super T> predicate){
        T[] array = this.temp_array?this.array:(T[])Array.newInstance(this.clazz,this.length);
        int length = 0;
        for (int i = 0; i < this.length; i++) {
            T val = this.array[i];
            if(predicate.test(val)){
                array[length++]=val;
            }
        }
        this.array = array;
        this.length = length;
        this.temp_array = true;
        return this;
    }


    public ArrayOperator<T> removeLast(Predicate<? super T> predicate){
        int lastIdx = -1;
        for (int i = this.length-1; i >=0; i--) {
            T val = this.array[i];
            if(predicate.test(val)){
                lastIdx=i;
                break;
            }
        }
        if(lastIdx!=-1){
            if(this.temp_array){
                System.arraycopy(this.array,lastIdx+1,this.array,lastIdx,this.length-lastIdx-1);
            } else {
                T[] array = (T[])Array.newInstance(this.clazz,this.length);
                System.arraycopy(this.array,0,array,0,lastIdx);
                System.arraycopy(this.array,lastIdx+1,array,lastIdx,this.length-lastIdx-1);
                this.array = array;
                this.temp_array = true;
            }
            this.length--;
        }
        return this;
    }

    public boolean equals(T[] array){
        if(array==null||array.length!=this.length){
            return false;
        }
        for (int i = 0; i < array.length; i++) {
            if(!Objects.equals(array[i],this.array[i])){
                return false;
            }
        }
        return true;
    }

    public ArrayOperator<T> concat(T[] array){
        T[] concat_array = (T[])Array.newInstance(this.clazz,this.length+array.length);
        System.arraycopy(this.array,0,concat_array,0,this.length);
        System.arraycopy(array,0,concat_array,this.length,array.length);
        this.array = concat_array;
        this.length = concat_array.length;
        this.temp_array = true;
        return this;
    }

    public ArrayOperator<T> concat(T element){
        if(this.length<this.array.length){
            this.array[this.length++]=element;
        } else {
            T[] concat_array = (T[])Array.newInstance(this.clazz,this.length+1);
            System.arraycopy(this.array,0,concat_array,0,this.length);
            this.array = concat_array;
            this.array[this.length++]=element;
            this.temp_array = true;
        }
        return this;
    }

    /**
     * 元素必须要实现Comparable接口，否则会报类转换异常
     */
    public ArrayOperator<T> sorted(){
        Arrays.sort(array,(v1,v2)-> ((Comparable<T>)v1).compareTo(v2));
        return this;
    }

    public ArrayOperator<T> sorted(Comparator<? super T> comparator){
        Arrays.sort(array,comparator);
        return this;
    }

    public <R> ArrayOperator<R> map_to(Class<R> basic_type){
        R[] array = (R[])Array.newInstance(basic_type,this.length);
        Function<? super T, ? extends R> mapper = Converter.getConverter(basic_type);
        for (int i = 0; i < this.length; i++) {
            T val = this.array[i];
            array[i]=mapper.apply(val);
        }
        ArrayOperator<R> operator = new ArrayOperator<>(array);
        operator.temp_array = true;
        return operator;
    }

    public <R> ArrayOperator<R> map(Class<R> toClass, Function<? super T, ? extends R> mapper){
        Object[] array = (Object[]) Array.newInstance(toClass,this.length);
        for (int i = 0; i < this.length; i++) {
            T val = this.array[i];
            array[i]=mapper.apply(val);
        }
        ArrayOperator<R> operator = new ArrayOperator<>((R[]) array);
        operator.temp_array = true;
        return operator;
    }

    public ArrayOperator<T> sub(int pos,int length){
        if(this.length<pos+length){
            throw new ArrayIndexOutOfBoundsException(pos+length);
        }
        T[] array = (T[])Array.newInstance(clazz,length);
        System.arraycopy(this.array,pos,array,0,length);
        this.array = array;
        this.length = length;
        this.temp_array = true;
        return this;
    }

    public void forEach(Consumer<T> consumer){
        for (int i = 0; i < this.length; i++) {
            consumer.accept(this.array[i]);
        }
    }

    public T[] get(){
        if(this.array.length!=this.length){
            T[] array = (T[])Array.newInstance(this.clazz,this.length);
            System.arraycopy(this.array,0,array,0,this.length);
            return array;
        } else {
            return this.array;
        }
    }

    public Stream<T> stream() {
        return Stream.of(get());
    }

    public T get(int idx){
        if(idx>=0&&idx<this.length){
            return this.array[idx];
        } else {
            return null;
        }
    }
}