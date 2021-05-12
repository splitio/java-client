package io.split.telemetry.utils;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

public class AtomicLongArrayTest {

    private static final int SIZE = 23;

    @Test
    public void testAtomicLong() {
        AtomicLongArray atomicLongArray = new AtomicLongArray(SIZE);
        Assert.assertNotNull(atomicLongArray);
    }

    @Test
    public void testArraySizeError() {
        AtomicLongArray atomicLongArray = new AtomicLongArray(0);
        Logger log = Mockito.mock(Logger.class);
        atomicLongArray.increment(2);
        Assert.assertEquals(1, atomicLongArray.fetchAndClearAll().stream().mapToInt(Long::intValue).sum());
    }

    @Test
    public void testIncrement() {
        AtomicLongArray atomicLongArray = new AtomicLongArray(SIZE);
        atomicLongArray.increment(2);
        Assert.assertEquals(1, atomicLongArray.fetchAndClearAll().stream().mapToInt(Long::intValue).sum());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testIncrementError() {
        AtomicLongArray atomicLongArray = new AtomicLongArray(SIZE);
        atomicLongArray.increment(25);
    }

}