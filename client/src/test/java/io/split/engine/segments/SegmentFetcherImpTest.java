package io.split.engine.segments;

import com.google.common.collect.Sets;
import io.split.cache.SegmentCacheInMemoryImpl;
import io.split.client.dtos.SegmentChange;
import io.split.engine.SDKReadinessGates;
import io.split.cache.SegmentCache;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Tests for RefreshableSegmentFetcher.
 *
 * @author adil
 */
public class SegmentFetcherImpTest {
    private static final Logger _log = LoggerFactory.getLogger(SegmentFetcherImpTest.class);

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
        SDKReadinessGates gates = new SDKReadinessGates();
        gates.registerSegment("foo");
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();

        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChange = new SegmentChange();
        segmentChange.name = "foo";
        segmentChange.since = -1;
        segmentChange.till = 10;
        segmentChange.added = new ArrayList<>();
        segmentChange.removed = new ArrayList<>();
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong())).thenReturn(segmentChange);

        SegmentFetcherImp fetcher = new SegmentFetcherImp("foo", segmentChangeFetcher, gates, segmentCache);

        // execute the fetcher for a little bit.
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(fetcher, 0L, 100, TimeUnit.MICROSECONDS);
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

        assertNotNull(segmentCache.getChangeNumber("foo"));
        assertEquals(10L, segmentCache.getChangeNumber("foo"));
        assertThat(gates.areSegmentsReady(10), is(true));

    }

    private void works(long startingChangeNumber) throws InterruptedException {
        SDKReadinessGates gates = new SDKReadinessGates();
        String segmentName = "foo";
        gates.registerSegment(segmentName);
        SegmentCache segmentCache = Mockito.mock(SegmentCache.class);
        Mockito.when(segmentCache.getChangeNumber("foo")).thenReturn(-1L).thenReturn(-1L)
        .thenReturn(-1L)
        .thenReturn(0L);

        //TheseManyChangesSegmentChangeFetcher segmentChangeFetcher = new TheseManyChangesSegmentChangeFetcher(2);
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChange = new SegmentChange();
        segmentChange.name = "foo";
        segmentChange.since = -1;
        segmentChange.till = -1;
        Mockito.when(segmentChangeFetcher.fetch("foo", -1L)).thenReturn(segmentChange);
        Mockito.when(segmentChangeFetcher.fetch("foo", 0L)).thenReturn(segmentChange);
        SegmentFetcherImp fetcher = new SegmentFetcherImp(segmentName, segmentChangeFetcher, gates, segmentCache);

        // execute the fetcher for a little bit.
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(fetcher, 0L, Integer.MAX_VALUE, TimeUnit.SECONDS);
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
        Mockito.verify(segmentChangeFetcher, Mockito.times(2)).fetch(Mockito.anyString(), Mockito.anyLong());
        assertThat(gates.areSegmentsReady(10), is(true));

    }


    @Test(expected = NullPointerException.class)
    public void does_not_work_if_segment_change_fetcher_is_null() {
        SegmentCache segmentCache = Mockito.mock(SegmentCache.class);
        SegmentFetcher fetcher = new SegmentFetcherImp("foo", null, new SDKReadinessGates(), segmentCache);
    }

    @Test(expected = NullPointerException.class)
    public void does_not_work_if_segment_name_is_null() {
        SegmentCache segmentCache = Mockito.mock(SegmentCache.class);
        AChangePerCallSegmentChangeFetcher segmentChangeFetcher = new AChangePerCallSegmentChangeFetcher();
        SegmentFetcher fetcher = new SegmentFetcherImp(null, segmentChangeFetcher, new SDKReadinessGates(), segmentCache);
    }

    @Test(expected = NullPointerException.class)
    public void does_not_work_if_sdk_readiness_gates_are_null() {
        SegmentCache segmentCache = Mockito.mock(SegmentCache.class);
        AChangePerCallSegmentChangeFetcher segmentChangeFetcher = new AChangePerCallSegmentChangeFetcher();
        SegmentFetcher fetcher = new SegmentFetcherImp("foo", segmentChangeFetcher, null, segmentCache);
    }
}
