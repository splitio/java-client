package io.split.telemetry.utils;

import java.util.Arrays;

/**
 * Calculates buckets from latency
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
 */
public class BucketCalculator {

    static final long[] BUCKETS = {
            1000, 1500, 2250, 3375, 5063,
            7594, 11391, 17086, 25629, 38443,
            57665, 86498, 129746, 194620, 291929,
            437894, 656841, 985261, 1477892, 2216838,
            3325257, 4987885, 7481828
    };

    static final long MAX_LATENCY = 7481828;

    public static int getBucketForLatencyMillis(long latency) {
        long micros = latency * 1000;
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
