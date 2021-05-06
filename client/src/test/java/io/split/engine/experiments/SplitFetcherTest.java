package io.split.engine.experiments;

import com.google.common.collect.Lists;
import io.split.cache.InMemoryCacheImp;
import io.split.cache.SegmentCache;
import io.split.cache.SegmentCacheInMemoryImpl;
import io.split.cache.SplitCache;
import io.split.client.dtos.*;
import io.split.engine.ConditionsTestUtil;
import io.split.engine.SDKReadinessGates;
import io.split.engine.matchers.AllKeysMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.segments.SegmentChangeFetcher;
import io.split.engine.segments.SegmentSynchronizationTask;
import io.split.engine.segments.SegmentSynchronizationTaskImp;
import io.split.grammar.Treatments;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by adilaijaz on 5/11/15.
 */
public class SplitFetcherTest {
    private static final Logger _log = LoggerFactory.getLogger(SplitFetcherTest.class);
    private static final TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);

    @Test
    public void works_when_we_start_without_any_state() throws InterruptedException {
        works(0);
    }

    @Test
    public void works_when_we_start_with_any_state() throws InterruptedException {
        works(11L);
    }

    private void works(long startingChangeNumber) throws InterruptedException {
        AChangePerCallSplitChangeFetcher splitChangeFetcher = new AChangePerCallSplitChangeFetcher();
        SDKReadinessGates gates = new SDKReadinessGates();
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentSynchronizationTask segmentSynchronizationTask = new SegmentSynchronizationTaskImp(segmentChangeFetcher,1,10, gates, segmentCache, TELEMETRY_STORAGE);
        SplitCache cache = new InMemoryCacheImp(startingChangeNumber);
        SplitFetcherImp fetcher = new SplitFetcherImp(splitChangeFetcher, new SplitParser(segmentSynchronizationTask, segmentCache), gates, cache, TELEMETRY_STORAGE);

        // execute the fetcher for a little bit.
        executeWaitAndTerminate(fetcher, 1, 3, TimeUnit.SECONDS);

        assertThat(splitChangeFetcher.lastAdded(), is(greaterThan(startingChangeNumber)));
        assertThat(cache.getChangeNumber(), is(equalTo(splitChangeFetcher.lastAdded())));

        // all previous splits have been removed since they are dead
        for (long i = startingChangeNumber; i < cache.getChangeNumber(); i++) {
            assertThat("Asking for " + i + " " + cache.getAll(), cache.get("" + i), is(not(nullValue())));
            assertThat(cache.get("" + i).killed(), is(true));
        }

        ParsedCondition expectedParsedCondition = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(ConditionsTestUtil.partition("on", 10)));
        List<ParsedCondition> expectedListOfMatcherAndSplits = Lists.newArrayList(expectedParsedCondition);
        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("" + cache.getChangeNumber(), (int) cache.getChangeNumber(), false, Treatments.OFF, expectedListOfMatcherAndSplits, null, cache.getChangeNumber(), 1);

        ParsedSplit actual = cache.get("" + cache.getChangeNumber());

        assertThat(actual, is(equalTo(expected)));
        assertThat(gates.areSplitsReady(0), is(equalTo(true)));
    }

    @Test
    public void when_parser_fails_we_remove_the_experiment() throws InterruptedException {
        SDKReadinessGates gates = new SDKReadinessGates();
        Split validSplit = new Split();
        validSplit.status = Status.ACTIVE;
        validSplit.seed = (int) -1;
        validSplit.conditions = Lists.newArrayList(ConditionsTestUtil.makeAllKeysCondition(Lists.newArrayList(ConditionsTestUtil.partition("on", 10))));
        validSplit.defaultTreatment = Treatments.OFF;
        validSplit.name = "-1";

        SplitChange validReturn = new SplitChange();
        validReturn.splits = Lists.newArrayList(validSplit);
        validReturn.since = -1L;
        validReturn.till = 0L;

        MatcherGroup invalidMatcherGroup = new MatcherGroup();
        invalidMatcherGroup.matchers = Lists.<Matcher>newArrayList();

        Condition invalidCondition = new Condition();
        invalidCondition.matcherGroup = invalidMatcherGroup;
        invalidCondition.partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 10));

        Split invalidSplit = new Split();
        invalidSplit.status = Status.ACTIVE;
        invalidSplit.seed = (int) -1;
        invalidSplit.conditions = Lists.newArrayList(invalidCondition);
        invalidSplit.defaultTreatment = Treatments.OFF;
        invalidSplit.name = "-1";

        SplitChange invalidReturn = new SplitChange();
        invalidReturn.splits = Lists.newArrayList(invalidSplit);
        invalidReturn.since = 0L;
        invalidReturn.till = 1L;

        SplitChange noReturn = new SplitChange();
        noReturn.splits = Lists.<Split>newArrayList();
        noReturn.since = 1L;
        noReturn.till = 1L;

        SplitChangeFetcher splitChangeFetcher = mock(SplitChangeFetcher.class);
        when(splitChangeFetcher.fetch(-1L, false)).thenReturn(validReturn);
        when(splitChangeFetcher.fetch(0L, false)).thenReturn(invalidReturn);
        when(splitChangeFetcher.fetch(1L, false)).thenReturn(noReturn);

        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();

        SegmentChangeFetcher segmentChangeFetcher = mock(SegmentChangeFetcher.class);
        SegmentSynchronizationTask segmentSynchronizationTask = new SegmentSynchronizationTaskImp(segmentChangeFetcher, 1,10, gates, segmentCache, TELEMETRY_STORAGE);
        segmentSynchronizationTask.startPeriodicFetching();
        SplitCache cache = new InMemoryCacheImp(-1);
        SplitFetcherImp fetcher = new SplitFetcherImp(splitChangeFetcher, new SplitParser(segmentSynchronizationTask, segmentCache), gates, cache, TELEMETRY_STORAGE);

        // execute the fetcher for a little bit.
        executeWaitAndTerminate(fetcher, 1, 5, TimeUnit.SECONDS);

        assertThat(cache.getChangeNumber(), is(equalTo(1L)));
        // verify that the fetcher return null
        assertThat(cache.get("-1"), is(nullValue()));
    }

    @Test
    public void if_there_is_a_problem_talking_to_split_change_count_down_latch_is_not_decremented() throws Exception {
        SDKReadinessGates gates = new SDKReadinessGates();
        SplitCache cache = new InMemoryCacheImp(-1);

        SplitChangeFetcher splitChangeFetcher = mock(SplitChangeFetcher.class);
        when(splitChangeFetcher.fetch(-1L, false)).thenThrow(new RuntimeException());
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();

        SegmentChangeFetcher segmentChangeFetcher = mock(SegmentChangeFetcher.class);
        SegmentSynchronizationTask segmentSynchronizationTask = new SegmentSynchronizationTaskImp(segmentChangeFetcher, 1,10, gates, segmentCache, TELEMETRY_STORAGE);
        segmentSynchronizationTask.startPeriodicFetching();
        SplitFetcherImp fetcher = new SplitFetcherImp(splitChangeFetcher, new SplitParser(segmentSynchronizationTask, segmentCache), gates, cache, TELEMETRY_STORAGE);

        // execute the fetcher for a little bit.
        executeWaitAndTerminate(fetcher, 1, 5, TimeUnit.SECONDS);

        assertThat(cache.getChangeNumber(), is(equalTo(-1L)));
        assertThat(gates.areSplitsReady(0), is(equalTo(false)));
    }

    private void executeWaitAndTerminate(Runnable runnable, long frequency, long waitInBetween, TimeUnit unit) throws InterruptedException {
        // execute the fetcher for a little bit.
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(runnable, 0L, frequency, unit);
        Thread.currentThread().sleep(unit.toMillis(waitInBetween));

        scheduledExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(10L, TimeUnit.SECONDS)) {
                _log.info("Executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = scheduledExecutorService.shutdownNow();
                _log.info("Executor was abruptly shut down. These tasks will not be executed: " + droppedTasks);
            }
        } catch (InterruptedException e) {
            // reset the interrupt.
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void works_with_user_defined_segments() throws Exception {
        long startingChangeNumber = -1;
        String segmentName = "foosegment";
        AChangePerCallSplitChangeFetcher experimentChangeFetcher = new AChangePerCallSplitChangeFetcher(segmentName);
        SDKReadinessGates gates = new SDKReadinessGates();
        SplitCache cache = new InMemoryCacheImp(startingChangeNumber);
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();

        SegmentChangeFetcher segmentChangeFetcher = mock(SegmentChangeFetcher.class);
        SegmentChange segmentChange = getSegmentChange(0L, 0L, segmentName);
        when(segmentChangeFetcher.fetch(anyString(), anyLong(), anyBoolean())).thenReturn(segmentChange);
        SegmentSynchronizationTask segmentSynchronizationTask = new SegmentSynchronizationTaskImp(segmentChangeFetcher, 1,10, gates, segmentCache, TELEMETRY_STORAGE);
        segmentSynchronizationTask.startPeriodicFetching();
        SplitFetcherImp fetcher = new SplitFetcherImp(experimentChangeFetcher, new SplitParser(segmentSynchronizationTask, segmentCache), gates, cache, TELEMETRY_STORAGE);

        // execute the fetcher for a little bit.
        executeWaitAndTerminate(fetcher, 1, 5, TimeUnit.SECONDS);

        assertThat(experimentChangeFetcher.lastAdded(), is(greaterThan(startingChangeNumber)));
        assertThat(cache.getChangeNumber(), is(equalTo(experimentChangeFetcher.lastAdded())));

        // all previous splits have been removed since they are dead
        for (long i = startingChangeNumber; i < cache.getChangeNumber(); i++) {
            assertThat("Asking for " + i + " " + cache.getAll(), cache.get("" + i), is(not(nullValue())));
            assertThat(cache.get("" + i).killed(), is(true));
        }

        assertThat(gates.areSplitsReady(0), is(equalTo(true)));
        assertThat(gates.isSegmentRegistered(segmentName), is(equalTo(true)));
        assertThat(gates.areSegmentsReady(100), is(equalTo(true)));
        assertThat(gates.isSDKReady(0), is(equalTo(true)));
    }

    private SegmentChange getSegmentChange(long since, long till, String segmentName){
        SegmentChange segmentChange = new SegmentChange();
        segmentChange.name = segmentName;
        segmentChange.since = since;
        segmentChange.till = till;
        segmentChange.added = new ArrayList<>();
        segmentChange.removed = new ArrayList<>();
        return  segmentChange;
    }
}
