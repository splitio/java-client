package io.split.client.impressions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ImpressionCounter {

    private static final long TIME_INTERVAL_MS = 3600 * 1000;

    private final ConcurrentHashMap<String, AtomicInteger> _counts;

    public ImpressionCounter() {
        _counts = new ConcurrentHashMap<>();
    }

    public void inc(String featureName, long timeFrame, int amount) {
        String key = makeKey(featureName, timeFrame);
        AtomicInteger count = _counts.get(key);
        if (Objects.isNull(count)) {
            count = new AtomicInteger();
            AtomicInteger old = _counts.putIfAbsent(key, count);
            if (!Objects.isNull(old)) { // Some other thread won the race, use that AtomicInteger instead
                count = old;
            }
        }
        count.addAndGet(amount);
    }

    public HashMap<String, Integer> popAll() {
        HashMap<String, Integer> toReturn = new HashMap<>();
        for (String key : _counts.keySet()) {
            AtomicInteger curr = _counts.remove(key);
            toReturn.put(key ,curr.get());
        }
        return toReturn;
    }

    static String makeKey(String featureName, long timeFrame) {
        return String.join("::", featureName, String.valueOf(truncateTimeframe(timeFrame)));
    }

    static long truncateTimeframe(long timestampInMs) {
        return timestampInMs - (timestampInMs % TIME_INTERVAL_MS);
    }
}
