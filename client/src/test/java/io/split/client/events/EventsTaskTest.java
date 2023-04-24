package io.split.client.events;

import io.split.client.SplitClientConfig;
import io.split.client.dtos.Event;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class EventsTaskTest {
    private static final EventsSender EVENTS_SENDER = Mockito.mock(EventsSender.class);
    private final SplitClientConfig _config = Mockito.mock(SplitClientConfig.class);

    @Test
    public void testEventsAreSending() throws InterruptedException {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        EventsStorage eventsStorage = new InMemoryEventsStorage(10000, telemetryRuntimeProducer);
        EventsSender eventsSender = Mockito.mock(EventsSender.class);
        EventsTask eventClient = new EventsTask(eventsStorage,
                2000,
                eventsSender,
                _config);
        eventClient.start();

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
    public void testEventsWhenCloseTask() throws InterruptedException {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        EventsSender eventsSender = Mockito.mock(EventsSender.class);
        EventsStorage eventsStorage = new InMemoryEventsStorage(10000, telemetryRuntimeProducer);
        EventsTask eventClient = new EventsTask(eventsStorage,
                2000,
                eventsSender,
                _config);

        for (int i = 0; i < 159; ++i) {
            Event event = new Event();
            eventsStorage.track(event, 1024 * 32);
        }

        eventClient.close();
        Thread.sleep(2000);
        Mockito.verify(eventsSender, Mockito.times(1)).sendEvents(Mockito.anyObject());
    }

    @Test
    public void testCheckQueFull() {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        EventsStorage eventsStorage = new InMemoryEventsStorage(10, telemetryRuntimeProducer);
        EventsTask eventClient = new EventsTask(eventsStorage,
                2000,
                EVENTS_SENDER,
                _config);

        for (int i = 0; i < 10; ++i) {
            Event event = new Event();
            eventsStorage.track(event, 1024 * 32);
        }
        Assert.assertTrue(eventsStorage.isFull());
    }

    @Test
    public void testTimesSendingEvents() throws InterruptedException {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        EventsSender eventsSender = Mockito.mock(EventsSender.class);
        EventsStorage eventsStorage = new InMemoryEventsStorage(100, telemetryRuntimeProducer);
        EventsTask eventClient = new EventsTask(eventsStorage,
                2000,
                eventsSender,
                _config);
        eventClient.start();

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