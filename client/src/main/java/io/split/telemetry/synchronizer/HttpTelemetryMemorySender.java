package io.split.telemetry.synchronizer;

import io.split.client.impressions.HttpImpressionsSender;
import io.split.client.utils.Utils;
import io.split.telemetry.domain.InitConfig;
import io.split.telemetry.domain.Stats;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class HttpTelemetryMemorySender{

    private static final String CONFIG_ENDPOINT_PATH = "api/telemetry/config";
    private static final String STATS_ENDPOINT_PATH = "api/telemetry/stats";

    private static final Logger _logger = LoggerFactory.getLogger(HttpImpressionsSender.class);

    private final CloseableHttpClient _client;
    private final URI _impressionConfigTarget;
    private final URI _impressionStatsTarget;

    public static HttpTelemetryMemorySender create(CloseableHttpClient client, URI telemetryRootEndpoint) throws URISyntaxException {
        return new HttpTelemetryMemorySender(client,
                Utils.appendPath(telemetryRootEndpoint,CONFIG_ENDPOINT_PATH),
                Utils.appendPath(telemetryRootEndpoint, STATS_ENDPOINT_PATH)
        );
    }

    private HttpTelemetryMemorySender(CloseableHttpClient client,  URI impressionConfigTarget, URI impressionStatsTarget) {
        _client = client;
        _impressionConfigTarget = impressionConfigTarget;
        _impressionStatsTarget = impressionStatsTarget;
    }

    public void postConfig(InitConfig config, long timedUntilReady, Map<String, Long> factoryInstances, List<String> tags) {

    }

    public void postStats(Stats stats) {

    }
}
