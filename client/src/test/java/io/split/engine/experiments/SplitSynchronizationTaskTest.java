package io.split.engine.experiments;

import io.split.client.JsonLocalhostSplitChangeFetcher;
import io.split.engine.common.FetchOptions;
import io.split.storages.SplitCacheProducer;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.telemetry.storage.NoopTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Test;
import org.mockito.Mockito;

public class SplitSynchronizationTaskTest {

    private static final TelemetryStorage TELEMETRY_STORAGE_NOOP = Mockito.mock(NoopTelemetryStorage.class);

    @Test
    public void testLocalhost() throws InterruptedException {
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp();

        SplitChangeFetcher splitChangeFetcher = Mockito.mock(JsonLocalhostSplitChangeFetcher.class);
        SplitParser splitParser = new SplitParser();
        FetchOptions fetchOptions = new FetchOptions.Builder().build();
        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheProducer, TELEMETRY_STORAGE_NOOP);

        SplitSynchronizationTask splitSynchronizationTask = new SplitSynchronizationTask(splitFetcher, splitCacheProducer, 1000, null);

        splitSynchronizationTask.start();

        Thread.sleep(2000);

        Mockito.verify(splitChangeFetcher, Mockito.times(1)).fetch(-1, fetchOptions);
    }

    @Test
    public void testStartAndStop() throws InterruptedException {
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp();
        SplitFetcherImp splitFetcherImp = Mockito.mock(SplitFetcherImp.class);
        SplitSynchronizationTask splitSynchronizationTask = new SplitSynchronizationTask(splitFetcherImp, splitCacheProducer, 1000, null);
        splitSynchronizationTask.start();

        Thread.sleep(2000);

        Mockito.verify(splitFetcherImp, Mockito.times(1)).run();
    }
}