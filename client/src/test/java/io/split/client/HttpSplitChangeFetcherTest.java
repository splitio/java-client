package io.split.client;

import io.split.TestHelper;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.engine.common.FetchOptions;
import io.split.engine.metrics.Metrics;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.telemetry.storage.TelemetryStorage;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.when;

public class HttpSplitChangeFetcherTest {
    private static final TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);
    @Test
    public void testDefaultURL() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null));

        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget, TELEMETRY_STORAGE);
        Assert.assertEquals("https://api.split.io/api/splitChanges", fetcher.getTarget().toString());
    }

    @Test
    public void testCustomURLNoPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null));

        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget, TELEMETRY_STORAGE);
        Assert.assertEquals("https://kubernetesturl.com/split/api/splitChanges", fetcher.getTarget().toString());
    }

    @Test
    public void testCustomURLAppendingPath() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split/");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null));
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget, TELEMETRY_STORAGE);
        Assert.assertEquals("https://kubernetesturl.com/split/api/splitChanges", fetcher.getTarget().toString());
    }

    @Test
    public void testCustomURLAppendingPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null));
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget, TELEMETRY_STORAGE);
        Assert.assertEquals("https://kubernetesturl.com/split/api/splitChanges", fetcher.getTarget().toString());
    }

    @Test
    public void testFetcherWithSpecialCharacters() throws URISyntaxException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        URI rootTarget = URI.create("https://api.split.io");

        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("split-change-special-characters.json", HttpStatus.SC_OK);
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClientMock, new RequestDecorator(null));

        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget, TELEMETRY_STORAGE);

        SplitChange change = fetcher.fetch(1234567, new FetchOptions.Builder().cacheControlHeaders(true).build());

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
    public void testFetcherWithCDNBypassOption() throws IOException, URISyntaxException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        URI rootTarget = URI.create("https://api.split.io");

        HttpEntity entityMock = Mockito.mock(HttpEntity.class);
        when(entityMock.getContent()).thenReturn(new ByteArrayInputStream("{\"till\": 1}".getBytes(StandardCharsets.UTF_8)));
        ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
        when(response.getCode()).thenReturn(200);
        when(response.getEntity()).thenReturn(entityMock);
        when(response.getHeaders()).thenReturn(new Header[0]);

        ArgumentCaptor<ClassicHttpRequest> requestCaptor = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        when(httpClientMock.execute(requestCaptor.capture())).thenReturn(TestHelper.classicResponseToCloseableMock(response));
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClientMock, new RequestDecorator(null));

        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget, Mockito.mock(TelemetryRuntimeProducer.class));

        fetcher.fetch(-1, new FetchOptions.Builder().targetChangeNumber(123).build());
        fetcher.fetch(-1, new FetchOptions.Builder().build());
        List<ClassicHttpRequest> captured = requestCaptor.getAllValues();
        Assert.assertEquals(captured.size(), 2);
        Assert.assertTrue(captured.get(0).getUri().toString().contains("till=123"));
        Assert.assertFalse(captured.get(1).getUri().toString().contains("till="));
    }

    @Test
    public void testRandomNumberGeneration() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClientMock, new RequestDecorator(null));
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget, Mockito.mock(TelemetryRuntimeProducer.class));

        Set<Long> seen = new HashSet<>();
        long min = (long)Math.pow(2, 63) * (-1);
        final long total = 10000000;
        for (long x = 0; x < total; x++) {
            long r = fetcher.makeRandomTill();
            Assert.assertTrue(r < 0 && r > min);
            seen.add(r);
        }

        Assert.assertTrue(seen.size() >= (total * 0.9999));
    }
}