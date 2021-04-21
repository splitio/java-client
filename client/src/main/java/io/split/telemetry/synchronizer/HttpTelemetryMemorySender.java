package io.split.telemetry.synchronizer;

import io.split.client.SplitClientConfig;
import io.split.client.impressions.HttpImpressionsSender;
import io.split.client.utils.Utils;
import io.split.telemetry.domain.Config;
import io.split.telemetry.domain.Stats;
import io.split.telemetry.utils.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class HttpTelemetryMemorySender extends HttpPost {

    private static final String CONFIG_ENDPOINT_PATH = "metrics/config";
    private static final String STATS_ENDPOINT_PATH = "metrics/usage";
    private static final String CONFIG_METRICS = "Config metrics ";
    private static final String STATS_METRICS = "Stats metrics ";

    private static final Logger _logger = LoggerFactory.getLogger(HttpImpressionsSender.class);

    private final URI _impressionConfigTarget;
    private final URI _impressionStatsTarget;

    public static HttpTelemetryMemorySender create(CloseableHttpClient client, URI telemetryRootEndpoint) throws URISyntaxException {
        return new HttpTelemetryMemorySender(client,
                Utils.appendPath(telemetryRootEndpoint,CONFIG_ENDPOINT_PATH),
                Utils.appendPath(telemetryRootEndpoint, STATS_ENDPOINT_PATH)
        );
    }

    private HttpTelemetryMemorySender(CloseableHttpClient client,  URI impressionConfigTarget, URI impressionStatsTarget) {
        super(client);
        _impressionConfigTarget = impressionConfigTarget;
        _impressionStatsTarget = impressionStatsTarget;
    }

    public void postConfig(Config config) {
        post(_impressionConfigTarget, config, CONFIG_METRICS);
    }

    public void postStats(Stats stats) {
        post(_impressionStatsTarget, stats, STATS_METRICS);
    }

}
