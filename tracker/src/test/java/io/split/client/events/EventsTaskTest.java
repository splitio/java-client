package io.split.client.events;

import io.split.client.dtos.Event;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EventsTaskTest {

    private EventsStorageConsumer _storage;
    private EventSender _sender;
    private EventsTask _task;

    @Before
    public void setup() {
        _storage = mock(EventsStorageConsumer.class);
        _sender = mock(EventSender.class);
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "test-events-thread");
            t.setDaemon(true);
            return t;
        };
        _task = new EventsTask(_storage, 30_000L, _sender, tf);
    }

    @Test
    public void sendEventsDoesNothingWhenQueueEmpty() {
        when(_storage.popAll()).thenReturn(Collections.<WrappedEvent>emptyList());
        _task.sendEvents();
        verifyZeroInteractions(_sender);
    }

    @Test
    public void sendEventsBatchesAllQueuedEvents() {
        Event e1 = makeEvent("click");
        Event e2 = makeEvent("purchase");
        when(_storage.popAll()).thenReturn(Arrays.asList(
            new WrappedEvent(e1, 10), new WrappedEvent(e2, 20)));

        _task.sendEvents();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(_sender).send(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains(e1));
        assertTrue(captor.getValue().contains(e2));
    }

    @Test
    public void closeFlushesRemainingEventsBeforeShutdown() {
        Event e = makeEvent("close-event");
        when(_storage.popAll()).thenReturn(
            Collections.singletonList(new WrappedEvent(e, 10)));

        _task.close();

        verify(_sender, atLeastOnce()).send(anyList());
    }

    @Test
    public void sendEventsDoesNotThrowWhenQueueIsFull() {
        when(_storage.isFull()).thenReturn(true);
        when(_storage.popAll()).thenReturn(Collections.<WrappedEvent>emptyList());
        _task.sendEvents();
    }

    private static Event makeEvent(String type) {
        Event e = new Event();
        e.eventTypeId = type;
        e.key = "k";
        e.trafficTypeName = "user";
        e.timestamp = System.currentTimeMillis();
        return e;
    }
}
