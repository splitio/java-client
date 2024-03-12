package io.split.client;

import io.split.client.exceptions.UriTooLongException;
import io.split.client.utils.Utils;
import io.split.engine.common.FetchOptions;
import io.split.telemetry.domain.enums.HttpParamsWrapper;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SplitHttpClient {
    private static final Logger _log = LoggerFactory.getLogger(SplitHttpClient.class);
    private static final String HEADER_CACHE_CONTROL_NAME = "Cache-Control";
    private static final String HEADER_CACHE_CONTROL_VALUE = "no-cache";
    private final CloseableHttpClient _client;
    private final RequestDecorator _requestDecorator;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public static SplitHttpClient create(CloseableHttpClient client, TelemetryRuntimeProducer telemetryRuntimeProducer, RequestDecorator requestDecorator)
            throws URISyntaxException {
        return new SplitHttpClient(client, telemetryRuntimeProducer, requestDecorator);
    }

    private SplitHttpClient(CloseableHttpClient client, TelemetryRuntimeProducer telemetryRuntimeProducer, RequestDecorator requestDecorator) {
        _client = client;
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        _requestDecorator = requestDecorator;
    }

    public String get(URI uri, FetchOptions options, HttpParamsWrapper telemetryParamsWrapper) {
        long start = System.currentTimeMillis();

        CloseableHttpResponse response = null;

        try {
            HttpGet request = new HttpGet(uri);
            if(options.cacheControlHeadersEnabled()) {
                request.setHeader(HEADER_CACHE_CONTROL_NAME, HEADER_CACHE_CONTROL_VALUE);
            }
            request = (HttpGet) _requestDecorator.decorateHeaders(request);

            response = _client.execute(request);

            int statusCode = response.getCode();

            if (_log.isDebugEnabled()) {
                _log.debug(String.format("[%s] %s. Status code: %s", request.getMethod(), uri.toURL(), statusCode));
            }

            if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
                _telemetryRuntimeProducer.recordSyncError(telemetryParamsWrapper.getResourceEnum(), statusCode);
                if (statusCode == HttpStatus.SC_REQUEST_URI_TOO_LONG) {
                    _log.error("The amount of flag sets provided are big causing uri length error.");
                    throw new UriTooLongException(String.format("Status code: %s. Message: %s", statusCode, response.getReasonPhrase()));
                }
                _log.warn(String.format("Response status was: %s. Reason: %s", statusCode , response.getReasonPhrase()));
                throw new IllegalStateException(String.format("Http get received non-successful return code %s", statusCode));
            }

            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Problem in http get operation: %s", e), e);
        } finally {
            _telemetryRuntimeProducer.recordSyncLatency(telemetryParamsWrapper.getHttpLatenciesEnum(), System.currentTimeMillis()-start);
            Utils.forceClose(response);
        }
    }

    public void post(URI uri, HttpEntity entity, Map<String, String> additionalHeaders, HttpParamsWrapper telemetryParamsWrapper) throws IOException {
        CloseableHttpResponse response = null;
        long initTime = System.currentTimeMillis();
        try {
            HttpPost request = new HttpPost(uri);
            if (additionalHeaders != null) {
                for (Map.Entry entry : additionalHeaders.entrySet()) {
                    request.addHeader(entry.getKey().toString(), entry.getValue());
                }
            }
            request.setEntity(entity);
            request = (HttpPost) _requestDecorator.decorateHeaders(request);

            response = _client.execute(request);

            int status = response.getCode();

            if (status < HttpStatus.SC_OK || status >= HttpStatus.SC_MULTIPLE_CHOICES) {
                _telemetryRuntimeProducer.recordSyncError(telemetryParamsWrapper.getResourceEnum(), status);
                _log.warn(String.format("Response status was: %s. Reason: %s", status, response.getReasonPhrase()));
            }
            _telemetryRuntimeProducer.recordSuccessfulSync(telemetryParamsWrapper.getLastSynchronizationRecordsEnum(), System.currentTimeMillis());
        } catch (Exception e) {
            throw new IOException(String.format("Problem in http post operation: %s", e), e);
        } finally {
            _telemetryRuntimeProducer.recordSyncLatency(telemetryParamsWrapper.getHttpLatenciesEnum(), System.currentTimeMillis() - initTime);
            Utils.forceClose(response);
        }
    }
}
