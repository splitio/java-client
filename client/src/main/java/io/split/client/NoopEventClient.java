package io.split.client;

import io.split.client.dtos.Event;

public class NoopEventClient implements EventClient {

    @Override
    public boolean track(Event event) {
        return false;
    }

    @Override
    public void close() {
        // Nothing to close
    }

    public static EventClient create() {
        return new NoopEventClient();
    }
}
