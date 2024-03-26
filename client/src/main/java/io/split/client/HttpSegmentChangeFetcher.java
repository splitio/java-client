package io.split.client;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.dtos.SegmentChange;
import io.split.client.dtos.SplitHttpResponse;
import io.split.client.utils.Json;
import io.split.client.utils.Utils;
import io.split.engine.common.FetchOptions;
import io.split.engine.segments.SegmentChangeFetcher;
import io.split.service.SplitHttpClient;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by adilaijaz on 5/22/15.
 */
public final class HttpSegmentChangeFetcher implements SegmentChangeFetcher {
    private static final Logger _log = LoggerFactory.getLogger(HttpSegmentChangeFetcher.class);

    private static final String SINCE = "since";
    private static final String TILL = "till";

    private final SplitHttpClient _client;
    private final URI _target;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public static HttpSegmentChangeFetcher create(SplitHttpClient client, URI root, TelemetryRuntimeProducer telemetryRuntimeProducer)
            throws URISyntaxException {
        return new HttpSegmentChangeFetcher(client, Utils.appendPath(root, "api/segmentChanges"), telemetryRuntimeProducer);
    }

    private HttpSegmentChangeFetcher(SplitHttpClient client, URI uri, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _client = client;
        _target = uri;
        checkNotNull(_target);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    public SegmentChange fetch(String segmentName, long since, FetchOptions options) {
        long start = System.currentTimeMillis();

        SplitHttpResponse response;

        try {
            String path = _target.getPath() + "/" + segmentName;
            URIBuilder uriBuilder = new URIBuilder(_target)
                    .setPath(path)
                    .addParameter(SINCE, "" + since);
            if (options.hasCustomCN()) {
                uriBuilder.addParameter(TILL, "" + options.targetCN());
            }

            URI uri = uriBuilder.build();

            response = _client.get(uri, options);

            if (response.statusCode < HttpStatus.SC_OK || response.statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
                _telemetryRuntimeProducer.recordSyncError(ResourceEnum.SEGMENT_SYNC, response.statusCode);
                if (response.statusCode == HttpStatus.SC_FORBIDDEN) {
                    _log.error("factory instantiation: you passed a client side type sdkKey, " +
                            "please grab an sdk key from the Split user interface that is of type server side");
                }
                throw new IllegalStateException(String.format("Could not retrieve segment changes for %s, since %s; http return code %s",
                        segmentName, since, response.statusCode));
            }
            _telemetryRuntimeProducer.recordSuccessfulSync(LastSynchronizationRecordsEnum.SEGMENTS, System.currentTimeMillis());

            return Json.fromJson(response.body, SegmentChange.class);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Error occurred when trying to sync segment: %s, since: %s. Details: %s",
                    segmentName, since, e), e);
        } finally {
            _telemetryRuntimeProducer.recordSyncLatency(HTTPLatenciesEnum.SEGMENTS, System.currentTimeMillis()-start);
        }
    }

    @VisibleForTesting
    URI getTarget() {
        return _target;
    }
}