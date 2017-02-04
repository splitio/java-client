package io.split.engine.segments;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.split.engine.SDKReadinessGates;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for RefreshableSegmentFetcher.
 *
 * @author adil
 */
public class RefreshableSegmentTest {
    private static final Logger _log = LoggerFactory.getLogger(RefreshableSegmentTest.class);

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
        gates.registerSegments(Lists.newArrayList("foo"));

        OneChangeOnlySegmentChangeFetcher segmentChangeFetcher = new OneChangeOnlySegmentChangeFetcher();
        RefreshableSegment fetcher = new RefreshableSegment("foo", segmentChangeFetcher, startingChangeNumber, gates);

        // execute the fetcher for a little bit.
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> future = scheduledExecutorService.scheduleWithFixedDelay(fetcher, 0L, 100, TimeUnit.MICROSECONDS);
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

        assertThat(segmentChangeFetcher.changeHappenedAlready(), is(true));
        assertThat(fetcher.changeNumber(), is(equalTo((startingChangeNumber + 1))));
        assertThat(fetcher.fetch(), is(equalTo(expected)));
        assertThat(fetcher.segmentName(), is(equalTo("foo")));

        assertThat(gates.areSegmentsReady(10), is(true));

        try {
            fetcher.fetch().add("foo");
            fail("Client should not be able to edit the contents of a segment");
        } catch (Exception e) {
            // pass. we do not allow change in segment keys from the client.
        }
    }

    private void works(long startingChangeNumber) throws InterruptedException {
        SDKReadinessGates gates = new SDKReadinessGates();
        String segmentName = "foo";
        gates.registerSegments(Lists.newArrayList(segmentName));

        TheseManyChangesSegmentChangeFetcher segmentChangeFetcher = new TheseManyChangesSegmentChangeFetcher(2);
        RefreshableSegment fetcher = new RefreshableSegment(segmentName, segmentChangeFetcher, startingChangeNumber, gates);

        // execute the fetcher for a little bit.
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> future = scheduledExecutorService.scheduleWithFixedDelay(fetcher, 0L, Integer.MAX_VALUE, TimeUnit.SECONDS);
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


        Set<String> expected = Sets.newHashSet("" + fetcher.changeNumber());

        assertThat(segmentChangeFetcher.howManyChangesHappened(), is(greaterThan(1)));
        assertThat(fetcher.changeNumber(), is(greaterThan(startingChangeNumber)));
        assertThat(fetcher.fetch(), is(equalTo(expected)));
        assertThat(fetcher.contains("foobar"), is(false));
        assertThat(fetcher.segmentName(), is(equalTo("foo")));
        assertThat(gates.areSegmentsReady(10), is(true));

        try {
            fetcher.fetch().add("foo");
            fail("Client should not be able to edit the contents of a segment");
        } catch (Exception e) {
            // pass. we do not allow change in segment keys from the client.
        }
    }


    @Test(expected = NullPointerException.class)
    public void does_not_work_if_segment_change_fetcher_is_null() {
        RefreshableSegment fetcher = RefreshableSegment.create("foo", null, new SDKReadinessGates());
    }

    @Test(expected = NullPointerException.class)
    public void does_not_work_if_segment_name_is_null() {
        AChangePerCallSegmentChangeFetcher segmentChangeFetcher = new AChangePerCallSegmentChangeFetcher();
        RefreshableSegment fetcher = RefreshableSegment.create(null, segmentChangeFetcher, new SDKReadinessGates());
    }

    @Test(expected = NullPointerException.class)
    public void does_not_work_if_sdk_readiness_gates_are_null() {
        AChangePerCallSegmentChangeFetcher segmentChangeFetcher = new AChangePerCallSegmentChangeFetcher();
        RefreshableSegment fetcher = RefreshableSegment.create("foo", segmentChangeFetcher, null);
    }
}
