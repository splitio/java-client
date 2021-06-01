package io.split.client.interceptors;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;

public class ClientKeyInterceptorFilter implements HttpRequestInterceptor {
    static final String CLIENT_KEY = "SplitSDKClientKey";

    private final String _clientKey;

    public static ClientKeyInterceptorFilter instance(String apiToken) {
        return new ClientKeyInterceptorFilter(getKey(apiToken));
    }

    private ClientKeyInterceptorFilter(String clientKey) {
        _clientKey = clientKey;
    }

    @Override
    public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) throws HttpException, IOException {
        httpRequest.addHeader(CLIENT_KEY, _clientKey);
    }

    private static String getKey(String clientKey) {
        return clientKey.length() >4 ? clientKey.substring(clientKey.length() - 4) : clientKey;
    }
}
