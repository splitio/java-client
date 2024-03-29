package io.split.engine.common;

import io.split.client.RequestDecorator;
import io.split.service.SplitHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplitAPI {

    private final SplitHttpClient _httpClient;
    private final CloseableHttpClient _sseHttpClient;
    private final RequestDecorator _requestDecorator;
    private static final Logger _log = LoggerFactory.getLogger(SplitAPI.class);

    private SplitAPI(SplitHttpClient httpClient, CloseableHttpClient sseHttpClient, RequestDecorator requestDecorator) {
        _httpClient = httpClient;
        _sseHttpClient = sseHttpClient;
        _requestDecorator = requestDecorator;
    }

    public static SplitAPI build(SplitHttpClient httpClient, CloseableHttpClient sseHttpClient,
            RequestDecorator requestDecorator) {
        return new SplitAPI(httpClient, sseHttpClient, requestDecorator);
    }

    public SplitHttpClient getHttpClient() {
        return _httpClient;
    }

    public CloseableHttpClient getSseHttpClient() {
        return _sseHttpClient;
    }

    public RequestDecorator getRequestDecorator() {
        return _requestDecorator;
    }

    public void close() {
        try {
            _httpClient.close();
        } catch (Exception e) {
            _log.error("Error trying to close regular http client", e);
        }
        try {
            _sseHttpClient.close();
        } catch (Exception e) {
            _log.error("Error trying to close sseHttpClient", e);
        }
    }
}
