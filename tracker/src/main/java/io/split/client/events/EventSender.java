package io.split.client.events;

import io.split.client.dtos.Event;
import java.util.List;

public interface EventSender {
    void send(List<Event> events);
}
