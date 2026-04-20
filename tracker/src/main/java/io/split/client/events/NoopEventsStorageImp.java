package io.split.client.events;


import io.split.client.dtos.Event;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<WrappedEvent> popAll() {
        //no-op
        return new ArrayList<>();
    }

    @Override
    public boolean isFull() {
        return false;
    }
}
