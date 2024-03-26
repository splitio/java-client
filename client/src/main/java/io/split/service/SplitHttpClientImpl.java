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

    private SplitHttpClientImpl
            (CloseableHttpClient client,
             RequestDecorator requestDecorator) {
        _client = client;
        _requestDecorator = requestDecorator;
    }

    public SplitHttpResponse get(URI uri, FetchOptions options, Map<String, String> additionalHeaders) {
        CloseableHttpResponse response = null;

        try {
            HttpGet request = new HttpGet(uri);
            if (additionalHeaders != null) {
                for (Map.Entry entry : additionalHeaders.entrySet()) {
                    request.addHeader(entry.getKey().toString(), entry.getValue());
                }
            }
            if(options.cacheControlHeadersEnabled()) {
                request.setHeader(HEADER_CACHE_CONTROL_NAME, HEADER_CACHE_CONTROL_VALUE);
            }
            request = (HttpGet) _requestDecorator.decorateHeaders(request);

            response = _client.execute(request);

            if (_log.isDebugEnabled()) {
                _log.debug(String.format("[%s] %s. Status code: %s", request.getMethod(), uri.toURL(), response.getCode()));
            }

            SplitHttpResponse httpResponse = new SplitHttpResponse();
            httpResponse.statusMessage = "";
            httpResponse.responseHeaders = response.getHeaders();
            if (response.getCode() < HttpStatus.SC_OK || response.getCode() >= HttpStatus.SC_MULTIPLE_CHOICES) {
                _log.warn(String.format("Response status was: %s. Reason: %s", response.getCode() , response.getReasonPhrase()));
                httpResponse.statusMessage = response.getReasonPhrase();
            }
            httpResponse.statusCode = response.getCode();
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

            String statusMessage = "";
            if (response.getCode() < HttpStatus.SC_OK || response.getCode() >= HttpStatus.SC_MULTIPLE_CHOICES) {
                statusMessage = response.getReasonPhrase();
                _log.warn(String.format("Response status was: %s. Reason: %s", response.getCode(), response.getReasonPhrase()));
            }
            SplitHttpResponse httpResponse = new SplitHttpResponse();
            httpResponse.statusCode = response.getCode();
            httpResponse.body = "";
            httpResponse.statusMessage = statusMessage;
            httpResponse.responseHeaders = response.getHeaders();
            return httpResponse;
        } catch (Exception e) {
            throw new IOException(String.format("Problem in http post operation: %s", e), e);
        } finally {
            Utils.forceClose(response);
        }
    }
}
