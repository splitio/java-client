package io.split.telemetry.synchronizer;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.utils.Utils;
import io.split.service.HttpPostImp;
import io.split.telemetry.domain.Config;
import io.split.telemetry.domain.Stats;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

import java.net.URI;
import java.net.URISyntaxException;

public class HttpTelemetryMemorySender extends HttpPostImp {

    private static final String CONFIG_ENDPOINT_PATH = "metrics/config";
    private static final String STATS_ENDPOINT_PATH = "metrics/usage";
    private static final String CONFIG_METRICS = "Config metrics ";
    private static final String STATS_METRICS = "Stats metrics ";

    private final URI _impressionConfigTarget;
    private final URI _impressionStatsTarget;

    public static HttpTelemetryMemorySender create(CloseableHttpClient client, URI telemetryRootEndpoint, TelemetryRuntimeProducer telemetryRuntimeProducer) throws URISyntaxException {
        return new HttpTelemetryMemorySender(client,
                Utils.appendPath(telemetryRootEndpoint,CONFIG_ENDPOINT_PATH),
                Utils.appendPath(telemetryRootEndpoint, STATS_ENDPOINT_PATH),
                telemetryRuntimeProducer
        );
    }

    @VisibleForTesting
    HttpTelemetryMemorySender(CloseableHttpClient client, URI impressionConfigTarget, URI impressionStatsTarget, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        super(client, telemetryRuntimeProducer);
        _impressionConfigTarget = impressionConfigTarget;
        _impressionStatsTarget = impressionStatsTarget;
    }

    public void postConfig(Config config) {
        post(_impressionConfigTarget, config, CONFIG_METRICS, HTTPLatenciesEnum.TELEMETRY, LastSynchronizationRecordsEnum.TELEMETRY, ResourceEnum.TELEMETRY_SYNC);
    }

    public void postStats(Stats stats) {
        post(_impressionStatsTarget, stats, STATS_METRICS, HTTPLatenciesEnum.TELEMETRY, LastSynchronizationRecordsEnum.TELEMETRY, ResourceEnum.TELEMETRY_SYNC);
    }

}
