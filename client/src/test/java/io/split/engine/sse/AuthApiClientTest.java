package io.split.engine.sse;

import io.split.engine.sse.dtos.AuthenticationResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.Assert.*;

public class AuthApiClientTest {
    @Test
    public void authenticateWithPushEnabledShouldReturnSuccess() throws IOException {
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponseMock = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLineMock = Mockito.mock(StatusLine.class);
        HttpEntity entityMock = Mockito.mock(HttpEntity.class);

        Mockito.when(statusLineMock.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        Mockito.when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
        Mockito.when(entityMock.getContent()).thenReturn(getClass().getClassLoader().getResourceAsStream("streaming-auth-push-enabled.json"));
        Mockito.when(httpResponseMock.getEntity()).thenReturn(entityMock);

        Mockito.when(httpClientMock.execute((HttpUriRequest) Mockito.anyObject())).thenReturn(httpResponseMock);

        AuthApiClient authApiClient = new AuthApiClientImp( "www.split-test.io", httpClientMock);
        AuthenticationResponse result = authApiClient.Authenticate();

        assertTrue(result.isPushEnabled());
        assertEquals("xxxx_xxxx_segments,xxxx_xxxx_splits,control", result.getChannels());
        assertFalse(result.isRetry());
        assertFalse(StringUtils.isEmpty(result.getToken()));
        assertTrue(result.getExpiration() > 0);
    }

    @Test
    public void authenticateWithPushEnabledWithWrongTokenShouldReturnError() throws IOException {
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponseMock = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLineMock = Mockito.mock(StatusLine.class);
        HttpEntity entityMock = Mockito.mock(HttpEntity.class);

        Mockito.when(statusLineMock.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        Mockito.when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
        Mockito.when(entityMock.getContent()).thenReturn(getClass().getClassLoader().getResourceAsStream("streaming-auth-push-enabled-wrong-token.json"));
        Mockito.when(httpResponseMock.getEntity()).thenReturn(entityMock);

        Mockito.when(httpClientMock.execute((HttpUriRequest) Mockito.anyObject())).thenReturn(httpResponseMock);

        AuthApiClient authApiClient = new AuthApiClientImp( "www.split-test.io", httpClientMock);
        AuthenticationResponse result = authApiClient.Authenticate();

        assertFalse(result.isPushEnabled());
        assertTrue(StringUtils.isEmpty(result.getChannels()));
        assertTrue(result.isRetry());
        assertTrue(StringUtils.isEmpty(result.getToken()));
        assertFalse(result.getExpiration() > 0);
    }

    @Test
    public void authenticateWithPushDisabledShouldReturnSuccess() throws IOException {
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponseMock = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLineMock = Mockito.mock(StatusLine.class);
        HttpEntity entityMock = Mockito.mock(HttpEntity.class);

        Mockito.when(statusLineMock.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        Mockito.when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
        Mockito.when(entityMock.getContent()).thenReturn(getClass().getClassLoader().getResourceAsStream("streaming-auth-push-disabled.json"));
        Mockito.when(httpResponseMock.getEntity()).thenReturn(entityMock);

        Mockito.when(httpClientMock.execute((HttpUriRequest) Mockito.anyObject())).thenReturn(httpResponseMock);

        AuthApiClient authApiClient = new AuthApiClientImp("www.split-test.io", httpClientMock);
        AuthenticationResponse result = authApiClient.Authenticate();

        assertFalse(result.isPushEnabled());
        assertTrue(StringUtils.isEmpty(result.getChannels()));
        assertFalse(result.isRetry());
        assertTrue(StringUtils.isEmpty(result.getToken()));
    }

    @Test
    public void authenticateServerErrorShouldReturnErrorWithRetry() throws IOException {
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponseMock = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLineMock = Mockito.mock(StatusLine.class);

        Mockito.when(statusLineMock.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Mockito.when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
        Mockito.when(httpClientMock.execute((HttpUriRequest) Mockito.anyObject())).thenReturn(httpResponseMock);

        AuthApiClient authApiClient = new AuthApiClientImp("www.split-test.io", httpClientMock);
        AuthenticationResponse result = authApiClient.Authenticate();

        assertFalse(result.isPushEnabled());
        assertTrue(StringUtils.isEmpty(result.getChannels()));
        assertTrue(StringUtils.isEmpty(result.getToken()));
        assertTrue(result.isRetry());
    }

    @Test
    public void authenticateServerBadRequestShouldReturnErrorWithoutRetry() throws IOException {
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponseMock = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLineMock = Mockito.mock(StatusLine.class);

        Mockito.when(statusLineMock.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        Mockito.when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
        Mockito.when(httpClientMock.execute((HttpUriRequest) Mockito.anyObject())).thenReturn(httpResponseMock);

        AuthApiClient authApiClient = new AuthApiClientImp("www.split-test.io", httpClientMock);
        AuthenticationResponse result = authApiClient.Authenticate();

        assertFalse(result.isPushEnabled());
        assertTrue(StringUtils.isEmpty(result.getChannels()));
        assertTrue(StringUtils.isEmpty(result.getToken()));
        assertFalse(result.isRetry());
    }

    @Test
    public void authenticateServerUnauthorizedShouldReturnErrorWithoutRetry() throws IOException {
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponseMock = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLineMock = Mockito.mock(StatusLine.class);

        Mockito.when(statusLineMock.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
        Mockito.when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
        Mockito.when(httpClientMock.execute((HttpUriRequest) Mockito.anyObject())).thenReturn(httpResponseMock);

        AuthApiClient authApiClient = new AuthApiClientImp("www.split-test.io", httpClientMock);
        AuthenticationResponse result = authApiClient.Authenticate();

        assertFalse(result.isPushEnabled());
        assertTrue(StringUtils.isEmpty(result.getChannels()));
        assertTrue(StringUtils.isEmpty(result.getToken()));
        assertFalse(result.isRetry());
    }
}
