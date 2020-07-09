package io.split.engine.experiments;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.engine.SDKReadinessGates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides an instance of RefreshableExperimentFetcher that is guaranteed to be a singleton.
 *
 * @author adil
 */
public class RefreshableSplitFetcherProvider implements Closeable {
    private static final Logger _log = LoggerFactory.getLogger(RefreshableSplitFetcherProvider.class);

    private final SplitParser _splitParser;
    private final SplitChangeFetcher _splitChangeFetcher;
    private final AtomicLong _refreshEveryNSeconds;
    private final AtomicReference<RefreshableSplitFetcher> _splitFetcher = new AtomicReference<RefreshableSplitFetcher>();
    private final SDKReadinessGates _gates;
    private final AtomicReference<ScheduledExecutorService> _executorService = new AtomicReference<>();
    private final ScheduledExecutorService _scheduledExecutorService;
    private final Object _lock = new Object();

    private ScheduledFuture<?> _scheduledFuture;

    public RefreshableSplitFetcherProvider(SplitChangeFetcher splitChangeFetcher, SplitParser splitParser, long refreshEveryNSeconds, SDKReadinessGates sdkBuildBlocker) {
        _splitChangeFetcher = splitChangeFetcher;
        checkNotNull(_splitChangeFetcher);

        _splitParser = splitParser;
        checkNotNull(_splitParser);

        checkArgument(refreshEveryNSeconds >= 0L);
        _refreshEveryNSeconds = new AtomicLong(refreshEveryNSeconds);

        _gates = sdkBuildBlocker;
        checkNotNull(_gates);

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("split-splitFetcher-%d")
                .build();

        _scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        _executorService.set(_scheduledExecutorService);
    }

    public RefreshableSplitFetcher getFetcher() {
        if (_splitFetcher.get() != null) {
            return _splitFetcher.get();
        }

        // we are locking here since we wanna make sure that we create only ONE RefreshableExperimentChangeFetcher
        synchronized (_lock) {
            // double check
            if (_splitFetcher.get() != null) {
                return _splitFetcher.get();
            }

            RefreshableSplitFetcher splitFetcher = new RefreshableSplitFetcher(_splitChangeFetcher, _splitParser, _gates);

            _splitFetcher.set(splitFetcher);
            return splitFetcher;
        }
    }

    public void startPeriodicFetching() {
        _scheduledFuture = _scheduledExecutorService.scheduleWithFixedDelay(getFetcher(), 0L, _refreshEveryNSeconds.get(), TimeUnit.SECONDS);
    }

    public void stop() {
        _scheduledFuture.cancel(false);
    }

    @Override
    public void close() {
        if (_executorService.get() == null) {
            return;
        }

        if (_splitFetcher.get() != null) {
            _splitFetcher.get().clear();
        }

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

}
