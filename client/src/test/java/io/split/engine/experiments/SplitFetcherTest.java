package io.split.engine.experiments;

import com.google.common.collect.Lists;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.interceptors.FlagSetsFilterImpl;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.storages.SegmentCache;
import io.split.storages.memory.SegmentCacheInMemoryImpl;
import io.split.storages.SplitCache;
import io.split.client.dtos.*;
import io.split.engine.ConditionsTestUtil;
import io.split.engine.common.FetchOptions;
import io.split.engine.matchers.AllKeysMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.segments.SegmentChangeFetcher;
import io.split.engine.segments.SegmentSynchronizationTask;
import io.split.engine.segments.SegmentSynchronizationTaskImp;
import io.split.grammar.Treatments;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
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
    private static final FlagSetsFilter FLAG_SETS_FILTER = new FlagSetsFilterImpl(new HashSet<>());

    @Test
    @Ignore //This test is ignore since is deprecated. We can review this in a future.
    public void worksWhenWeStartWithoutAnyState() throws InterruptedException {
        works(0);
    }

    @Test
    @Ignore //This test is ignore since is deprecated. We can review this in a future.
    public void worksWhenWeStartWithAnyState() throws InterruptedException {
        works(11L);
    }

    private void works(long startingChangeNumber) throws InterruptedException {
        AChangePerCallSplitChangeFetcher splitChangeFetcher = new AChangePerCallSplitChangeFetcher();
        SplitCache cache = new InMemoryCacheImp(startingChangeNumber, FLAG_SETS_FILTER);
        SplitFetcherImp fetcher = new SplitFetcherImp(splitChangeFetcher, new SplitParser(), cache, TELEMETRY_STORAGE, FLAG_SETS_FILTER);

        // execute the fetcher for a little bit.
        executeWaitAndTerminate(fetcher, 1, 3, TimeUnit.SECONDS);

        assertThat(splitChangeFetcher.lastAdded(), is(greaterThan(startingChangeNumber)));

        // all previous splits have been removed since they are dead
        for (long i = startingChangeNumber; i < cache.getChangeNumber(); i++) {
            assertThat("Asking for " + i + " " + cache.getAll(), cache.get("" + i), is(not(nullValue())));
            assertThat(cache.get("" + i).killed(), is(true));
        }

        ParsedCondition expectedParsedCondition = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(ConditionsTestUtil.partition("on", 10)));
        List<ParsedCondition> expectedListOfMatcherAndSplits = Lists.newArrayList(expectedParsedCondition);
        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("" + cache.getChangeNumber(), (int) cache.getChangeNumber(), false, Treatments.OFF, expectedListOfMatcherAndSplits, null, cache.getChangeNumber(), 1, new HashSet<>());

        ParsedSplit actual = cache.get("" + cache.getChangeNumber());
        Thread.sleep(1000);
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void whenParserFailsWeRemoveTheExperiment() throws InterruptedException {
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
        when(splitChangeFetcher.fetch(Mockito.eq(-1L), Mockito.any())).thenReturn(validReturn);
        when(splitChangeFetcher.fetch(Mockito.eq(0L), Mockito.any())).thenReturn(invalidReturn);
        when(splitChangeFetcher.fetch(Mockito.eq(1L), Mockito.any())).thenReturn(noReturn);

        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        SplitCache cache = new InMemoryCacheImp(-1, FLAG_SETS_FILTER);

        SegmentChangeFetcher segmentChangeFetcher = mock(SegmentChangeFetcher.class);
        SegmentSynchronizationTask segmentSynchronizationTask = new SegmentSynchronizationTaskImp(segmentChangeFetcher, 1,10, segmentCache, TELEMETRY_STORAGE, cache, null);
        segmentSynchronizationTask.start();
        SplitFetcherImp fetcher = new SplitFetcherImp(splitChangeFetcher, new SplitParser(), cache, TELEMETRY_STORAGE, FLAG_SETS_FILTER);

        // execute the fetcher for a little bit.
        executeWaitAndTerminate(fetcher, 1, 5, TimeUnit.SECONDS);

        assertEquals(1L, cache.getChangeNumber());
        // verify that the fetcher return null
        Assert.assertNull(cache.get("-1"));
    }

    @Test
    public void ifThereIsAProblemTalkingToSplitChangeCountDownLatchIsNotDecremented() throws Exception {
        SplitCache cache = new InMemoryCacheImp(-1, FLAG_SETS_FILTER);

        SplitChangeFetcher splitChangeFetcher = mock(SplitChangeFetcher.class);
        when(splitChangeFetcher.fetch(-1L, new FetchOptions.Builder().build())).thenThrow(new RuntimeException());
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();

        SegmentChangeFetcher segmentChangeFetcher = mock(SegmentChangeFetcher.class);
        SegmentSynchronizationTask segmentSynchronizationTask = new SegmentSynchronizationTaskImp(segmentChangeFetcher, 1,10, segmentCache, TELEMETRY_STORAGE, cache, null);
        segmentSynchronizationTask.start();
        SplitFetcherImp fetcher = new SplitFetcherImp(splitChangeFetcher, new SplitParser(), cache, TELEMETRY_STORAGE, FLAG_SETS_FILTER);

        // execute the fetcher for a little bit.
        executeWaitAndTerminate(fetcher, 1, 5, TimeUnit.SECONDS);

        Assert.assertEquals(-1L, cache.getChangeNumber());
    }

    @Test
    public void addFeatureFlags() throws InterruptedException {
        SplitCache cache = new InMemoryCacheImp(-1, new FlagSetsFilterImpl(new HashSet<>(Arrays.asList("set_1", "set_2"))));

        Split featureFlag1 = new Split();
        featureFlag1.status = Status.ACTIVE;
        featureFlag1.seed = (int) -1;
        featureFlag1.conditions = Lists.newArrayList(ConditionsTestUtil.makeAllKeysCondition(Lists.newArrayList(ConditionsTestUtil.partition("on", 10))));
        featureFlag1.defaultTreatment = Treatments.OFF;
        featureFlag1.name = "feature_flag";
        featureFlag1.sets = new HashSet<>(Arrays.asList("set_1", "set_2"));
        featureFlag1.trafficAllocation = 100;
        featureFlag1.trafficAllocationSeed = 147392224;

        SplitChange validReturn = new SplitChange();
        validReturn.splits = Lists.newArrayList(featureFlag1);
        validReturn.since = -1L;
        validReturn.till = 0L;

        SplitChangeFetcher splitChangeFetcher = mock(SplitChangeFetcher.class);
        when(splitChangeFetcher.fetch(Mockito.eq(-1L), Mockito.any())).thenReturn(validReturn);

        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(new HashSet<>(Arrays.asList("set_1", "set_2")));
        SplitFetcherImp fetcher = new SplitFetcherImp(splitChangeFetcher, new SplitParser(), cache, TELEMETRY_STORAGE, flagSetsFilter);

        executeWaitAndTerminate(fetcher, 1, 5, TimeUnit.SECONDS);

        Assert.assertTrue(cache.getNamesByFlagSets(Arrays.asList("set_1", "set_2")).get("set_1").contains("feature_flag"));
        Assert.assertTrue(cache.getNamesByFlagSets(Arrays.asList("set_1", "set_2")).get("set_2").contains("feature_flag"));

        featureFlag1.sets.remove("set_2");

        validReturn = new SplitChange();
        validReturn.splits = Lists.newArrayList(featureFlag1);
        validReturn.since = 0L;
        validReturn.till = 1L;

        when(splitChangeFetcher.fetch(Mockito.eq(0L), Mockito.any())).thenReturn(validReturn);

        executeWaitAndTerminate(fetcher, 1, 5, TimeUnit.SECONDS);

        Assert.assertTrue(cache.getNamesByFlagSets(Arrays.asList("set_1", "set_2")).get("set_1").contains("feature_flag"));
        Assert.assertFalse(cache.getNamesByFlagSets(Arrays.asList("set_1", "set_2")).get("set_2").contains("feature_flag"));
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
    @Ignore //This test is ignore since is deprecated. We can review this in a future.
    public void worksWithUserDefinedSegments() throws Exception {
        long startingChangeNumber = -1;
        String segmentName = "foosegment";
        AChangePerCallSplitChangeFetcher experimentChangeFetcher = new AChangePerCallSplitChangeFetcher(segmentName);
        SplitCache cache = new InMemoryCacheImp(startingChangeNumber, FLAG_SETS_FILTER);
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();

        SegmentChangeFetcher segmentChangeFetcher = mock(SegmentChangeFetcher.class);
        SegmentChange segmentChange = getSegmentChange(0L, 0L, segmentName);
        when(segmentChangeFetcher.fetch(anyString(), anyLong(), any())).thenReturn(segmentChange);
        SegmentSynchronizationTask segmentSynchronizationTask = new SegmentSynchronizationTaskImp(segmentChangeFetcher, 1,10, segmentCache, Mockito.mock(TelemetryStorage.class), cache, null);
        segmentSynchronizationTask.start();
        SplitFetcherImp fetcher = new SplitFetcherImp(experimentChangeFetcher, new SplitParser(), cache,  TELEMETRY_STORAGE, FLAG_SETS_FILTER);

        // execute the fetcher for a little bit.
        executeWaitAndTerminate(fetcher, 1, 5, TimeUnit.SECONDS);

        assertThat(experimentChangeFetcher.lastAdded(), is(greaterThan(startingChangeNumber)));
        assertThat(cache.getChangeNumber(), is(equalTo(experimentChangeFetcher.lastAdded())));

        // all previous splits have been removed since they are dead
        for (long i = startingChangeNumber; i < cache.getChangeNumber(); i++) {
            assertThat("Asking for " + i + " " + cache.getAll(), cache.get("" + i), is(not(nullValue())));
            assertThat(cache.get("" + i).killed(), is(true));
        }
    }

    @Test
    public void testBypassCdnClearedAfterFirstHit() {
        SplitChangeFetcher mockFetcher = Mockito.mock(SplitChangeFetcher.class);
        SplitParser mockParser = new SplitParser();
        SplitCache mockCache = new InMemoryCacheImp(FLAG_SETS_FILTER);
        SplitFetcherImp fetcher = new SplitFetcherImp(mockFetcher, mockParser, mockCache, Mockito.mock(TelemetryRuntimeProducer.class), FLAG_SETS_FILTER);


        SplitChange response1 = new SplitChange();
        response1.splits = new ArrayList<>();
        response1.since = -1;
        response1.till = 1;

        SplitChange response2 = new SplitChange();
        response2.splits = new ArrayList<>();
        response2.since = 1;
        response2.till = 1;


        ArgumentCaptor<FetchOptions> optionsCaptor = ArgumentCaptor.forClass(FetchOptions.class);
        ArgumentCaptor<Long> cnCaptor = ArgumentCaptor.forClass(Long.class);
        when(mockFetcher.fetch(cnCaptor.capture(), optionsCaptor.capture())).thenReturn(response1, response2);

        FetchOptions originalOptions = new FetchOptions.Builder().targetChangeNumber(123).build();
        fetcher.forceRefresh(originalOptions);
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