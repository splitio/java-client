package io.split.client.interceptors;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class AuthorizationInterceptorFilter implements HttpRequestInterceptor {
    static final String AUTHORIZATION_HEADER = "Authorization";

    private final String _apiTokenBearer;

    public static AuthorizationInterceptorFilter instance(String apiToken) {
        return new AuthorizationInterceptorFilter(apiToken);
    }

    private  AuthorizationInterceptorFilter(String apiToken) {
        _apiTokenBearer = "Bearer " + checkNotNull(apiToken);
    }

    @Override
    public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) throws HttpException, IOException {
        httpRequest.addHeader(AUTHORIZATION_HEADER, _apiTokenBearer);
    }
}
