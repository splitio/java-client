package io.split.telemetry.synchronizer;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.SplitClientConfig;
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

public class TelemetrySubmitter implements TelemetrySynchronizer{

    private static final int OPERATION_MODE = 0;
    private static  final String STORAGE = "memory";

    private HttpTelemetryMemorySender _httpHttpTelemetryMemorySender;
    private TelemetryStorageConsumer _teleTelemetryStorageConsumer;
    private SplitCacheConsumer _splitCacheConsumer;
    private SegmentCacheConsumer _segmentCacheConsumer;
    private final long _initStartTime;

    public TelemetrySubmitter(CloseableHttpClient client, URI telemetryRootEndpoint, TelemetryStorageConsumer telemetryStorageConsumer, SplitCacheConsumer splitCacheConsumer,
                              SegmentCacheConsumer segmentCacheConsumer, TelemetryRuntimeProducer telemetryRuntimeProducer, long initStartTime) throws URISyntaxException {
        _httpHttpTelemetryMemorySender = HttpTelemetryMemorySender.create(client, telemetryRootEndpoint, telemetryRuntimeProducer);
        _teleTelemetryStorageConsumer = checkNotNull(telemetryStorageConsumer);
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
    public void finalSynchronization(long splitCount, long segmentCount, long segmentKeyCount) throws Exception {
        Stats stats = generateStats();
        stats.set_splitCount(splitCount);
        stats.set_segmentCount(segmentCount);
        stats.set_segmentKeyCount(segmentKeyCount);
        _httpHttpTelemetryMemorySender.postStats(stats);
    }

    @VisibleForTesting
    Stats generateStats() throws Exception {
        Stats stats = new Stats();
        stats.set_lastSynchronization(_teleTelemetryStorageConsumer.getLastSynchronization());
        stats.set_methodLatencies(_teleTelemetryStorageConsumer.popLatencies());
        stats.set_methodExceptions(_teleTelemetryStorageConsumer.popExceptions());
        stats.set_httpErrors(_teleTelemetryStorageConsumer.popHTTPErrors());
        stats.set_httpLatencies(_teleTelemetryStorageConsumer.popHTTPLatencies());
        stats.set_tokenRefreshes(_teleTelemetryStorageConsumer.popTokenRefreshes());
        stats.set_authRejections(_teleTelemetryStorageConsumer.popAuthRejections());
        stats.set_impressionsQueued(_teleTelemetryStorageConsumer.getImpressionsStats(ImpressionsDataTypeEnum.IMPRESSIONS_QUEUED));
        stats.set_impressionsDeduped(_teleTelemetryStorageConsumer.getImpressionsStats(ImpressionsDataTypeEnum.IMPRESSIONS_DEDUPED));
        stats.set_impressionsDropped(_teleTelemetryStorageConsumer.getImpressionsStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED));
        stats.set_splitCount(_splitCacheConsumer.getAll().stream().count());
        stats.set_segmentCount(_segmentCacheConsumer.getSegmentCount());
        stats.set_segmentKeyCount(_segmentCacheConsumer.getKeyCount());
        stats.set_sessionLengthMs(_teleTelemetryStorageConsumer.getSessionLength());
        stats.set_eventsQueued(_teleTelemetryStorageConsumer.getEventStats(EventsDataRecordsEnum.EVENTS_QUEUED));
        stats.set_eventsDropped(_teleTelemetryStorageConsumer.getEventStats(EventsDataRecordsEnum.EVENTS_DROPPED));
        stats.set_streamingEvents(_teleTelemetryStorageConsumer.popStreamingEvents());
        stats.set_tags(_teleTelemetryStorageConsumer.popTags());
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

        rates.set_telemetry(splitClientConfig.get_telemetryRefreshRate());
        rates.set_events(splitClientConfig.eventFlushIntervalInMillis());
        rates.set_impressions(splitClientConfig.impressionsRefreshRate());
        rates.set_segments(splitClientConfig.segmentsRefreshRate());
        rates.set_splits(splitClientConfig.featuresRefreshRate());

        urlOverrides.set_auth(!SplitClientConfig.AUTH_ENDPOINT.equals(splitClientConfig.authServiceURL()));
        urlOverrides.set_stream(!SplitClientConfig.STREAMING_ENDPOINT.equals(splitClientConfig.streamingServiceURL()));
        urlOverrides.set_sdk(!SplitClientConfig.SDK_ENDPOINT.equals(splitClientConfig.endpoint()));
        urlOverrides.set_events(!SplitClientConfig.EVENTS_ENDPOINT.equals(splitClientConfig.eventsEndpoint()));
        urlOverrides.set_telemetry(!SplitClientConfig.TELEMETRY_ENDPOINT.equals(splitClientConfig.telemetryURL()));

        config.set_burTimeouts(_teleTelemetryStorageConsumer.getBURTimeouts());
        config.set_nonReadyUsages(_teleTelemetryStorageConsumer.getNonReadyUsages());
        config.set_httpProxyDetected(splitClientConfig.proxy() != null);
        config.set_impressionsMode(getImpressionsMode(splitClientConfig));
        config.set_integrations(impressions);
        config.set_impressionsListenerEnabled((impressionsListeners.size()-impressions.size()) > 0);
        config.set_operationMode(OPERATION_MODE);
        config.set_storage(STORAGE);
        config.set_impressionsQueueSize(splitClientConfig.impressionsQueueSize());
        config.set_redundantFactories(getRedundantFactories(factoryInstances));
        config.set_eventsQueueSize(splitClientConfig.eventsQueueSize());
        config.set_tags(getListMaxSize(tags));
        config.set_activeFactories(factoryInstances.size());
        config.set_timeUntilReady(readyTimestamp - _initStartTime);
        config.set_rates(rates);
        config.set_urlOverrides(urlOverrides);
        config.set_streamingEnabled(splitClientConfig.streamingEnabled());
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
