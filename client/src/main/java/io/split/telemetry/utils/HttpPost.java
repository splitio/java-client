package io.split.telemetry.utils;

import io.split.client.utils.Utils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class HttpPost {
    private static final Logger _logger = LoggerFactory.getLogger(HttpPost.class);
    private CloseableHttpClient _client;

    public HttpPost(CloseableHttpClient client) {
        _client = client;
    }

    public void post(URI uri, Object object, String posted) {
        CloseableHttpResponse response = null;

        try {
            HttpEntity entity = Utils.toJsonEntity(object);
            org.apache.hc.client5.http.classic.methods.HttpPost request = new org.apache.hc.client5.http.classic.methods.HttpPost(uri);
            request.setEntity(entity);

            response = _client.execute(request);

            int status = response.getCode();

            if (status < 200 || status >= 300) {
                _logger.warn("Response status was: " + status);
            }

        } catch (Throwable t) {
            _logger.warn("Exception when posting " + posted + object, t);
        } finally {
            Utils.forceClose(response);
        }
    }
}
