package io.split.client.impressions;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.split.TestHelper;
import io.split.client.RequestDecorator;
import io.split.client.dtos.ImpressionCount;
import io.split.client.dtos.KeyImpression;
import io.split.client.dtos.TestImpressions;
import io.split.client.utils.SDKMetadata;
import io.split.service.SplitHttpClient;
import io.split.service.SplitHttpClientImpl;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.verify;

public class HttpImpressionsSenderTest {
    private static final TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);

    @Test
    public void testDefaultURL() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null), "qwerty",
                metadata());
        HttpImpressionsSender fetcher = HttpImpressionsSender.create(splitHtpClient, rootTarget,
                ImpressionsManager.Mode.DEBUG, TELEMETRY_STORAGE);
        Assert.assertThat(fetcher.getTarget().toString(),
                Matchers.is(Matchers.equalTo("https://api.split.io/api/testImpressions/bulk")));
    }

    @Test
    public void testCustomURLNoPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null), "qwerty",
                metadata());
        HttpImpressionsSender fetcher = HttpImpressionsSender.create(splitHtpClient, rootTarget,
                ImpressionsManager.Mode.DEBUG, TELEMETRY_STORAGE);
        Assert.assertThat(fetcher.getTarget().toString(),
                Matchers.is(Matchers.equalTo("https://kubernetesturl.com/api/testImpressions/bulk")));
    }

    @Test
    public void testCustomURLAppendingPath() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split/");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null), "qwerty",
                metadata());
        HttpImpressionsSender fetcher = HttpImpressionsSender.create(splitHtpClient, rootTarget,
                ImpressionsManager.Mode.DEBUG, TELEMETRY_STORAGE);
        Assert.assertThat(fetcher.getTarget().toString(),
                Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/testImpressions/bulk")));
    }

    @Test
    public void testCustomURLAppendingPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null), "qwerty",
                metadata());
        HttpImpressionsSender fetcher = HttpImpressionsSender.create(splitHtpClient, rootTarget,
                ImpressionsManager.Mode.DEBUG, TELEMETRY_STORAGE);
        Assert.assertThat(fetcher.getTarget().toString(),
                Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/testImpressions/bulk")));
    }

    @Test
    public void testImpressionCountsEndpointOptimized() throws URISyntaxException, IOException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");

        // Setup response mock
        CloseableHttpClient httpClient = TestHelper.mockHttpClient("", HttpStatus.SC_OK);
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null), "qwerty",
                metadata());

        // Send counters
        HttpImpressionsSender sender = HttpImpressionsSender.create(splitHtpClient, rootTarget,
                ImpressionsManager.Mode.OPTIMIZED, TELEMETRY_STORAGE);
        HashMap<ImpressionCounter.Key, Integer> toSend = new HashMap<>();
        toSend.put(new ImpressionCounter.Key("test1", 0), 4);
        toSend.put(new ImpressionCounter.Key("test2", 0), 5);
        sender.postCounters(toSend);

        // Capture outgoing request and validate it
        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(captor.capture());
        HttpUriRequest request = captor.getValue();
        assertThat(request.getUri(),
                is(equalTo(URI.create("https://kubernetesturl.com/split/api/testImpressions/count"))));
        assertThat(request.getHeaders().length, is(5));
        assertThat(request, instanceOf(HttpPost.class));
        HttpPost asPostRequest = (HttpPost) request;
        InputStreamReader reader = new InputStreamReader(asPostRequest.getEntity().getContent());
        Gson gson = new Gson();
        ImpressionCount payload = gson.fromJson(reader, ImpressionCount.class);
        assertThat(payload.perFeature.size(), is(equalTo(2)));
        assertThat(payload.perFeature, contains(new ImpressionCount.CountPerFeature("test1", 0, 4),
                new ImpressionCount.CountPerFeature("test2", 0, 5)));
    }

    @Test
    public void testImpressionCountsEndpointDebug() throws URISyntaxException, IOException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");

        // Setup response mock
        CloseableHttpClient httpClient = TestHelper.mockHttpClient("", HttpStatus.SC_OK);
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null), "qwerty",
                metadata());

        // Send counters
        HttpImpressionsSender sender = HttpImpressionsSender.create(splitHtpClient, rootTarget,
                ImpressionsManager.Mode.DEBUG, TELEMETRY_STORAGE);
        HashMap<ImpressionCounter.Key, Integer> toSend = new HashMap<>();
        toSend.put(new ImpressionCounter.Key("test1", 0), 4);
        toSend.put(new ImpressionCounter.Key("test2", 0), 5);
        sender.postCounters(toSend);

        // Assert that the HTTP client was not called
        verify(httpClient, Mockito.never()).execute(Mockito.any());
    }

    @Test
    public void testImpressionBulksEndpoint() throws URISyntaxException, IOException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");

        // Setup response mock
        CloseableHttpClient httpClient = TestHelper.mockHttpClient("", HttpStatus.SC_OK);
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null), "qwerty",
                metadata());

        HttpImpressionsSender sender = HttpImpressionsSender.create(splitHtpClient, rootTarget,
                ImpressionsManager.Mode.OPTIMIZED, TELEMETRY_STORAGE);

        // Send impressions
        List<TestImpressions> toSend = Arrays.asList(new TestImpressions("t1", Arrays.asList(
                KeyImpression.fromImpression(new Impression("k1", null, "t1", "on", 123L, "r1", 456L, null)),
                KeyImpression.fromImpression(new Impression("k2", null, "t1", "on", 123L, "r1", 456L, null)),
                KeyImpression.fromImpression(new Impression("k3", null, "t1", "on", 123L, "r1", 456L, null)))),
                new TestImpressions("t2", Arrays.asList(
                        KeyImpression.fromImpression(new Impression("k1", null, "t2", "on", 123L, "r1", 456L, null)),
                        KeyImpression.fromImpression(new Impression("k2", null, "t2", "on", 123L, "r1", 456L, null)),
                        KeyImpression.fromImpression(new Impression("k3", null, "t2", "on", 123L, "r1", 456L, null)))));
        sender.postImpressionsBulk(toSend);

        // Capture outgoing request and validate it
        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(captor.capture());
        HttpUriRequest request = captor.getValue();
        assertThat(request.getUri(),
                is(equalTo(URI.create("https://kubernetesturl.com/split/api/testImpressions/bulk"))));
        assertThat(request.getHeaders().length, is(6));
        assertThat(request.getFirstHeader("SplitSDKImpressionsMode").getValue(), is(equalTo("OPTIMIZED")));
        assertThat(request, instanceOf(HttpPost.class));
        HttpPost asPostRequest = (HttpPost) request;
        InputStreamReader reader = new InputStreamReader(asPostRequest.getEntity().getContent());
        Gson gson = new Gson();
        List<TestImpressions> payload = gson.fromJson(reader, new TypeToken<List<TestImpressions>>() {
        }.getType());
        assertThat(payload.size(), is(equalTo(2)));

        // Do the same flow for imrpessionsMode = debug
        CloseableHttpClient httpClientDebugMode = TestHelper.mockHttpClient("", HttpStatus.SC_OK);
        SplitHttpClient splitHtpClient2 = SplitHttpClientImpl.create(httpClientDebugMode, new RequestDecorator(null),
                "qwerty", metadata());

        sender = HttpImpressionsSender.create(splitHtpClient2, rootTarget, ImpressionsManager.Mode.DEBUG,
                TELEMETRY_STORAGE);
        sender.postImpressionsBulk(toSend);
        captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClientDebugMode).execute(captor.capture());
        request = captor.getValue();
        assertThat(request.getHeaders().length, is(6));
        assertThat(request.getFirstHeader("SplitSDKImpressionsMode").getValue(), is(equalTo("DEBUG")));
    }

    @Test
    public void testHttpError() throws URISyntaxException, IOException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = TestHelper.mockHttpClient("", HttpStatus.SC_BAD_REQUEST);
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null), "qwerty",
                metadata());
        HttpImpressionsSender sender = HttpImpressionsSender.create(splitHtpClient, rootTarget,
                ImpressionsManager.Mode.OPTIMIZED, TELEMETRY_STORAGE);
        // Should not raise exception
        sender.postImpressionsBulk(new ArrayList<>());
        sender.postCounters(new HashMap<>());
    }

    private SDKMetadata metadata() {
        return new SDKMetadata("java-1.2.3", "1.2.3.4", "someIP");
    }

}
