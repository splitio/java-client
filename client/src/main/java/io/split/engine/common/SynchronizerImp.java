package io.split.engine.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.engine.experiments.RefreshableSplitFetcher;
import io.split.engine.experiments.RefreshableSplitFetcherProvider;
import io.split.engine.segments.RefreshableSegmentFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class SynchronizerImp implements Synchronizer {
    private static final Logger _log = LoggerFactory.getLogger(Synchronizer.class);

    private final RefreshableSplitFetcherProvider _refreshableSplitFetcherProvider;
    private final RefreshableSplitFetcher _splitFetcher;
    private final RefreshableSegmentFetcher _segmentFetcher;
    private final ScheduledExecutorService _syncAllScheduledExecutorService;

    public SynchronizerImp(RefreshableSplitFetcherProvider refreshableSplitFetcherProvider,
                                          RefreshableSegmentFetcher segmentFetcher) {
        _refreshableSplitFetcherProvider = checkNotNull(refreshableSplitFetcherProvider);
        _splitFetcher = checkNotNull(_refreshableSplitFetcherProvider.getFetcher());
        _segmentFetcher = checkNotNull(segmentFetcher);

        ThreadFactory splitsThreadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-SyncAll-%d")
                .build();
        _syncAllScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(splitsThreadFactory);
    }

    @Override
    public void syncAll() {
        _syncAllScheduledExecutorService.schedule(() -> {
            _splitFetcher.forceRefresh();
            _segmentFetcher.forceRefreshAll();
        }, 0, TimeUnit.SECONDS);
    }

    @Override
    public void startPeriodicFetching() {
        _log.debug("Starting Periodic Fetching ...");
        _refreshableSplitFetcherProvider.startPeriodicFetching();
        _segmentFetcher.startPeriodicFetching();
    }

    @Override
    public void stopPeriodicFetching() {
        _log.debug("Stop Periodic Fetching ...");
        _refreshableSplitFetcherProvider.stop();
        _segmentFetcher.stop();
    }
}
