package io.split.engine.common;

import io.split.engine.experiments.RefreshableSplitFetcher;
import io.split.engine.segments.RefreshableSegmentFetcher;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncAllServiceImp implements Runnable {
    private final RefreshableSplitFetcher _splitFetcher;
    private final RefreshableSegmentFetcher _segmentFetcher;

    public SyncAllServiceImp(RefreshableSplitFetcher refreshableSplitFetcher,
                             RefreshableSegmentFetcher segmentFetcher) {
        _splitFetcher = checkNotNull(refreshableSplitFetcher);
        _segmentFetcher = checkNotNull(segmentFetcher);
    }

    @Override
    public void run() {
        _splitFetcher.forceRefresh();
        _segmentFetcher.forceRefreshAll();
    }
}
