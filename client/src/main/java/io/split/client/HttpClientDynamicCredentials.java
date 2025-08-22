package io.split.client;

import io.split.client.dtos.BearerCredentialsProvider;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.BearerToken;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.core5.http.protocol.HttpContext;

class HttpClientDynamicCredentials implements  org.apache.hc.client5.http.auth.CredentialsProvider {

    private final BearerCredentialsProvider _bearerCredentialsProvider;

    public HttpClientDynamicCredentials (BearerCredentialsProvider bearerCredentialsProvider) {
        _bearerCredentialsProvider = bearerCredentialsProvider;
    }

    @Override
    public Credentials getCredentials(AuthScope authScope, HttpContext context) {

        // This Provider is invoked every time a request is made.
        // This should invoke a user-custom provider responsible for:
        return new BearerToken(_bearerCredentialsProvider.getToken());
    }

}

