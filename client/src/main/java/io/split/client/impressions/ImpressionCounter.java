package io.split.client.impressions;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImpressionCounter {

    public static class Key {
        private final String _featureFlagName;
        private final long _timeFrame;

        public Key(String featureFlagName, long timeframe) {
            _featureFlagName = checkNotNull(featureFlagName);
            _timeFrame = timeframe;
        }

        public String featureName() { return _featureFlagName; }
        public long timeFrame() { return  _timeFrame; }

        @Override
        public int hashCode() {
            return Objects.hash(_featureFlagName, _timeFrame);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;
            return Objects.equals(_featureFlagName, key._featureFlagName) && Objects.equals(_timeFrame, key._timeFrame);
        }
    }


    private final ConcurrentHashMap<Key, AtomicInteger> _counts;

    public ImpressionCounter() {
        _counts = new ConcurrentHashMap<>();
    }

    public void inc(String featureFlagName, long timeFrame, int amount) {
        Key key = new Key(featureFlagName, ImpressionUtils.truncateTimeframe(timeFrame));
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

    public HashMap<Key, Integer> popAll() {
        HashMap<Key, Integer> toReturn = new HashMap<>();
        for (Key key : _counts.keySet()) {
            AtomicInteger curr = _counts.remove(key);
            toReturn.put(key, curr.get());
        }
        return toReturn;
    }

    public boolean isEmpty() { return _counts.isEmpty(); }
}