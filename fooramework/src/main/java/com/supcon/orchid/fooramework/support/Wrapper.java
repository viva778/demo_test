package com.supcon.orchid.fooramework.support;

public class Wrapper<T> {
    private T value;

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public Wrapper(T value) {
        this.value = value;
    }

    public Wrapper() {
    }
}
