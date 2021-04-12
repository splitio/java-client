package io.split.telemetry.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class AtomicLongArray {
    private AtomicLong[] array;

    public AtomicLongArray(int size) throws Exception {
        if(size == 0) {
            throw new Exception("Invalid array size");
        }
        array = new AtomicLong[size];
        IntStream.range(0, array.length).forEach(x -> array[x] = new AtomicLong());
    }

    public void increment(int index) {
        if (index < 0 || index >= array.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        array[index].getAndIncrement();
    }

    public List<Long> fetchAndClearAll() {
        List<Long> listValues = new ArrayList<>();
        for (AtomicLong a: array) {
            listValues.add(a.longValue());
        }

        IntStream.range(0, array.length).forEach(x -> array[x] = new AtomicLong());

        return listValues;
    }
}
