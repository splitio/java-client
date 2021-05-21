package io.split.engine.common;

import io.split.cache.SegmentCache;
import io.split.cache.SplitCache;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.SplitFetcherImp;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentFetcher;
import io.split.engine.segments.SegmentSynchronizationTask;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SynchronizerTest {
    private SplitSynchronizationTask _refreshableSplitFetcherTask;
    private SegmentSynchronizationTask _segmentFetcher;
    private SplitFetcherImp _splitFetcher;
    private SplitCache _splitCache;
    private Synchronizer _synchronizer;
    private SegmentCache _segmentCache;
    private SDKReadinessGates _gates;

    @Before
    public void beforeMethod() {
        _refreshableSplitFetcherTask = Mockito.mock(SplitSynchronizationTask.class);
        _segmentFetcher = Mockito.mock(SegmentSynchronizationTask.class);
        _splitFetcher = Mockito.mock(SplitFetcherImp.class);
        _splitCache = Mockito.mock(SplitCache.class);
        _segmentCache = Mockito.mock(SegmentCache.class);
        _gates = Mockito.mock(SDKReadinessGates.class);

        _synchronizer = new SynchronizerImp(_refreshableSplitFetcherTask, _splitFetcher, _segmentFetcher, _splitCache, _segmentCache, 50, _gates);
    }

    @Test
    public void syncAll() throws InterruptedException {
        Mockito.when(_splitFetcher.fetchAll(true)).thenReturn(true);
        Mockito.when(_segmentFetcher.fetchAllSynchronous()).thenReturn(true);
        _synchronizer.syncAll();

        Thread.sleep(1000);
        Mockito.verify(_splitFetcher, Mockito.times(1)).fetchAll(true);
        Mockito.verify(_segmentFetcher, Mockito.times(1)).fetchAllSynchronous();
    }

    @Test
    public void startPeriodicFetching() {
        _synchronizer.startPeriodicFetching();

        Mockito.verify(_refreshableSplitFetcherTask, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_segmentFetcher, Mockito.times(1)).startPeriodicFetching();
    }

    @Test
    public void stopPeriodicFetching() {
        _synchronizer.stopPeriodicFetching();

        Mockito.verify(_refreshableSplitFetcherTask, Mockito.times(1)).stop();
        Mockito.verify(_segmentFetcher, Mockito.times(1)).stop();
    }

    @Test
    public void streamingRetryOnSplit() {
        Mockito.when(_splitCache.getChangeNumber()).thenReturn(0l).thenReturn(0l).thenReturn(1l);
        _synchronizer.refreshSplits(1l);

        Mockito.verify(_splitCache, Mockito.times(3)).getChangeNumber();
    }

    @Test
    public void streamingRetryOnSegment() {
        SegmentFetcher fetcher = Mockito.mock(SegmentFetcher.class);
        Mockito.when(_segmentFetcher.getFetcher(Mockito.anyString())).thenReturn(fetcher);
        Mockito.when(_segmentCache.getChangeNumber(Mockito.anyString())).thenReturn(0l).thenReturn(0l).thenReturn(1l);
        _synchronizer.refreshSegment("Segment",1l);

        Mockito.verify(_segmentCache, Mockito.times(3)).getChangeNumber(Mockito.anyString());
    }

}
