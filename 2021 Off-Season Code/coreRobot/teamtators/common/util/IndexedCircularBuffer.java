package org.teamtators.common.util;

public class IndexedCircularBuffer<T> {
    private Object[] buf;
    private int capacity;
    private int idx = 0;
    public IndexedCircularBuffer(int capacity) {
        this.capacity = capacity;
        buf = new Object[capacity];
    }

    public Object get(int index) {
        return buf[index];
    }

    public int push(T obj) {
        buf[idx] = obj;
        int a = idx++;
        if(idx >= capacity)
            idx = 0;
        return a;
    }
}
