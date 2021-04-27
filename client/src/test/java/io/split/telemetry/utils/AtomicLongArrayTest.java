package io.split.telemetry.utils;

import org.junit.Assert;
import org.junit.Test;

public class AtomicLongArrayTest {

    private static final int SIZE = 23;

    @Test
    public void testAtomicLong() throws Exception {
        AtomicLongArray atomicLongArray = new AtomicLongArray(SIZE);
        Assert.assertNotNull(atomicLongArray);
    }

    @Test
    public void testArraySizeError() {
        Exception exception = Assert.assertThrows(Exception.class, () -> {
            AtomicLongArray atomicLongArray = new AtomicLongArray(0);
        });
        String messageExpected = "Invalid array size";
        Assert.assertEquals(messageExpected, exception.getMessage());
    }

    @Test
    public void testIncrement() throws Exception {
        AtomicLongArray atomicLongArray = new AtomicLongArray(SIZE);
        atomicLongArray.increment(2);
        Assert.assertEquals(1, atomicLongArray.fetchAndClearAll().stream().mapToInt(Long::intValue).sum());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testIncrementError() throws Exception {
        AtomicLongArray atomicLongArray = new AtomicLongArray(SIZE);
        atomicLongArray.increment(25);
    }

}