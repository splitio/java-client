package io.split.client.events;

public interface EventsStorageConsumer {
    WrappedEvent pop();
}
