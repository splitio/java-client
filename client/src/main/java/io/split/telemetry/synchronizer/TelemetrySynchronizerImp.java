package io.split.telemetry.synchronizer;

import io.split.cache.SegmentCache;
import io.split.cache.SplitCache;
import io.split.client.SplitClientConfig;
import io.split.telemetry.domain.Config;
import io.split.telemetry.domain.Rates;
import io.split.telemetry.domain.Stats;
import io.split.telemetry.domain.URLOverrides;
import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.storage.TelemetryStorageConsumer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class TelemetrySynchronizerImp implements TelemetrySynchronizer{

    private HttpTelemetryMemorySender _httpHttpTelemetryMemorySender;
    private TelemetryStorageConsumer _teleTelemetryStorageConsumer;
    private SplitCache _splitCache;
    private SegmentCache _segmentCache;

    public TelemetrySynchronizerImp(CloseableHttpClient client, URI telemetryRootEndpoint, TelemetryStorageConsumer telemetryStorageConsumer, SplitCache splitCache,
            SegmentCache segmentCache) throws URISyntaxException {
        _httpHttpTelemetryMemorySender = HttpTelemetryMemorySender.create(client, telemetryRootEndpoint);
        _teleTelemetryStorageConsumer = telemetryStorageConsumer;
        _splitCache = splitCache;
        _segmentCache = segmentCache;
    }

    @Override
    public void synchronizeConfig(SplitClientConfig config, long timeUntilReady, Map<String, Long> factoryInstances, List<String> tags) {
        _httpHttpTelemetryMemorySender.postConfig(generateConfig(config, timeUntilReady, factoryInstances, tags));
    }

    @Override
    public void synchronizeStats() throws Exception {
        _httpHttpTelemetryMemorySender.postStats(generateStats());
    }

    private Stats generateStats() throws Exception {
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
        stats.set_splitCount(_splitCache.getAll().stream().count());
        stats.set_segmentCount(_segmentCache.getAll().stream().count());
        stats.set_segmentKeyCount(_segmentCache.getAllKeys().stream().count());
        stats.set_sessionLengthMs(_teleTelemetryStorageConsumer.getSessionLength());
        stats.set_eventsQueued(_teleTelemetryStorageConsumer.getEventStats(EventsDataRecordsEnum.EVENTS_QUEUED));
        stats.set_eventsDropped(_teleTelemetryStorageConsumer.getEventStats(EventsDataRecordsEnum.EVENTS_DROPPED));
        stats.set_streamingEvents(_teleTelemetryStorageConsumer.popStreamingEvents());
        stats.set_tags(_teleTelemetryStorageConsumer.popTags());
        return stats;
    }

    private Config generateConfig(SplitClientConfig splitClientConfig, long timeUntilReady, Map<String, Long> factoryInstances, List<String> tags) {
        Config config = new Config();
        Rates rates = new Rates();
        URLOverrides urlOverrides = new URLOverrides();

        rates.set_telemetry(splitClientConfig.get_telemetryRefreshRate());
        rates.set_events(splitClientConfig.eventFlushIntervalInMillis());
        rates.set_impressions(splitClientConfig.impressionsRefreshRate());
        rates.set_segments(splitClientConfig.segmentsRefreshRate());
        rates.set_splits(splitClientConfig.featuresRefreshRate());

        urlOverrides.set_auth(SplitClientConfig.AUTH_ENDPOINT.equals(splitClientConfig.authServiceURL()));
        urlOverrides.set_stream(SplitClientConfig.STREAMING_ENDPOINT.equals(splitClientConfig.streamingServiceURL()));
        urlOverrides.set_sdk(SplitClientConfig.SDK_ENDPOINT.equals(splitClientConfig.endpoint()));
        urlOverrides.set_events(SplitClientConfig.EVENTS_ENDPOINT.equals(splitClientConfig.eventsEndpoint()));
        urlOverrides.set_telemetry(SplitClientConfig.TELEMETRY_ENDPOINT.equals(splitClientConfig.get_telemetryURL()));

        config.set_burTimeouts(_teleTelemetryStorageConsumer.getBURTimeouts());
        config.set_nonReadyUsages(_teleTelemetryStorageConsumer.getNonReadyUsages());
        config.set_impressionsListenerEnabled(splitClientConfig.);
        config.set_httpProxyDetected(splitClientConfig.proxy() != null);
        config.set_impressionsMode(splitClientConfig.impressionsMode());
        config.set_integrations(splitClientConfig);
        config.set_operationMode(0); //Standalone -> 0
        config.set_storage("memory");
        config.set_impressionsQueueSize(splitClientConfig.impressionsQueueSize());
        config.set_redundantFactories(getRedundantFactories(factoryInstances));
        config.set_eventsQueueSize(splitClientConfig.eventsQueueSize());
        config.set_tags(tags);
        config.set_activeFactories(factoryInstances.size());
        config.set_timeUntilReady(timeUntilReady);
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
}
