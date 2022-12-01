package io.split.engine.experiments;

import io.split.engine.SDKReadinessGates;
import io.split.engine.common.FetchOptions;
import io.split.storages.SplitCacheConsumer;
import io.split.storages.SplitCacheProducer;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.NoopTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SplitSynchronizationTaskTest {

    private static final Logger _log = LoggerFactory.getLogger(SplitSynchronizationTaskTest.class);

    private static final TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);
    private static final TelemetryStorage TELEMETRY_STORAGE_NOOP = Mockito.mock(NoopTelemetryStorage.class);

    @Test
    public void works() {
        SDKReadinessGates gates = new SDKReadinessGates();
        SplitCacheProducer splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        final SplitSynchronizationTask fetcher = new SplitSynchronizationTask(splitFetcher, splitCacheProducer, 1L);


        // create two tasks that will separately call segment and make sure
        // that both of them get the exact same instance.
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                fetcher.start();
            }
        });

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10L, TimeUnit.SECONDS)) {
                _log.info("Executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = executorService.shutdownNow();
                _log.info("Executor was abruptly shut down. These tasks will not be executed: " + droppedTasks);
            }
        } catch (InterruptedException e) {
            // reset the interrupt.
            Thread.currentThread().interrupt();
        }
        Mockito.when(splitFetcher.forceRefresh(Mockito.anyObject())).thenReturn(Mockito.anyObject());
    }

    @Test
    public void testLocalhost() {
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp();
        SplitCacheConsumer splitCacheConsumer = Mockito.mock(SplitCacheConsumer.class);

        SplitChangeFetcher splitChangeFetcher = new LocalhostSplitChangeFetcher("src/test/resources/split_init.json");
        SplitParser splitParser = new SplitParser();
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);
        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheConsumer, splitCacheProducer, TELEMETRY_STORAGE_NOOP);

        FetchResult fetchResult = splitFetcher.forceRefresh(fetchOptions);

        Assert.assertEquals(1, fetchResult.getSegments().size());
    }
}