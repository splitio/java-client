package io.split.httpmodules.okhttp;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.split.client.RequestDecorator;
import io.split.client.dtos.SplitHttpResponse;
import io.split.client.utils.SDKMetadata;
import io.split.engine.common.FetchOptions;
import io.split.service.SplitHttpClient;

import split.org.apache.hc.client5.http.classic.methods.HttpGet;
import split.org.apache.hc.core5.http.Header;
import split.org.apache.hc.core5.http.HttpEntity;
import split.org.apache.hc.core5.http.HttpRequest;
import split.org.apache.hc.core5.http.io.entity.EntityUtils;
import split.org.apache.hc.core5.http.message.BasicHeader;

import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Request.*;
import okhttp3.RequestBody;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OkHttpModule implements SplitHttpClient {
    private static final int DEFAULT_CONNECTION_TIMEOUT = 10000;
    private static final int DEFAULT_READ_TIMEOUT = 10000;
    private final boolean _debugEnabled;
    private final int _connectionTimeout;
    private final int _readTimeout;
    private final Proxy _proxy;
    private final ProxyAuthScheme _proxyAuthScheme;
    private final String _proxyAuthKerberosPrincipalName;
    public final OkHttpClient httpClient;
    private static final Logger _log = LoggerFactory.getLogger(OkHttpModule.class);
    private static final String HEADER_CACHE_CONTROL_NAME = "Cache-Control";
    private static final String HEADER_CACHE_CONTROL_VALUE = "no-cache";
    private static final String HEADER_API_KEY = "Authorization";
    private static final String HEADER_CLIENT_KEY = "SplitSDKClientKey";
    private static final String HEADER_CLIENT_MACHINE_NAME = "SplitSDKMachineName";
    private static final String HEADER_CLIENT_MACHINE_IP = "SplitSDKMachineIP";
    private static final String HEADER_CLIENT_VERSION = "SplitSDKVersion";
    private RequestDecorator _requestDecorator;
    private String _apikey;
    private SDKMetadata _metadata;

    public static Builder builder() {
        return new Builder();
    }

    private OkHttpModule(ProxyAuthScheme proxyAuthScheme,
                         String proxyAuthKerberosPrincipalName,
                         Proxy proxy,
                        int connectionTimeout,
                        int readTimeout,
                         boolean debugEnabled) throws IOException {
        _proxyAuthScheme = proxyAuthScheme;
        _proxyAuthKerberosPrincipalName = proxyAuthKerberosPrincipalName;
        _proxy = proxy;
        _connectionTimeout = connectionTimeout;
        _readTimeout = readTimeout;
        _debugEnabled = debugEnabled;

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        if (_debugEnabled) {
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        } else {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        Map<String, String> kerberosOptions = new HashMap<>();
        kerberosOptions.put("com.sun.security.auth.module.Krb5LoginModule", "required");
        kerberosOptions.put("refreshKrb5Config", "false");
        kerberosOptions.put("doNotPrompt", "false");
        kerberosOptions.put("useTicketCache", "true");

        Authenticator proxyAuthenticator = getProxyAuthenticator(_proxyAuthKerberosPrincipalName, kerberosOptions);
        httpClient = new okhttp3.OkHttpClient.Builder()
                .proxy(_proxy)
                .readTimeout(_readTimeout, TimeUnit.MILLISECONDS)
                .connectTimeout(_connectionTimeout, TimeUnit.MILLISECONDS)
                .addInterceptor(logging)
                .proxyAuthenticator(proxyAuthenticator)
                .build();
    }

    public OkHttpClient httpClient() {
        return httpClient;
    }
    public Proxy proxy() {
        return _proxy;
    }
    public ProxyAuthScheme proxyAuthScheme() {
        return _proxyAuthScheme;
    }
    public String proxyKerberosPrincipalName() { return _proxyAuthKerberosPrincipalName; }
    public int connectionTimeout() {
        return _connectionTimeout;
    }
    public boolean debugEnabled() {
        return _debugEnabled;
    }
    public int readTimeout() {
        return _readTimeout;
    }

    public static final class Builder {
        private int _connectionTimeout = 15000;
        private int _readTimeout = 15000;
        private String _proxyHost = "localhost";
        private int _proxyPort = -1;
        private ProxyAuthScheme _proxyAuthScheme = null;
        private String _proxyKerberosPrincipalName = null;
        private boolean _debugEnabled = false;

        public Builder() {
        }

        public Builder debugEnabled() {
            _debugEnabled = true;
            return this;
        }

        /**
         * The host location of the proxy. Default is localhost.
         *
         * @param proxyHost location of the proxy
         * @return this builder
         */
        public Builder proxyHost(String proxyHost) {
            _proxyHost = proxyHost;
            return this;
        }

        /**
         * The port of the proxy. Default is -1.
         *
         * @param proxyPort port for the proxy
         * @return this builder
         */
        public Builder proxyPort(int proxyPort) {
            _proxyPort = proxyPort;
            return this;
        }

        Proxy proxy() {
            if (_proxyPort != -1) {
                return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(_proxyHost, _proxyPort));
            }
            // Default is no proxy.
            return null;
        }

        /**
         * Authentication Scheme
         *
         * @param proxyAuthScheme
         * @return this builder
         */
        public Builder proxyAuthScheme(ProxyAuthScheme proxyAuthScheme) {
            _proxyAuthScheme = proxyAuthScheme;
            return this;
        }

        /**
         * Kerberos Principal Account Name
         *
         * @param proxyKerberosPrincipalName
         * @return this builder
         */
        public Builder proxyKerberosPrincipalName(String proxyKerberosPrincipalName) {
            _proxyKerberosPrincipalName = proxyKerberosPrincipalName;
            return this;
        }

        private void verifyAuthScheme() {
            if (_proxyAuthScheme == ProxyAuthScheme.KERBEROS) {
                if (proxy() == null) {
                    throw new IllegalStateException("Kerberos mode require Proxy parameters.");
                }
                if (_proxyKerberosPrincipalName == null) {
                    throw new IllegalStateException("Kerberos mode require Kerberos Principal Name.");
                }
            }
        }

        private void verifyTimeouts() {
            if (_connectionTimeout <= 0 || _connectionTimeout > DEFAULT_CONNECTION_TIMEOUT) {
                _connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
            }
            if (_readTimeout <= 0 || _readTimeout > DEFAULT_READ_TIMEOUT) {
                _readTimeout = DEFAULT_READ_TIMEOUT;
            }
        }

        public OkHttpModule build() throws IOException {
            verifyTimeouts();
            verifyAuthScheme();

            return new OkHttpModule(
                    _proxyAuthScheme,
                    _proxyKerberosPrincipalName,
                    proxy(),
                    _connectionTimeout,
                    _readTimeout,
                    _debugEnabled);
        }
    }

    public HTTPKerberosAuthInterceptor getProxyAuthenticator(String proxyKerberosPrincipalName,
                                                             Map<String, String> kerberosOptions) throws IOException {
        return new HTTPKerberosAuthInterceptor(proxyKerberosPrincipalName, kerberosOptions);
    }

    @Override
    public void setApiKey(String apikey) {
        _apikey = apikey;
    }

    @Override
    public void setMetaData(SDKMetadata metadata) {
        _metadata = metadata;
    }

    @Override
    public void setRequestDecorator(RequestDecorator requestDecorator) {
        _requestDecorator = requestDecorator;
    }

    @Override
    public SplitHttpResponse get(URI uri, FetchOptions options, Map<String, List<String>> additionalHeaders) {
        try {
            okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder();
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
    public SplitHttpResponse post(URI url, HttpEntity entity,
                                  Map<String, List<String>> additionalHeaders) {
        try {
            okhttp3.Request.Builder requestBuilder = getRequestBuilder();
            requestBuilder.url(url.toString());
            setBasicHeaders(requestBuilder);
            setAdditionalAndDecoratedHeaders(requestBuilder, additionalHeaders);
            requestBuilder.addHeader("Accept-Encoding", "gzip");
            requestBuilder.addHeader("Content-Type", "application/json");
            String post = EntityUtils.toString((HttpEntity) entity);
            RequestBody postBody = RequestBody.create(post.getBytes());
            requestBuilder.post(postBody);

            Request request = getRequest(requestBuilder);
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

    protected void setAdditionalAndDecoratedHeaders(okhttp3.Request.Builder requestBuilder, Map<String, List<String>> additionalHeaders) {
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

    protected Header[] getResponseHeaders(Response response) {
        List<BasicHeader> responseHeaders = new ArrayList<>();
        Map<String, List<String>> map = response.headers().toMultimap();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                BasicHeader responseHeader = new BasicHeader(entry.getKey(), entry.getValue());
                responseHeaders.add(responseHeader);
            }
        }
        return responseHeaders.toArray(new split.org.apache.hc.core5.http.Header[0]);
    }
    @Override
    public void close() throws IOException {
        httpClient.dispatcher().executorService().shutdown();
    }

}
