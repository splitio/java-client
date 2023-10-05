package io.split.telemetry.synchronizer;

import io.split.TestHelper;
import io.split.client.dtos.UniqueKeys;
import io.split.storages.SegmentCacheConsumer;
import io.split.storages.SplitCacheConsumer;
import io.split.client.ApiKeyCounter;
import io.split.client.SplitClientConfig;
import io.split.telemetry.domain.Config;
import io.split.telemetry.domain.Stats;
import io.split.telemetry.domain.StreamingEvent;

import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.MethodEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import io.split.telemetry.domain.enums.UpdatesFromSSEEnum;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.telemetry.storage.TelemetryStorage;
import io.split.telemetry.storage.TelemetryStorageConsumer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TelemetryInMemorySubmitterTest {
    private static final String FIRST_KEY = "KEY_1";
    private static final String SECOND_KEY = "KEY_2";
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

    @Test
    public void testSynchronizeUniqueKeys() throws Exception {
        CloseableHttpClient httpClient = TestHelper.mockHttpClient(TELEMETRY_ENDPOINT, HttpStatus.SC_OK);
        TelemetrySynchronizer telemetrySynchronizer = getTelemetrySynchronizer(httpClient);

        List<String> keys = new ArrayList<>();
        keys.add("key-1");
        keys.add("key-2");
        List<UniqueKeys.UniqueKey> uniqueKeys = new ArrayList<>();
        uniqueKeys.add(new UniqueKeys.UniqueKey("feature-1", keys));
        UniqueKeys imp = new UniqueKeys(uniqueKeys);

        telemetrySynchronizer.synchronizeUniqueKeys(imp);
        Mockito.verify(httpClient, Mockito.times(1)).execute(Mockito.any());
    }

    @Test
    public void testConfig() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException, URISyntaxException, NoSuchFieldException {
        ApiKeyCounter.getApiKeyCounterInstance().clearApiKeys();
        ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
        ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
        ApiKeyCounter.getApiKeyCounterInstance().add(FIRST_KEY);
        ApiKeyCounter.getApiKeyCounterInstance().add(SECOND_KEY);
        ApiKeyCounter.getApiKeyCounterInstance().add(SECOND_KEY);
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        CloseableHttpClient httpClient = TestHelper.mockHttpClient(TELEMETRY_ENDPOINT, HttpStatus.SC_OK);
        TelemetryInMemorySubmitter telemetrySynchronizer = getTelemetrySynchronizer(httpClient);
        SplitClientConfig splitClientConfig = SplitClientConfig.builder().flagSetsFilter(Arrays.asList("set1", "set2", "set-3")).build();
        populateConfig(telemetryStorage);
        Field teleTelemetryStorageConsumer = TelemetryInMemorySubmitter.class.getDeclaredField("_teleTelemetryStorageConsumer");
        teleTelemetryStorageConsumer.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(teleTelemetryStorageConsumer, teleTelemetryStorageConsumer.getModifiers() & ~Modifier.FINAL);
        teleTelemetryStorageConsumer.set(telemetrySynchronizer, telemetryStorage);
        Config config = telemetrySynchronizer.generateConfig(splitClientConfig, 100l, ApiKeyCounter.getApiKeyCounterInstance().getFactoryInstances(), new ArrayList<>());
        Assert.assertEquals(3, config.getRedundantFactories());
        Assert.assertEquals(2, config.getBurTimeouts());
        Assert.assertEquals(3, config.getNonReadyUsages());
        Assert.assertEquals(3, config.getFlagSetsTotal());
        Assert.assertEquals(1, config.getFlagSetsInvalid());
    }

    @Test
    public void testStats() throws Exception {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        CloseableHttpClient httpClient = TestHelper.mockHttpClient(TELEMETRY_ENDPOINT, HttpStatus.SC_OK);
        TelemetryInMemorySubmitter telemetrySynchronizer = getTelemetrySynchronizer(httpClient);
        populateStats(telemetryStorage);
        Field teleTelemetryStorageConsumer = TelemetryInMemorySubmitter.class.getDeclaredField("_teleTelemetryStorageConsumer");
        teleTelemetryStorageConsumer.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(teleTelemetryStorageConsumer, teleTelemetryStorageConsumer.getModifiers() & ~Modifier.FINAL);

        teleTelemetryStorageConsumer.set(telemetrySynchronizer, telemetryStorage);
        Stats stats = telemetrySynchronizer.generateStats();
        Assert.assertEquals(2, stats.getMethodLatencies().getTreatment().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(2, stats.getMethodLatencies().getTreatments().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(1, stats.getMethodLatencies().getTreatmentsWithConfig().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(1, stats.getMethodLatencies().getTreatmentWithConfig().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(1, stats.getMethodLatencies().getTreatmentByFlagSet().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(1, stats.getMethodLatencies().getTreatmentByFlagSets().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(1, stats.getMethodLatencies().getTreatmentWithConfigByFlagSet().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(1, stats.getMethodLatencies().getTreatmentWithConfigByFlagSets().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(0, stats.getMethodLatencies().getTrack().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(3, stats.getHttpLatencies().get_splits().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(2, stats.getHttpLatencies().get_telemetry().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(2, stats.getHttpLatencies().get_events().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(1, stats.getHttpLatencies().get_segments().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(1, stats.getHttpLatencies().get_impressions().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(1, stats.getHttpLatencies().get_impressionsCount().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(0, stats.getHttpLatencies().get_token().stream().mapToInt(Long::intValue).sum());
        Assert.assertEquals(2, stats.getMethodExceptions().getTreatment());
        Assert.assertEquals(2, stats.getMethodExceptions().getTreatments());
        Assert.assertEquals(1, stats.getMethodExceptions().getTreatmentsWithConfig());
        Assert.assertEquals(1, stats.getMethodExceptions().getTreatmentWithConfig());
        Assert.assertEquals(1, stats.getMethodExceptions().getTreatmentWithConfig());
        Assert.assertEquals(1, stats.getMethodExceptions().getTreatmentByFlagSet());
        Assert.assertEquals(1, stats.getMethodExceptions().getTreatmentByFlagSets());
        Assert.assertEquals(1, stats.getMethodExceptions().getTreatmentWithConfigByFlagSet());
        Assert.assertEquals(1, stats.getMethodExceptions().getTreatmentWithConfigByFlagSets());
        Assert.assertEquals(0, stats.getMethodExceptions().getTrack());
        Assert.assertEquals(1, stats.getAuthRejections());
        Assert.assertEquals(2, stats.getTokenRefreshes());
        Assert.assertEquals(4, stats.getImpressionsDeduped());
        Assert.assertEquals(12, stats.getImpressionsDropped());
        Assert.assertEquals(0, stats.getImpressionsQueued());
        Assert.assertEquals(10, stats.getEventsDropped());
        Assert.assertEquals(3, stats.getEventsQueued());
        Assert.assertEquals(800, stats.getLastSynchronization().get_events());
        Assert.assertEquals(129, stats.getLastSynchronization().get_token());
        Assert.assertEquals(1580, stats.getLastSynchronization().get_segments());
        Assert.assertEquals(0, stats.getLastSynchronization().get_splits());
        Assert.assertEquals(10500, stats.getLastSynchronization().get_impressions());
        Assert.assertEquals(1500, stats.getLastSynchronization().get_impressionsCount());
        Assert.assertEquals(265, stats.getLastSynchronization().get_telemetry());
        Assert.assertEquals(91218, stats.getSessionLengthMs());
        Assert.assertEquals(2, stats.getHttpErrors().get_telemetry().get(400l).intValue());
        Assert.assertEquals(1, stats.getHttpErrors().get_segments().get(501l).intValue());
        Assert.assertEquals(2, stats.getHttpErrors().get_impressions().get(403l).intValue());
        Assert.assertEquals(1, stats.getHttpErrors().get_impressionsCount().get(403l).intValue());
        Assert.assertEquals(1, stats.getHttpErrors().get_events().get(503l).intValue());
        Assert.assertEquals(1, stats.getHttpErrors().get_splits().get(403l).intValue());
        Assert.assertEquals(1, stats.getHttpErrors().get_token().get(403l).intValue());
        List<StreamingEvent> streamingEvents = stats.getStreamingEvents();
        Assert.assertEquals(290, streamingEvents.get(0).get_data());
        Assert.assertEquals(1, streamingEvents.get(0).get_type());
        Assert.assertEquals(91218, streamingEvents.get(0).getTimestamp());
        Assert.assertEquals(1, stats.getUpdatesFromSSE().getSplits());
    }

    private TelemetryInMemorySubmitter getTelemetrySynchronizer(CloseableHttpClient httpClient) throws URISyntaxException {
        TelemetryStorageConsumer consumer = Mockito.mock(InMemoryTelemetryStorage.class);
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        SplitCacheConsumer splitCacheConsumer = Mockito.mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = Mockito.mock(SegmentCacheConsumer.class);
        TelemetryInMemorySubmitter telemetrySynchronizer = new TelemetryInMemorySubmitter(httpClient, URI.create(TELEMETRY_ENDPOINT), consumer, splitCacheConsumer, segmentCacheConsumer, telemetryRuntimeProducer, 0l);
        return telemetrySynchronizer;
    }

    private void populateStats(TelemetryStorage telemetryStorage) {
        telemetryStorage.recordLatency(MethodEnum.TREATMENT, 1500l * 1000);
        telemetryStorage.recordLatency(MethodEnum.TREATMENT, 2000l * 1000);
        telemetryStorage.recordLatency(MethodEnum.TREATMENTS, 3000l * 1000);
        telemetryStorage.recordLatency(MethodEnum.TREATMENTS, 500l * 1000);
        telemetryStorage.recordLatency(MethodEnum.TREATMENT_WITH_CONFIG, 800l * 1000);
        telemetryStorage.recordLatency(MethodEnum.TREATMENTS_WITH_CONFIG, 1000l * 1000);
        telemetryStorage.recordLatency(MethodEnum.TREATMENTS_BY_FLAG_SET, 1000l * 1000);
        telemetryStorage.recordLatency(MethodEnum.TREATMENTS_BY_FLAG_SETS, 1000l * 1000);
        telemetryStorage.recordLatency(MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SET, 1000l * 1000);
        telemetryStorage.recordLatency(MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS, 1000l * 1000);

        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.TELEMETRY, 1500l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.TELEMETRY, 2000l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.EVENTS, 1500l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.EVENTS, 2000l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.SEGMENTS, 1500l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.SPLITS, 2000l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.SPLITS, 1500l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.SPLITS, 2000l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.IMPRESSIONS, 1500l * 1000);
        telemetryStorage.recordSyncLatency(HTTPLatenciesEnum.IMPRESSIONS_COUNT, 2000l * 1000);

        telemetryStorage.recordException(MethodEnum.TREATMENT);
        telemetryStorage.recordException(MethodEnum.TREATMENTS);
        telemetryStorage.recordException(MethodEnum.TREATMENT);
        telemetryStorage.recordException(MethodEnum.TREATMENTS);
        telemetryStorage.recordException(MethodEnum.TREATMENT_WITH_CONFIG);
        telemetryStorage.recordException(MethodEnum.TREATMENTS_WITH_CONFIG);
        telemetryStorage.recordException(MethodEnum.TREATMENTS_BY_FLAG_SET);
        telemetryStorage.recordException(MethodEnum.TREATMENTS_BY_FLAG_SETS);
        telemetryStorage.recordException(MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SET);
        telemetryStorage.recordException(MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS);

        telemetryStorage.recordAuthRejections();

        telemetryStorage.recordTokenRefreshes();
        telemetryStorage.recordTokenRefreshes();

        telemetryStorage.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DEDUPED, 3);
        telemetryStorage.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DEDUPED, 1);
        telemetryStorage.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED, 4);
        telemetryStorage.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED, 6);
        telemetryStorage.recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED, 2);

        telemetryStorage.recordEventStats(EventsDataRecordsEnum.EVENTS_DROPPED, 3);
        telemetryStorage.recordEventStats(EventsDataRecordsEnum.EVENTS_DROPPED, 7);
        telemetryStorage.recordEventStats(EventsDataRecordsEnum.EVENTS_QUEUED, 3);

        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.EVENTS, 1500);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.EVENTS, 800);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.IMPRESSIONS, 2500);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.IMPRESSIONS, 10500);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.IMPRESSIONS_COUNT, 1500);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.SEGMENTS, 1580);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.TELEMETRY, 265);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecordsEnum.TOKEN, 129);

        telemetryStorage.recordSessionLength(91218);

        telemetryStorage.recordSyncError(ResourceEnum.TELEMETRY_SYNC, 400);
        telemetryStorage.recordSyncError(ResourceEnum.TELEMETRY_SYNC, 400);
        telemetryStorage.recordSyncError(ResourceEnum.SEGMENT_SYNC, 501);
        telemetryStorage.recordSyncError(ResourceEnum.IMPRESSION_SYNC, 403);
        telemetryStorage.recordSyncError(ResourceEnum.IMPRESSION_SYNC, 403);
        telemetryStorage.recordSyncError(ResourceEnum.EVENT_SYNC, 503);
        telemetryStorage.recordSyncError(ResourceEnum.SPLIT_SYNC, 403);
        telemetryStorage.recordSyncError(ResourceEnum.IMPRESSION_COUNT_SYNC, 403);
        telemetryStorage.recordSyncError(ResourceEnum.TOKEN_SYNC, 403);

        StreamingEvent streamingEvent = new StreamingEvent(1, 290, 91218);
        telemetryStorage.recordStreamingEvents(streamingEvent);
        telemetryStorage.recordUpdatesFromSSE(UpdatesFromSSEEnum.SPLITS);
    }

    private void populateConfig(TelemetryStorage telemetryStorage) {
        telemetryStorage.recordBURTimeout();
        telemetryStorage.recordBURTimeout();
        telemetryStorage.recordNonReadyUsage();
        telemetryStorage.recordNonReadyUsage();
        telemetryStorage.recordNonReadyUsage();
    }
}