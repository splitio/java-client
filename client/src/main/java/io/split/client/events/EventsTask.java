package io.split.client.events;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.dtos.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Responsible for sending events added via .track() to Split collection services
 */
public class EventsTask{

    private final EventsStorageConsumer _eventsStorageConsumer;
    private final EventsSender _eventsSender;
    private final long _sendIntervalMillis;

    private final ScheduledExecutorService _senderScheduledExecutorService;
    private static final Logger _log = LoggerFactory.getLogger(EventsTask.class);

    public static EventsTask create(long sendIntervalMillis, EventsStorageConsumer eventsStorageConsumer, EventsSender eventsSender) throws URISyntaxException {
        return new EventsTask(eventsStorageConsumer,
                sendIntervalMillis,
                eventsSender);
    }

    EventsTask(EventsStorageConsumer eventsStorageConsumer,
               long sendIntervalMillis, EventsSender eventsSender) {

        _eventsStorageConsumer = checkNotNull(eventsStorageConsumer);

        _sendIntervalMillis = sendIntervalMillis;

        _eventsSender = checkNotNull(eventsSender);

        ThreadFactory senderThreadFactory = eventClientThreadFactory("Sender-events-%d");
        _senderScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(senderThreadFactory);

        try {
            this.start();
        } catch (Exception e) {
            _log.error("Error trying to init EventTask synchronizer task.", e);
        }
    }

    ThreadFactory eventClientThreadFactory(final String name) {
        return new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat(name)
                .build();
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
        _eventsSender.sendEvents(eventsToSend);
    }
}