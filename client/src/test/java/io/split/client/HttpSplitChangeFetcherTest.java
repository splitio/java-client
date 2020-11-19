package io.split.client;

import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.engine.metrics.Metrics;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

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

    // TODO: Fix this test by mocking .getCode() instead of the status line of the request
    /*
    @Test
    public void testFetcherWithSpecialCharacters() throws URISyntaxException, IOException {
        URI rootTarget = URI.create("https://api.split.io");

        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponseMock = Mockito.mock(CloseableHttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
        StatusLine statusLineMock = Mockito.mock(StatusLine.class);
        HttpEntity entityMock = Mockito.mock(HttpEntity.class);

        Mockito.when(statusLineMock.getStatusCode()).thenReturn(200);
        Mockito.when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
        Mockito.when(entityMock.getContent()).thenReturn(getClass().getClassLoader().getResourceAsStream("split-change-special-characters.json"));
        Mockito.when(httpResponseMock.getEntity()).thenReturn(entityMock);

        Mockito.when(httpClientMock.execute((HttpUriRequest) Mockito.anyObject())).thenReturn(httpResponseMock);

        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(httpClientMock, rootTarget, metrics);

        SplitChange change = fetcher.fetch(1234567);

        Assert.assertNotNull(change);
        Assert.assertEquals(1, change.splits.size());
        Assert.assertNotNull(change.splits.get(0));

        Split split = change.splits.get(0);
        Map<String, String> configs = split.configurations;
        Assert.assertEquals(2, configs.size());
        Assert.assertEquals("{\"test\": \"blue\",\"grüne Straße\": 13}", configs.get("on"));
        Assert.assertEquals("{\"test\": \"blue\",\"size\": 15}", configs.get("off"));
    }

     */
}
