package io.split.telemetry.synchronizer;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.SplitClientConfig;
import io.split.client.dtos.UniqueKeys;
import io.split.client.impressions.ImpressionListener;
import io.split.client.impressions.ImpressionsManager;
import io.split.integrations.IntegrationsConfig;
import io.split.integrations.NewRelicListener;
import io.split.storages.SegmentCacheConsumer;
import io.split.storages.SplitCacheConsumer;
import io.split.telemetry.domain.Config;
import io.split.telemetry.domain.Rates;
import io.split.telemetry.domain.Stats;
import io.split.telemetry.domain.URLOverrides;
import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.telemetry.storage.TelemetryStorageConsumer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class TelemetryInMemorySubmitter implements TelemetrySynchronizer{

    private static final int OPERATION_MODE = 0;
    private static  final String STORAGE = "memory";

    private HttpTelemetryMemorySender _httpHttpTelemetryMemorySender;
    private TelemetryStorageConsumer _telemetryStorageConsumer;
    private SplitCacheConsumer _splitCacheConsumer;
    private SegmentCacheConsumer _segmentCacheConsumer;
    private final long _initStartTime;

    public TelemetryInMemorySubmitter(CloseableHttpClient client, URI telemetryRootEndpoint, TelemetryStorageConsumer telemetryStorageConsumer,
                                      SplitCacheConsumer splitCacheConsumer, SegmentCacheConsumer segmentCacheConsumer,
                                      TelemetryRuntimeProducer telemetryRuntimeProducer, long initStartTime) throws URISyntaxException {
        _httpHttpTelemetryMemorySender = HttpTelemetryMemorySender.create(client, telemetryRootEndpoint, telemetryRuntimeProducer);
        _telemetryStorageConsumer = checkNotNull(telemetryStorageConsumer);
        _splitCacheConsumer = checkNotNull(splitCacheConsumer);
        _segmentCacheConsumer = checkNotNull(segmentCacheConsumer);
        _initStartTime = initStartTime;
    }

    @Override
    public void synchronizeConfig(SplitClientConfig config, long readyTimeStamp, Map<String, Long> factoryInstances, List<String> tags) {
        _httpHttpTelemetryMemorySender.postConfig(generateConfig(config, readyTimeStamp, factoryInstances, tags));
    }

    @Override
    public void synchronizeStats() throws Exception {
        _httpHttpTelemetryMemorySender.postStats(generateStats());
    }

    @Override
    public void synchronizeUniqueKeys(UniqueKeys uniqueKeys){
        _httpHttpTelemetryMemorySender.postUniqueKeys(uniqueKeys);
    }

    @Override
    public void finalSynchronization() throws Exception {
        Stats stats = generateStats();
        stats.setSplitCount(_splitCacheConsumer.getAll().stream().count());
        stats.setSegmentCount(_segmentCacheConsumer.getSegmentCount());
        stats.setSegmentKeyCount(_segmentCacheConsumer.getKeyCount());
        _httpHttpTelemetryMemorySender.postStats(stats);
    }

    @VisibleForTesting
    Stats generateStats() throws Exception {
        Stats stats = new Stats();
        stats.setLastSynchronization(_telemetryStorageConsumer.getLastSynchronization());
        stats.setMethodLatencies(_telemetryStorageConsumer.popLatencies());
        stats.setMethodExceptions(_telemetryStorageConsumer.popExceptions());
        stats.setHttpErrors(_telemetryStorageConsumer.popHTTPErrors());
        stats.setHttpLatencies(_telemetryStorageConsumer.popHTTPLatencies());
        stats.setTokenRefreshes(_telemetryStorageConsumer.popTokenRefreshes());
        stats.setAuthRejections(_telemetryStorageConsumer.popAuthRejections());
        stats.setImpressionsQueued(_telemetryStorageConsumer.getImpressionsStats(ImpressionsDataTypeEnum.IMPRESSIONS_QUEUED));
        stats.setImpressionsDeduped(_telemetryStorageConsumer.getImpressionsStats(ImpressionsDataTypeEnum.IMPRESSIONS_DEDUPED));
        stats.setImpressionsDropped(_telemetryStorageConsumer.getImpressionsStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED));
        stats.setSplitCount(_splitCacheConsumer.getAll().stream().count());
        stats.setSegmentCount(_segmentCacheConsumer.getSegmentCount());
        stats.setSegmentKeyCount(_segmentCacheConsumer.getKeyCount());
        stats.setSessionLengthMs(_telemetryStorageConsumer.getSessionLength());
        stats.setEventsQueued(_telemetryStorageConsumer.getEventStats(EventsDataRecordsEnum.EVENTS_QUEUED));
        stats.setEventsDropped(_telemetryStorageConsumer.getEventStats(EventsDataRecordsEnum.EVENTS_DROPPED));
        stats.setStreamingEvents(_telemetryStorageConsumer.popStreamingEvents());
        stats.setTags(_telemetryStorageConsumer.popTags());
        stats.setUpdatesFromSSE(_telemetryStorageConsumer.popUpdatesFromSSE());
        return stats;
    }

    @VisibleForTesting
    Config generateConfig(SplitClientConfig splitClientConfig, long readyTimestamp, Map<String, Long> factoryInstances, List<String> tags) {
        Config config = new Config();
        Rates rates = new Rates();
        URLOverrides urlOverrides = new URLOverrides();
        List<IntegrationsConfig.ImpressionListenerWithMeta> impressionsListeners = new ArrayList<>();
        if(splitClientConfig.integrationsConfig() != null) {
            impressionsListeners.addAll(splitClientConfig.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.ASYNC));
            impressionsListeners.addAll(splitClientConfig.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.SYNC));
        }
        List<String> impressions = getImpressions(impressionsListeners);

        rates.setTelemetry(splitClientConfig.getTelemetryRefreshRate());
        rates.setEvents(splitClientConfig.eventSendIntervalInMillis());
        rates.setImpressions(splitClientConfig.impressionsRefreshRate());
        rates.setSegments(splitClientConfig.segmentsRefreshRate());
        rates.setSplits(splitClientConfig.featuresRefreshRate());

        urlOverrides.setAuth(!SplitClientConfig.AUTH_ENDPOINT.equals(splitClientConfig.authServiceURL()));
        urlOverrides.setStream(!SplitClientConfig.STREAMING_ENDPOINT.equals(splitClientConfig.streamingServiceURL()));
        urlOverrides.setSdk(!SplitClientConfig.SDK_ENDPOINT.equals(splitClientConfig.endpoint()));
        urlOverrides.setEvents(!SplitClientConfig.EVENTS_ENDPOINT.equals(splitClientConfig.eventsEndpoint()));
        urlOverrides.setTelemetry(!SplitClientConfig.TELEMETRY_ENDPOINT.equals(splitClientConfig.telemetryURL()));

        config.setBurTimeouts(_telemetryStorageConsumer.getBURTimeouts());
        config.setNonReadyUsages(_telemetryStorageConsumer.getNonReadyUsages());
        config.setHttpProxyDetected(splitClientConfig.proxy() != null);
        config.setImpressionsMode(getImpressionsMode(splitClientConfig));
        config.setIntegrations(impressions);
        config.setImpressionsListenerEnabled((impressionsListeners.size()-impressions.size()) > 0);
        config.setOperationMode(OPERATION_MODE);
        config.setStorage(STORAGE);
        config.setImpressionsQueueSize(splitClientConfig.impressionsQueueSize());
        config.setRedundantFactories(getRedundantFactories(factoryInstances));
        config.setEventsQueueSize(splitClientConfig.eventsQueueSize());
        config.setTags(getListMaxSize(tags));
        config.setActiveFactories(factoryInstances.size());
        config.setTimeUntilReady(readyTimestamp - _initStartTime);
        config.setRates(rates);
        config.setUrlOverrides(urlOverrides);
        config.setStreamingEnabled(splitClientConfig.streamingEnabled());
        int invalidSets = splitClientConfig.getInvalidSets();
        config.setFlagSetsTotal(splitClientConfig.getSetsFilter().size() + invalidSets);
        config.setFlagSetsInvalid(invalidSets);
        return config;
    }

    private long getRedundantFactories(Map<String, Long> factoryInstances) {
        long count = 0;
        for(Long l :factoryInstances.values()) {
            count = count + l - 1l;
        }
        return count;
    }

    private int getImpressionsMode(SplitClientConfig config) {
        return ImpressionsManager.Mode.OPTIMIZED.equals(config.impressionsMode()) ? 0 : 1;
    }

    private List<String> getListMaxSize(List<String> list) {
        return list.size()> 10 ? list.subList(0, 10) : list;
    }

    private List<String> getImpressions(List<IntegrationsConfig.ImpressionListenerWithMeta> impressionsListeners) {
        List<String> impressions = new ArrayList<>();
        for(IntegrationsConfig.ImpressionListenerWithMeta il: impressionsListeners) {
            ImpressionListener listener = il.listener();
            if(listener instanceof NewRelicListener) {
                impressions.add(NewRelicListener.class.getName());
            }
        }
        return impressions;
    }
}