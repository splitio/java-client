package io.split.client;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.dtos.SegmentChange;
import io.split.client.utils.Json;
import io.split.client.utils.Utils;
import io.split.engine.common.FetchOptions;
import io.split.engine.metrics.Metrics;
import io.split.engine.segments.SegmentChangeFetcher;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
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
    private final Metrics _metrics;

    public static HttpSegmentChangeFetcher create(CloseableHttpClient client, URI root) throws URISyntaxException {
        return create(client, root, new Metrics.NoopMetrics());
    }

    public static HttpSegmentChangeFetcher create(CloseableHttpClient client, URI root, Metrics metrics) throws URISyntaxException {
        return new HttpSegmentChangeFetcher(client, Utils.appendPath(root, "api/segmentChanges"), metrics);
    }

    private HttpSegmentChangeFetcher(CloseableHttpClient client, URI uri, Metrics metrics) {
        _client = client;
        _target = uri;
        _metrics = metrics;
        checkNotNull(_target);
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
            options.handleResponseHeaders(Arrays.stream(response.getHeaders())
                    .collect(Collectors.toMap(Header::getName, Header::getValue)));

            int statusCode = response.getCode();

            if (statusCode < 200 || statusCode >= 300) {
                _log.error("Response status was: " + statusCode);
                if (statusCode == 403) {
                    _log.error("factory instantiation: you passed a browser type api_key, " +
                            "please grab an api key from the Split console that is of type sdk");
                }
                _metrics.count(PREFIX + ".status." + statusCode, 1);
                throw new IllegalStateException("Could not retrieve segment changes for " + segmentName + "; http return code " + statusCode);
            }



            String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (_log.isDebugEnabled()) {
                _log.debug("Received json: " + json);
            }

            return Json.fromJson(json, SegmentChange.class);
        } catch (Throwable t) {
            _metrics.count(PREFIX + ".exception", 1);
            throw new IllegalStateException("Problem fetching segmentChanges: " + t.getMessage(), t);
        } finally {
            Utils.forceClose(response);
            _metrics.time(PREFIX + ".time", System.currentTimeMillis() - start);
        }


    }

    @VisibleForTesting
    URI getTarget() {
        return _target;
    }
}
