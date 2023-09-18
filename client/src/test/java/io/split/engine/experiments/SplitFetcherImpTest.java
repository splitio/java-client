package io.split.engine.experiments;

import io.split.client.JsonLocalhostSplitChangeFetcher;
import io.split.client.utils.FileInputStreamProvider;
import io.split.client.utils.InputStreamProvider;
import io.split.engine.common.FetchOptions;
import io.split.storages.SplitCacheProducer;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.telemetry.storage.NoopTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;

public class SplitFetcherImpTest {

    private static final TelemetryStorage TELEMETRY_STORAGE_NOOP = Mockito.mock(NoopTelemetryStorage.class);

    @Test
    public void testLocalHost() {
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp();

        InputStreamProvider inputStreamProvider = new FileInputStreamProvider("src/test/resources/split_init.json");
        SplitChangeFetcher splitChangeFetcher = new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
        SplitParser splitParser = new SplitParser();
        FetchOptions fetchOptions = new FetchOptions.Builder().build();
        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheProducer, TELEMETRY_STORAGE_NOOP, new HashSet<>());

        FetchResult fetchResult = splitFetcher.forceRefresh(fetchOptions);

        Assert.assertEquals(1, fetchResult.getSegments().size());
    }
}