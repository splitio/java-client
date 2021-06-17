package io.split.client.events;

import io.split.client.dtos.Event;
import io.split.client.events.EventsStorage;
import io.split.client.events.EventsTask;
import io.split.client.events.InMemoryEventsStorage;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
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

public class EventsClientImplTest {
    private static final TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);

    @Test
    public void testDefaultURL() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        EventsStorage eventsStorage = Mockito.mock(EventsStorage.class);
        EventsTask fetcher = EventsTask.create(httpClient, rootTarget, 5, 5, 5, TELEMETRY_STORAGE, eventsStorage);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://api.split.io/api/events/bulk")));
    }

    @Test
    public void testCustomURLNoPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        EventsStorage eventsStorage = Mockito.mock(EventsStorage.class);
        EventsTask fetcher = EventsTask.create(httpClient, rootTarget, 5, 5, 5, TELEMETRY_STORAGE, eventsStorage);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/api/events/bulk")));
    }

    @Test
    public void testCustomURLAppendingPath() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split/");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        EventsStorage eventsStorage = Mockito.mock(EventsStorage.class);
        EventsTask fetcher = EventsTask.create(httpClient, rootTarget, 5, 5, 5, TELEMETRY_STORAGE, eventsStorage);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/events/bulk")));
    }

    @Test
    public void testCustomURLAppendingPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        EventsStorage eventsStorage = Mockito.mock(EventsStorage.class);
        EventsTask fetcher = EventsTask.create(httpClient, rootTarget, 5, 5, 5, TELEMETRY_STORAGE, eventsStorage);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/events/bulk")));
    }

    @Test
    public void testEventsFlushedWhenSizeLimitReached() throws URISyntaxException, InterruptedException, IOException {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        EventsStorage eventsStorageConsumer = new InMemoryEventsStorage(10000, telemetryRuntimeProducer);
        EventsTask eventClient = new EventsTask(eventsStorageConsumer,
                client,
                URI.create("https://kubernetesturl.com/split"),
                10000, // Long queue so it doesn't flush by # of events
                100000, // Long period so it doesn't flush by timeout expiration.
                0, TELEMETRY_STORAGE);

        for (int i = 0; i < 159; ++i) {
            Event event = new Event();
            eventsStorageConsumer.track(event, 1024 * 32); // 159 32kb events should be about to flush
        }

        Thread.sleep(2000);
        Mockito.verifyZeroInteractions(client);

        Event event = new Event();
        eventsStorageConsumer.track(event, 1024 * 32); // 159 32kb events should be about to flush
        Thread.sleep(2000);
        Mockito.verify(client, Mockito.times(1)).execute((HttpUriRequest) Mockito.any());
    }
}
