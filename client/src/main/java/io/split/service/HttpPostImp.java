package io.split.service;

import io.split.client.utils.Utils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hc.client5.http.classic.methods.HttpPost;

import java.net.URI;

public class HttpPostImp {
    private static final int STATUS_OKEY = 200;
    private static final int STATUS_MIN = 300;
    private static final Logger _logger = LoggerFactory.getLogger(HttpPostImp.class);
    private CloseableHttpClient _client;

    public HttpPostImp(CloseableHttpClient client) {
        _client = client;
    }

    public void post(URI uri, Object object, String posted) {
        CloseableHttpResponse response = null;

        try {
            HttpEntity entity = Utils.toJsonEntity(object);
            HttpPost request = new HttpPost(uri);
            request.setEntity(entity);

            response = _client.execute(request);

            int status = response.getCode();

            if (status < STATUS_OKEY || status >= STATUS_MIN) {
                _logger.warn("Response status was: " + status);
            }

        } catch (Throwable t) {
            _logger.warn("Exception when posting " + posted + object, t);
        } finally {
            Utils.forceClose(response);
        }
    }
}
