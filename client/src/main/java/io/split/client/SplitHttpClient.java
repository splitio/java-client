package io.split.client;

import io.split.engine.common.FetchOptions;
import io.split.telemetry.domain.enums.HttpParamsWrapper;

import org.apache.hc.core5.http.HttpEntity;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

public interface SplitHttpClient {
    public String get(URI uri, FetchOptions options, HttpParamsWrapper telemetryParamsWrapper);
    public void post
            (URI uri,
             HttpEntity entity,
             Map<String, String> additionalHeaders,
             HttpParamsWrapper telemetryParamsWrapper) throws IOException;
}
