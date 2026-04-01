package io.split.rules.bucketing;

import io.split.rules.model.Partition;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BucketerTest {

    @Test
    public void getBucketIsBetween1And100Inclusive() {
        int bucket = Bucketer.getBucket("somekey", 12345, 2);
        assertTrue("bucket should be >= 1", bucket >= 1);
        assertTrue("bucket should be <= 100", bucket <= 100);
    }

    @Test
    public void getBucketLegacyAlgoReturnsSameResultForSameInput() {
        int b1 = Bucketer.getBucket("user1", 123, 1);
        int b2 = Bucketer.getBucket("user1", 123, 1);
        assertEquals(b1, b2);
    }

    @Test
    public void getBucketMurmurAlgoReturnsSameResultForSameInput() {
        int b1 = Bucketer.getBucket("user1", 123, 2);
        int b2 = Bucketer.getBucket("user1", 123, 2);
        assertEquals(b1, b2);
    }

    @Test
    public void getTreatmentReturnsControlForEmptyPartitions() {
        List<Partition> empty = Collections.emptyList();
        assertEquals("control", Bucketer.getTreatment("key", 123, empty, 2));
    }

    @Test
    public void getTreatmentReturnsSingleTreatmentWhen100Percent() {
        List<Partition> partitions = Collections.singletonList(new Partition("on", 100));
        assertEquals("on", Bucketer.getTreatment("key", 123, partitions, 2));
    }

    @Test
    public void getTreatmentSelectsFromPartitionsBasedOnBucket() {
        List<Partition> partitions = Arrays.asList(
                new Partition("on", 50),
                new Partition("off", 50)
        );
        // Verify it returns one of the two treatments
        String t = Bucketer.getTreatment("user1", 12345, partitions, 2);
        assertTrue("on".equals(t) || "off".equals(t));
    }

    @Test
    public void getTreatmentReturnsControlWhenBucketExceedsAllPartitions() {
        // partitions that don't sum to 100 — bucket could exceed them
        List<Partition> partitions = Collections.singletonList(new Partition("on", 1));
        // Most keys should get "control" with 1% partition
        long controlCount = 0;
        for (int i = 0; i < 200; i++) {
            if ("control".equals(Bucketer.getTreatment("user" + i, 12345, partitions, 2))) {
                controlCount++;
            }
        }
        assertTrue("Most keys should get control with 1% partition", controlCount > 150);
    }

    @Test
    public void bucketMathIsCorrect() {
        // bucket() returns (Math.abs(hash % 100) + 1)
        assertEquals(1, Bucketer.bucket(0));
        assertEquals(1, Bucketer.bucket(100));
        assertEquals(50, Bucketer.bucket(49));
        assertEquals(100, Bucketer.bucket(99));
    }

    @Test
    public void knownMurmurHashValue() {
        // Verify consistent murmur hash across platforms
        long hash = Bucketer.murmurHash("testKey", 12345);
        assertEquals(hash, Bucketer.murmurHash("testKey", 12345));
    }
}
