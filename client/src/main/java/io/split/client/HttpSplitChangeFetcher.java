package io.split.client;

import com.google.common.annotations.VisibleForTesting;

import io.split.Spec;
import io.split.client.dtos.SplitChange;
import io.split.client.dtos.SplitHttpResponse;
import io.split.client.dtos.SplitChangesOldPayloadDto;
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
import static io.split.Spec.SPEC_1_3;
import static io.split.Spec.SPEC_1_1;

/**
 * Created by adilaijaz on 5/30/15.
 */
public final class HttpSplitChangeFetcher implements SplitChangeFetcher {
    private static final Logger _log = LoggerFactory.getLogger(HttpSplitChangeFetcher.class);

    private final Object _lock = new Object();
    private static final String SINCE = "since";
    private static final String RB_SINCE = "rbSince";
    private static final String TILL = "till";
    private static final String SETS = "sets";
    private static final String SPEC = "s";
    private String specVersion = SPEC_1_3;
    private int PROXY_CHECK_INTERVAL_MILLISECONDS_SS =  24 * 60 * 60 * 1000;
    private Long _lastProxyCheckTimestamp = 0L;
    private final SplitHttpClient _client;
    private final URI _target;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;
    private final boolean _rootURIOverriden;

    public static HttpSplitChangeFetcher create(SplitHttpClient client, URI root, TelemetryRuntimeProducer telemetryRuntimeProducer,
                                                boolean rootURIOverriden)
            throws URISyntaxException {
        return new HttpSplitChangeFetcher(client, Utils.appendPath(root, "api/splitChanges"), telemetryRuntimeProducer, rootURIOverriden);
    }

    private HttpSplitChangeFetcher(SplitHttpClient client, URI uri, TelemetryRuntimeProducer telemetryRuntimeProducer, boolean rootURIOverriden) {
        _client = client;
        _target = uri;
        checkNotNull(_target);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        _rootURIOverriden = rootURIOverriden;
    }

    long makeRandomTill() {

        return (-1)*(long)Math.floor(Math.random()*(Math.pow(2, 63)));
    }

    @Override
    public SplitChange fetch(long since, long sinceRBS, FetchOptions options) {
        long start = System.currentTimeMillis();
        try {
            if (specVersion.equals(SPEC_1_1) && (System.currentTimeMillis() - _lastProxyCheckTimestamp >= PROXY_CHECK_INTERVAL_MILLISECONDS_SS)) {
                _log.info("Switching to new Feature flag spec ({}) and fetching.", SPEC_1_3);
                specVersion = SPEC_1_3;
            }
            URI uri = buildURL(options, since, sinceRBS);
            SplitHttpResponse response = _client.get(uri, options, null);
            if (response.statusCode() < HttpStatus.SC_OK || response.statusCode() >= HttpStatus.SC_MULTIPLE_CHOICES) {
                if (response.statusCode() == HttpStatus.SC_REQUEST_URI_TOO_LONG) {
                    _log.error("The amount of flag sets provided are big causing uri length error.");
                    throw new UriTooLongException(String.format("Status code: %s. Message: %s", response.statusCode(), response.statusMessage()));
                }

                if (response.statusCode() == HttpStatus.SC_BAD_REQUEST && specVersion.equals(Spec.SPEC_1_3) && _rootURIOverriden) {
                    specVersion = Spec.SPEC_1_1;
                    _log.warn("Detected proxy without support for Feature flags spec {} version, will switch to spec version {}",
                            SPEC_1_3, SPEC_1_1);
                    _lastProxyCheckTimestamp = System.currentTimeMillis();
                    return fetch(since, sinceRBS, options);
                }

                _telemetryRuntimeProducer.recordSyncError(ResourceEnum.SPLIT_SYNC, response.statusCode());
                throw new IllegalStateException(
                        String.format("Could not retrieve splitChanges since %s; http return code %s", since, response.statusCode())
                );
            }

            if (specVersion.equals(Spec.SPEC_1_1)) {
                return Json.fromJson(response.body(), SplitChangesOldPayloadDto.class).toSplitChange();
            }

            SplitChange splitChange = Json.fromJson(response.body(), SplitChange.class);
            splitChange.clearCache = _lastProxyCheckTimestamp != 0;
            _lastProxyCheckTimestamp = 0L;
            return splitChange;
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Problem fetching splitChanges since %s: %s", since, e), e);
        } finally {
            _telemetryRuntimeProducer.recordSyncLatency(HTTPLatenciesEnum.SPLITS, System.currentTimeMillis() - start);
        }
    }


    private URI buildURL(FetchOptions options, long since, long sinceRBS) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(_target).addParameter(SPEC, "" + specVersion);
        uriBuilder.addParameter(SINCE, "" + since);
        if (specVersion.equals(SPEC_1_3)) {
            uriBuilder.addParameter(RB_SINCE, "" + sinceRBS);
        }
        if (!options.flagSetsFilter().isEmpty()) {
            uriBuilder.addParameter(SETS, "" + options.flagSetsFilter());
        }
        if (options.hasCustomCN()) {
            uriBuilder.addParameter(TILL, "" + options.targetCN());
        }
        return uriBuilder.build();
    }

    @VisibleForTesting
    URI getTarget() {
        return _target;
    }
}