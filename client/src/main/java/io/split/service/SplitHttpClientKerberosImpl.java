package io.split.service;

import io.split.client.RequestDecorator;
import io.split.client.dtos.SplitHttpResponse;
import io.split.client.utils.SDKMetadata;
import io.split.engine.common.FetchOptions;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SplitHttpClientKerberosImpl implements SplitHttpClient {

    private static final Logger _log = LoggerFactory.getLogger(SplitHttpClientKerberosImpl.class);
    private static final String HEADER_CACHE_CONTROL_NAME = "Cache-Control";
    private static final String HEADER_CACHE_CONTROL_VALUE = "no-cache";
    private static final String HEADER_API_KEY = "Authorization";
    private static final String HEADER_CLIENT_KEY = "SplitSDKClientKey";
    private static final String HEADER_CLIENT_MACHINE_NAME = "SplitSDKMachineName";
    private static final String HEADER_CLIENT_MACHINE_IP = "SplitSDKMachineIP";
    private static final String HEADER_CLIENT_VERSION = "SplitSDKVersion";

    private final RequestDecorator _requestDecorator;
    private final String _apikey;
    private final SDKMetadata _metadata;
    private final OkHttpClient _client;

    public static SplitHttpClientKerberosImpl create(OkHttpClient client, RequestDecorator requestDecorator,
                                                     String apikey,
                                                     SDKMetadata metadata) {
        return new SplitHttpClientKerberosImpl(client, requestDecorator, apikey, metadata);
    }

    SplitHttpClientKerberosImpl(OkHttpClient client, RequestDecorator requestDecorator,
                                String apikey,
                                SDKMetadata metadata) {
        _requestDecorator = requestDecorator;
        _apikey = apikey;
        _metadata = metadata;
        _client = client;
    }

    public SplitHttpResponse get(URI uri, FetchOptions options, Map<String, List<String>> additionalHeaders) {
        try {
            Builder requestBuilder = new Builder();
            requestBuilder.url(uri.toString());
            setBasicHeaders(requestBuilder);
            setAdditionalAndDecoratedHeaders(requestBuilder, additionalHeaders);
            if (options.cacheControlHeadersEnabled()) {
                requestBuilder.addHeader(HEADER_CACHE_CONTROL_NAME, HEADER_CACHE_CONTROL_VALUE);
            }

            Request request = requestBuilder.build();
            _log.debug(String.format("Request Headers: %s", request.headers()));

            Response response = _client.newCall(request).execute();

            int responseCode = response.code();

            if (_log.isDebugEnabled()) {
                _log.debug(String.format("[GET] %s. Status code: %s",
                        request.url().toString(),
                        responseCode));
            }

            String statusMessage = "";
            if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                _log.warn(String.format("Response status was: %s. Reason: %s", responseCode,
                        response.message()));
                statusMessage = response.message();
            }

            String responseBody = response.body().string();
            response.close();

            return new SplitHttpResponse(responseCode,
                    statusMessage,
                    responseBody,
                    getResponseHeaders(response));
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Problem in http get operation: %s", e), e);
        }
    }

    public SplitHttpResponse post(URI url, HttpEntity entity,
                                  Map<String, List<String>> additionalHeaders) {
        try {
            Builder requestBuilder = new Builder();
            requestBuilder.url(url.toString());
            setBasicHeaders(requestBuilder);
            setAdditionalAndDecoratedHeaders(requestBuilder, additionalHeaders);
            requestBuilder.addHeader("Accept-Encoding", "gzip");
            requestBuilder.addHeader("Content-Type", "application/json");
            String post = EntityUtils.toString(entity);
            RequestBody postBody = RequestBody.create(post.getBytes());
            requestBuilder.post(postBody);

            Request request = requestBuilder.build();
            _log.debug(String.format("Request Headers: %s", request.headers()));

            Response response = _client.newCall(request).execute();

            int responseCode = response.code();

            if (_log.isDebugEnabled()) {
                _log.debug(String.format("[GET] %s. Status code: %s",
                        request.url().toString(),
                        responseCode));
            }

            String statusMessage = "";
            if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                _log.warn(String.format("Response status was: %s. Reason: %s", responseCode,
                        response.message()));
                statusMessage = response.message();
            }
            response.close();

            return new SplitHttpResponse(responseCode, statusMessage, "", getResponseHeaders(response));
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Problem in http post operation: %s", e), e);
        }
    }

    private void setBasicHeaders(Builder requestBuilder) {
        requestBuilder.addHeader(HEADER_API_KEY, "Bearer " + _apikey);
        requestBuilder.addHeader(HEADER_CLIENT_VERSION, _metadata.getSdkVersion());
        requestBuilder.addHeader(HEADER_CLIENT_MACHINE_IP, _metadata.getMachineIp());
        requestBuilder.addHeader(HEADER_CLIENT_MACHINE_NAME, _metadata.getMachineName());
        requestBuilder.addHeader(HEADER_CLIENT_KEY, _apikey.length() > 4
                ? _apikey.substring(_apikey.length() - 4)
                : _apikey);
    }

    private void setAdditionalAndDecoratedHeaders(Builder requestBuilder, Map<String, List<String>> additionalHeaders) {
        if (additionalHeaders != null) {
            for (Map.Entry<String, List<String>> entry : additionalHeaders.entrySet()) {
                for (String value : entry.getValue()) {
                    requestBuilder.addHeader(entry.getKey(), value);
                }
            }
        }
        HttpRequest request = new HttpGet("");
        _requestDecorator.decorateHeaders(request);
        for (Header header : request.getHeaders()) {
            requestBuilder.addHeader(header.getName(), header.getValue());
        }
    }

    private Header[] getResponseHeaders(Response response) {
        List<BasicHeader> responseHeaders = new ArrayList<>();
        Map<String, List<String>> map = response.headers().toMultimap();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                BasicHeader responseHeader = new BasicHeader(entry.getKey(), entry.getValue());
                responseHeaders.add(responseHeader);
            }
        }
        return responseHeaders.toArray(new Header[0]);
    }
    @Override
    public void close() throws IOException {
        _client.dispatcher().executorService().shutdown();
    }
}
