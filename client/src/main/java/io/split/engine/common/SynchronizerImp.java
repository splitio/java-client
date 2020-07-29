package io.split.engine.common;

import com.google.common.annotations.VisibleForTesting;
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
    private final SyncAllServiceImp _syncAllService;
    private final ScheduledExecutorService _splitsScheduledExecutorService;

    @VisibleForTesting
    /* package private */ SynchronizerImp(RefreshableSplitFetcherProvider refreshableSplitFetcherProvider,
                                          RefreshableSegmentFetcher segmentFetcher,
                                          SyncAllServiceImp syncAllService) {
        _refreshableSplitFetcherProvider = checkNotNull(refreshableSplitFetcherProvider);
        _splitFetcher = checkNotNull(_refreshableSplitFetcherProvider.getFetcher());
        _segmentFetcher = checkNotNull(segmentFetcher);
        _syncAllService = checkNotNull(syncAllService);

        ThreadFactory splitsThreadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-SyncAll-%d")
                .build();
        _splitsScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(splitsThreadFactory);
    }

    public static SynchronizerImp build(RefreshableSplitFetcherProvider refreshableSplitFetcherProvider, RefreshableSegmentFetcher segmentFetcher) {
        return new SynchronizerImp(refreshableSplitFetcherProvider, segmentFetcher, new SyncAllServiceImp(refreshableSplitFetcherProvider.getFetcher(), segmentFetcher));
    }

    @Override
    public void syncAll() {
        _splitsScheduledExecutorService.schedule(_syncAllService, 0, TimeUnit.SECONDS);
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
