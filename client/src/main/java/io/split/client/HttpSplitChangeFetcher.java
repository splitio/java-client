package io.split.client;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.dtos.SplitChange;
import io.split.client.exceptions.UriTooLongException;
import io.split.client.utils.Json;
import io.split.client.utils.Utils;
import io.split.engine.common.FetchOptions;
import io.split.engine.experiments.SplitChangeFetcher;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
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
 * Created by adilaijaz on 5/30/15.
 */
public final class HttpSplitChangeFetcher implements SplitChangeFetcher {
    private static final Logger _log = LoggerFactory.getLogger(HttpSplitChangeFetcher.class);

    private static final String SINCE = "since";
    private static final String TILL = "till";
    private static final String SETS = "sets";

    private static final String HEADER_CACHE_CONTROL_NAME = "Cache-Control";
    private static final String HEADER_CACHE_CONTROL_VALUE = "no-cache";

    private final CloseableHttpClient _client;
    private final URI _target;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public static HttpSplitChangeFetcher create(CloseableHttpClient client, URI root, TelemetryRuntimeProducer telemetryRuntimeProducer)
            throws URISyntaxException {
        return new HttpSplitChangeFetcher(client, Utils.appendPath(root, "api/splitChanges"), telemetryRuntimeProducer);
    }

    private HttpSplitChangeFetcher(CloseableHttpClient client, URI uri, TelemetryRuntimeProducer telemetryRuntimeProducer) {
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

        CloseableHttpResponse response = null;

        try {
            URIBuilder uriBuilder = new URIBuilder(_target).addParameter(SINCE, "" + since);
            if (options.hasCustomCN()) {
                uriBuilder.addParameter(TILL, "" + options.targetCN());
            }
            if (!options.flagSetsFilter().isEmpty()) {
                uriBuilder.addParameter(SETS, "" + options.flagSetsFilter());
            }
            URI uri = uriBuilder.build();

            HttpGet request = new HttpGet(uri);
            if(options.cacheControlHeadersEnabled()) {
                request.setHeader(HEADER_CACHE_CONTROL_NAME, HEADER_CACHE_CONTROL_VALUE);
            }

            response = _client.execute(request);

            int statusCode = response.getCode();

            if (_log.isDebugEnabled()) {
                _log.debug(String.format("[%s] %s. Status code: %s", request.getMethod(), uri.toURL(), statusCode));
            }

            if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
                _telemetryRuntimeProducer.recordSyncError(ResourceEnum.SPLIT_SYNC, statusCode);
                if (statusCode == HttpStatus.SC_REQUEST_URI_TOO_LONG) {
                    _log.error("The amount of flag sets provided are big causing uri length error.");
                    throw new UriTooLongException(String.format("Status code: %s. Message: %s", statusCode, response.getReasonPhrase()));
                }
                _log.warn(String.format("Response status was: %s. Reason: %s", statusCode , response.getReasonPhrase()));
                throw new IllegalStateException(String.format("Could not retrieve splitChanges since %s; http return code %s", since, statusCode));
            }

            String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            return Json.fromJson(json, SplitChange.class);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Problem fetching splitChanges since %s: %s", since, e), e);
        } finally {
            _telemetryRuntimeProducer.recordSyncLatency(HTTPLatenciesEnum.SPLITS, System.currentTimeMillis()-start);
            Utils.forceClose(response);
        }
    }

    @VisibleForTesting
    URI getTarget() {
        return _target;
    }
}