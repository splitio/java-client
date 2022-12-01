package io.split.engine.experiments;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.storages.SplitCacheProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides an instance of RefreshableExperimentFetcher that is guaranteed to be a singleton.
 *
 * @author adil
 */
public class SplitSynchronizationTask implements SyncTask, Closeable {
    private static final Logger _log = LoggerFactory.getLogger(SplitSynchronizationTask.class);

    private final AtomicReference<SplitFetcher> _splitFetcher = new AtomicReference<>();
    private final AtomicReference<SplitCacheProducer> _splitCacheProducer = new AtomicReference<SplitCacheProducer>();
    private final AtomicReference<ScheduledExecutorService> _executorService = new AtomicReference<>();
    private final AtomicLong _refreshEveryNSeconds;
    private final ScheduledExecutorService _scheduledExecutorService;
    private final AtomicBoolean _running;

    private ScheduledFuture<?> _scheduledFuture;

    public SplitSynchronizationTask(SplitFetcher splitFetcher, SplitCacheProducer splitCachesplitCacheProducer, long refreshEveryNSeconds) {
        _splitFetcher.set(checkNotNull(splitFetcher));
        _splitCacheProducer.set(checkNotNull(splitCachesplitCacheProducer));
        checkArgument(refreshEveryNSeconds >= 0L);
        _refreshEveryNSeconds = new AtomicLong(refreshEveryNSeconds);

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("split-splitFetcher-%d")
                .build();

        _scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        _executorService.set(_scheduledExecutorService);

        _running = new AtomicBoolean();
    }

    public void start() {
        if (_running.getAndSet(true)) {
            _log.debug("Splits PeriodicFetching is running...");
            return;
        }

        _log.debug("Starting PeriodicFetching Splits ...");
        _scheduledFuture = _scheduledExecutorService.scheduleWithFixedDelay(_splitFetcher.get(), 0L, _refreshEveryNSeconds.get(), TimeUnit.SECONDS);
    }

    public void stop() {
        if (!_running.getAndSet(false) || _scheduledFuture == null) {
            _log.debug("Splits PeriodicFetching not running...");
            return;
        }

        _scheduledFuture.cancel(false);
        _log.debug("Stopped PeriodicFetching Splits ...");
    }

    @Override
    public void close() {
        if (_executorService.get() == null) {
            return;
        }

        if (_splitFetcher.get() != null) {
            _splitCacheProducer.get().clear();
        }

        stop();

        ScheduledExecutorService scheduledExecutorService = _executorService.get();
        if (scheduledExecutorService.isShutdown()) {
            return;
        }

        scheduledExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(2L, TimeUnit.SECONDS)) { //optional *
                _log.warn("Executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = scheduledExecutorService.shutdownNow(); //optional **
                _log.warn("Executor was abruptly shut down. These tasks will not be executed: " + droppedTasks);
            }
        } catch (InterruptedException e) {
            // reset the interrupt.
            _log.warn("Shutdown hook for split fetchers has been interrupted");
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return _running.get();
    }
}
