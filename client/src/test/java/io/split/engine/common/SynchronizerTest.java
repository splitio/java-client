package io.split.engine.common;

import io.split.client.events.EventsTask;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.impressions.UniqueKeysTracker;
import io.split.engine.segments.SegmentChangeFetcher;
import io.split.engine.segments.SegmentSynchronizationTaskImp;
import io.split.storages.*;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.FetchResult;
import io.split.engine.experiments.SplitFetcherImp;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentFetcher;
import io.split.engine.segments.SegmentSynchronizationTask;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.telemetry.synchronizer.TelemetrySyncTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;

public class SynchronizerTest {
    private SplitSynchronizationTask _refreshableSplitFetcherTask;
    private SegmentSynchronizationTask _segmentFetcher;
    private SplitFetcherImp _splitFetcher;
    private SplitCacheProducer _splitCacheProducer;
    private Synchronizer _synchronizer;
    private SegmentCacheProducer _segmentCacheProducer;
    private SDKReadinessGates _gates;
    private SplitTasks _splitTasks;
    private TelemetrySyncTask _telemetrySyncTask;
    private ImpressionsManager _impressionsManager;
    private EventsTask _eventsTask;
    private UniqueKeysTracker _uniqueKeysTracker;

    @Before
    public void beforeMethod() {
        _refreshableSplitFetcherTask = Mockito.mock(SplitSynchronizationTask.class);
        _segmentFetcher = Mockito.mock(SegmentSynchronizationTask.class);
        _splitFetcher = Mockito.mock(SplitFetcherImp.class);
        _splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        _segmentCacheProducer = Mockito.mock(SegmentCache.class);
        _gates = Mockito.mock(SDKReadinessGates.class);
        _telemetrySyncTask = Mockito.mock(TelemetrySyncTask.class);
        _impressionsManager = Mockito.mock(ImpressionsManager.class);
        _eventsTask = Mockito.mock(EventsTask.class);
        _uniqueKeysTracker = Mockito.mock(UniqueKeysTracker.class);

        _splitTasks = SplitTasks.build(_refreshableSplitFetcherTask, _segmentFetcher, _impressionsManager, _eventsTask, _telemetrySyncTask, _uniqueKeysTracker);

        _synchronizer = new SynchronizerImp(_splitTasks, _splitFetcher, _splitCacheProducer, _segmentCacheProducer, 50, 10, 5, false, _gates);
    }

    @Test
    public void syncAll() throws InterruptedException {
        Mockito.when(_splitFetcher.forceRefresh(Mockito.anyObject())).thenReturn(new FetchResult(true, new HashSet<>()));
        Mockito.when(_segmentFetcher.fetchAllSynchronous()).thenReturn(true);
        _synchronizer.syncAll();

        Thread.sleep(1000);
        Mockito.verify(_splitFetcher, Mockito.times(1)).forceRefresh(Mockito.anyObject());
        Mockito.verify(_segmentFetcher, Mockito.times(1)).fetchAllSynchronous();
    }

    @Test
    public void testSyncAllSegments() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        SegmentSynchronizationTask segmentSynchronizationTask = new SegmentSynchronizationTaskImp(Mockito.mock(SegmentChangeFetcher.class),
                20L, 1, new SDKReadinessGates(), _segmentCacheProducer, Mockito.mock(TelemetryRuntimeProducer.class),
                Mockito.mock(SplitCacheConsumer.class));
        Field synchronizerSegmentFetcher = SynchronizerImp.class.getDeclaredField("_segmentSynchronizationTaskImp");
        synchronizerSegmentFetcher.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(synchronizerSegmentFetcher, synchronizerSegmentFetcher.getModifiers() & ~Modifier.FINAL);
        synchronizerSegmentFetcher.set(_synchronizer, segmentSynchronizationTask);
        Mockito.when(_splitFetcher.forceRefresh(Mockito.anyObject())).thenReturn(new FetchResult(true, Stream.of("Segment1", "Segment2").collect(Collectors.toSet())));
        Mockito.when(_segmentFetcher.fetchAllSynchronous()).thenReturn(true);
        _synchronizer.syncAll();

