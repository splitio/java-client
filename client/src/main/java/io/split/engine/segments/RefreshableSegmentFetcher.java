package io.split.engine.segments;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.engine.SDKReadinessGates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A SegmentFetchers implementation that creates RefreshableSegmentFetcher instances.
 *
 * @author adil
 */
public class RefreshableSegmentFetcher implements Closeable, SegmentFetcher, Runnable {
    private static final Logger _log = LoggerFactory.getLogger(RefreshableSegmentFetcher.class);

    private final SegmentChangeFetcher _segmentChangeFetcher;
    private final AtomicLong _refreshEveryNSeconds;

    private final Object _lock = new Object();
    private final ConcurrentMap<String, RefreshableSegment> _segmentFetchers = Maps.newConcurrentMap();
    private final SDKReadinessGates _gates;
    private final ScheduledExecutorService _scheduledExecutorService;

    private ScheduledFuture<?> _scheduledFuture;

    public RefreshableSegmentFetcher(SegmentChangeFetcher segmentChangeFetcher, long refreshEveryNSeconds, int numThreads, SDKReadinessGates gates) {
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
    }

    public RefreshableSegment segment(String segmentName) {
        RefreshableSegment segment = _segmentFetchers.get(segmentName);
        if (segment != null) {
            return segment;
        }

        // we are locking here since we wanna make sure that we create only ONE RefreableSegmentFetcher
        // per segment.
        synchronized (_lock) {
            // double check
            segment = _segmentFetchers.get(segmentName);
            if (segment != null) {
                return segment;
            }

            try {
                _gates.registerSegment(segmentName);
            } catch (InterruptedException e) {
                _log.error("Unable to register segment " + segmentName);
                // We will try again inside the RefreshableSegment.
            }
            segment = RefreshableSegment.create(segmentName, _segmentChangeFetcher, _gates);

            _segmentFetchers.putIfAbsent(segmentName, segment);

            return segment;
        }
    }

    @Override
    public long getChangeNumber(String segmentName) {
        RefreshableSegment segment = _segmentFetchers.get(segmentName);

        if (segment == null) {
            return -1;
        }

        return segment.changeNumber();
    }

    @Override
    public void forceRefresh(String segmentName) {
        RefreshableSegment segment = _segmentFetchers.get(segmentName);

        if (segment == null) {
            return;
        }

        segment.forceRefresh();
    }

    @Override
    public void forceRefreshAll() {
        for (ConcurrentMap.Entry<String, RefreshableSegment> entry : _segmentFetchers.entrySet()) {
            RefreshableSegment refreshableSegment = entry.getValue();

            if (refreshableSegment == null) {
                continue;
            }

            _scheduledExecutorService.submit(refreshableSegment);
        }
    }

    @Override
    public void startPeriodicFetching() {
        _scheduledFuture = _scheduledExecutorService.scheduleWithFixedDelay(this, 0L, _refreshEveryNSeconds.get(), TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        _scheduledFuture.cancel(false);
    }

    @Override
    public void run() {
        forceRefreshAll();
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
}
