package io.split.client;

import com.google.common.annotations.VisibleForTesting;

import io.split.client.dtos.SplitChange;
import io.split.client.dtos.SplitHttpResponse;
import io.split.client.exceptions.UriTooLongException;
import io.split.client.utils.Json;
import io.split.client.utils.Utils;
import io.split.engine.common.FetchOptions;
import io.split.engine.experiments.SplitChangeFetcher;
import io.split.service.SplitHttpClient;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.split.Spec.SPEC_VERSION;

/**
 * Created by adilaijaz on 5/30/15.
 */
public final class HttpSplitChangeFetcher implements SplitChangeFetcher {
    private static final Logger _log = LoggerFactory.getLogger(HttpSplitChangeFetcher.class);

    private static final String SINCE = "since";
    private static final String TILL = "till";
    private static final String SETS = "sets";
    private static final String SPEC = "s";
    private final SplitHttpClient _client;
    private final URI _target;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public static HttpSplitChangeFetcher create(SplitHttpClient client, URI root, TelemetryRuntimeProducer telemetryRuntimeProducer)
            throws URISyntaxException {
        return new HttpSplitChangeFetcher(client, Utils.appendPath(root, "api/splitChanges"), telemetryRuntimeProducer);
    }

    private HttpSplitChangeFetcher(SplitHttpClient client, URI uri, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _client = client;
        _target = uri;
        checkNotNull(_target);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    long makeRandomTill() {

        return (-1)*(long)Math.floor(Math.random()*(Math.pow(2, 63)));
    }

    @Override
    public SplitChange fetch(long since, FetchOptions options) {

        long start = System.currentTimeMillis();

        try {
            URIBuilder uriBuilder = new URIBuilder(_target).addParameter(SPEC, "" + SPEC_VERSION);
            uriBuilder.addParameter(SINCE, "" + since);
            if (!options.flagSetsFilter().isEmpty()) {
                uriBuilder.addParameter(SETS, "" + options.flagSetsFilter());
            }
            if (options.hasCustomCN()) {
                uriBuilder.addParameter(TILL, "" + options.targetCN());
            }
            URI uri = uriBuilder.build();
            SplitHttpResponse response = _client.get(uri, options, null);

            if (response.statusCode() < HttpStatus.SC_OK || response.statusCode() >= HttpStatus.SC_MULTIPLE_CHOICES) {
                if (response.statusCode() == HttpStatus.SC_REQUEST_URI_TOO_LONG) {
                    _log.error("The amount of flag sets provided are big causing uri length error.");
                    throw new UriTooLongException(String.format("Status code: %s. Message: %s", response.statusCode(), response.statusMessage()));
                }
                _telemetryRuntimeProducer.recordSyncError(ResourceEnum.SPLIT_SYNC, response.statusCode());
                throw new IllegalStateException(
                        String.format("Could not retrieve splitChanges since %s; http return code %s", since, response.statusCode())
                );
            }

            return Json.fromJson(response.body(), SplitChange.class);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Problem fetching splitChanges since %s: %s", since, e), e);
        } finally {
            _telemetryRuntimeProducer.recordSyncLatency(HTTPLatenciesEnum.SPLITS, System.currentTimeMillis()-start);
        }
    }

    @VisibleForTesting
    URI getTarget() {
        return _target;
    }
}