package io.split.client.events;

import io.split.client.dtos.Event;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class EventsTaskTest {
    private static final EventsStorage EVENTS_STORAGE = Mockito.mock(EventsStorage.class);
    private static final EventsSender EVENTS_SENDER = Mockito.mock(EventsSender.class);

    @Test
    public void testDefaultURL() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        EventsTask fetcher = EventsTask.create(rootTarget, 5, EVENTS_STORAGE, EVENTS_SENDER);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://api.split.io/api/events/bulk")));
    }

    @Test
    public void testCustomURLNoPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com");
        EventsTask fetcher = EventsTask.create(rootTarget, 5, EVENTS_STORAGE, EVENTS_SENDER);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/api/events/bulk")));
    }

    @Test
    public void testCustomURLAppendingPath() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split/");
        EventsTask fetcher = EventsTask.create(rootTarget, 5, EVENTS_STORAGE, EVENTS_SENDER);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/events/bulk")));
    }

    @Test
    public void testCustomURLAppendingPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        EventsTask fetcher = EventsTask.create(rootTarget, 5, EVENTS_STORAGE, EVENTS_SENDER);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/events/bulk")));
    }

    @Test
    public void testEventsAreSending() throws URISyntaxException, InterruptedException, IOException {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        EventsStorage eventsStorage = new InMemoryEventsStorage(10000, telemetryRuntimeProducer);
        EventsSender eventsSender = Mockito.mock(EventsSender.class);
        EventsTask eventClient = new EventsTask(eventsStorage,
                URI.create("https://kubernetesturl.com/split"),
                2000,
                eventsSender);

        for (int i = 0; i < 159; ++i) {
            Event event = new Event();
            eventsStorage.track(event, 1024 * 32);
        }

        Thread.sleep(1000);

        Event event = new Event();
        eventsStorage.track(event, 1024 * 32);
        Thread.sleep(2000);
        Mockito.verify(eventsSender, Mockito.times(1)).sendEvents(Mockito.anyObject());
    }

    @Test
    public void testEventsWhenCloseTask() throws URISyntaxException, InterruptedException, IOException {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        EventsSender eventsSender = Mockito.mock(EventsSender.class);
        EventsStorage eventsStorage = new InMemoryEventsStorage(10000, telemetryRuntimeProducer);
        EventsTask eventClient = new EventsTask(eventsStorage,
                URI.create("https://kubernetesturl.com/split"),
                2000,
                eventsSender);

        for (int i = 0; i < 159; ++i) {
            Event event = new Event();
            eventsStorage.track(event, 1024 * 32);
        }

        eventClient.close();
        Thread.sleep(2000);
        Mockito.verify(eventsSender, Mockito.times(1)).sendEvents(Mockito.anyObject());
    }

    @Test
    public void testCheckQueFull() throws URISyntaxException, InterruptedException, IOException {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        EventsStorage eventsStorage = new InMemoryEventsStorage(10, telemetryRuntimeProducer);
        EventsTask eventClient = new EventsTask(eventsStorage,
                URI.create("https://kubernetesturl.com/split"),
                2000,
                EVENTS_SENDER);

        for (int i = 0; i < 10; ++i) {
            Event event = new Event();
            eventsStorage.track(event, 1024 * 32);
        }
        Assert.assertTrue(eventsStorage.isFull());
    }

    @Test
    public void testTimesSendingEvents() throws URISyntaxException, InterruptedException, IOException {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        EventsSender eventsSender = Mockito.mock(EventsSender.class);
        EventsStorage eventsStorage = new InMemoryEventsStorage(100, telemetryRuntimeProducer);
        EventsTask eventClient = new EventsTask(eventsStorage,
                URI.create("https://kubernetesturl.com/split"),
                2000,
                eventsSender);

        for (int i = 0; i < 10; ++i) {
            Event event = new Event();
            eventsStorage.track(event, 1024 * 32);
        }

        Thread.sleep(3000);
        Mockito.verify(eventsSender, Mockito.times(1)).sendEvents(Mockito.anyObject());

        for (int i = 0; i < 10; ++i) {
            Event event = new Event();
            eventsStorage.track(event, 1024 * 32);
        }

        Thread.sleep(3000);
        Mockito.verify(eventsSender, Mockito.times(2)).sendEvents(Mockito.anyObject());
        eventClient.close();
        Thread.sleep(1000);
        Mockito.verify(eventsSender, Mockito.times(2)).sendEvents(Mockito.anyObject());
    }
}