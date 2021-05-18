package io.split.client;

import io.split.TestHelper;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.engine.common.FetchOptions;
import io.split.engine.metrics.Metrics;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

public class HttpSplitChangeFetcherTest {
    @Test
    public void testDefaultURL() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(httpClient, rootTarget, metrics);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://api.split.io/api/splitChanges")));
    }

    @Test
    public void testCustomURLNoPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(httpClient, rootTarget, metrics);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/splitChanges")));
    }

    @Test
    public void testCustomURLAppendingPath() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split/");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(httpClient, rootTarget, metrics);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/splitChanges")));
    }

    @Test
    public void testCustomURLAppendingPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(httpClient, rootTarget, metrics);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/splitChanges")));
    }

    @Test
    public void testFetcherWithSpecialCharacters() throws URISyntaxException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        URI rootTarget = URI.create("https://api.split.io");

        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("split-change-special-characters.json", HttpStatus.SC_OK);

        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(httpClientMock, rootTarget, metrics);

        SplitChange change = fetcher.fetch(1234567, new FetchOptions.Builder().cacheControlHeaders(true).build());

        Assert.assertNotNull(change);
        Assert.assertEquals(1, change.splits.size());
        Assert.assertNotNull(change.splits.get(0));

        Split split = change.splits.get(0);
        Map<String, String> configs = split.configurations;
        Assert.assertEquals(2, configs.size());
        Assert.assertEquals("{\"test\": \"blue\",\"grüne Straße\": 13}", configs.get("on"));
        Assert.assertEquals("{\"test\": \"blue\",\"size\": 15}", configs.get("off"));
    }

    @Test
    public void testFetcherWithCDNBypassOption() throws IOException, URISyntaxException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        URI rootTarget = URI.create("https://api.split.io");

        HttpEntity entityMock = Mockito.mock(HttpEntity.class);
        when(entityMock.getContent()).thenReturn(new StringBufferInputStream("{\"till\": 1}"));
        ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
        when(response.getCode()).thenReturn(200);
        when(response.getEntity()).thenReturn(entityMock);
        when(response.getHeaders()).thenReturn(new Header[0]);

        ArgumentCaptor<ClassicHttpRequest> requestCaptor = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        when(httpClientMock.execute(requestCaptor.capture())).thenReturn(TestHelper.classicResponseToCloseableMock(response));

        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(httpClientMock, rootTarget, metrics);

        fetcher.fetch(-1, new FetchOptions.Builder().cdnBypass(true).build());
        fetcher.fetch(-1, new FetchOptions.Builder().build());
        List<ClassicHttpRequest> captured = requestCaptor.getAllValues();
        Assert.assertEquals(captured.size(), 2);
        Assert.assertTrue(captured.get(0).getUri().toString().contains("till="));
        Assert.assertFalse(captured.get(1).getUri().toString().contains("till="));
    }

    @Test
    public void testRandomNumberGeneration() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(httpClientMock, rootTarget, metrics);

        long min = (long)Math.pow(2, 63) * (-1);
        for (long x = 0; x < 100000000; x++) {
            long r = fetcher.makeRandomTill();
            Assert.assertTrue(r < 0 && r > min);
        }
    }
}
