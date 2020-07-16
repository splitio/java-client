package io.split.engine.common;

import io.split.engine.experiments.RefreshableSplitFetcherProvider;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.segments.SegmentFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynchronizerImp implements Synchronizer {
    private static final Logger _log = LoggerFactory.getLogger(Synchronizer.class);

    private final RefreshableSplitFetcherProvider _refreshableSplitFetcherProvider;
    private final SplitFetcher _splitFetcher;
    private final SegmentFetcher _segmentFetcher;

    public SynchronizerImp(RefreshableSplitFetcherProvider refreshableSplitFetcherProvider,
                           SegmentFetcher segmentFetcher) {
        _refreshableSplitFetcherProvider = refreshableSplitFetcherProvider;
        _splitFetcher = _refreshableSplitFetcherProvider.getFetcher();
        _segmentFetcher = segmentFetcher;
    }

    @Override
    public void syncAll() {
        _splitFetcher.forceRefresh();
        _segmentFetcher.forceRefreshAll();
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
        _refreshableSplitFetcherProvider.startPeriodicFetching();
        _segmentFetcher.startPeriodicFetching();
    }

    @Override
    public void stopPeriodicFetching() {
        _refreshableSplitFetcherProvider.stop();
        _segmentFetcher.stop();
    }
}
