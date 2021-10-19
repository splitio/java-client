package io.split.client.events;


import io.split.client.dtos.Event;

public class NoopEventsStorageImp implements EventsStorage {

    @Override
    public boolean track(Event event, int eventSize) {
        return true;
    }

    public static NoopEventsStorageImp create() {
        return new NoopEventsStorageImp();
    }

    @Override
    public WrappedEvent pop() {
        return new WrappedEvent(new Event(), 0l);
    }
}
