package io.split.client.events;

import io.split.client.dtos.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for sending events added via .track() to Split collection services
 */
public class EventsTask{

    private final EventsStorageConsumer _eventsStorageConsumer;
    private final EventSender _eventsSender;
    private final long _sendIntervalMillis;

    private final ScheduledExecutorService _senderScheduledExecutorService;
    private static final Logger _log = LoggerFactory.getLogger(EventsTask.class);

    public static EventsTask create(long sendIntervalMillis, EventsStorageConsumer eventsStorageConsumer, EventSender eventsSender,
                                    ThreadFactory threadFactory) {
        return new EventsTask(eventsStorageConsumer,
                sendIntervalMillis,
                eventsSender,
                threadFactory);
    }

    public EventsTask(EventsStorageConsumer eventsStorageConsumer,
               long sendIntervalMillis, EventSender eventsSender, ThreadFactory threadFactory) {

        _eventsStorageConsumer = Objects.requireNonNull(eventsStorageConsumer);
        _sendIntervalMillis = sendIntervalMillis;
        _eventsSender = Objects.requireNonNull(eventsSender);
        _senderScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    public void start(){
        _senderScheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                sendEvents();
            } catch (Exception e) {
                _log.error("Error executing Event Action", e);
            }
        }, _sendIntervalMillis, _sendIntervalMillis, TimeUnit.MILLISECONDS);
    }

    public void close() {
        try {
            sendEvents();
            _senderScheduledExecutorService.shutdown();
        } catch (Exception e) {
            _log.warn("Error when shutting down EventClientImpl", e);
        }
    }

    void sendEvents(){
        if (_eventsStorageConsumer.isFull()) {
            _log.warn("Split SDK events queue is full. Events may have been dropped. Consider increasing capacity.");
        }

        List<WrappedEvent> wrappedEventList = _eventsStorageConsumer.popAll();
        List<Event> eventsToSend = new ArrayList<>();
        for (WrappedEvent wrappedEvent: wrappedEventList){
            Event event = wrappedEvent.event();
            eventsToSend.add(event);
        }

        if (eventsToSend.isEmpty()){
            return;
        }
        _eventsSender.send(eventsToSend);
    }
}
