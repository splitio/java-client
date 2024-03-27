package io.split.service;

import io.split.client.dtos.SplitHttpResponse;
import io.split.client.utils.Utils;
import io.split.telemetry.domain.enums.HttpParamsWrapper;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpPostImp {
    private static final Logger _logger = LoggerFactory.getLogger(HttpPostImp.class);
    private SplitHttpClient _client;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public HttpPostImp(SplitHttpClient client, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _client = client;
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    public void post(URI uri, Object object, String posted, HttpParamsWrapper httpParamsWrapper) {
        long initTime = System.currentTimeMillis();
        HttpEntity entity = Utils.toJsonEntity(object);

        try {
            SplitHttpResponse response = _client.post(uri, entity, null);
            if (response.statusCode() < HttpStatus.SC_OK || response.statusCode() >= HttpStatus.SC_MULTIPLE_CHOICES) {
                _telemetryRuntimeProducer.recordSyncError(httpParamsWrapper.getResourceEnum(), response.statusCode());
                return;
            }
            _telemetryRuntimeProducer.recordSyncLatency(httpParamsWrapper.getHttpLatenciesEnum(), System.currentTimeMillis() - initTime);
            _telemetryRuntimeProducer.recordSuccessfulSync(httpParamsWrapper.getLastSynchronizationRecordsEnum(), System.currentTimeMillis());
        } catch (Throwable t) {
            _logger.warn("Exception when posting " + posted + object, t);
        }
    }
}
