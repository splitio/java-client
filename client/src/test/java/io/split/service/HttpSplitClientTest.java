package io.split.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.split.TestHelper;
import io.split.client.RequestDecorator;
import io.split.client.dtos.*;
import io.split.client.impressions.Impression;
import io.split.client.utils.Json;
import io.split.client.utils.SDKMetadata;
import io.split.client.utils.Utils;
import io.split.engine.common.FetchOptions;
import io.split.service.SplitHttpClient;
import io.split.service.SplitHttpClientImpl;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Header;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.verify;

public class HttpSplitClientTest {

    @Test
    public void testGetWithSpecialCharacters() throws URISyntaxException, InvocationTargetException,
            NoSuchMethodException, IllegalAccessException, IOException {
        URI rootTarget = URI.create("https://api.split.io/splitChanges?since=1234567");
        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("split-change-special-characters.json",
                HttpStatus.SC_OK);
        RequestDecorator decorator = new RequestDecorator(null);

        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClientMock, decorator, "qwerty", metadata());
        Map<String, List<String>> additionalHeaders = Collections.singletonMap("AdditionalHeader",
                Collections.singletonList("add"));

        SplitHttpResponse splitHttpResponse = splitHtpClient.get(rootTarget,
                new FetchOptions.Builder().cacheControlHeaders(true).build(), additionalHeaders);
        SplitChange change = Json.fromJson(splitHttpResponse.body(), SplitChange.class);

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClientMock).execute(captor.capture());
        HttpUriRequest request = captor.getValue();
        assertThat(request.getFirstHeader("AdditionalHeader").getValue(), is(equalTo("add")));

        Header[] headers = splitHttpResponse.responseHeaders();
        assertThat(headers[0].getName(), is(equalTo("Via")));
        assertThat(headers[0].getValue(), is(equalTo("HTTP/1.1 m_proxy_rio1")));
        Assert.assertNotNull(change);
        Assert.assertEquals(1, change.splits.size());
        Assert.assertNotNull(change.splits.get(0));

        Split split = change.splits.get(0);
        Map<String, String> configs = split.configurations;
        Assert.assertEquals(2, configs.size());
        Assert.assertEquals("{\"test\": \"blue\",\"grüne Straße\": 13}", configs.get("on"));
        Assert.assertEquals("{\"test\": \"blue\",\"size\": 15}", configs.get("off"));
        Assert.assertEquals(2, split.sets.size());
    }

    @Test
    public void testGetError() throws URISyntaxException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, IOException {
        URI rootTarget = URI.create("https://api.split.io/splitChanges?since=1234567");
        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("split-change-special-characters.json",
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
        RequestDecorator decorator = new RequestDecorator(null);

        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClientMock, decorator, "qwerty", metadata());
        SplitHttpResponse splitHttpResponse = splitHtpClient.get(rootTarget,
                new FetchOptions.Builder().cacheControlHeaders(true).build(), null);
        Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, (long) splitHttpResponse.statusCode());
    }

    @Test(expected = IllegalStateException.class)
    public void testException() throws URISyntaxException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, IOException {
        URI rootTarget = URI.create("https://api.split.io/splitChanges?since=1234567");
        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("split-change-special-characters.json",
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
        RequestDecorator decorator = null;

        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClientMock, decorator, "qwerty", metadata());
        splitHtpClient.get(rootTarget,
                new FetchOptions.Builder().cacheControlHeaders(true).build(), null);
    }

    @Test
    public void testPost() throws URISyntaxException, IOException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split/api/testImpressions/bulk");

        // Setup response mock
        CloseableHttpClient httpClient = TestHelper.mockHttpClient("", HttpStatus.SC_OK);
        RequestDecorator decorator = new RequestDecorator(null);

        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, decorator, "qwerty", metadata());

        // Send impressions
        List<TestImpressions> toSend = Arrays.asList(new TestImpressions("t1", Arrays.asList(
                KeyImpression.fromImpression(new Impression("k1", null, "t1", "on", 123L, "r1", 456L, null)),
                KeyImpression.fromImpression(new Impression("k2", null, "t1", "on", 123L, "r1", 456L, null)),
                KeyImpression.fromImpression(new Impression("k3", null, "t1", "on", 123L, "r1", 456L, null)))),
                new TestImpressions("t2", Arrays.asList(
                        KeyImpression.fromImpression(new Impression("k1", null, "t2", "on", 123L, "r1", 456L, null)),
                        KeyImpression.fromImpression(new Impression("k2", null, "t2", "on", 123L, "r1", 456L, null)),
                        KeyImpression.fromImpression(new Impression("k3", null, "t2", "on", 123L, "r1", 456L, null)))));

        Map<String, List<String>> additionalHeaders = Collections.singletonMap("SplitSDKImpressionsMode",
                Collections.singletonList("OPTIMIZED"));
        SplitHttpResponse splitHttpResponse = splitHtpClient.post(rootTarget, Utils.toJsonEntity(toSend),
                additionalHeaders);

        // Capture outgoing request and validate it
        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(captor.capture());
        HttpUriRequest request = captor.getValue();
        assertThat(request.getUri(),
                is(equalTo(URI.create("https://kubernetesturl.com/split/api/testImpressions/bulk"))));
        assertThat(request.getFirstHeader("SplitSDKImpressionsMode").getValue(), is(equalTo("OPTIMIZED")));
        assertThat(request, instanceOf(HttpPost.class));
        HttpPost asPostRequest = (HttpPost) request;
        InputStreamReader reader = new InputStreamReader(asPostRequest.getEntity().getContent());
        Gson gson = new Gson();
        List<TestImpressions> payload = gson.fromJson(reader, new TypeToken<List<TestImpressions>>() {
        }.getType());
        assertThat(payload.size(), is(equalTo(2)));
        Assert.assertEquals(200, (long) splitHttpResponse.statusCode());
    }

    @Test
    public void testPosttNoExceptionOnHttpErrorCode() throws URISyntaxException, InvocationTargetException,
            NoSuchMethodException, IllegalAccessException, IOException {
        URI rootTarget = URI.create("https://api.split.io/splitChanges?since=1234567");
        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("split-change-special-characters.json",
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
        RequestDecorator decorator = new RequestDecorator(null);

        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClientMock, decorator, "qwerty", metadata());
        SplitHttpResponse splitHttpResponse = splitHtpClient.post(rootTarget,
                Utils.toJsonEntity(Arrays.asList(new String[] { "A", "B", "C", "D" })), null);
        Assert.assertEquals(500, (long) splitHttpResponse.statusCode());

    }

    @Test(expected = IOException.class)
    public void testPosttException() throws URISyntaxException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, IOException {
        URI rootTarget = URI.create("https://api.split.io/splitChanges?since=1234567");
        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("split-change-special-characters.json",
                HttpStatus.SC_INTERNAL_SERVER_ERROR);

        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClientMock, null, "qwerty", metadata());
        splitHtpClient.post(rootTarget, Utils.toJsonEntity(Arrays.asList(new String[] { "A", "B", "C", "D" })), null);
    }

    private SDKMetadata metadata() {
        return new SDKMetadata("java-1.2.3", "1.2.3.4", "someIP");
    }

}
