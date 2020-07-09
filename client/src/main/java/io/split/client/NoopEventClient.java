package io.split.client;

import io.split.client.dtos.Event;

public class NoopEventClient implements EventClient {

    @Override
    public boolean track(Event event, int eventSize) {
        return true;
    }

    @Override
    public void startPeriodicDataRecording() { return; }

    @Override
    public void close() {
        // Nothing to close
    }

    public static EventClient create() {
        return new NoopEventClient();
    }
}
