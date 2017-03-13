package io.split.client.metrics;

import java.util.Arrays;

/**
 * Tracks latencies pero bucket of time.
 * Each bucket represent a latency greater than the one before
 * and each number within each bucket is a number of calls in the range.
 * <p/>
 * (1)  1.00
 * (2)  1.50
 * (3)  2.25
 * (4)  3.38
 * (5)  5.06
 * (6)  7.59
 * (7)  11.39
 * (8)  17.09
 * (9)  25.63
 * (10) 38.44
 * (11) 57.67
 * (12) 86.50
 * (13) 129.75
 * (14) 194.62
 * (15) 291.93
 * (16) 437.89
 * (17) 656.84
 * (18) 985.26
 * (19) 1,477.89
 * (20) 2,216.84
 * (21) 3,325.26
 * (22) 4,987.89
 * (23) 7,481.83
 * <p/>
 * Thread-safety: This class is not thread safe.
 * <p/>
 * Created by patricioe on 2/10/16.
 */
public class BinarySearchLatencyTracker implements ILatencyTracker {

    static final long[] BUCKETS = {
            1000, 1500, 2250, 3375, 5063,
            7594, 11391, 17086, 25629, 38443,
            57665, 86498, 129746, 194620, 291929,
            437894, 656841, 985261, 1477892, 2216838,
            3325257, 4987885, 7481828
    };

    static final long MAX_LATENCY = 7481828;

    long[] latencies = new long[BUCKETS.length];

    /**
     * Increment the internal counter for the bucket this latency falls into.
     *
     * @param millis
     */
    public void addLatencyMillis(long millis) {
        int index = findIndex(millis * 1000);
        latencies[index]++;
    }

    /**
     * Increment the internal counter for the bucket this latency falls into.
     *
     * @param micros
     */
    public void addLatencyMicros(long micros) {
        int index = findIndex(micros);
        latencies[index]++;
    }

    /**
     * Returns the list of latencies buckets as an array.
     *
     * @return the list of latencies buckets as an array.
     */
    public long[] getLatencies() {
        return latencies;
    }

    @Override
    public long getLatency(int index) {
        return latencies[index];
    }

    public void clear() {
        latencies = new long[BUCKETS.length];
    }

    /**
     * Returns the counts in the bucket this latency falls into.
     * The latencies will no be updated.
     *
     * @param latency
     * @return the bucket content for the latency.
     */
    public long getBucketForLatencyMillis(long latency) {
        return latencies[findIndex(latency * 1000)];
    }

    /**
     * Returns the counts in the bucket this latency falls into.
     * The latencies will no be updated.
     *
     * @param latency
     * @return the bucket content for the latency.
     */
    public long getBucketForLatencyMicros(long latency) {
        return latencies[findIndex(latency)];
    }


    private int findIndex(long micros) {
        if (micros > MAX_LATENCY) {
            return BUCKETS.length - 1;
        }

        int index = Arrays.binarySearch(BUCKETS, micros);

        if (index < 0) {

            // Adjust the index based on Java Array javadocs. <0 means the value wasn't found and it's module value
            // is where it should be inserted (in this case, it means the counter it applies - unless it's equals to the
            // length of the array).

            index = -(index + 1);
        }
        return index;
    }

}
