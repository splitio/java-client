package io.split.telemetry.synchronizer;

import io.split.cache.SegmentCache;
import io.split.cache.SplitCache;
import io.split.client.dtos.Split;
import io.split.telemetry.domain.InitConfig;
import io.split.telemetry.domain.Stats;
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
    public void synchronizeConfig(InitConfig config, long timedUntilReady, Map<String, Long> factoryInstances, List<String> tags) {
        _httpHttpTelemetryMemorySender.postConfig(config, timedUntilReady, factoryInstances, tags);
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
        stats.set_splitCount(_splitCache.getAll().stream().count()); //TODO
        stats.set_segmentCount(1l);//TODO
        stats.set_segmentKeyCount(1l);//TODO
        stats.set_sessionLengthMs(_teleTelemetryStorageConsumer.getSessionLength());
        stats.set_eventsQueued(_teleTelemetryStorageConsumer.getEventStats(EventsDataRecordsEnum.EVENTS_QUEUED));
        stats.set_eventsDropped(_teleTelemetryStorageConsumer.getEventStats(EventsDataRecordsEnum.EVENTS_DROPPED));
        stats.set_streamingEvents(_teleTelemetryStorageConsumer.popStreamingEvents());
        stats.set_tags(_teleTelemetryStorageConsumer.popTags();
        return null;
    }
}
