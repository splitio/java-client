package io.split.client.events;

import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.net.URISyntaxException;

public class EventsTaskTest {
    private static final TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);

    @Test
    public void testDefaultURL() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        EventsStorage eventsStorage = Mockito.mock(EventsStorage.class);
        EventsTask fetcher = EventsTask.create(httpClient, rootTarget, 5, TELEMETRY_STORAGE, eventsStorage);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://api.split.io/api/events/bulk")));
    }

    @Test
    public void testCustomURLNoPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        EventsStorage eventsStorage = Mockito.mock(EventsStorage.class);
        EventsTask fetcher = EventsTask.create(httpClient, rootTarget, 5, TELEMETRY_STORAGE, eventsStorage);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/api/events/bulk")));
    }

    @Test
    public void testCustomURLAppendingPath() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split/");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        EventsStorage eventsStorage = Mockito.mock(EventsStorage.class);
        EventsTask fetcher = EventsTask.create(httpClient, rootTarget, 5, TELEMETRY_STORAGE, eventsStorage);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/events/bulk")));
    }

    @Test
    public void testCustomURLAppendingPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        EventsStorage eventsStorage = Mockito.mock(EventsStorage.class);
        EventsTask fetcher = EventsTask.create(httpClient, rootTarget, 5, TELEMETRY_STORAGE, eventsStorage);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/events/bulk")));
    }
}
