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
import java.util.Collections;
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

    public static final Long MAX_SIZE_BYTES = 5 * 1024 * 1024L;

    private final EventsStorageConsumer _eventsStorageConsumer;
    private final EventsStorageProducer _eventsStorageProducer;
    private final EventsSender _eventsSender;
    private final int _maxQueueSize;
    private final long _flushIntervalMillis;
    private final long _consumeIntervalMillis;
    private final long _sendIntervalMillis;

    private final ScheduledExecutorService _senderScheduledExecutorService;
    private final ScheduledExecutorService _consumerScheduledExecutorService;

    private final ScheduledExecutorService _flushScheduledExecutorService;

    static final Event SENTINEL = new Event();
    private static final Logger _log = LoggerFactory.getLogger(EventsTask.class);
    private final CloseableHttpClient _httpclient;
    private final URI _target;
    private final int _waitBeforeShutdown;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;
    private List<Event> eventsList;
    private long sizeAccumulated;

    public static EventsTask create(CloseableHttpClient httpclient, URI eventsRootTarget, int maxQueueSize,
                                    long flushIntervalMillis, long consumeIntervalMillis, long sendIntervalMillis, int waitBeforeShutdown, TelemetryRuntimeProducer telemetryRuntimeProducer, EventsStorageConsumer eventsStorageConsumer, EventsStorageProducer _eventsStorageProducer) throws URISyntaxException {
        return new EventsTask(eventsStorageConsumer,
                _eventsStorageProducer,
                httpclient,
                Utils.appendPath(eventsRootTarget, "api/events/bulk"),
                maxQueueSize,
                flushIntervalMillis,
                consumeIntervalMillis,
                sendIntervalMillis,
                waitBeforeShutdown,
                telemetryRuntimeProducer);
    }

    EventsTask(EventsStorageConsumer eventsStorageConsumer, EventsStorageProducer eventsStorageProducer, CloseableHttpClient httpclient, URI target, int maxQueueSize,
               long flushIntervalMillis, long consumeInvervalMillis, long sendIntervalMillis, int waitBeforeShutdown, TelemetryRuntimeProducer telemetryRuntimeProducer) throws URISyntaxException {

        _httpclient = checkNotNull(httpclient);

        _target = checkNotNull(target);

        _eventsStorageConsumer = checkNotNull(eventsStorageConsumer);
        _eventsStorageProducer = checkNotNull(eventsStorageProducer);
        _waitBeforeShutdown = waitBeforeShutdown;

        _maxQueueSize = maxQueueSize;
        _flushIntervalMillis = flushIntervalMillis;
        _consumeIntervalMillis = consumeInvervalMillis;
        _sendIntervalMillis = sendIntervalMillis;

        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        eventsList = Collections.synchronizedList(new ArrayList<>());
        sizeAccumulated = 0;

        _eventsSender = EventsSender.create(_httpclient, _target, _telemetryRuntimeProducer);

        ThreadFactory senderThreadFactory = eventClientThreadFactory("Sender-events-%d");
        _senderScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(senderThreadFactory);

        ThreadFactory consumerThreadFactory = eventClientThreadFactory("Consumer-events-%d");
        _consumerScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(consumerThreadFactory);

        ThreadFactory flushThreadFactory = eventClientThreadFactory("Flush-events-%d");
        _flushScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(flushThreadFactory);

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
        scheduleWithFixedDelay(_consumerScheduledExecutorService, _consumeIntervalMillis, new ExecuteConsumeEvents());
        scheduleWithFixedDelay(_senderScheduledExecutorService, _sendIntervalMillis, new ExecuteSendEvents());
        scheduleWithFixedDelay(_flushScheduledExecutorService, _flushIntervalMillis, new ExecuteFlushEvents());
    }

    public void close() {
        try {
            sendEvents();
            flush();
            _consumerScheduledExecutorService.shutdownNow();
            _flushScheduledExecutorService.shutdownNow();
            _senderScheduledExecutorService.shutdown();
        } catch (Exception e) {
            _log.warn("Error when shutting down EventClientImpl", e);
        }
    }

    private void scheduleWithFixedDelay(ScheduledExecutorService scheduledExecutorService, long refreshRate, ExecuteEventAction executeEventAction) {
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                executeEventAction.execute();
            } catch (Exception e) {
                _log.error("Error executing Event Action", e);
            }
        }, refreshRate, refreshRate, TimeUnit.MILLISECONDS);
    }

    /**
     * the existence of this message in the queue triggers a send event in the consumer thread.
     */
    public void flush() {
        _eventsStorageProducer.track(SENTINEL, 0);
    }  // SENTINEL event won't be queued, so no size needed.

    private synchronized void consumeEvents(){
        WrappedEvent data = _eventsStorageConsumer.pop();
        if (data == null) {
            return;
        }
        Event event = data.event();
        eventsList.add(event);
        sizeAccumulated += data.size();
        if (eventsList.size() >= _maxQueueSize ||  sizeAccumulated >= MAX_SIZE_BYTES || event == SENTINEL){
            // Send over the network
            if (_log.isDebugEnabled()) {
                _log.debug(String.format("Sending %d events", eventsList.size()));
            }
            sendEvents();
        }
    }

    private synchronized void sendEvents(){
        if (eventsList == null || eventsList.isEmpty()){
            _log.warn("The Event List is empty");
            return;
        }
        List<Event> listToSend = new ArrayList<>(eventsList);
        _eventsSender.sendEvents(listToSend);
        eventsList.clear();
        sizeAccumulated = 0;
    }

    @VisibleForTesting
    URI getTarget() {
        return _target  ;
    }

    private interface ExecuteEventAction{
        void execute();
    }

    private class ExecuteSendEvents implements ExecuteEventAction{

        @Override
        public void execute(){
            sendEvents();
        }
    }

    private class ExecuteConsumeEvents implements  ExecuteEventAction{
        @Override
        public void execute(){
            consumeEvents();
        }
    }

    private class ExecuteFlushEvents implements  ExecuteEventAction{
        @Override
        public void execute(){
            flush();
        }
    }
}