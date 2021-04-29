package io.split.engine.segments;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.cache.SegmentCache;
import io.split.engine.SDKReadinessGates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class SegmentSynchronizationTaskImp implements SegmentSynchronizationTask, Closeable {
    private static final Logger _log = LoggerFactory.getLogger(SegmentSynchronizationTaskImp.class);

    private final SegmentChangeFetcher _segmentChangeFetcher;
    private final AtomicLong _refreshEveryNSeconds;
    private final AtomicBoolean _running;
    private final Object _lock = new Object();
    private final ConcurrentMap<String, SegmentFetcher> _segmentFetchers = Maps.newConcurrentMap();
    private final SegmentCache _segmentCache;
    private final SDKReadinessGates _gates;
    private final ScheduledExecutorService _scheduledExecutorService;

    private ScheduledFuture<?> _scheduledFuture;

    public SegmentSynchronizationTaskImp(SegmentChangeFetcher segmentChangeFetcher, long refreshEveryNSeconds, int numThreads, SDKReadinessGates gates, SegmentCache segmentCache) {
        _segmentChangeFetcher = checkNotNull(segmentChangeFetcher);

        checkArgument(refreshEveryNSeconds >= 0L);
        _refreshEveryNSeconds = new AtomicLong(refreshEveryNSeconds);

        _gates = checkNotNull(gates);

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("split-segmentFetcher-" + "%d")
                .build();

        _scheduledExecutorService = Executors.newScheduledThreadPool(numThreads, threadFactory);

        _running = new AtomicBoolean(false);

        _segmentCache = checkNotNull(segmentCache);
    }

    @Override
    public void run() {
        try {
            _gates.waitUntilInternalReady();
        } catch (InterruptedException ex) {
            _log.debug(ex.getMessage());
        }

        this.fetchAll(false);
    }

    @Override
    public void initializeSegment(String segmentName) {
        SegmentFetcher segment = _segmentFetchers.get(segmentName);
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

            segment = new SegmentFetcherImp(segmentName, _segmentChangeFetcher, _gates, _segmentCache);

            if (_running.get()) {
                _scheduledExecutorService.submit(segment::fetchAll);
            }

            _segmentFetchers.putIfAbsent(segmentName, segment);
        }
    }

    @Override
    public SegmentFetcher getFetcher(String segmentName) {
        initializeSegment(segmentName);

        return _segmentFetchers.get(segmentName);
    }

    @Override
    public void startPeriodicFetching() {
        if (_running.getAndSet(true)) {
            _log.debug("Segments PeriodicFetching is running...");
            return;
        }

        _log.debug("Starting PeriodicFetching Segments ...");
        _scheduledFuture = _scheduledExecutorService.scheduleWithFixedDelay(this, 0L, _refreshEveryNSeconds.get(), TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (!_running.getAndSet(false) || _scheduledFuture == null) {
            _log.debug("Segments PeriodicFetching not running...");
            return;
        }

        _scheduledFuture.cancel(false);
        _log.debug("Stopped PeriodicFetching Segments ...");
    }

    @Override
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

    @Override
    public void fetchAll(boolean addCacheHeader) {
        for (Map.Entry<String, SegmentFetcher> entry : _segmentFetchers.entrySet()) {
            SegmentFetcher fetcher = entry.getValue();

            if (fetcher == null) {
                continue;
            }

            if(addCacheHeader) {
                _scheduledExecutorService.submit(fetcher::runWhitCacheHeader);
                continue;
            }

            _scheduledExecutorService.submit(fetcher::fetchAll);
        }
    }

    @Override
    public void fetchAllSynchronous() {
        _segmentFetchers
                .entrySet()
                .stream().map(e -> _scheduledExecutorService.submit(e.getValue()::runWhitCacheHeader))
                .collect(Collectors.toList())
                .stream().forEach(future -> {
                    try {
                        future.get();
                    } catch (Exception ex) {
                        _log.error(ex.getMessage());
                    }});
    }
}
