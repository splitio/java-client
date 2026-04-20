package io.split.client.events;

import java.util.List;

public interface EventsStorageConsumer {
    WrappedEvent pop();
    List<WrappedEvent> popAll();
    boolean isFull();
}
