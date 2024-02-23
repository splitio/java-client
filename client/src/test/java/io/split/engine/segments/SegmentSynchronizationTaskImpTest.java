package io.split.engine.segments;

import com.google.common.collect.Maps;
import io.split.client.LocalhostSegmentChangeFetcher;
import io.split.client.JsonLocalhostSplitChangeFetcher;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.interceptors.FlagSetsFilterImpl;
import io.split.client.utils.InputStreamProvider;
import io.split.client.utils.StaticContentInputStreamProvider;
import io.split.engine.common.FetchOptions;
import io.split.engine.experiments.SplitChangeFetcher;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitFetcherImp;
import io.split.engine.experiments.SplitParser;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.storages.SegmentCacheProducer;
import io.split.storages.SplitCache;
import io.split.storages.SplitCacheConsumer;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.storages.memory.SegmentCacheInMemoryImpl;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.NoopTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

/**
 * Tests for SegmentSynchronizationTaskImp
 *
 * @author adil+
 */
public class SegmentSynchronizationTaskImpTest {
    private static final Logger _log = LoggerFactory.getLogger(SegmentSynchronizationTaskImpTest.class);
    private static final TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);
    private static final TelemetryStorage TELEMETRY_STORAGE_NOOP = Mockito.mock(NoopTelemetryStorage.class);

    private AtomicReference<SegmentFetcher> fetcher1 = null;
    private AtomicReference<SegmentFetcher> fetcher2 = null;

    @Before
    public void beforeMethod() {
        fetcher1 = new AtomicReference<>(null);
        fetcher2 = new AtomicReference<>(null);
    }

    @Test
    public void works() {
        SegmentCacheProducer segmentCacheProducer = Mockito.mock(SegmentCacheProducer.class);

        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        final SegmentSynchronizationTaskImp fetchers = new SegmentSynchronizationTaskImp(segmentChangeFetcher, 1L, 1, segmentCacheProducer,
                TELEMETRY_STORAGE, Mockito.mock(SplitCacheConsumer.class), null);


        // create two tasks that will separately call segment and make sure
        // that both of them get the exact same instance.
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                fetcher1.set(fetchers.getFetcher("foo"));
            }
        });

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                fetcher2.set(fetchers.getFetcher("foo"));
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

        Assert.assertNotNull(fetcher1.get());
        assertEquals(fetcher1.get(), fetcher2.get());
    }

    @Test
    public void testFetchAllAsynchronousAndGetFalse() throws NoSuchFieldException, IllegalAccessException {
        SegmentCacheProducer segmentCacheProducer = Mockito.mock(SegmentCacheProducer.class);
        ConcurrentMap<String, SegmentFetcher> _segmentFetchers = Maps.newConcurrentMap();

        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentFetcherImp segmentFetcher = Mockito.mock(SegmentFetcherImp.class);
        _segmentFetchers.put("SF", segmentFetcher);
        final SegmentSynchronizationTaskImp fetchers = new SegmentSynchronizationTaskImp(segmentChangeFetcher, 1L, 1,
                segmentCacheProducer, TELEMETRY_STORAGE, Mockito.mock(SplitCacheConsumer.class), null);
        Mockito.when(segmentFetcher.runWhitCacheHeader()).thenReturn(false);
        Mockito.when(segmentFetcher.fetch(Mockito.anyObject())).thenReturn(false);

        // Before executing, we'll update the map of segmentFecthers via reflection.
        Field segmentFetchersForced = SegmentSynchronizationTaskImp.class.getDeclaredField("_segmentFetchers");
        segmentFetchersForced.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(segmentFetchersForced, segmentFetchersForced.getModifiers() & ~Modifier.FINAL);

        segmentFetchersForced.set(fetchers, _segmentFetchers);
        boolean fetch = fetchers.fetchAllSynchronous();
        Assert.assertEquals(false, fetch);
    }

    @Test
    public void testFetchAllAsynchronousAndGetTrue() throws NoSuchFieldException, IllegalAccessException {
        SegmentCacheProducer segmentCacheProducer = Mockito.mock(SegmentCacheProducer.class);

        ConcurrentMap<String, SegmentFetcher> _segmentFetchers = Maps.newConcurrentMap();
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentFetcherImp segmentFetcher = Mockito.mock(SegmentFetcherImp.class);
        final SegmentSynchronizationTaskImp fetchers = new SegmentSynchronizationTaskImp(segmentChangeFetcher, 1L, 1, segmentCacheProducer,
                TELEMETRY_STORAGE, Mockito.mock(SplitCacheConsumer.class),  null);

        // Before executing, we'll update the map of segmentFecthers via reflection.
        Field segmentFetchersForced = SegmentSynchronizationTaskImp.class.getDeclaredField("_segmentFetchers");
        segmentFetchersForced.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(segmentFetchersForced, segmentFetchersForced.getModifiers() & ~Modifier.FINAL);
        segmentFetchersForced.set(fetchers, _segmentFetchers);
        Mockito.when(segmentFetcher.runWhitCacheHeader()).thenReturn(true);
        Mockito.when(segmentFetcher.fetch(Mockito.anyObject())).thenReturn(true);
        boolean fetch = fetchers.fetchAllSynchronous();
        Assert.assertEquals(true, fetch);
    }

    @Test
    public void testLocalhostSegmentChangeFetcher() throws InterruptedException, FileNotFoundException {
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(new HashSet<>());
        SplitCache splitCacheProducer = new InMemoryCacheImp(flagSetsFilter);

        InputStream inputStream = new FileInputStream("src/test/resources/split_init.json");
        InputStreamProvider inputStreamProvider = new StaticContentInputStreamProvider(inputStream);
        SplitChangeFetcher splitChangeFetcher = new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
        SplitParser splitParser = new SplitParser();
        FetchOptions fetchOptions = new FetchOptions.Builder().build();
        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheProducer, TELEMETRY_STORAGE_NOOP, flagSetsFilter);

        SplitSynchronizationTask splitSynchronizationTask = new SplitSynchronizationTask(splitFetcher, splitCacheProducer, 1000, null);

        splitSynchronizationTask.start();

        Thread.sleep(2000);

        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(LocalhostSegmentChangeFetcher.class);
        SegmentCacheProducer segmentCacheProducer = new SegmentCacheInMemoryImpl();

        SegmentSynchronizationTaskImp segmentSynchronizationTaskImp = new SegmentSynchronizationTaskImp(segmentChangeFetcher, 1000, 1, segmentCacheProducer,
                TELEMETRY_STORAGE_NOOP, splitCacheProducer, null);

        segmentSynchronizationTaskImp.start();

        Thread.sleep(2000);

        Mockito.verify(segmentChangeFetcher, Mockito.times(1)).fetch("segment_1",-1, fetchOptions);
    }
}