package io.split.service;

import io.split.client.RequestDecorator;
import io.split.client.utils.ApacheRequestDecorator;
import io.split.client.utils.SDKMetadata;
import io.split.client.utils.Utils;
import io.split.engine.common.FetchOptions;
import io.split.client.dtos.SplitHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SplitHttpClientImpl implements SplitHttpClient {

    private static final Logger _log = LoggerFactory.getLogger(SplitHttpClient.class);
    private static final String HEADER_CACHE_CONTROL_NAME = "Cache-Control";
    private static final String HEADER_CACHE_CONTROL_VALUE = "no-cache";
    private static final String HEADER_API_KEY = "Authorization";
    private static final String HEADER_CLIENT_KEY = "SplitSDKClientKey";
    private static final String HEADER_CLIENT_MACHINE_NAME = "SplitSDKMachineName";
    private static final String HEADER_CLIENT_MACHINE_IP = "SplitSDKMachineIP";
    private static final String HEADER_CLIENT_VERSION = "SplitSDKVersion";

    private final CloseableHttpClient _client;
    private final RequestDecorator _requestDecorator;
    private final String _apikey;
    private final SDKMetadata _metadata;

    public static SplitHttpClientImpl create(CloseableHttpClient client,
            RequestDecorator requestDecorator,
            String apikey,
            SDKMetadata metadata) throws URISyntaxException {
        return new SplitHttpClientImpl(client, requestDecorator, apikey, metadata);
    }

    private SplitHttpClientImpl(CloseableHttpClient client,
            RequestDecorator requestDecorator,
            String apikey,
            SDKMetadata metadata) {
        _client = client;
        _requestDecorator = requestDecorator;
        _apikey = apikey;
        _metadata = metadata;
    }

    public SplitHttpResponse get(URI uri, FetchOptions options, Map<String, List<String>> additionalHeaders) {
        CloseableHttpResponse response = null;

        try {
            HttpGet request = new HttpGet(uri);
            setBasicHeaders(request);
            if (additionalHeaders != null) {
                for (Map.Entry<String, List<String>> entry : additionalHeaders.entrySet()) {
                    for (String value : entry.getValue()) {
                        request.addHeader(entry.getKey(), value);
                    }
                }
            }
            if (options.cacheControlHeadersEnabled()) {
                request.setHeader(HEADER_CACHE_CONTROL_NAME, HEADER_CACHE_CONTROL_VALUE);
            }

            request = (HttpGet) ApacheRequestDecorator.decorate(request, _requestDecorator);

            response = _client.execute(request);

            if (_log.isDebugEnabled()) {
                _log.debug(String.format("[%s] %s. Status code: %s", request.getMethod(), uri.toURL(),
                        response.getCode()));
            }

            String statusMessage = "";
            int code = response.getCode();
            String body = "";
            if (code < HttpStatus.SC_OK || code >= HttpStatus.SC_MULTIPLE_CHOICES) {
                statusMessage = response.getReasonPhrase();
                _log.warn(String.format("Response status was: %s. Reason: %s", code, statusMessage));
            } else {
                body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }

            return new SplitHttpResponse(code,
                    statusMessage,
                    body,
                    Arrays.stream(response.getHeaders()).map(
                                    h -> new SplitHttpResponse.Header(h.getName(), Collections.singletonList(h.getValue())))
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Problem in http get operation: %s", e), e);
        } finally {
            Utils.forceClose(response);
        }
    }

    public SplitHttpResponse post(URI uri, String body, Map<String, List<String>> additionalHeaders)
            throws IOException {

        CloseableHttpResponse response = null;
        try {
            HttpPost request = new HttpPost(uri);
            setBasicHeaders(request);
            if (additionalHeaders != null) {
                for (Map.Entry<String, List<String>> entry : additionalHeaders.entrySet()) {
                    for (String value : entry.getValue()) {
                        request.addHeader(entry.getKey(), value);
                    }
                }
            }
            request.setEntity(HttpEntities.create(body, ContentType.APPLICATION_JSON));
            request = (HttpPost) ApacheRequestDecorator.decorate(request, _requestDecorator);

            response = _client.execute(request);

            String statusMessage = "";
            if (response.getCode() < HttpStatus.SC_OK || response.getCode() >= HttpStatus.SC_MULTIPLE_CHOICES) {
                statusMessage = response.getReasonPhrase();
                _log.warn(String.format("Response status was: %s. Reason: %s", response.getCode(),
                        response.getReasonPhrase()));
            }
            return new SplitHttpResponse(response.getCode(), statusMessage, "",
                    Arrays.stream(response.getHeaders()).map(
                            h -> new SplitHttpResponse.Header(h.getName(), Collections.singletonList(h.getValue())))
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            throw new IOException(String.format("Problem in http post operation: %s", e), e);
        } finally {
            Utils.forceClose(response);
        }
    }

    private void setBasicHeaders(HttpRequest request) {
        request.setHeader(HEADER_API_KEY, "Bearer " + _apikey);
        request.setHeader(HEADER_CLIENT_VERSION, _metadata.getSdkVersion());
        request.setHeader(HEADER_CLIENT_MACHINE_IP, _metadata.getMachineIp());
        request.setHeader(HEADER_CLIENT_MACHINE_NAME, _metadata.getMachineName());
        request.setHeader(HEADER_CLIENT_KEY, _apikey.length() > 4
                ? _apikey.substring(_apikey.length() - 4)
                : _apikey);
    }

    @Override
    public void close() throws IOException {
        _client.close();
    }
}
