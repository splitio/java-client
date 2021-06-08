package io.split.engine.common;

import io.split.cache.InMemoryCacheImp;
import io.split.cache.SegmentCache;
import io.split.cache.SplitCache;
import io.split.engine.experiments.SplitFetcherImp;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentFetcher;
import io.split.engine.segments.SegmentSynchronizationTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.when;

public class SynchronizerTest {
    private SplitSynchronizationTask _refreshableSplitFetcherTask;
    private SegmentSynchronizationTask _segmentFetcher;
    private SplitFetcherImp _splitFetcher;
    private SplitCache _splitCache;
    private Synchronizer _synchronizer;
    private SegmentCache _segmentCache;

    @Before
    public void beforeMethod() {
        _refreshableSplitFetcherTask = Mockito.mock(SplitSynchronizationTask.class);
        _segmentFetcher = Mockito.mock(SegmentSynchronizationTask.class);
        _splitFetcher = Mockito.mock(SplitFetcherImp.class);
        _splitCache = Mockito.mock(SplitCache.class);
        _segmentCache = Mockito.mock(SegmentCache.class);

        _synchronizer = new SynchronizerImp(_refreshableSplitFetcherTask, _splitFetcher, _segmentFetcher, _splitCache, _segmentCache, 50, 10, 5, false, null);
    }

    @Test
    public void syncAll() throws InterruptedException {
        _synchronizer.syncAll();

        Thread.sleep(100);
        Mockito.verify(_splitFetcher, Mockito.times(1)).fetchAll(new FetchOptions.Builder().cacheControlHeaders(true).build());
        Mockito.verify(_segmentFetcher, Mockito.times(1)).fetchAll(true);
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
        when(_splitCache.getChangeNumber()).thenReturn(0l).thenReturn(0l).thenReturn(1l);
        _synchronizer.refreshSplits(1l);

        Mockito.verify(_splitCache, Mockito.times(3)).getChangeNumber();
    }

    @Test
    public void streamingRetryOnSegment() {
        SegmentFetcher fetcher = Mockito.mock(SegmentFetcher.class);
        when(_segmentFetcher.getFetcher(Mockito.anyString())).thenReturn(fetcher);
        when(_segmentCache.getChangeNumber(Mockito.anyString())).thenReturn(0l).thenReturn(0l).thenReturn(1l);
        _synchronizer.refreshSegment("Segment",1l);

        Mockito.verify(_segmentCache, Mockito.times(3)).getChangeNumber(Mockito.anyString());
    }

    @Test
    public void testCDNBypassIsRequestedAfterNFailures() throws NoSuchFieldException, IllegalAccessException {

        SplitCache cache = new InMemoryCacheImp();
        Synchronizer imp = new SynchronizerImp(_refreshableSplitFetcherTask,
                _splitFetcher,
                _segmentFetcher,
                cache,
                _segmentCache,
                50,
                3,
                1,
                true,
                null);

        ArgumentCaptor<FetchOptions> optionsCaptor = ArgumentCaptor.forClass(FetchOptions.class);
        AtomicInteger calls = new AtomicInteger();
        Mockito.doAnswer(invocationOnMock -> {
            calls.getAndIncrement();
            switch (calls.get()) {
                case 4: cache.setChangeNumber(123);
            }
            return null;
        }).when(_splitFetcher).forceRefresh(optionsCaptor.capture());

        imp.refreshSplits(123);

        List<FetchOptions> options = optionsCaptor.getAllValues();
        Assert.assertEquals(options.size(), 4);
        Assert.assertFalse(options.get(0).hasCustomCN());
        Assert.assertFalse(options.get(1).hasCustomCN());
        Assert.assertFalse(options.get(2).hasCustomCN());
        Assert.assertTrue(options.get(3).hasCustomCN());
    }

