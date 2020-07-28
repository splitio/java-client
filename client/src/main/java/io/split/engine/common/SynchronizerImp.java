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
    private final ScheduledExecutorService _splitsScheduledExecutorService;
    private final ScheduledExecutorService _segmentsScheduledExecutorService;

    public SynchronizerImp(RefreshableSplitFetcherProvider refreshableSplitFetcherProvider,
                                          RefreshableSegmentFetcher segmentFetcher) {
        _refreshableSplitFetcherProvider = checkNotNull(refreshableSplitFetcherProvider);
        _splitFetcher = checkNotNull(_refreshableSplitFetcherProvider.getFetcher());
        _segmentFetcher = checkNotNull(segmentFetcher);

        ThreadFactory splitsThreadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-SplitsRefresh-%d")
                .build();
        _splitsScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(splitsThreadFactory);

        ThreadFactory segmentsThreadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-SegmentsRefresh-%d")
                .build();
        _segmentsScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(segmentsThreadFactory);
    }

    @Override
    public void syncAll() {
        _splitsScheduledExecutorService.schedule(_splitFetcher, 0, TimeUnit.SECONDS);
        _segmentsScheduledExecutorService.schedule(_segmentFetcher, 1, TimeUnit.SECONDS);
    }

    @Override
    public void synchronizeSplits() {
        _splitFetcher.forceRefresh();
    }

    @Override
    public void synchronizeSegment(String segmentName) {
        _segmentFetcher.forceRefresh(segmentName);
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
