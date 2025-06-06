package io.split.engine.experiments;

import io.split.client.JsonLocalhostSplitChangeFetcher;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.interceptors.FlagSetsFilterImpl;
import io.split.engine.common.FetchOptions;
import io.split.storages.RuleBasedSegmentCacheProducer;
import io.split.storages.SplitCacheProducer;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.storages.memory.RuleBasedSegmentCacheInMemoryImp;
import io.split.telemetry.storage.NoopTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;

public class SplitSynchronizationTaskTest {

    private static final TelemetryStorage TELEMETRY_STORAGE_NOOP = Mockito.mock(NoopTelemetryStorage.class);
    private static final FlagSetsFilter FLAG_SETS_FILTER = new FlagSetsFilterImpl(new HashSet<>());

    @Test
    public void testLocalhost() throws InterruptedException {
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp(FLAG_SETS_FILTER);

        SplitChangeFetcher splitChangeFetcher = Mockito.mock(JsonLocalhostSplitChangeFetcher.class);
        SplitParser splitParser = new SplitParser();
        FetchOptions fetchOptions = new FetchOptions.Builder().build();
        RuleBasedSegmentCacheProducer ruleBasedSegmentCacheProducer = new RuleBasedSegmentCacheInMemoryImp();
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();
        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheProducer, TELEMETRY_STORAGE_NOOP, FLAG_SETS_FILTER,
                ruleBasedSegmentParser, ruleBasedSegmentCacheProducer);

        SplitSynchronizationTask splitSynchronizationTask = new SplitSynchronizationTask(splitFetcher, splitCacheProducer, 1000, null);

        splitSynchronizationTask.start();

        Thread.sleep(2000);

        Mockito.verify(splitChangeFetcher, Mockito.times(1)).fetch(-1, -1, fetchOptions);
    }

    @Test
    public void testStartAndStop() throws InterruptedException {
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp(FLAG_SETS_FILTER);
        SplitFetcherImp splitFetcherImp = Mockito.mock(SplitFetcherImp.class);
        SplitSynchronizationTask splitSynchronizationTask = new SplitSynchronizationTask(splitFetcherImp, splitCacheProducer, 1000, null);
        splitSynchronizationTask.start();

        Thread.sleep(2000);

        Mockito.verify(splitFetcherImp, Mockito.times(1)).run();
    }
}