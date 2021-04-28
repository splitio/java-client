package io.split.telemetry.synchronizer;

import io.split.TestHelper;
import io.split.cache.SegmentCache;
import io.split.cache.SegmentCacheInMemoryImpl;
import io.split.cache.SplitCache;
import io.split.client.SplitClientConfig;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorageConsumer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

public class SynchronizerMemoryTest {

    public static final String TELEMETRY_ENDPOINT = "https://telemetry.split.io/api/v1";

    @Test
    public void testSynchronizeConfig() throws URISyntaxException, NoSuchMethodException, IOException, IllegalAccessException, InvocationTargetException {
        CloseableHttpClient httpClient = TestHelper.mockHttpClient(TELEMETRY_ENDPOINT, HttpStatus.SC_OK);
        TelemetrySynchronizer telemetrySynchronizer = getTelemetrySynchronizer(httpClient);
        SplitClientConfig splitClientConfig = SplitClientConfig.builder().build();

        telemetrySynchronizer.synchronizeConfig(splitClientConfig, 100l, new HashMap<String,Long>(), new ArrayList<String>());
        Mockito.verify(httpClient, Mockito.times(1)).execute(Mockito.any());
    }


    @Test
    public void testSynchronizeStats() throws Exception {
        CloseableHttpClient httpClient = TestHelper.mockHttpClient(TELEMETRY_ENDPOINT, HttpStatus.SC_OK);
        TelemetrySynchronizer telemetrySynchronizer = getTelemetrySynchronizer(httpClient);

        telemetrySynchronizer.synchronizeStats();
        Mockito.verify(httpClient, Mockito.times(1)).execute(Mockito.any());
    }

    private TelemetrySynchronizer getTelemetrySynchronizer(CloseableHttpClient httpClient) throws URISyntaxException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        TelemetryStorageConsumer consumer = Mockito.mock(InMemoryTelemetryStorage.class);
        SplitCache splitCache = Mockito.mock(SplitCache.class);
        SegmentCache segmentCache = Mockito.mock(SegmentCacheInMemoryImpl.class);
        TelemetrySynchronizer telemetrySynchronizer = new SynchronizerMemory(httpClient, URI.create(TELEMETRY_ENDPOINT), consumer, splitCache, segmentCache);
        return telemetrySynchronizer;
    }

}