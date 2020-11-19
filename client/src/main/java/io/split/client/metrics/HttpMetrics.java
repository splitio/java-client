package io.split.client.metrics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.split.client.dtos.Counter;
import io.split.client.dtos.Latency;
import io.split.client.utils.Utils;
import io.split.engine.metrics.Metrics;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by adilaijaz on 9/4/15.
 */
public class HttpMetrics implements Metrics, DTOMetrics {
    private static final Logger _log = LoggerFactory.getLogger(HttpMetrics.class);

    private final CloseableHttpClient _client;
    private final URI _timeTarget;
    private final URI _counterTarget;


    public static HttpMetrics create(CloseableHttpClient client, URI root) throws URISyntaxException {
        return new HttpMetrics(client, root);
    }


    public HttpMetrics(CloseableHttpClient client, URI root) throws URISyntaxException {
        Preconditions.checkNotNull(root);
        _client = Preconditions.checkNotNull(client);
        _timeTarget = Utils.appendPath(root, "api/metrics/time");
        _counterTarget = Utils.appendPath(root, "api/metrics/counter");
    }


    @Override
    public void time(Latency dto) {
        if (dto.latencies.isEmpty()) {
            return;
        }

        try {
            post(_timeTarget, dto);
        } catch (Throwable t) {
            _log.warn("Exception when posting metric" + dto, t);
        }
        ;

    }

    @Override
    public void count(Counter dto) {
        try {
            post(_counterTarget, dto);
        } catch (Throwable t) {
            _log.warn("Exception when posting metric" + dto, t);
        }

    }

    private void post(URI uri, Object dto) {

        CloseableHttpResponse response = null;

        try {
            HttpEntity entity = Utils.toJsonEntity(dto);

            HttpPost request = new HttpPost(uri);
            request.setEntity(entity);

            response = _client.execute(request);

            int status = response.getCode();

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

    @VisibleForTesting
    URI getTimeTarget() {
        return _timeTarget;
    }

    @VisibleForTesting
    URI getCounterTarget() {
        return _counterTarget;
    }

}
