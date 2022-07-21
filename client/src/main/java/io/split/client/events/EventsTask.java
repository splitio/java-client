package io.split.client.events;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.dtos.Event;
import io.split.client.utils.Utils;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
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
    private final CloseableHttpClient _httpclient;
    private final URI _target;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public static EventsTask create(CloseableHttpClient httpclient, URI eventsRootTarget,
                                    long sendIntervalMillis, TelemetryRuntimeProducer telemetryRuntimeProducer, EventsStorageConsumer eventsStorageConsumer) throws URISyntaxException {
        return new EventsTask(eventsStorageConsumer,
                httpclient,
                Utils.appendPath(eventsRootTarget, "api/events/bulk"),
                sendIntervalMillis,
                telemetryRuntimeProducer);
    }

    EventsTask(EventsStorageConsumer eventsStorageConsumer, CloseableHttpClient httpclient, URI target,
               long sendIntervalMillis, TelemetryRuntimeProducer telemetryRuntimeProducer) throws URISyntaxException {

        _httpclient = checkNotNull(httpclient);

        _target = checkNotNull(target);

        _eventsStorageConsumer = checkNotNull(eventsStorageConsumer);

        _sendIntervalMillis = sendIntervalMillis;

        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);

        _eventsSender = EventsSender.create(_httpclient, _target, _telemetryRuntimeProducer);

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
            _log.warn("The Event List is empty");
            return;
        }
        _eventsSender.sendEvents(eventsToSend);
    }

    @VisibleForTesting
    URI getTarget() {
        return _target  ;
    }
}