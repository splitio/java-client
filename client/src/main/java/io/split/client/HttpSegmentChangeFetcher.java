package io.split.client;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.dtos.SegmentChange;
import io.split.client.utils.Json;
import io.split.client.utils.Utils;
import io.split.engine.metrics.Metrics;
import io.split.engine.segments.SegmentChangeFetcher;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
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
    private static final String PREFIX = "segmentChangeFetcher";

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
    public SegmentChange fetch(String segmentName, long since) {
        long start = System.currentTimeMillis();

        CloseableHttpResponse response = null;

        try {
            String path = _target.getPath() + "/" + segmentName;
            URI uri = new URIBuilder(_target).setPath(path).addParameter(SINCE, "" + since).build();
            HttpGet request = new HttpGet(uri);
            response = _client.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode < 200 || statusCode >= 300) {
                _log.error("Response status was: " + statusCode);
                _metrics.count(PREFIX + ".status." + statusCode, 1);
                throw new IllegalStateException("Could not retrieve segment changes for " + segmentName + "; http return code " + statusCode);
            }


            String json = EntityUtils.toString(response.getEntity());
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
