package io.split.client.events;

import io.split.client.dtos.Event;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InMemoryEventsStorageTest {

    private EventQueueStats _stats;
    private InMemoryEventsStorage _storage;

    @Before
    public void setup() {
        _stats = Mockito.mock(EventQueueStats.class);
        _storage = new InMemoryEventsStorage(5, _stats);
    }

    @Test
    public void trackReturnsTrue_andNotifiesQueued() {
        assertTrue(_storage.track(makeEvent("myType"), 100));
        verify(_stats).onQueued(1);
        verifyNoMoreInteractions(_stats);
    }

    @Test
    public void trackNullEventReturnsFalse() {
        assertFalse(_storage.track(null, 0));
        verifyNoInteractions(_stats);
    }

    @Test
    public void isFullWhenCapacityReached() {
        for (int i = 0; i < 5; i++) _storage.track(makeEvent("t" + i), 10);
        assertTrue(_storage.isFull());
    }

    @Test
    public void dropWhenFullNotifiesDropped() {
        for (int i = 0; i < 5; i++) _storage.track(makeEvent("t" + i), 10);
        assertFalse(_storage.track(makeEvent("overflow"), 10));
        verify(_stats).onDropped(1);
    }

    @Test
    public void popAllDrainsQueue() {
        _storage.track(makeEvent("a"), 10);
        _storage.track(makeEvent("b"), 20);
        List<WrappedEvent> popped = _storage.popAll();
        assertEquals(2, popped.size());
        assertEquals("a", popped.get(0).event().eventTypeId);
        assertEquals("b", popped.get(1).event().eventTypeId);
        assertTrue(_storage.popAll().isEmpty());
    }

    @Test
    public void popAllReturnsEmptyWhenQueueIsEmpty() {
        assertTrue(_storage.popAll().isEmpty());
    }

    private static Event makeEvent(String eventTypeId) {
        Event e = new Event();
        e.eventTypeId = eventTypeId;
        e.key = "key";
        e.trafficTypeName = "user";
        e.timestamp = System.currentTimeMillis();
        return e;
    }
}
