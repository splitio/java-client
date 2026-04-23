package io.split.client.events;

import io.split.client.dtos.Event;

public class WrappedEvent {
    private final Event _event;
    private final long _size;

    public WrappedEvent(Event event, long size) {
        _event = event;
        _size = size;
    }

    public Event event() {
        return _event;
    }

    public long size() {
        return _size;
    }
}
