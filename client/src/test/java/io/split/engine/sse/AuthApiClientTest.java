package io.split.engine.sse;

import io.split.TestHelper;
import io.split.client.RequestDecorator;
import io.split.engine.sse.dtos.AuthenticationResponse;
import io.split.service.SplitHttpClient;
import io.split.service.SplitHttpClientImpl;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

public class AuthApiClientTest {
    private static TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);

    @Before
    public void setUp() {
        TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);
    }
    @Test
    public void authenticateWithPushEnabledShouldReturnSuccess() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, URISyntaxException {
        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("streaming-auth-push-enabled.json", HttpStatus.SC_OK);
        SplitHttpClient splitHttpClient = SplitHttpClientImpl.create(httpClientMock, new RequestDecorator(null));
        AuthApiClient authApiClient = new AuthApiClientImp( "www.split-test.io", splitHttpClient, TELEMETRY_STORAGE);
        AuthenticationResponse result = authApiClient.Authenticate();

        Assert.assertTrue(result.isPushEnabled());
        Assert.assertEquals("xxxx_xxxx_segments,xxxx_xxxx_splits,control", result.getChannels());
        Assert.assertFalse(result.isRetry());
        Assert.assertFalse(StringUtils.isEmpty(result.getToken()));
        Assert.assertTrue(result.getExpiration() > 0);
        Mockito.verify(TELEMETRY_STORAGE, Mockito.times(1)).recordTokenRefreshes();
        Mockito.verify(TELEMETRY_STORAGE, Mockito.times(1)).recordSyncLatency(Mockito.anyObject(), Mockito.anyLong());
        Mockito.verify(TELEMETRY_STORAGE, Mockito.times(1)).recordSuccessfulSync(Mockito.anyObject(), Mockito.anyLong());

    }

    @Test
    public void authenticateWithPushEnabledWithWrongTokenShouldReturnError() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, URISyntaxException {
        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("streaming-auth-push-enabled-wrong-token.json", HttpStatus.SC_OK);
        SplitHttpClient splitHttpClient = SplitHttpClientImpl.create(httpClientMock, new RequestDecorator(null));

        AuthApiClient authApiClient = new AuthApiClientImp( "www.split-test.io", splitHttpClient, TELEMETRY_STORAGE);
        AuthenticationResponse result = authApiClient.Authenticate();

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertTrue(StringUtils.isEmpty(result.getChannels()));
        Assert.assertTrue(result.isRetry());
        Assert.assertTrue(StringUtils.isEmpty(result.getToken()));
        Assert.assertFalse(result.getExpiration() > 0);
    }

    @Test
    public void authenticateWithPushDisabledShouldReturnSuccess() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, URISyntaxException {
        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("streaming-auth-push-disabled.json", HttpStatus.SC_OK);
        SplitHttpClient splitHttpClient = SplitHttpClientImpl.create(httpClientMock, new RequestDecorator(null));

        AuthApiClient authApiClient = new AuthApiClientImp("www.split-test.io", splitHttpClient, TELEMETRY_STORAGE);
        AuthenticationResponse result = authApiClient.Authenticate();

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertTrue(StringUtils.isEmpty(result.getChannels()));
        Assert.assertFalse(result.isRetry());
        Assert.assertTrue(StringUtils.isEmpty(result.getToken()));
    }

    @Test
    public void authenticateServerErrorShouldReturnErrorWithRetry() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, URISyntaxException {
        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        SplitHttpClient splitHttpClient = SplitHttpClientImpl.create(httpClientMock, new RequestDecorator(null));

        AuthApiClient authApiClient = new AuthApiClientImp("www.split-test.io", splitHttpClient, TELEMETRY_STORAGE);
        AuthenticationResponse result = authApiClient.Authenticate();

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertTrue(StringUtils.isEmpty(result.getChannels()));
        Assert.assertTrue(StringUtils.isEmpty(result.getToken()));
        Assert.assertTrue(result.isRetry());
    }

    @Test
    public void authenticateServerBadRequestShouldReturnErrorWithoutRetry() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, URISyntaxException {
        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("", HttpStatus.SC_BAD_REQUEST);
        SplitHttpClient splitHttpClient = SplitHttpClientImpl.create(httpClientMock, new RequestDecorator(null));

        AuthApiClient authApiClient = new AuthApiClientImp("www.split-test.io", splitHttpClient, TELEMETRY_STORAGE);
        AuthenticationResponse result = authApiClient.Authenticate();

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertTrue(StringUtils.isEmpty(result.getChannels()));
        Assert.assertTrue(StringUtils.isEmpty(result.getToken()));
        Assert.assertFalse(result.isRetry());
    }

    @Test
    public void authenticateServerUnauthorizedShouldReturnErrorWithoutRetry() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, URISyntaxException {
        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("", HttpStatus.SC_UNAUTHORIZED);
        SplitHttpClient splitHttpClient = SplitHttpClientImpl.create(httpClientMock, new RequestDecorator(null));

        AuthApiClient authApiClient = new AuthApiClientImp("www.split-test.io", splitHttpClient, TELEMETRY_STORAGE);
        AuthenticationResponse result = authApiClient.Authenticate();

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertTrue(StringUtils.isEmpty(result.getChannels()));
        Assert.assertTrue(StringUtils.isEmpty(result.getToken()));
        Assert.assertFalse(result.isRetry());
        Mockito.verify(TELEMETRY_STORAGE, Mockito.times(1)).recordAuthRejections();
    }
}
