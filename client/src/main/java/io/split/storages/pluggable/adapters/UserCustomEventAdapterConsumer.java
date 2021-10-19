package io.split.storages.pluggable.adapters;

import io.split.client.dtos.Event;
import io.split.client.events.EventsStorageConsumer;
import io.split.client.events.WrappedEvent;

public class UserCustomEventAdapterConsumer implements EventsStorageConsumer {
    @Override
    public WrappedEvent pop() {
        //No-Op
        return new WrappedEvent(new Event(), 0L);
    }
}
