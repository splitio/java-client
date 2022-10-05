package io.split.engine.common;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplitAPI {

    private final CloseableHttpClient _httpClient;
    private final CloseableHttpClient _sseHttpClient;
    private static final Logger _log = LoggerFactory.getLogger(SplitAPI.class);

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

    public void close(){
        try {
            _httpClient.close();
        } catch (Exception e){
            _log.error("Error trying to close httpcClient", e);
        }
        try {
            _sseHttpClient.close();
        } catch (Exception e){
            _log.error("Error trying to close sseHttpClient", e);
        }
    }
}