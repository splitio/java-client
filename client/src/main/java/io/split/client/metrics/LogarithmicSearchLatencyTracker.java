package io.split.client.metrics;

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
public class LogarithmicSearchLatencyTracker implements ILatencyTracker {

    static final int BUCKETS = 23;
    private static final double LOG_10_1000_MICROS = Math.log10(1000);
    private static final double LOG_10_1_5_MICROS = Math.log10(Double.valueOf("1.5").doubleValue());


    long[] latencies = new long[BUCKETS];

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
        latencies = new long[BUCKETS];
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

        if (micros <= 1000) return 0;
        if (micros > 4987885) return 22;

        double raw = (Math.log10(micros) - LOG_10_1000_MICROS) / LOG_10_1_5_MICROS;
        double rounded = Math.round(raw * 1000000d) / 1000000d;
        return (int) Math.ceil(rounded);
    }

}
