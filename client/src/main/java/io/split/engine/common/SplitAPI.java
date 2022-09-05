package io.split.engine.common;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

public class SplitAPI {

    private final CloseableHttpClient _httpClient;
    private final CloseableHttpClient _sseHttpClient;

    private SplitAPI(CloseableHttpClient httpClient, CloseableHttpClient sseHttpClient) {
        _httpClient = httpClient;
        _sseHttpClient = sseHttpClient;
    }

    public static SplitAPI build(CloseableHttpClient httpClient, CloseableHttpClient sseHttpClient){
        return new SplitAPI(httpClient,sseHttpClient);
    }

    public CloseableHttpClient getHttpClient() {
        return _httpClient;
    }

    public CloseableHttpClient getSseHttpClient() {
        return _sseHttpClient;
    }
}