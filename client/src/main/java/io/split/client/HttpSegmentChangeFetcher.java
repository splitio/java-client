package io.split.client;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.dtos.SegmentChange;
import io.split.client.utils.Json;
import io.split.client.utils.Utils;
import io.split.engine.common.FetchOptions;
import io.split.engine.segments.SegmentChangeFetcher;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by adilaijaz on 5/22/15.
 */
public final class HttpSegmentChangeFetcher implements SegmentChangeFetcher {
    private static final Logger _log = LoggerFactory.getLogger(HttpSegmentChangeFetcher.class);

    private static final String SINCE = "since";
    private static final String TILL = "till";
    private static final String PREFIX = "segmentChangeFetcher";
    private static final String CACHE_CONTROL_HEADER_NAME = "Cache-Control";
    private static final String CACHE_CONTROL_HEADER_VALUE = "no-cache";

    private static final String HEADER_FASTLY_DEBUG_NAME = "Fastly-Debug";
    private static final String HEADER_FASTLY_DEBUG_VALUE = "1";

    private final CloseableHttpClient _client;
    private final URI _target;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public static HttpSegmentChangeFetcher create(CloseableHttpClient client, URI root, TelemetryRuntimeProducer telemetryRuntimeProducer)
            throws URISyntaxException {
        return new HttpSegmentChangeFetcher(client, Utils.appendPath(root, "api/segmentChanges"), telemetryRuntimeProducer);
    }

    private HttpSegmentChangeFetcher(CloseableHttpClient client, URI uri, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _client = client;
        _target = uri;
        checkNotNull(_target);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    public SegmentChange fetch(String segmentName, long since, FetchOptions options) {
        long start = System.currentTimeMillis();

        CloseableHttpResponse response = null;

        try {
            String path = _target.getPath() + "/" + segmentName;
            URIBuilder uriBuilder = new URIBuilder(_target)
                    .setPath(path)
                    .addParameter(SINCE, "" + since);
            if (options.hasCustomCN()) {
                uriBuilder.addParameter(TILL, "" + options.targetCN());
            }

            URI uri = uriBuilder.build();
            HttpGet request = new HttpGet(uri);

            if(options.cacheControlHeadersEnabled()) {
                request.setHeader(CACHE_CONTROL_HEADER_NAME, CACHE_CONTROL_HEADER_VALUE);
            }

            if (options.fastlyDebugHeaderEnabled()) {
                request.addHeader(HEADER_FASTLY_DEBUG_NAME, HEADER_FASTLY_DEBUG_VALUE);
            }

            response = _client.execute(request);

            options.handleResponseHeaders(response.getHeaders());

            int statusCode = response.getCode();

            if (_log.isDebugEnabled()) {
                _log.debug(String.format("[%s] %s. Status code: %s", request.getMethod(), uri.toURL(), statusCode));
            }

            if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
                _telemetryRuntimeProducer.recordSyncError(ResourceEnum.SEGMENT_SYNC, statusCode);
                _log.error(String.format("Response status was: %s. Reason: %s", statusCode , response.getReasonPhrase()));
                if (statusCode == HttpStatus.SC_FORBIDDEN) {
                    _log.error("factory instantiation: you passed a client side type sdkKey, " +
                            "please grab an sdk key from the Split user interface that is of type server side");
                }
                throw new IllegalStateException(String.format("Could not retrieve segment changes for %s, since %s; http return code %s",
                        segmentName, since, statusCode));
            }

            _telemetryRuntimeProducer.recordSuccessfulSync(LastSynchronizationRecordsEnum.SEGMENTS, System.currentTimeMillis());

            String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            return Json.fromJson(json, SegmentChange.class);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Error occurred when trying to sync segment: %s, since: %s. Details: %s",
                    segmentName, since, e), e);
        } finally {
            _telemetryRuntimeProducer.recordSyncLatency(HTTPLatenciesEnum.SEGMENTS, System.currentTimeMillis()-start);
            Utils.forceClose(response);
        }


    }

    @VisibleForTesting
    URI getTarget() {
        return _target;
    }
}