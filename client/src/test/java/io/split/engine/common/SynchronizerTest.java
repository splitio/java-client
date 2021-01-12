package io.split.engine.common;

import io.split.cache.SegmentCache;
import io.split.cache.SplitCache;
import io.split.engine.experiments.SplitFetcherImp;
import io.split.engine.experiments.SplitSynchronizationTask;
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

    @Before
    public void beforeMethod() {
        _refreshableSplitFetcherTask = Mockito.mock(SplitSynchronizationTask.class);
        _segmentFetcher = Mockito.mock(SegmentSynchronizationTask.class);
        _splitFetcher = Mockito.mock(SplitFetcherImp.class);
        _splitCache = Mockito.mock(SplitCache.class);
        SegmentCache _segmentCache = Mockito.mock(SegmentCache.class);

        _synchronizer = new SynchronizerImp(_refreshableSplitFetcherTask, _splitFetcher, _segmentFetcher, _splitCache, _segmentCache);
    }

    @Test
    public void syncAll() throws InterruptedException {
        _synchronizer.syncAll();

        Thread.sleep(100);
        Mockito.verify(_splitFetcher, Mockito.times(1)).run();
        Mockito.verify(_segmentFetcher, Mockito.times(1)).run();
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
}
