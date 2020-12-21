package io.split.engine.segments;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.cache.SegmentCache;
import io.split.engine.SDKReadinessGates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class SegmentSynchronizationTaskMauro implements Runnable {
    private static final Logger _log = LoggerFactory.getLogger(SegmentSynchronizationTaskMauro.class);

    private final SegmentChangeFetcher _segmentChangeFetcher;
    private final AtomicLong _refreshEveryNSeconds;
    private final AtomicBoolean _running;
    private final Object _lock = new Object();
    private final ConcurrentMap<String, SegmentFetcherImpMauro> _segmentFetchers = Maps.newConcurrentMap();
    private final SegmentCache _segmentCache;
    private final SDKReadinessGates _gates;
    private final ScheduledExecutorService _scheduledExecutorService;

    private ScheduledFuture<?> _scheduledFuture;

    public SegmentSynchronizationTaskMauro(SegmentChangeFetcher segmentChangeFetcher, long refreshEveryNSeconds, int numThreads, SDKReadinessGates gates, SegmentCache segmentCache) {
        _segmentChangeFetcher = segmentChangeFetcher;
        checkNotNull(_segmentChangeFetcher);

        checkArgument(refreshEveryNSeconds >= 0L);
        _refreshEveryNSeconds = new AtomicLong(refreshEveryNSeconds);

        _gates = gates;
        checkNotNull(_gates);

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("split-segmentFetcher-" + "%d")
                .build();

        _scheduledExecutorService = Executors.newScheduledThreadPool(numThreads, threadFactory);

        _running = new AtomicBoolean(false);

        _segmentCache = segmentCache;
    }

    @Override
    public void run() {
        for (ConcurrentMap.Entry<String, SegmentFetcherImpMauro> entry : _segmentFetchers.entrySet()) {
            SegmentFetcherImpMauro fetcher = entry.getValue();

            if (fetcher == null) {
                continue;
            }

            _scheduledExecutorService.submit(fetcher);
        }
    }

    public void initializeSegment(String segmentName) {
        SegmentFetcherImpMauro segment = _segmentFetchers.get(segmentName);
        if (segment != null) {
            return;
        }

        // we are locking here since we wanna make sure that we create only ONE RefreableSegmentFetcher
        // per segment.
        synchronized (_lock) {
            // double check
            segment = _segmentFetchers.get(segmentName);
            if (segment != null) {
                return;
            }

            try {
                _gates.registerSegment(segmentName);
            } catch (InterruptedException e) {
                _log.error("Unable to register segment " + segmentName);
            }

            segment = new SegmentFetcherImpMauro(segmentName, _segmentChangeFetcher, _gates, _segmentCache);

            if (_running.get()) {
                _scheduledExecutorService.submit(segment);
            }

            _segmentFetchers.putIfAbsent(segmentName, segment);
        }
    }

    public SegmentFetcherImpMauro getFetcher(String segmentName) {
        initializeSegment(segmentName);

        return _segmentFetchers.get(segmentName);
    }

    public void startPeriodicFetching() {
        if (_running.getAndSet(true)) {
            _log.warn("Segments PeriodicFetching is running...");
            return;
        }

        _log.debug("Starting PeriodicFetching Segments ...");
        _scheduledFuture = _scheduledExecutorService.scheduleWithFixedDelay(this, 0L, _refreshEveryNSeconds.get(), TimeUnit.SECONDS);
    }

    public void stop() {
        if (!_running.getAndSet(false) || _scheduledFuture == null) {
            _log.warn("Segments PeriodicFetching not running...");
            return;
        }

        _scheduledFuture.cancel(false);
        _log.debug("Stopped PeriodicFetching Segments ...");
    }

    public void close() {
        if (_scheduledExecutorService == null || _scheduledExecutorService.isShutdown()) {
            return;
        }
        _scheduledExecutorService.shutdown();
        try {
            if (!_scheduledExecutorService.awaitTermination(2L, TimeUnit.SECONDS)) { //optional *
                _log.info("Executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = _scheduledExecutorService.shutdownNow(); //optional **
                _log.info("Executor was abruptly shut down. These tasks will not be executed: " + droppedTasks);
            }
        } catch (InterruptedException e) {
            // reset the interrupt.
            _log.error("Shutdown of SegmentFetchers was interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
