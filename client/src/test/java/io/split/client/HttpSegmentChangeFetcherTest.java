package io.split.client;

import io.split.client.dtos.SegmentChange;
import io.split.engine.metrics.Metrics;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class HttpSegmentChangeFetcherTest {

    @Test
    public void testDefaultURL() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSegmentChangeFetcher fetcher = HttpSegmentChangeFetcher.create(httpClient, rootTarget, metrics);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://api.split.io/api/segmentChanges")));
    }

    @Test
    public void testCustomURLNoPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSegmentChangeFetcher fetcher = HttpSegmentChangeFetcher.create(httpClient, rootTarget, metrics);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/segmentChanges")));
    }

    @Test
    public void testCustomURLAppendingPath() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split/");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSegmentChangeFetcher fetcher = HttpSegmentChangeFetcher.create(httpClient, rootTarget, metrics);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/segmentChanges")));
    }

    @Test
    public void testCustomURLAppendingPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSegmentChangeFetcher fetcher = HttpSegmentChangeFetcher.create(httpClient, rootTarget, metrics);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/segmentChanges")));
    }

    @Test
    public void testFetcherWithSpecialCharacters() throws URISyntaxException, IOException {
        URI rootTarget = URI.create("https://api.split.io/api/segmentChanges");

        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponseMock = Mockito.mock(CloseableHttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
        StatusLine statusLineMock = Mockito.mock(StatusLine.class);
        HttpEntity entityMock = Mockito.mock(HttpEntity.class);

        Mockito.when(statusLineMock.getStatusCode()).thenReturn(200);
        Mockito.when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
        Mockito.when(entityMock.getContent()).thenReturn(getClass().getClassLoader().getResourceAsStream("segment-change-special-chatacters.json"));
        Mockito.when(httpResponseMock.getEntity()).thenReturn(entityMock);

        Mockito.when(httpClientMock.execute((HttpUriRequest) Mockito.anyObject())).thenReturn(httpResponseMock);

        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSegmentChangeFetcher fetcher = HttpSegmentChangeFetcher.create(httpClientMock, rootTarget, metrics);

        SegmentChange change = fetcher.fetch("some_segment", 1234567);

        Assert.assertNotNull(change);
        Assert.assertEquals(1, change.added.size());
        Assert.assertEquals("grüne_Straße", change.added.get(0));
        Assert.assertEquals(1, change.removed.size());
        Assert.assertEquals("other_user", change.removed.get(0));
    }
}
