package com.supcon.orchid.fooramework.support;


import lombok.Data;

import java.util.Objects;
import java.util.function.BiConsumer;

@Data
public class Pair<F,S> {
    private F first;
    private S second;

    public static <F,S> Pair<F,S> of(F first, S second){
        return new Pair<>(first,second);
    }

    private Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public void consume(BiConsumer<F,S> consumer){
        consumer.accept(first,second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}