    @Test
    public void testCDNBypassRequestLimitAndBackoff() throws NoSuchFieldException, IllegalAccessException {

        SplitCache cache = new InMemoryCacheImp();
        Synchronizer imp = new SynchronizerImp(_refreshableSplitFetcherTask,
                _splitFetcher,
                _segmentFetcher,
                cache,
                _segmentCache,
                50,
                3,
                1,
                true,
                null);

        ArgumentCaptor<FetchOptions> optionsCaptor = ArgumentCaptor.forClass(FetchOptions.class);
        AtomicInteger calls = new AtomicInteger();
        Mockito.doAnswer(invocationOnMock -> {
            calls.getAndIncrement();
            switch (calls.get()) {
                case 14: Assert.assertTrue(false); // should never get here
            }
            return null;
        }).when(_splitFetcher).forceRefresh(optionsCaptor.capture());

        // Before executing, we'll update the backoff via reflection, to avoid waiting minutes for the test to run.
        Field backoffBase = SynchronizerImp.class.getDeclaredField("ON_DEMAND_FETCH_BACKOFF_BASE_MS");
        backoffBase.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(backoffBase, backoffBase.getModifiers() & ~Modifier.FINAL);
        backoffBase.set(imp, 1); // 1ms

        long before = System.currentTimeMillis();
        imp.refreshSplits(1);
        long after = System.currentTimeMillis();

        List<FetchOptions> options = optionsCaptor.getAllValues();
        Assert.assertEquals(options.size(), 13);
        Assert.assertFalse(options.get(0).hasCustomCN());
        Assert.assertFalse(options.get(1).hasCustomCN());
        Assert.assertFalse(options.get(2).hasCustomCN());
        Assert.assertTrue(options.get(3).hasCustomCN());
        Assert.assertTrue(options.get(4).hasCustomCN());
        Assert.assertTrue(options.get(5).hasCustomCN());
        Assert.assertTrue(options.get(6).hasCustomCN());
        Assert.assertTrue(options.get(7).hasCustomCN());
        Assert.assertTrue(options.get(8).hasCustomCN());
        Assert.assertTrue(options.get(9).hasCustomCN());
        Assert.assertTrue(options.get(10).hasCustomCN());
        Assert.assertTrue(options.get(11).hasCustomCN());
        Assert.assertTrue(options.get(12).hasCustomCN());
        
        Assert.assertEquals(calls.get(), 13);
        long minDiffExpected = 1 + 2 + 4 + 8 + 16 + 32 + 64 + 128 + 256;
        Assert.assertTrue((after - before) > minDiffExpected);
    }

    @Test
    public void testCDNBypassRequestLimitAndForSegmentsBackoff() throws NoSuchFieldException, IllegalAccessException {

        SplitCache cache = new InMemoryCacheImp();
        Synchronizer imp = new SynchronizerImp(_refreshableSplitFetcherTask,
                _splitFetcher,
                _segmentFetcher,
                cache,
                _segmentCache,
                50,
                3,
                1,
                true,
                null);

        SegmentFetcher fetcher = Mockito.mock(SegmentFetcher.class);
        when(_segmentFetcher.getFetcher("someSegment")).thenReturn(fetcher);

        ArgumentCaptor<FetchOptions> optionsCaptor = ArgumentCaptor.forClass(FetchOptions.class);
        AtomicInteger calls = new AtomicInteger();
        Mockito.doAnswer(invocationOnMock -> {
            calls.getAndIncrement();
            switch (calls.get()) {
                case 14: Assert.assertTrue(false); // should never get here
            }
            return null;
        }).when(fetcher).fetch(optionsCaptor.capture());

        // Before executing, we'll update the backoff via reflection, to avoid waiting minutes for the test to run.
        Field backoffBase = SynchronizerImp.class.getDeclaredField("ON_DEMAND_FETCH_BACKOFF_BASE_MS");
        backoffBase.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(backoffBase, backoffBase.getModifiers() & ~Modifier.FINAL);
        backoffBase.set(imp, 1); // 1ms

        long before = System.currentTimeMillis();
        imp.refreshSegment("someSegment",1);
        long after = System.currentTimeMillis();

        List<FetchOptions> options = optionsCaptor.getAllValues();
        Assert.assertEquals(options.size(), 13);
        Assert.assertFalse(options.get(0).hasCustomCN());
        Assert.assertFalse(options.get(1).hasCustomCN());
        Assert.assertFalse(options.get(2).hasCustomCN());
        Assert.assertTrue(options.get(3).hasCustomCN());
        Assert.assertTrue(options.get(4).hasCustomCN());
        Assert.assertTrue(options.get(5).hasCustomCN());
        Assert.assertTrue(options.get(6).hasCustomCN());
        Assert.assertTrue(options.get(7).hasCustomCN());
        Assert.assertTrue(options.get(8).hasCustomCN());
        Assert.assertTrue(options.get(9).hasCustomCN());
        Assert.assertTrue(options.get(10).hasCustomCN());
        Assert.assertTrue(options.get(11).hasCustomCN());
        Assert.assertTrue(options.get(12).hasCustomCN());

        Assert.assertEquals(calls.get(), 13);
        long minDiffExpected = 1 + 2 + 4 + 8 + 16 + 32 + 64 + 128 + 256;
        Assert.assertTrue((after - before) > minDiffExpected);
    }
}