        Thread.sleep(1000);
        Mockito.verify(_splitFetcher, Mockito.times(1)).forceRefresh(Mockito.anyObject());
        Assert.assertNotNull(segmentSynchronizationTask.getFetcher("Segment1"));
        Assert.assertNotNull(segmentSynchronizationTask.getFetcher("Segment2"));
    }

    @Test
    public void startPeriodicFetching() {
        _synchronizer.startPeriodicFetching();

        Mockito.verify(_refreshableSplitFetcherTask, Mockito.times(1)).start();
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
        when(_splitCacheProducer.getChangeNumber()).thenReturn(0l).thenReturn(0l).thenReturn(1l);
        when(_splitFetcher.forceRefresh(Mockito.anyObject())).thenReturn(new FetchResult(true, new HashSet<>()));
        _synchronizer.refreshSplits(1l);

        Mockito.verify(_splitCacheProducer, Mockito.times(3)).getChangeNumber();
    }

    @Test
    public void streamingRetryOnSegment() {
        SegmentFetcher fetcher = Mockito.mock(SegmentFetcher.class);
        when(_segmentFetcher.getFetcher(Mockito.anyString())).thenReturn(fetcher);
        when(_segmentCacheProducer.getChangeNumber(Mockito.anyString())).thenReturn(0l).thenReturn(0l).thenReturn(1l);
        _synchronizer.refreshSegment("Segment",1l);

        Mockito.verify(_segmentCacheProducer, Mockito.times(3)).getChangeNumber(Mockito.anyString());
    }

    @Test
    public void streamingRetryOnSplitAndSegment() {
        when(_splitCacheProducer.getChangeNumber()).thenReturn(0l).thenReturn(0l).thenReturn(1l);
        Set<String> segments = new HashSet<>();
        segments.add("segment1");
        segments.add("segment2");
        when(_splitFetcher.forceRefresh(Mockito.anyObject())).thenReturn(new FetchResult(true, segments));
        SegmentFetcher fetcher = Mockito.mock(SegmentFetcher.class);
        when(_segmentCacheProducer.getChangeNumber(Mockito.anyString())).thenReturn(0l).thenReturn(0l).thenReturn(1l);
        when(_segmentFetcher.getFetcher(Mockito.anyString())).thenReturn(fetcher);
        _synchronizer.refreshSplits(1l);

        Mockito.verify(_splitCacheProducer, Mockito.times(3)).getChangeNumber();
        Mockito.verify(_segmentFetcher, Mockito.times(2)).getFetcher(Mockito.anyString());
    }

    @Test
    public void testCDNBypassIsRequestedAfterNFailures() throws NoSuchFieldException, IllegalAccessException {

        SplitCache cache = new InMemoryCacheImp();
        Synchronizer imp = new SynchronizerImp(_splitTasks,
                _splitFetcher,
                cache,
                _segmentCacheProducer,
                50,
                3,
                1,
                true,
                Mockito.mock(SDKReadinessGates.class));

        ArgumentCaptor<FetchOptions> optionsCaptor = ArgumentCaptor.forClass(FetchOptions.class);
        AtomicInteger calls = new AtomicInteger();
        Mockito.doAnswer(invocationOnMock -> {
            calls.getAndIncrement();
            switch (calls.get()) {
                case 4: cache.setChangeNumber(123);
            }
            return new FetchResult(true, new HashSet<>());
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
        Synchronizer imp = new SynchronizerImp(_splitTasks,
                _splitFetcher,
                cache,
                _segmentCacheProducer,
                50,
                3,
                1,
                true,
                Mockito.mock(SDKReadinessGates.class));

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
        Synchronizer imp = new SynchronizerImp(_splitTasks,
                _splitFetcher,
                cache,
                _segmentCacheProducer,
                50,
                3,
                1,
                true,
                Mockito.mock(SDKReadinessGates.class));

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

    @Test
    public void testDataRecording(){
        SplitCache cache = new InMemoryCacheImp();
        Synchronizer imp = new SynchronizerImp(_splitTasks,
                _splitFetcher,
                cache,
                _segmentCacheProducer,
                50,
                3,
                1,
                true,
                Mockito.mock(SDKReadinessGates.class));
        imp.startPeriodicDataRecording();

        Mockito.verify(_eventsTask, Mockito.times(1)).start();
        Mockito.verify(_impressionsManager, Mockito.times(1)).start();
        Mockito.verify(_uniqueKeysTracker, Mockito.times(1)).start();
        Mockito.verify(_telemetrySyncTask, Mockito.times(1)).startScheduledTask();

        imp.stopPeriodicDataRecording(3L,1L,1L);

        Mockito.verify(_eventsTask, Mockito.times(1)).close();
        Mockito.verify(_impressionsManager, Mockito.times(1)).close();
        Mockito.verify(_uniqueKeysTracker, Mockito.times(1)).stop();
        Mockito.verify(_telemetrySyncTask, Mockito.times(1)).stopScheduledTask(3L,1L,1L);
    }
}
