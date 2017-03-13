
package io.split.client.metrics;

import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;
import io.split.client.dtos.Counter;
import io.split.client.dtos.Latency;
import io.split.engine.metrics.Metrics;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * Created by adilaijaz on 9/4/15.
 */
public class CachedMetrics implements Metrics {

    private final DTOMetrics _metrics;

    private final Map<String, ILatencyTracker> _latencyMap;
    private final Map<String, SumAndCount> _countMap;


    private final Object _latencyLock = new Object();
    private AtomicLong _latencyLastUpdateTimeMillis = new AtomicLong(System.currentTimeMillis());

    private final Object _counterLock = new Object();
    private AtomicLong _counterLastUpdateTimeMillis = new AtomicLong(System.currentTimeMillis());

    private long _refreshPeriodInMillis;

    private final int _queueForTheseManyCalls;

    /**
     * For unit testing only.
     *
     * @param httpMetrics
     * @param queueForTheseManyCalls
     */
    /*package private*/ CachedMetrics(DTOMetrics httpMetrics, int queueForTheseManyCalls) {
        this(httpMetrics, queueForTheseManyCalls, TimeUnit.MINUTES.toMillis(1));
    }

    public CachedMetrics(DTOMetrics httpMetrics, long refreshPeriodInMillis) {
        this(httpMetrics, 100, refreshPeriodInMillis);
    }

    private CachedMetrics(DTOMetrics metrics, int queueForTheseManyCalls, long refreshPeriodInMillis) {
        _metrics = metrics;
        _latencyMap = Maps.newHashMap();
        _countMap = Maps.newHashMap();
        checkArgument(queueForTheseManyCalls > 0, "queue for cache should be greater than zero");
        _queueForTheseManyCalls = queueForTheseManyCalls;
        _refreshPeriodInMillis = refreshPeriodInMillis;
    }

    @Override
    public void count(String counter, long delta) {
        if (delta <= 0) {
            return;
        }

        if (counter == null || counter.trim().isEmpty()) {
            return;
        }

        synchronized (_counterLock) {
            SumAndCount sumAndCount = _countMap.get(counter);
            if (sumAndCount == null) {
                sumAndCount = new SumAndCount();
                _countMap.put(counter, sumAndCount);
            }

            sumAndCount.addDelta(delta);

            if (sumAndCount._count >= _queueForTheseManyCalls || hasTimeElapsed(_counterLastUpdateTimeMillis)) {
                Counter dto = new Counter();
                dto.name = counter;
                dto.delta = sumAndCount._sum;

                sumAndCount.clear();
                _counterLastUpdateTimeMillis.set(System.currentTimeMillis());
                _metrics.count(dto);
            }
        }
    }

    private boolean hasTimeElapsed(AtomicLong lastRefreshTime) {
        return (System.currentTimeMillis() - lastRefreshTime.get()) > _refreshPeriodInMillis;
    }

    @Override
    public void time(String operation, long timeInMs) {
        if (operation == null || operation.trim().isEmpty() || timeInMs < 0L) {
            // error
            return;
        }
        synchronized (_latencyLock) {
            if (!_latencyMap.containsKey(operation)) {
                ILatencyTracker latencies = new BinarySearchLatencyTracker();
                _latencyMap.put(operation, latencies);
            }

            ILatencyTracker tracker = _latencyMap.get(operation);
            tracker.addLatencyMillis((int) timeInMs);

            if (hasTimeElapsed(_latencyLastUpdateTimeMillis)) {

                Latency dto = new Latency();
                dto.name = operation;
                dto.latencies = Longs.asList(tracker.getLatencies());

                tracker.clear();
                _latencyLastUpdateTimeMillis.set(System.currentTimeMillis());
                _metrics.time(dto);

            }
        }
    }


    private static final class SumAndCount {
        private int _count = 0;
        private long _sum = 0L;

        public void addDelta(long delta) {
            _count++;
            _sum += delta;
        }

        public void clear() {
            _count = 0;
            _sum = 0L;
        }

    }

}
