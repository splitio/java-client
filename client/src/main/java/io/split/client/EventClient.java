package io.split.client;

import io.split.client.dtos.Event;

public interface EventClient {

    boolean track(Event event);

    void close();
}
