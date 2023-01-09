package io.split.engine.segments;

import com.google.common.collect.Sets;
import io.split.storages.SegmentCache;
import io.split.storages.SegmentCacheProducer;
import io.split.storages.memory.SegmentCacheInMemoryImpl;
import io.split.client.dtos.SegmentChange;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.telemetry.storage.TelemetryStorage;
import io.split.engine.common.FetchOptions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Tests for RefreshableSegmentFetcher.
 *
 * @author adil
 */
public class SegmentFetcherImpTest {
    private static final Logger _log = LoggerFactory.getLogger(SegmentFetcherImpTest.class);
    private static final String SEGMENT_NAME = "foo";
    private static final TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);

    @Test
    public void works_when_we_start_without_state() throws InterruptedException {
        works(-1L);
    }

    @Test
    public void works_when_we_start_with_state() throws InterruptedException {
        works(20L);

    }

    @Test
    public void works_when_there_are_no_changes() throws InterruptedException {
        long startingChangeNumber = -1L;
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();

        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChange = getSegmentChange(-1L, 10L);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChange);

        SegmentFetcherImp fetcher = new SegmentFetcherImp(SEGMENT_NAME, segmentChangeFetcher, segmentCache, TELEMETRY_STORAGE);

        // execute the fetcher for a little bit.
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(() -> fetcher.fetch(new FetchOptions.Builder().build()), 0L, 100, TimeUnit.MICROSECONDS);
        Thread.currentThread().sleep(5 * 100);

        scheduledExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(1L, TimeUnit.SECONDS)) {
                _log.info("Executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = scheduledExecutorService.shutdownNow();
                _log.info("Executor was abruptly shut down. These tasks will not be executed: " + droppedTasks);
            }
        } catch (InterruptedException e) {
            // reset the interrupt.
            Thread.currentThread().interrupt();
        }

        Set<String> expected = Sets.newHashSet("" + (startingChangeNumber + 1));

        assertNotNull(segmentCache.getChangeNumber(SEGMENT_NAME));
        assertEquals(10L, segmentCache.getChangeNumber(SEGMENT_NAME));

    }

    private void works(long startingChangeNumber) throws InterruptedException {
        String segmentName = SEGMENT_NAME;
        SegmentCacheProducer segmentCacheProducer = Mockito.mock(SegmentCacheProducer.class);
        Mockito.when(segmentCacheProducer.getChangeNumber(SEGMENT_NAME)).thenReturn(-1L).thenReturn(-1L)
        .thenReturn(-1L)
        .thenReturn(0L);

        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChange = getSegmentChange(-1L, -1L);
        
        Mockito.when(segmentChangeFetcher.fetch(Mockito.eq(SEGMENT_NAME),Mockito.eq( -1L), Mockito.any())).thenReturn(segmentChange);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.eq(SEGMENT_NAME),Mockito.eq( 0L), Mockito.any())).thenReturn(segmentChange);
        SegmentFetcher fetcher = new SegmentFetcherImp(segmentName, segmentChangeFetcher, segmentCacheProducer, Mockito.mock(TelemetryRuntimeProducer.class));

        // execute the fetcher for a little bit.
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(() -> fetcher.fetch(new FetchOptions.Builder().build()), 0L, Integer.MAX_VALUE, TimeUnit.SECONDS);
        Thread.currentThread().sleep(5 * 100);

        scheduledExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(1L, TimeUnit.SECONDS)) {
                _log.info("Executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = scheduledExecutorService.shutdownNow();
                _log.info("Executor was abruptly shut down. These tasks will not be executed: " + droppedTasks);
            }
        } catch (InterruptedException e) {
            // reset the interrupt.
            Thread.currentThread().interrupt();
        }
        Mockito.verify(segmentChangeFetcher, Mockito.times(2)).fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.anyObject());

    }


    @Test(expected = NullPointerException.class)
    public void does_not_work_if_segment_change_fetcher_is_null() {
        SegmentCacheProducer segmentCacheProducer = Mockito.mock(SegmentCacheProducer.class);
        SegmentFetcher fetcher = new SegmentFetcherImp(SEGMENT_NAME, null, segmentCacheProducer, TELEMETRY_STORAGE);
    }

    @Test(expected = NullPointerException.class)
    public void does_not_work_if_segment_name_is_null() {
        SegmentCacheProducer segmentCacheProducer = Mockito.mock(SegmentCacheProducer.class);
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentFetcher fetcher = new SegmentFetcherImp(null, segmentChangeFetcher, segmentCacheProducer, TELEMETRY_STORAGE);
    }

    @Test
    public void testBypassCdnClearedAfterFirstHit() {
        SegmentChangeFetcher mockFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentCache segmentCacheMock = new SegmentCacheInMemoryImpl();
        SegmentFetcher fetcher = new SegmentFetcherImp("someSegment", mockFetcher, segmentCacheMock, Mockito.mock(TelemetryRuntimeProducer.class));


        SegmentChange response1 = new SegmentChange();
        response1.name = "someSegment";
        response1.added = new ArrayList<>();
        response1.removed = new ArrayList<>();
        response1.since = -1;
        response1.till = 1;

        SegmentChange response2 = new SegmentChange();
        response2.name = "someSegment";
        response2.added = new ArrayList<>();
        response2.removed = new ArrayList<>();
        response2.since = 1;
        response1.till = 1;

        ArgumentCaptor<FetchOptions> optionsCaptor = ArgumentCaptor.forClass(FetchOptions.class);
        ArgumentCaptor<Long> cnCaptor = ArgumentCaptor.forClass(Long.class);
        when(mockFetcher.fetch(Mockito.eq("someSegment"), cnCaptor.capture(), optionsCaptor.capture())).thenReturn(response1, response2);

        FetchOptions originalOptions = new FetchOptions.Builder().targetChangeNumber(123).build();
        fetcher.fetch(originalOptions);
        List<Long> capturedCNs = cnCaptor.getAllValues();
        List<FetchOptions> capturedOptions = optionsCaptor.getAllValues();

        Assert.assertEquals(capturedOptions.size(), 2);
        Assert.assertEquals(capturedCNs.size(), 2);

        Assert.assertEquals(capturedCNs.get(0), Long.valueOf(-1));
        Assert.assertEquals(capturedCNs.get(1), Long.valueOf(1));

        Assert.assertEquals(capturedOptions.get(0).targetCN(), 123);
        Assert.assertEquals(capturedOptions.get(1).targetCN(), -1);

        // Ensure that the original value hasn't been modified
        Assert.assertEquals(originalOptions.targetCN(), 123);
    }

    private SegmentChange getSegmentChange(long since, long till){
        SegmentChange segmentChange = new SegmentChange();
        segmentChange.name = SEGMENT_NAME;
        segmentChange.since = since;
        segmentChange.till = till;
        segmentChange.added = new ArrayList<>();
        segmentChange.removed = new ArrayList<>();
        return  segmentChange;
    }
}
