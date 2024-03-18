package io.split.service;

import io.split.client.RequestDecorator;
import io.split.client.utils.Utils;
import io.split.engine.common.FetchOptions;
import io.split.client.dtos.SplitHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class SplitHttpClientImpl implements SplitHttpClient {
    private static final Logger _log = LoggerFactory.getLogger(SplitHttpClient.class);
    private static final String HEADER_CACHE_CONTROL_NAME = "Cache-Control";
    private static final String HEADER_CACHE_CONTROL_VALUE = "no-cache";
    private final CloseableHttpClient _client;
    private final RequestDecorator _requestDecorator;

    public static SplitHttpClientImpl create(
            CloseableHttpClient client,
            RequestDecorator requestDecorator
    ) throws URISyntaxException {
        return new SplitHttpClientImpl(client, requestDecorator);
    }

    public SplitHttpClientImpl
            (CloseableHttpClient client,
             RequestDecorator requestDecorator) {
        _client = client;
        _requestDecorator = requestDecorator;
    }

    public SplitHttpResponse get(URI uri, FetchOptions options) {
        CloseableHttpResponse response = null;

        try {
            HttpGet request = new HttpGet(uri);
            if(options.cacheControlHeadersEnabled()) {
                request.setHeader(HEADER_CACHE_CONTROL_NAME, HEADER_CACHE_CONTROL_VALUE);
            }
            request = (HttpGet) _requestDecorator.decorateHeaders(request);

            response = _client.execute(request);

            int statusCode = response.getCode();

            if (_log.isDebugEnabled()) {
                _log.debug(String.format("[%s] %s. Status code: %s", request.getMethod(), uri.toURL(), statusCode));
            }

            SplitHttpResponse httpResponse = new SplitHttpResponse();
            httpResponse.statusMessage = "";
            if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
                _log.warn(String.format("Response status was: %s. Reason: %s", statusCode , response.getReasonPhrase()));
                httpResponse.statusMessage = response.getReasonPhrase();
            }
            httpResponse.statusCode = statusCode;
            httpResponse.body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return httpResponse;
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Problem in http get operation: %s", e), e);
        } finally {
            Utils.forceClose(response);
        }
    }

    public SplitHttpResponse post
            (URI uri,
             HttpEntity entity,
             Map<String, String> additionalHeaders) throws IOException {
        CloseableHttpResponse response = null;
        long initTime = System.currentTimeMillis();
        try {
            HttpPost request = new HttpPost(uri);
            if (additionalHeaders != null) {
                for (Map.Entry entry : additionalHeaders.entrySet()) {
                    request.addHeader(entry.getKey().toString(), entry.getValue());
                }
            }
            request.setEntity(entity);
            request = (HttpPost) _requestDecorator.decorateHeaders(request);

            response = _client.execute(request);

            int status = response.getCode();

            String statusMessage = new String("");
            if (status < HttpStatus.SC_OK || status >= HttpStatus.SC_MULTIPLE_CHOICES) {
                statusMessage = response.getReasonPhrase();
                _log.warn(String.format("Response status was: %s. Reason: %s", status, response.getReasonPhrase()));
            }
            SplitHttpResponse httpResponse = new SplitHttpResponse();
            httpResponse.statusCode = status;
            httpResponse.body = "";
            httpResponse.statusMessage = statusMessage;
            return httpResponse;
        } catch (Exception e) {
            throw new IOException(String.format("Problem in http post operation: %s", e), e);
        } finally {
            Utils.forceClose(response);
        }
    }
}
