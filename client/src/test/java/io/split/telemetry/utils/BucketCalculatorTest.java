package io.split.telemetry.utils;

import org.junit.Assert;
import org.junit.Test;

public class BucketCalculatorTest{

    @Test
    public void testBucketCalculator() {
        int bucket = BucketCalculator.getBucketForLatency(500l * 1000);
        Assert.assertEquals(0, bucket);

        bucket = BucketCalculator.getBucketForLatency(1500l * 1000);
        Assert.assertEquals(1, bucket);

        bucket = BucketCalculator.getBucketForLatency(8000l * 1000);
        Assert.assertEquals(6, bucket);

        bucket = BucketCalculator.getBucketForLatency(7481829l * 1000);
        Assert.assertEquals(22, bucket);
    }
}
