package io.split.client;

import io.split.TestHelper;
import io.split.client.dtos.SegmentChange;
import io.split.engine.metrics.Metrics;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
    public void testFetcherWithSpecialCharacters() throws URISyntaxException, IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        URI rootTarget = URI.create("https://api.split.io/api/segmentChanges");

        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("segment-change-special-chatacters.json", HttpStatus.SC_OK);

        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSegmentChangeFetcher fetcher = HttpSegmentChangeFetcher.create(httpClientMock, rootTarget, metrics);

        SegmentChange change = fetcher.fetch("some_segment", 1234567, true);

        Assert.assertNotNull(change);
        Assert.assertEquals(1, change.added.size());
        Assert.assertEquals("grüne_Straße", change.added.get(0));
        Assert.assertEquals(1, change.removed.size());
        Assert.assertEquals("other_user", change.removed.get(0));
    }
}
