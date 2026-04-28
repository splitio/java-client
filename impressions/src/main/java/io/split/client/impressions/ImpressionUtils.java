package io.split.client.impressions;

public class ImpressionUtils {

    private static final long TIME_INTERVAL_MS = 3600L * 1000L;

    public static long truncateTimeframe(long timestampInMs) {
        return timestampInMs - (timestampInMs % TIME_INTERVAL_MS);
    }
}
