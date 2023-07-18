package io.split.engine.experiments;

import io.split.client.JsonLocalhostSplitChangeFetcher;
import io.split.engine.common.FetchOptions;
import io.split.storages.SplitCacheProducer;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.telemetry.storage.NoopTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class SplitFetcherImpTest {

    private static final TelemetryStorage TELEMETRY_STORAGE_NOOP = Mockito.mock(NoopTelemetryStorage.class);

    @Test
    public void testLocalHost(){
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp();

        SplitChangeFetcher splitChangeFetcher = new JsonLocalhostSplitChangeFetcher("src/test/resources/split_init.json");
        SplitParser splitParser = new SplitParser();
        FetchOptions fetchOptions = new FetchOptions.Builder().build();
        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheProducer, TELEMETRY_STORAGE_NOOP);

        FetchResult fetchResult = splitFetcher.forceRefresh(fetchOptions);

        Assert.assertEquals(1, fetchResult.getSegments().size());
    }
}