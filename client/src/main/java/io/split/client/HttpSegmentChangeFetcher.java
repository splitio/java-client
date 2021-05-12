package io.split.client;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.dtos.SegmentChange;
import io.split.client.utils.Json;
import io.split.client.utils.Utils;
import io.split.engine.metrics.Metrics;
import io.split.engine.segments.SegmentChangeFetcher;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by adilaijaz on 5/22/15.
 */
public final class HttpSegmentChangeFetcher implements SegmentChangeFetcher {
    private static final Logger _log = LoggerFactory.getLogger(HttpSegmentChangeFetcher.class);

    private static final String SINCE = "since";
    private static final String PREFIX = "segmentChangeFetcher";
    private static final String NAME_CACHE = "Cache-Control";
    private static final String VALUE_CACHE = "no-cache";

    private final CloseableHttpClient _client;
    private final URI _target;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public static HttpSegmentChangeFetcher create(CloseableHttpClient client, URI root, TelemetryRuntimeProducer telemetryRuntimeProducer) throws URISyntaxException {
        return new HttpSegmentChangeFetcher(client, Utils.appendPath(root, "api/segmentChanges"), telemetryRuntimeProducer);
    }

    private HttpSegmentChangeFetcher(CloseableHttpClient client, URI uri, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _client = client;
        _target = uri;
        checkNotNull(_target);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    public SegmentChange fetch(String segmentName, long since, boolean addCacheHeader) {
        long start = System.currentTimeMillis();

        CloseableHttpResponse response = null;

        try {
            String path = _target.getPath() + "/" + segmentName;
            URI uri = new URIBuilder(_target).setPath(path).addParameter(SINCE, "" + since).build();
            HttpGet request = new HttpGet(uri);
            if(addCacheHeader) {
                request.setHeader(NAME_CACHE, VALUE_CACHE);
            }
            response = _client.execute(request);

            int statusCode = response.getCode();

            if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
                _telemetryRuntimeProducer.recordSyncError(ResourceEnum.SEGMENT_SYNC, statusCode);
                _log.error("Response status was: " + statusCode);
                if (statusCode == HttpStatus.SC_FORBIDDEN) {
                    _log.error("factory instantiation: you passed a browser type api_key, " +
                            "please grab an api key from the Split console that is of type sdk");
                }
                throw new IllegalStateException("Could not retrieve segment changes for " + segmentName + "; http return code " + statusCode);
            }

            _telemetryRuntimeProducer.recordSyncLatency(HTTPLatenciesEnum.SEGMENTS, System.currentTimeMillis()-start);
            _telemetryRuntimeProducer.recordSuccessfulSync(LastSynchronizationRecordsEnum.SEGMENTS, System.currentTimeMillis());

            String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (_log.isDebugEnabled()) {
                _log.debug("Received json: " + json);
            }

            return Json.fromJson(json, SegmentChange.class);
        } catch (Throwable t) {
            throw new IllegalStateException("Problem fetching segmentChanges: " + t.getMessage(), t);
        } finally {
            Utils.forceClose(response);
        }


    }

    @VisibleForTesting
    URI getTarget() {
        return _target;
    }
}
