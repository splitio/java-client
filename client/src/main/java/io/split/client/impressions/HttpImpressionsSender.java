package io.split.client.impressions;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.dtos.ImpressionCount;
import io.split.client.dtos.TestImpressions;
import io.split.client.utils.Utils;

import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by patricioe on 6/20/16.
 */
public class HttpImpressionsSender implements ImpressionsSender {

    private static final String BULK_ENDPOINT_PATH = "api/testImpressions/bulk";
    private static final String COUNT_ENDPOINT_PATH = "api/testImpressions/count";
    private static final String IMPRESSIONS_MODE_HEADER = "SplitSDKImpressionsMode";

    private static final Logger _logger = LoggerFactory.getLogger(HttpImpressionsSender.class);

    private final CloseableHttpClient _client;
    private final URI _impressionBulkTarget;
    private final URI _impressionCountTarget;
    private final ImpressionsManager.Mode _mode;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public static HttpImpressionsSender create(CloseableHttpClient client, URI eventsRootEndpoint, ImpressionsManager.Mode mode, TelemetryRuntimeProducer telemetryRuntimeProducer) throws URISyntaxException {
        return new HttpImpressionsSender(client,
                Utils.appendPath(eventsRootEndpoint, BULK_ENDPOINT_PATH),
                Utils.appendPath(eventsRootEndpoint, COUNT_ENDPOINT_PATH),
                mode,
                telemetryRuntimeProducer);
    }

    private HttpImpressionsSender(CloseableHttpClient client, URI impressionBulkTarget, URI impressionCountTarget, ImpressionsManager.Mode mode, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _client = client;
        _mode = mode;
        _impressionBulkTarget = impressionBulkTarget;
        _impressionCountTarget = impressionCountTarget;
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    public void postImpressionsBulk(List<TestImpressions> impressions) {

        CloseableHttpResponse response = null;
        long initTime = System.currentTimeMillis();
        try {
            HttpEntity entity = Utils.toJsonEntity(impressions);

            HttpPost request = new HttpPost(_impressionBulkTarget);
            request.addHeader(IMPRESSIONS_MODE_HEADER, _mode.toString());
            request.setEntity(entity);

            response = _client.execute(request);

            int status = response.getCode();

            if (status < HttpStatus.SC_OK || status >= HttpStatus.SC_MULTIPLE_CHOICES) {
                _telemetryRuntimeProducer.recordSyncError(ResourceEnum.IMPRESSION_SYNC, status);
                _logger.warn("Response status was: " + status);
            }
            _telemetryRuntimeProducer.recordSyncLatency(HTTPLatenciesEnum.IMPRESSIONS, System.currentTimeMillis() - initTime);
            _telemetryRuntimeProducer.recordSuccessfulSync(LastSynchronizationRecordsEnum.IMPRESSIONS, System.currentTimeMillis());

        } catch (Throwable t) {
            _logger.warn("Exception when posting impressions" + impressions, t);
        } finally {
            Utils.forceClose(response);
        }

    }

    @Override
    public void postCounters(HashMap<ImpressionCounter.Key, Integer> raw) {
        long initTime = System.currentTimeMillis();
        if (_mode.equals(ImpressionsManager.Mode.DEBUG)) {
            _logger.warn("Attempted to submit counters in impressions debugging mode. Ignoring");
            return;
        }

        HttpPost request = new HttpPost(_impressionCountTarget);
        request.setEntity(Utils.toJsonEntity(ImpressionCount.fromImpressionCounterData(raw)));
        try (CloseableHttpResponse response = _client.execute(request)) {
            int status = response.getCode();
            if (status < HttpStatus.SC_OK || status >= HttpStatus.SC_MULTIPLE_CHOICES) {
                _telemetryRuntimeProducer.recordSyncError(ResourceEnum.IMPRESSION_COUNT_SYNC, status);
                _logger.warn("Response status was: " + status);
            }
            _telemetryRuntimeProducer.recordSyncLatency(HTTPLatenciesEnum.IMPRESSIONS_COUNT, System.currentTimeMillis() - initTime);
            _telemetryRuntimeProducer.recordSuccessfulSync(LastSynchronizationRecordsEnum.IMPRESSIONS_COUNT, System.currentTimeMillis());
        } catch (IOException exc) {
            _logger.warn("Exception when posting impression counters: ", exc);
        }
    }

    @VisibleForTesting
    URI getTarget() {
        return _impressionBulkTarget;
    }
}
