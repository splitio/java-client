package io.split.client;

import com.google.common.annotations.VisibleForTesting;

import io.split.Spec;
import io.split.client.dtos.SplitChange;
import io.split.client.dtos.SplitHttpResponse;
import io.split.client.dtos.RuleBasedSegment;
import io.split.client.dtos.SplitChangesOldPayloadDto;
import io.split.client.dtos.ChangeDto;
import io.split.client.dtos.Split;
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
import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.split.Spec.SPEC_VERSION;
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
        SplitHttpResponse response;
        try {
            if (SPEC_VERSION.equals(SPEC_1_1) && (System.currentTimeMillis() - _lastProxyCheckTimestamp >= PROXY_CHECK_INTERVAL_MILLISECONDS_SS)) {
                _log.info("Switching to new Feature flag spec ({}) and fetching.", SPEC_1_3);
                SPEC_VERSION = SPEC_1_3;
            }
            URI uri = buildURL(options, since, sinceRBS);
            response = _client.get(uri, options, null);
            if (response.statusCode() < HttpStatus.SC_OK || response.statusCode() >= HttpStatus.SC_MULTIPLE_CHOICES) {
                if (response.statusCode() == HttpStatus.SC_REQUEST_URI_TOO_LONG) {
                    _log.error("The amount of flag sets provided are big causing uri length error.");
                    throw new UriTooLongException(String.format("Status code: %s. Message: %s", response.statusCode(), response.statusMessage()));
                }

                if (response.statusCode() == HttpStatus.SC_BAD_REQUEST && SPEC_VERSION.equals(Spec.SPEC_1_3) && _rootURIOverriden) {
                    SPEC_VERSION = Spec.SPEC_1_1;
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
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Problem fetching splitChanges since %s: %s", since, e), e);
        } finally {
            _telemetryRuntimeProducer.recordSyncLatency(HTTPLatenciesEnum.SPLITS, System.currentTimeMillis() - start);
        }

        SplitChange splitChange = new SplitChange();
        if (SPEC_VERSION.equals(Spec.SPEC_1_1)) {
            splitChange.featureFlags = convertBodyToOldSpec(response.body());
            splitChange.ruleBasedSegments = createEmptyDTO();
        } else {
            splitChange = Json.fromJson(response.body(), SplitChange.class);
        }
        return splitChange;
    }

    public Long getLastProxyCheckTimestamp() {
        return _lastProxyCheckTimestamp;
    }

    public void setLastProxyCheckTimestamp(long lastProxyCheckTimestamp) {
        synchronized (_lock) {
            _lastProxyCheckTimestamp = lastProxyCheckTimestamp;
        }
    }

    private ChangeDto<RuleBasedSegment> createEmptyDTO() {
        ChangeDto<RuleBasedSegment> dto = new ChangeDto<>();
        dto.d = new ArrayList<>();
        dto.t = -1;
        dto.s = -1;
        return dto;
    }
    private ChangeDto<Split> convertBodyToOldSpec(String body) {
        return Json.fromJson(body, SplitChangesOldPayloadDto.class).toChangeDTO();
    }

    private URI buildURL(FetchOptions options, long since, long sinceRBS) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(_target).addParameter(SPEC, "" + SPEC_VERSION);
        uriBuilder.addParameter(SINCE, "" + since);
        uriBuilder.addParameter(RB_SINCE, "" + sinceRBS);
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