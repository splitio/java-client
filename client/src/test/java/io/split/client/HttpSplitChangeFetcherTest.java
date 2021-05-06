package io.split.client;

import io.split.TestHelper;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.engine.metrics.Metrics;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class HttpSplitChangeFetcherTest {
    private static final TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);
    @Test
    public void testDefaultURL() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(httpClient, rootTarget, TELEMETRY_STORAGE);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://api.split.io/api/splitChanges")));
    }

    @Test
    public void testCustomURLNoPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(httpClient, rootTarget, TELEMETRY_STORAGE);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/splitChanges")));
    }

    @Test
    public void testCustomURLAppendingPath() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split/");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(httpClient, rootTarget, TELEMETRY_STORAGE);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/splitChanges")));
    }

    @Test
    public void testCustomURLAppendingPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(httpClient, rootTarget, TELEMETRY_STORAGE);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/splitChanges")));
    }

    @Test
    public void testFetcherWithSpecialCharacters() throws URISyntaxException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        URI rootTarget = URI.create("https://api.split.io");

        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("split-change-special-characters.json", HttpStatus.SC_OK);

        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(httpClientMock, rootTarget, TELEMETRY_STORAGE);

        SplitChange change = fetcher.fetch(1234567, true);

        Assert.assertNotNull(change);
        Assert.assertEquals(1, change.splits.size());
        Assert.assertNotNull(change.splits.get(0));

        Split split = change.splits.get(0);
        Map<String, String> configs = split.configurations;
        Assert.assertEquals(2, configs.size());
        Assert.assertEquals("{\"test\": \"blue\",\"grüne Straße\": 13}", configs.get("on"));
        Assert.assertEquals("{\"test\": \"blue\",\"size\": 15}", configs.get("off"));
    }
}
