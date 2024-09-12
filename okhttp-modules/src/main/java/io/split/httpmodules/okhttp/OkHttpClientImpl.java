package io.split.httpmodules.okhttp;

import io.split.client.dtos.SplitHttpResponse;
import io.split.client.utils.SDKMetadata;
import io.split.engine.common.FetchOptions;
import io.split.service.SplitHttpClient;

import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OkHttpClientImpl implements SplitHttpClient {
    protected OkHttpClient httpClient;
    private static final Logger _log = LoggerFactory.getLogger(OkHttpClientImpl.class);
    private static final String HEADER_CACHE_CONTROL_NAME = "Cache-Control";
    private static final String HEADER_CACHE_CONTROL_VALUE = "no-cache";
    private static final String HEADER_API_KEY = "Authorization";
    private static final String HEADER_CLIENT_KEY = "SplitSDKClientKey";
    private static final String HEADER_CLIENT_MACHINE_NAME = "SplitSDKMachineName";
    private static final String HEADER_CLIENT_MACHINE_IP = "SplitSDKMachineIP";
    private static final String HEADER_CLIENT_VERSION = "SplitSDKVersion";
    private String _apikey;
    protected SDKMetadata _metadata;

    public OkHttpClientImpl(String apiToken, SDKMetadata sdkMetadata,
            Proxy proxy, String proxyAuthKerberosPrincipalName, boolean debugEnabled,
            int readTimeout, int connectionTimeout) throws IOException {
        _apikey = apiToken;
        _metadata = sdkMetadata;
        setHttpClient(proxy, proxyAuthKerberosPrincipalName, debugEnabled,
                readTimeout, connectionTimeout);
    }

    protected void setHttpClient(Proxy proxy, String proxyAuthKerberosPrincipalName, boolean debugEnabled,
            int readTimeout, int connectionTimeout) throws IOException {
        httpClient = initializeClient(proxy, proxyAuthKerberosPrincipalName, debugEnabled,
                readTimeout, connectionTimeout);
    }

    protected OkHttpClient initializeClient(Proxy proxy, String proxyAuthKerberosPrincipalName, boolean debugEnabled,
            int readTimeout, int connectionTimeout) throws IOException {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        if (debugEnabled) {
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        } else {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        Map<String, String> kerberosOptions = new HashMap<>();
        kerberosOptions.put("com.sun.security.auth.module.Krb5LoginModule", "required");
        kerberosOptions.put("refreshKrb5Config", "false");
        kerberosOptions.put("doNotPrompt", "false");
        kerberosOptions.put("useTicketCache", "true");

        Authenticator proxyAuthenticator = getProxyAuthenticator(proxyAuthKerberosPrincipalName, kerberosOptions);

        return new okhttp3.OkHttpClient.Builder()
                .proxy(proxy)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                .addInterceptor(logging)
                .proxyAuthenticator(proxyAuthenticator)
                .build();
    }

    public HTTPKerberosAuthInterceptor getProxyAuthenticator(String proxyKerberosPrincipalName,
            Map<String, String> kerberosOptions) throws IOException {
        return new HTTPKerberosAuthInterceptor(proxyKerberosPrincipalName, kerberosOptions);
    }

    @Override
    public SplitHttpResponse get(URI uri, FetchOptions options, Map<String, List<String>> additionalHeaders) {
        try {
            okhttp3.Request.Builder requestBuilder = getRequestBuilder();
            requestBuilder.url(uri.toString());
            setBasicHeaders(requestBuilder);
            setAdditionalAndDecoratedHeaders(requestBuilder, additionalHeaders);
            if (options.cacheControlHeadersEnabled()) {
                requestBuilder.addHeader(HEADER_CACHE_CONTROL_NAME, HEADER_CACHE_CONTROL_VALUE);
            }

            Request request = requestBuilder.build();
            _log.debug(String.format("Request Headers: %s", request.headers()));

            Response response = httpClient.newCall(request).execute();

            int responseCode = response.code();

            _log.debug(String.format("[GET] %s. Status code: %s",
                    request.url().toString(),
                    responseCode));

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

    @Override
    public SplitHttpResponse post(URI url, String entity,
            Map<String, List<String>> additionalHeaders) {
        try {
            okhttp3.Request.Builder requestBuilder = getRequestBuilder();
            requestBuilder.url(url.toString());
            setBasicHeaders(requestBuilder);
            setAdditionalAndDecoratedHeaders(requestBuilder, additionalHeaders);
            requestBuilder.addHeader("Accept-Encoding", "gzip");
            requestBuilder.addHeader("Content-Type", "application/json");
            // String post = EntityUtils.toString((HttpEntity) entity);
            RequestBody postBody = RequestBody.create(entity.getBytes());
            requestBuilder.post(postBody);

            Request request = requestBuilder.build();
            System.out.println(request);
            _log.debug(String.format("Request Headers: %s", request.headers()));

            Response response = httpClient.newCall(request).execute();

            int responseCode = response.code();

            _log.debug(String.format("[GET] %s. Status code: %s",
                    request.url().toString(),
                    responseCode));

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

    protected okhttp3.Request.Builder getRequestBuilder() {
        return new okhttp3.Request.Builder();
    }

    protected Request getRequest(okhttp3.Request.Builder requestBuilder) {
        return requestBuilder.build();
    }

    protected void setBasicHeaders(okhttp3.Request.Builder requestBuilder) {
        requestBuilder.addHeader(HEADER_API_KEY, "Bearer " + _apikey);
        requestBuilder.addHeader(HEADER_CLIENT_VERSION, _metadata.getSdkVersion());
        requestBuilder.addHeader(HEADER_CLIENT_MACHINE_IP, _metadata.getMachineIp());
        requestBuilder.addHeader(HEADER_CLIENT_MACHINE_NAME, _metadata.getMachineName());
        requestBuilder.addHeader(HEADER_CLIENT_KEY, _apikey.length() > 4
                ? _apikey.substring(_apikey.length() - 4)
                : _apikey);
    }

    protected void setAdditionalAndDecoratedHeaders(okhttp3.Request.Builder requestBuilder,
            Map<String, List<String>> additionalHeaders) {
        if (additionalHeaders != null) {
            for (Map.Entry<String, List<String>> entry : additionalHeaders.entrySet()) {
                for (String value : entry.getValue()) {
                    requestBuilder.addHeader(entry.getKey(), value);
                }
            }
        }
    }

    protected SplitHttpResponse.Header[] getResponseHeaders(Response response) {
        List<SplitHttpResponse.Header> responseHeaders = new ArrayList<>();
        Map<String, List<String>> map = response.headers().toMultimap();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                responseHeaders.add(new SplitHttpResponse.Header(entry.getKey(), entry.getValue()));
            }
        }
        return responseHeaders.toArray(new SplitHttpResponse.Header[0]);
    }

    @Override
    public void close() throws IOException {
        httpClient.dispatcher().executorService().shutdown();
    }

}
