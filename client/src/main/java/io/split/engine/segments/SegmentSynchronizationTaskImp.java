package io.split.engine.segments;

import com.google.common.collect.Maps;
import io.split.client.utils.SplitExecutorFactory;
import io.split.engine.common.FetchOptions;
import io.split.storages.SegmentCacheProducer;
import io.split.storages.SplitCacheConsumer;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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
    private final SegmentCacheProducer _segmentCacheProducer;
    private final ScheduledExecutorService _scheduledExecutorService;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;
    private final SplitCacheConsumer _splitCacheConsumer;

    private ScheduledFuture<?> _scheduledFuture;

    public SegmentSynchronizationTaskImp(SegmentChangeFetcher segmentChangeFetcher, long refreshEveryNSeconds, int numThreads,
                                         SegmentCacheProducer segmentCacheProducer, TelemetryRuntimeProducer telemetryRuntimeProducer,
                                         SplitCacheConsumer splitCacheConsumer, ThreadFactory threadFactory) {
        _segmentChangeFetcher = checkNotNull(segmentChangeFetcher);

        checkArgument(refreshEveryNSeconds >= 0L);
        _refreshEveryNSeconds = new AtomicLong(refreshEveryNSeconds);
        _scheduledExecutorService = SplitExecutorFactory.buildScheduledExecutorService(threadFactory, "split-segmentFetcher-" + "%d", numThreads);
        _running = new AtomicBoolean(false);

        _segmentCacheProducer = checkNotNull(segmentCacheProducer);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        _splitCacheConsumer = checkNotNull(splitCacheConsumer);
    }

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

            SegmentFetcher newSegment = new SegmentFetcherImp(segmentName, _segmentChangeFetcher, _segmentCacheProducer, _telemetryRuntimeProducer);

            if (_running.get()) {
                _scheduledExecutorService.submit(() -> newSegment.fetch(new FetchOptions.Builder().build()));
            }

            _segmentFetchers.putIfAbsent(segmentName, newSegment);
        }
    }

    public SegmentFetcher getFetcher(String segmentName) {
        initializeSegment(segmentName);

        return _segmentFetchers.get(segmentName);
    }

    @Override
    public void start() {
        if (_running.getAndSet(true) ) {
            _log.debug("Segments PeriodicFetching is running...");
            return;
        }

        _log.debug("Starting PeriodicFetching Segments ...");
        _scheduledFuture = _scheduledExecutorService.scheduleWithFixedDelay(() -> {
                fetchAll(false);
            }, 0L, _refreshEveryNSeconds.get(), TimeUnit.SECONDS);
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
    public boolean isRunning() {
        return _running.get();
    }

    public void fetchAll(boolean addCacheHeader) {
        _splitCacheConsumer.getSegments().forEach(this::initialize);
        for (Map.Entry<String, SegmentFetcher> entry : _segmentFetchers.entrySet()) {
            SegmentFetcher fetcher = entry.getValue();

            if (fetcher == null) {
                continue;
            }

            if(addCacheHeader) {
                _scheduledExecutorService.submit(fetcher::runWhitCacheHeader);
                continue;
            }

            _scheduledExecutorService.submit(() -> fetcher.fetch(new FetchOptions.Builder().build()));
        }
    }

    public boolean fetchAllSynchronous() {
        _splitCacheConsumer.getSegments().forEach(this::initialize);
        List<Future<Boolean>> segmentFetchExecutions = _segmentFetchers.entrySet()
                .stream().map(e -> _scheduledExecutorService.submit(e.getValue()::runWhitCacheHeader))
                .collect(Collectors.toList());
        int failures = segmentFetchExecutions.stream()
                .mapToInt(f -> {
                    try {
                        return f.get() ? 0 : 1;
                    } catch (InterruptedException e) {
                        _log.error(e.getMessage());
                    } catch (ExecutionException e) {
                        _log.error(e.getMessage());
                    }
                    return 0;
                })
                .sum();
        return failures == 0;
    }

    private void initialize(String segmentName) {
        SegmentFetcher segment = _segmentFetchers.get(segmentName);
        if (segment != null) {
            return;
        }
        // we are locking here since we wanna make sure that we create only ONE RefreshableSegmentFetcher
        // per segment.
        synchronized (_lock) {
            // double check
            segment = _segmentFetchers.get(segmentName);
            if (segment != null) {
                return;
            }

            segment = new SegmentFetcherImp(segmentName, _segmentChangeFetcher, _segmentCacheProducer, _telemetryRuntimeProducer);

            _segmentFetchers.putIfAbsent(segmentName, segment);
        }
    }
}