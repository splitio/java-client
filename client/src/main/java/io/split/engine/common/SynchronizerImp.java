package io.split.engine.common;

import io.split.client.EventClient;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.metrics.FireAndForgetMetrics;
import io.split.engine.experiments.RefreshableSplitFetcherProvider;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.segments.RefreshableSegmentFetcher;
import io.split.engine.segments.SegmentFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynchronizerImp implements Synchronizer {
    private static final Logger _log = LoggerFactory.getLogger(Synchronizer.class);

    private final RefreshableSplitFetcherProvider _refreshableSplitFetcherProvider;
    private final SplitFetcher _splitFetcher;
    private final SegmentFetcher _segmentFetcher;
    private final ImpressionsManager _impressionsManager;
    private final FireAndForgetMetrics _fireAndForgetMetrics;
    private final EventClient _eventClient;

    public SynchronizerImp(RefreshableSplitFetcherProvider refreshableSplitFetcherProvider,
                           SegmentFetcher segmentFetcher,
                           ImpressionsManager impressionsManager,
                           FireAndForgetMetrics fireAndForgetMetrics,
                           EventClient eventClient) {
        _refreshableSplitFetcherProvider = refreshableSplitFetcherProvider;
        _splitFetcher = _refreshableSplitFetcherProvider.getFetcher();
        _segmentFetcher = segmentFetcher;
        _impressionsManager = impressionsManager;
        _fireAndForgetMetrics = fireAndForgetMetrics;
        _eventClient = eventClient;
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
        _refreshableSplitFetcherProvider.close();
        ((RefreshableSegmentFetcher)_segmentFetcher).close();
    }

    @Override
    public void startPeriodicDataRecording() {
        _impressionsManager.startPeriodicDataRecording();
        _fireAndForgetMetrics.startPeriodicDataRecording();
        _eventClient.startPeriodicDataRecording();
    }

    @Override
    public void stopPeriodicDataRecording() {
        _impressionsManager.close();
        _fireAndForgetMetrics.close();
        _eventClient.close();
    }
}
