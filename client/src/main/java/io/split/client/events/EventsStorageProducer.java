package io.split.client.events;

import io.split.client.dtos.Event;

public interface EventsStorageProducer {
    boolean track(Event event, int eventSize);
}
