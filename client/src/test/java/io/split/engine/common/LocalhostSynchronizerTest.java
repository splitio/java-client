package io.split.engine.common;

import io.split.engine.experiments.LocalhostSplitChangeFetcher;
import io.split.engine.experiments.SplitChangeFetcher;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitFetcherImp;
import io.split.engine.experiments.SplitParser;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.storages.SplitCacheConsumer;
import io.split.storages.SplitCacheProducer;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.telemetry.storage.NoopTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class LocalhostSynchronizerTest {

    private static final TelemetryStorage TELEMETRY_STORAGE_NOOP = Mockito.mock(NoopTelemetryStorage.class);

    @Test
    public void testSyncAll(){
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp();
        SplitCacheConsumer splitCacheConsumer = Mockito.mock(SplitCacheConsumer.class);

        SplitChangeFetcher splitChangeFetcher = new LocalhostSplitChangeFetcher("src/test/resources/split_init.json");
        SplitParser splitParser = new SplitParser();

        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheConsumer, splitCacheProducer, TELEMETRY_STORAGE_NOOP);
        SplitSynchronizationTask splitSynchronizationTask = new SplitSynchronizationTask(splitFetcher, splitCacheProducer, 1000L);
        SplitTasks splitTasks = SplitTasks.build(splitSynchronizationTask, null, null, null, null, null);

        LocalhostSynchronizer localhostSynchronizer = new LocalhostSynchronizer(splitTasks, splitFetcher);

        Assert.assertTrue(localhostSynchronizer.syncAll());
    }

    @Test
    public void testPeriodicFetching() throws InterruptedException {
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp();
        SplitCacheConsumer splitCacheConsumer = Mockito.mock(SplitCacheConsumer.class);

        SplitChangeFetcher splitChangeFetcher = Mockito.mock(LocalhostSplitChangeFetcher.class);
        SplitParser splitParser = new SplitParser();

        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheConsumer, splitCacheProducer, TELEMETRY_STORAGE_NOOP);
        SplitSynchronizationTask splitSynchronizationTask = new SplitSynchronizationTask(splitFetcher, splitCacheProducer, 1000L);
        FetchOptions fetchOptions = new FetchOptions.Builder().build();

        SplitTasks splitTasks = SplitTasks.build(splitSynchronizationTask, null, null, null, null, null);
        LocalhostSynchronizer localhostSynchronizer = new LocalhostSynchronizer(splitTasks, splitFetcher);

        localhostSynchronizer.startPeriodicFetching();

        Thread.sleep(2000);

        Mockito.verify(splitChangeFetcher, Mockito.times(1)).fetch(-1, fetchOptions);
    }

    @Test
    public void testRefreshSplits(){
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp();
        SplitCacheConsumer splitCacheConsumer = Mockito.mock(SplitCacheConsumer.class);
        SplitChangeFetcher splitChangeFetcher = Mockito.mock(SplitChangeFetcher.class);
        SplitParser splitParser = new SplitParser();

        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheConsumer, splitCacheProducer, TELEMETRY_STORAGE_NOOP);
        SplitSynchronizationTask splitSynchronizationTask = new SplitSynchronizationTask(splitFetcher, splitCacheProducer, 1000L);
        SplitTasks splitTasks = SplitTasks.build(splitSynchronizationTask, null, null, null, null, null);
        LocalhostSynchronizer localhostSynchronizer = new LocalhostSynchronizer(splitTasks, splitFetcher);

        localhostSynchronizer.refreshSplits(null);

        Mockito.verify(splitChangeFetcher, Mockito.times(1)).fetch(Mockito.anyLong(), Mockito.anyObject());
    }
}