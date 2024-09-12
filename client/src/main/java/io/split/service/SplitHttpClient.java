package io.split.service;

import io.split.engine.common.FetchOptions;
import io.split.client.dtos.SplitHttpResponse;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

public interface SplitHttpClient extends Closeable {
    /**
     * Wrapper for HTTP get method
     * 
     * @param uri     the URL to be used
     * @param options The FetchOptions object that contains headers.
     * @return The response structure SplitHttpResponse
     */
    public SplitHttpResponse get(URI uri, FetchOptions options, Map<String, List<String>> additionalHeaders);

    /**
     * Wrapper for HTTP post method
     * 
     * @param uri               the URL to be used
     * @param entity            HttpEntity object that has The body load
     * @param additionalHeaders Any additional headers to be added.
     * @return The response structure SplitHttpResponse
     */
    public SplitHttpResponse post(URI uri,
            String entity,
            Map<String, List<String>> additionalHeaders) throws IOException;
}
