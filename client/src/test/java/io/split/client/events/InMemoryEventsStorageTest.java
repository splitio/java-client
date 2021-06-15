package io.split.client.events;

import io.split.client.dtos.Event;
import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.BlockingQueue;

public class InMemoryEventsStorageTest{

    @Test
    public void testDropEvent() {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        EventsStorage eventsStorage = new InMemoryEventsStorage(2, telemetryRuntimeProducer);

        for (int i = 0; i < 3; ++i) {
            Event event = new Event();
            eventsStorage.track(event, 1);
        }

        Mockito.verify(telemetryRuntimeProducer, Mockito.times(2)).recordEventStats(EventsDataRecordsEnum.EVENTS_QUEUED, 1);
        Mockito.verify(telemetryRuntimeProducer, Mockito.times(1)).recordEventStats(EventsDataRecordsEnum.EVENTS_DROPPED, 1);
    }

    @Test
    public void testTrackAndPop() {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        InMemoryEventsStorage eventsStorage = new InMemoryEventsStorage(10, telemetryRuntimeProducer);

        for (int i = 0; i < 5; ++i) {
            Event event = new Event();
            eventsStorage.track(event, 1);
        }

        Assert.assertEquals(5, eventsStorage.queueSize());
        Assert.assertNotNull(eventsStorage.pop());
    }

    @Test
    public void testPopFailed() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        BlockingQueue blockingQueue = Mockito.mock(BlockingQueue.class);
        EventsStorage eventsStorage = new InMemoryEventsStorage(2, telemetryRuntimeProducer);
        Field eventsQueue = InMemoryEventsStorage.class.getDeclaredField("_eventQueue");
        eventsQueue.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(eventsQueue, eventsQueue.getModifiers() & ~Modifier.FINAL);
        eventsQueue.set(eventsStorage, blockingQueue);
        Mockito.when(blockingQueue.take()).thenThrow(new InterruptedException());
        Assert.assertNull(eventsStorage.pop());
    }

    @Test
    public void testTrackException() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        BlockingQueue blockingQueue = Mockito.mock(BlockingQueue.class);
        EventsStorage eventsStorage = new InMemoryEventsStorage(2, telemetryRuntimeProducer);

        Field eventsQueue = InMemoryEventsStorage.class.getDeclaredField("_eventQueue");
        eventsQueue.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(eventsQueue, eventsQueue.getModifiers() & ~Modifier.FINAL);
        eventsQueue.set(eventsStorage, blockingQueue);
        Mockito.when(blockingQueue.offer(Mockito.anyObject())).thenThrow(new ClassCastException());


        Assert.assertEquals(false, eventsStorage.track(new Event(), 1));
        Mockito.verify(telemetryRuntimeProducer, Mockito.times(1)).recordEventStats(EventsDataRecordsEnum.EVENTS_DROPPED, 1);
    }

    @Test
    public void testEventNullThenFalse() {
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
        BlockingQueue blockingQueue = Mockito.mock(BlockingQueue.class);
        EventsStorage eventsStorage = new InMemoryEventsStorage(2, telemetryRuntimeProducer);
        Assert.assertEquals(false, eventsStorage.track(null, 1));
    }
}