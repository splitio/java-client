package io.split.client.metrics;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class HttpMetricsTest {
    @Test
    public void testDefaultURL() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        HttpMetrics fetcher = HttpMetrics.create(httpClient, rootTarget);
        Assert.assertThat(fetcher.getTimeTarget().toString(), Matchers.is(Matchers.equalTo("https://api.split.io/api/metrics/time")));
        Assert.assertThat(fetcher.getCounterTarget().toString(), Matchers.is(Matchers.equalTo("https://api.split.io/api/metrics/counter")));
    }

    @Test
    public void testCustomURLNoPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        HttpMetrics fetcher = HttpMetrics.create(httpClient, rootTarget);
        Assert.assertThat(fetcher.getTimeTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/api/metrics/time")));
        Assert.assertThat(fetcher.getCounterTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/api/metrics/counter")));
    }

    @Test
    public void testCustomURLAppendingPath() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split/");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        HttpMetrics fetcher = HttpMetrics.create(httpClient, rootTarget);
        Assert.assertThat(fetcher.getTimeTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/metrics/time")));
        Assert.assertThat(fetcher.getCounterTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/metrics/counter")));
    }

    @Test
    public void testCustomURLAppendingPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        HttpMetrics fetcher = HttpMetrics.create(httpClient, rootTarget);
        Assert.assertThat(fetcher.getTimeTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/metrics/time")));
        Assert.assertThat(fetcher.getCounterTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/metrics/counter")));
    }
}
