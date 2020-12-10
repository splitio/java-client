package io.split.engine.common;

import io.split.engine.cache.SplitCache;
import io.split.engine.experiments.RefreshableSplitFetcher;
import io.split.engine.experiments.RefreshableSplitFetcherTask;
import io.split.engine.segments.RefreshableSegmentFetcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SynchronizerTest {
    private RefreshableSplitFetcherTask _refreshableSplitFetcherTask;
    private RefreshableSegmentFetcher _segmentFetcher;
    private RefreshableSplitFetcher _splitFetcher;
    private SplitCache _splitCache;
    private Synchronizer _synchronizer;

    @Before
    public void beforeMethod() {
        _refreshableSplitFetcherTask = Mockito.mock(RefreshableSplitFetcherTask.class);
        _segmentFetcher = Mockito.mock(RefreshableSegmentFetcher.class);
        _splitFetcher = Mockito.mock(RefreshableSplitFetcher.class);
        _splitCache = Mockito.mock(SplitCache.class);

        _synchronizer = new SynchronizerImp(_refreshableSplitFetcherTask, _splitFetcher, _segmentFetcher, _splitCache);
    }

    @Test
    public void syncAll() throws InterruptedException {
        _synchronizer.syncAll();

        Thread.sleep(100);
        Mockito.verify(_splitFetcher, Mockito.times(1)).run();
        Mockito.verify(_segmentFetcher, Mockito.times(1)).forceRefreshAll();
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
