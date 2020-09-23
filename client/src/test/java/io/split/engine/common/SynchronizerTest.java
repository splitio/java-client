package io.split.engine.common;

import io.split.client.HttpSegmentChangeFetcher;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.RefreshableSplitFetcher;
import io.split.engine.experiments.RefreshableSplitFetcherProvider;
import io.split.engine.segments.RefreshableSegmentFetcher;
import io.split.engine.segments.SegmentChangeFetcher;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.mockito.Mockito;

public class SynchronizerTest {

    @Test
    public void syncAll() throws InterruptedException {
        RefreshableSplitFetcherProvider refreshableSplitFetcherProvider = Mockito.mock(RefreshableSplitFetcherProvider.class);
        RefreshableSegmentFetcher segmentFetcher = Mockito.mock(RefreshableSegmentFetcher.class);
        RefreshableSplitFetcher splitFetcher = Mockito.mock(RefreshableSplitFetcher.class);

        Mockito.when(refreshableSplitFetcherProvider.getFetcher())
                .thenReturn(splitFetcher);

        Synchronizer synchronizer = new SynchronizerImp(refreshableSplitFetcherProvider, segmentFetcher);
        synchronizer.syncAll();

        Thread.sleep(100);
        Mockito.verify(splitFetcher, Mockito.times(1)).forceRefresh();
        Mockito.verify(segmentFetcher, Mockito.times(1)).forceRefreshAll();
    }

    @Test
    public void startPeriodicFetching() {
        RefreshableSplitFetcherProvider refreshableSplitFetcherProvider = Mockito.mock(RefreshableSplitFetcherProvider.class);
        RefreshableSegmentFetcher segmentFetcher = Mockito.mock(RefreshableSegmentFetcher.class);
        RefreshableSplitFetcher splitFetcher = Mockito.mock(RefreshableSplitFetcher.class);

        Mockito.when(refreshableSplitFetcherProvider.getFetcher())
                .thenReturn(splitFetcher);

        Synchronizer synchronizer = new SynchronizerImp(refreshableSplitFetcherProvider, segmentFetcher);
        synchronizer.startPeriodicFetching();

        Mockito.verify(refreshableSplitFetcherProvider, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(segmentFetcher, Mockito.times(1)).startPeriodicFetching();
    }

    @Test
    public void stopPeriodicFetching() {
        RefreshableSplitFetcherProvider refreshableSplitFetcherProvider = Mockito.mock(RefreshableSplitFetcherProvider.class);
        RefreshableSegmentFetcher segmentFetcher = Mockito.mock(RefreshableSegmentFetcher.class);
        RefreshableSplitFetcher splitFetcher = Mockito.mock(RefreshableSplitFetcher.class);

        Mockito.when(refreshableSplitFetcherProvider.getFetcher())
                .thenReturn(splitFetcher);

        Synchronizer synchronizer = new SynchronizerImp(refreshableSplitFetcherProvider, segmentFetcher);
        synchronizer.stopPeriodicFetching();

        Mockito.verify(refreshableSplitFetcherProvider, Mockito.times(1)).stop();
        Mockito.verify(segmentFetcher, Mockito.times(1)).stop();
    }

    @Test
    public void refreshSplits() {
        RefreshableSplitFetcherProvider refreshableSplitFetcherProvider = Mockito.mock(RefreshableSplitFetcherProvider.class);
        RefreshableSegmentFetcher segmentFetcher = Mockito.mock(RefreshableSegmentFetcher.class);
        RefreshableSplitFetcher splitFetcher = Mockito.mock(RefreshableSplitFetcher.class);

        Mockito.when(refreshableSplitFetcherProvider.getFetcher())
                .thenReturn(splitFetcher);

        Mockito.when(splitFetcher.changeNumber())
                .thenReturn(450L)
                .thenReturn(460L)
                .thenReturn(470L)
                .thenReturn(480L)
                .thenReturn(500L);

        Synchronizer synchronizer = new SynchronizerImp(refreshableSplitFetcherProvider, segmentFetcher);
        synchronizer.refreshSplits(500);

        Mockito.verify(splitFetcher, Mockito.times(4)).forceRefresh();
    }

    @Test
    public void refreshSegment() {
        RefreshableSplitFetcherProvider refreshableSplitFetcherProvider = Mockito.mock(RefreshableSplitFetcherProvider.class);
        RefreshableSegmentFetcher segmentFetcher = Mockito.mock(RefreshableSegmentFetcher.class);
        RefreshableSplitFetcher splitFetcher = Mockito.mock(RefreshableSplitFetcher.class);

        Mockito.when(refreshableSplitFetcherProvider.getFetcher())
                .thenReturn(splitFetcher);

        Mockito.when(segmentFetcher.getChangeNumber("segment-name"))
                .thenReturn(450L)
                .thenReturn(460L)
                .thenReturn(470L)
                .thenReturn(480L)
                .thenReturn(500L);

        Synchronizer synchronizer = new SynchronizerImp(refreshableSplitFetcherProvider, segmentFetcher);
        synchronizer.refreshSegment("segment-name", 500);

        Mockito.verify(segmentFetcher, Mockito.times(4)).forceRefresh("segment-name");
    }
}
