package io.split.engine.sse;

import io.split.TestHelper;
import io.split.engine.sse.dtos.AuthenticationResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class AuthApiClientTest {
    private final TestHelper _testHelper = new TestHelper();

    @Test
    public void authenticateWithPushEnabledShouldReturnSuccess() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        CloseableHttpClient httpClientMock = _testHelper.mockHttpClient("streaming-auth-push-enabled.json", HttpStatus.SC_OK);

        AuthApiClient authApiClient = new AuthApiClientImp( "www.split-test.io", httpClientMock);
        AuthenticationResponse result = authApiClient.Authenticate();

        Assert.assertTrue(result.isPushEnabled());
        Assert.assertEquals("xxxx_xxxx_segments,xxxx_xxxx_splits,control", result.getChannels());
        Assert.assertFalse(result.isRetry());
        Assert.assertFalse(StringUtils.isEmpty(result.getToken()));
        Assert.assertTrue(result.getExpiration() > 0);

    }

    @Test
    public void authenticateWithPushEnabledWithWrongTokenShouldReturnError() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        CloseableHttpClient httpClientMock = _testHelper.mockHttpClient("streaming-auth-push-enabled-wrong-token.json", HttpStatus.SC_OK);

        AuthApiClient authApiClient = new AuthApiClientImp( "www.split-test.io", httpClientMock);
        AuthenticationResponse result = authApiClient.Authenticate();

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertTrue(StringUtils.isEmpty(result.getChannels()));
        Assert.assertTrue(result.isRetry());
        Assert.assertTrue(StringUtils.isEmpty(result.getToken()));
        Assert.assertFalse(result.getExpiration() > 0);
    }

    @Test
    public void authenticateWithPushDisabledShouldReturnSuccess() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        CloseableHttpClient httpClientMock = _testHelper.mockHttpClient("streaming-auth-push-disabled.json", HttpStatus.SC_OK);

        AuthApiClient authApiClient = new AuthApiClientImp("www.split-test.io", httpClientMock);
        AuthenticationResponse result = authApiClient.Authenticate();

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertTrue(StringUtils.isEmpty(result.getChannels()));
        Assert.assertFalse(result.isRetry());
        Assert.assertTrue(StringUtils.isEmpty(result.getToken()));
    }

    @Test
    public void authenticateServerErrorShouldReturnErrorWithRetry() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        CloseableHttpClient httpClientMock = _testHelper.mockHttpClient("", HttpStatus.SC_INTERNAL_SERVER_ERROR);

        AuthApiClient authApiClient = new AuthApiClientImp("www.split-test.io", httpClientMock);
        AuthenticationResponse result = authApiClient.Authenticate();

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertTrue(StringUtils.isEmpty(result.getChannels()));
        Assert.assertTrue(StringUtils.isEmpty(result.getToken()));
        Assert.assertTrue(result.isRetry());
    }

    @Test
    public void authenticateServerBadRequestShouldReturnErrorWithoutRetry() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        CloseableHttpClient httpClientMock = _testHelper.mockHttpClient("", HttpStatus.SC_BAD_REQUEST);

        AuthApiClient authApiClient = new AuthApiClientImp("www.split-test.io", httpClientMock);
        AuthenticationResponse result = authApiClient.Authenticate();

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertTrue(StringUtils.isEmpty(result.getChannels()));
        Assert.assertTrue(StringUtils.isEmpty(result.getToken()));
        Assert.assertFalse(result.isRetry());
    }

    @Test
    public void authenticateServerUnauthorizedShouldReturnErrorWithoutRetry() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        CloseableHttpClient httpClientMock = _testHelper.mockHttpClient("", HttpStatus.SC_UNAUTHORIZED);

        AuthApiClient authApiClient = new AuthApiClientImp("www.split-test.io", httpClientMock);
        AuthenticationResponse result = authApiClient.Authenticate();

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertTrue(StringUtils.isEmpty(result.getChannels()));
        Assert.assertTrue(StringUtils.isEmpty(result.getToken()));
        Assert.assertFalse(result.isRetry());
    }
}
