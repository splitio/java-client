package io.split.client.metrics;

import com.google.common.collect.Lists;
import io.split.client.dtos.Counter;
import io.split.client.dtos.Latency;
import io.split.client.utils.Utils;
import io.split.engine.metrics.Metrics;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by adilaijaz on 9/4/15.
 */
public class HttpMetrics implements Metrics, DTOMetrics {
    private static final Logger _log = LoggerFactory.getLogger(HttpMetrics.class);

    private final CloseableHttpClient _client;
    private final URI _target;


    public static HttpMetrics create(CloseableHttpClient client, URI root) throws URISyntaxException {
        return new HttpMetrics(client, new URIBuilder(root).build());
    }


    public HttpMetrics(CloseableHttpClient client, URI uri) {
        _client = client;
        _target = uri;
        checkNotNull(_client);
        checkNotNull(_target);
    }


    @Override
    public void time(Latency dto) {

        if (dto.latencies.isEmpty()) {
            return;
        }

        try {
            post(new URIBuilder(_target).setPath("/api/metrics/time").build(), dto);
        } catch (Throwable t) {
            _log.warn("Exception when posting metric" + dto, t);
        }
        ;

    }

    @Override
    public void count(Counter dto) {

        try {
            post(new URIBuilder(_target).setPath("/api/metrics/counter").build(), dto);
        } catch (Throwable t) {
            _log.warn("Exception when posting metric" + dto, t);
        }

    }

    private void post(URI uri, Object dto) {

        CloseableHttpResponse response = null;

        try {
            StringEntity entity = Utils.toJsonEntity(dto);

            HttpPost request = new HttpPost(uri);
            request.setEntity(entity);

            response = _client.execute(request);

            int status = response.getStatusLine().getStatusCode();

            if (status < 200 || status >= 300) {
                _log.warn("Response status was: " + status);
            }

        } catch (Throwable t) {
            _log.warn("Exception when posting metrics:" + t.getMessage());
            if (_log.isDebugEnabled()) {
                _log.debug("Reason:", t);
            }
        } finally {
            Utils.forceClose(response);
        }

    }

    @Override
    public void count(String counter, long delta) {
        try {
            Counter dto = new Counter();
            dto.name = counter;
            dto.delta = delta;

            count(dto);
        } catch (Throwable t) {
            _log.info("Could not count metric " + counter, t);
        }

    }

    @Override
    public void time(String operation, long timeInMs) {
        try {
            Latency dto = new Latency();
            dto.name = operation;
            dto.latencies = Lists.newArrayList(timeInMs);

            time(dto);
        } catch (Throwable t) {
            _log.info("Could not time metric " + operation, t);
        }
    }

}
