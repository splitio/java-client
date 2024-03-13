package io.split.client;

import io.split.engine.common.FetchOptions;
import io.split.telemetry.domain.enums.HttpParamsWrapper;
import io.split.client.dtos.SplitHttpResponse;

import org.apache.hc.core5.http.HttpEntity;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

public interface SplitHttpClient {
    public SplitHttpResponse get(URI uri, FetchOptions options);
    public SplitHttpResponse post
            (URI uri,
             HttpEntity entity,
             Map<String, String> additionalHeaders) throws IOException;
}
