package io.split.engine.common;

import io.split.client.LocalhostSegmentChangeFetcher;
import io.split.client.JsonLocalhostSplitChangeFetcher;
import io.split.client.utils.FileInputStreamProvider;
import io.split.client.utils.InputStreamProvider;
import io.split.engine.experiments.SplitChangeFetcher;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitFetcherImp;
import io.split.engine.experiments.SplitParser;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentChangeFetcher;
import io.split.engine.segments.SegmentSynchronizationTaskImp;
import io.split.storages.SegmentCacheProducer;
import io.split.storages.SplitCache;
import io.split.storages.SplitCacheProducer;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.storages.memory.SegmentCacheInMemoryImpl;
import io.split.telemetry.storage.NoopTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.FileNotFoundException;

public class LocalhostSynchronizerTest {

    private static final TelemetryStorage TELEMETRY_STORAGE_NOOP = Mockito.mock(NoopTelemetryStorage.class);

    @Test
    public void testSyncAll() throws FileNotFoundException {
        SplitCache splitCacheProducer = new InMemoryCacheImp();

        InputStreamProvider inputStreamProvider = new FileInputStreamProvider("src/test/resources/split_init.json");
        SplitChangeFetcher splitChangeFetcher = new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
        SplitParser splitParser = new SplitParser();

        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheProducer, TELEMETRY_STORAGE_NOOP);
        SplitSynchronizationTask splitSynchronizationTask = new SplitSynchronizationTask(splitFetcher, splitCacheProducer, 1000L, null);

        SegmentChangeFetcher segmentChangeFetcher = new LocalhostSegmentChangeFetcher("src/test/resources/");
        SegmentCacheProducer segmentCacheProducer = new SegmentCacheInMemoryImpl();

        SegmentSynchronizationTaskImp segmentSynchronizationTaskImp = new SegmentSynchronizationTaskImp(segmentChangeFetcher, 1000, 1, segmentCacheProducer,
                TELEMETRY_STORAGE_NOOP, splitCacheProducer, null);
        SplitTasks splitTasks = SplitTasks.build(splitSynchronizationTask, segmentSynchronizationTaskImp, null, null, null, null);

        LocalhostSynchronizer localhostSynchronizer = new LocalhostSynchronizer(splitTasks, splitFetcher, false);

        Assert.assertTrue(localhostSynchronizer.syncAll());
    }

    @Test
    public void testPeriodicFetching() throws InterruptedException {
        SplitCache splitCacheProducer = new InMemoryCacheImp();

        SplitChangeFetcher splitChangeFetcher = Mockito.mock(JsonLocalhostSplitChangeFetcher.class);
        SplitParser splitParser = new SplitParser();

        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheProducer, TELEMETRY_STORAGE_NOOP);
        SplitSynchronizationTask splitSynchronizationTask = new SplitSynchronizationTask(splitFetcher, splitCacheProducer, 1000L, null);
        FetchOptions fetchOptions = new FetchOptions.Builder().build();

        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(LocalhostSegmentChangeFetcher.class);
        SegmentCacheProducer segmentCacheProducer = new SegmentCacheInMemoryImpl();

        SegmentSynchronizationTaskImp segmentSynchronizationTaskImp = new SegmentSynchronizationTaskImp(segmentChangeFetcher, 1000, 1, segmentCacheProducer,
                TELEMETRY_STORAGE_NOOP, splitCacheProducer, null);

        SplitTasks splitTasks = SplitTasks.build(splitSynchronizationTask, segmentSynchronizationTaskImp, null, null, null, null);
        LocalhostSynchronizer localhostSynchronizer = new LocalhostSynchronizer(splitTasks, splitFetcher, true);

        localhostSynchronizer.startPeriodicFetching();

        Thread.sleep(2000);

        Mockito.verify(splitChangeFetcher, Mockito.times(1)).fetch(-1, fetchOptions);
    }

    @Test
    public void testRefreshSplits() {
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp();
        SplitChangeFetcher splitChangeFetcher = Mockito.mock(SplitChangeFetcher.class);
        SplitParser splitParser = new SplitParser();

        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheProducer, TELEMETRY_STORAGE_NOOP);
        SplitSynchronizationTask splitSynchronizationTask = new SplitSynchronizationTask(splitFetcher, splitCacheProducer, 1000L, null);
        SplitTasks splitTasks = SplitTasks.build(splitSynchronizationTask, null, null, null, null, null);
        LocalhostSynchronizer localhostSynchronizer = new LocalhostSynchronizer(splitTasks, splitFetcher, false);

        localhostSynchronizer.refreshSplits(null);

        Mockito.verify(splitChangeFetcher, Mockito.times(1)).fetch(Mockito.anyLong(), Mockito.anyObject());
    }
}