package io.split.client;

import io.split.client.dtos.Event;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.LinkedBlockingQueue;

public class EventsClientImplTest {
    private static final TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);

    @Test
    public void testDefaultURL() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        EventClientImpl fetcher = EventClientImpl.create(httpClient, rootTarget, 5, 5, 5, TELEMETRY_STORAGE);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://api.split.io/api/events/bulk")));
    }

    @Test
    public void testCustomURLNoPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        EventClientImpl fetcher = EventClientImpl.create(httpClient, rootTarget, 5, 5, 5, TELEMETRY_STORAGE);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/api/events/bulk")));
    }

    @Test
    public void testCustomURLAppendingPath() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split/");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        EventClientImpl fetcher = EventClientImpl.create(httpClient, rootTarget, 5, 5, 5, TELEMETRY_STORAGE);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/events/bulk")));
    }

    @Test
    public void testCustomURLAppendingPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        EventClientImpl fetcher = EventClientImpl.create(httpClient, rootTarget, 5, 5, 5, TELEMETRY_STORAGE);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/events/bulk")));
    }

    @Test
    public void testEventsFlushedWhenSizeLimitReached() throws URISyntaxException, InterruptedException, IOException {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        EventClientImpl eventClient = new EventClientImpl(new LinkedBlockingQueue<EventClientImpl.WrappedEvent>(),
                client,
                URI.create("https://kubernetesturl.com/split"),
                10000, // Long queue so it doesn't flush by # of events
                100000, // Long period so it doesn't flush by timeout expiration.
                0, TELEMETRY_STORAGE);

        for (int i = 0; i < 159; ++i) {
            Event event = new Event();
            eventClient.track(event, 1024 * 32); // 159 32kb events should be about to flush
        }

        Thread.sleep(2000);
        Mockito.verifyZeroInteractions(client);

        Event event = new Event();
        eventClient.track(event, 1024 * 32); // 159 32kb events should be about to flush
        Thread.sleep(2000);
        Mockito.verify(client, Mockito.times(1)).execute((HttpUriRequest) Mockito.any());
    }
}
