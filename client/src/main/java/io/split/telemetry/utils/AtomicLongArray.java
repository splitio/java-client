package io.split.telemetry.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class AtomicLongArray {
    private AtomicLong[] array;
    private static final int MAX_LENGTH = 23;

    private static final Logger _log = LoggerFactory.getLogger(AtomicLongArray.class);

    public AtomicLongArray(int size) {
        if(size <= 0) {
            _log.error("Invalid array size. Using default size: " + MAX_LENGTH);
            size = MAX_LENGTH;
        }
        array = new AtomicLong[size];
        IntStream.range(0, array.length).forEach(x -> array[x] = new AtomicLong());
    }

    public void increment(int index) {
        if (index < 0 || index >= array.length) {
           _log.error("Index is out of bounds. Did not incremented.");
           return;
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
