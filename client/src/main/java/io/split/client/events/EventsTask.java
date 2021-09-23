package io.split.client.events;

import com.google.common.annotations.VisibleForTesting;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.MIN_PRIORITY;
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

    private final ExecutorService _senderExecutor;
    private final ExecutorService _consumerExecutor;

    private final ScheduledExecutorService _flushScheduler;

    static final Event SENTINEL = new Event();
    private static final Logger _log = LoggerFactory.getLogger(EventsTask.class);
    private final CloseableHttpClient _httpclient;
    private final URI _target;
    private final int _waitBeforeShutdown;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    ThreadFactory eventClientThreadFactory(final String name) {
        return r -> new Thread(() -> {
            Thread.currentThread().setPriority(MIN_PRIORITY);
            r.run();
        }, name);
    }


    public static EventsTask create(CloseableHttpClient httpclient, URI eventsRootTarget, int maxQueueSize,
                                    long flushIntervalMillis, int waitBeforeShutdown, TelemetryRuntimeProducer telemetryRuntimeProducer, EventsStorageConsumer eventsStorageConsumer, EventsStorageProducer _eventsStorageProducer) throws URISyntaxException {
        return new EventsTask(eventsStorageConsumer,
                _eventsStorageProducer,
                httpclient,
                Utils.appendPath(eventsRootTarget, "api/events/bulk"),
                maxQueueSize,
                flushIntervalMillis,
                waitBeforeShutdown,
                telemetryRuntimeProducer);
    }

    EventsTask(EventsStorageConsumer eventsStorageConsumer, EventsStorageProducer eventsStorageProducer, CloseableHttpClient httpclient, URI target, int maxQueueSize,
               long flushIntervalMillis, int waitBeforeShutdown, TelemetryRuntimeProducer telemetryRuntimeProducer) throws URISyntaxException {

        _httpclient = checkNotNull(httpclient);

        _target = checkNotNull(target);

        _eventsStorageConsumer = checkNotNull(eventsStorageConsumer);
        _eventsStorageProducer = checkNotNull(eventsStorageProducer);
        _waitBeforeShutdown = waitBeforeShutdown;

        _maxQueueSize = maxQueueSize;
        _flushIntervalMillis = flushIntervalMillis;
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);

        _eventsSender = EventsSender.create(_httpclient, _target, _telemetryRuntimeProducer);
        _senderExecutor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(50),
                eventClientThreadFactory("eventclient-sender"),
                (r, executor) -> _log.warn("Executor queue full. Dropping events."));

        _consumerExecutor = Executors.newSingleThreadExecutor(eventClientThreadFactory("eventclient-consumer"));
        _consumerExecutor.submit(runConsumer());

        _flushScheduler = Executors.newScheduledThreadPool(1, eventClientThreadFactory("eventclient-flush"));
        _flushScheduler.scheduleAtFixedRate(() -> flush(), _flushIntervalMillis, _flushIntervalMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * the existence of this message in the queue triggers a send event in the consumer thread.
     */
    public void flush() {
        _eventsStorageProducer.track(SENTINEL, 0);
    }  // SENTINEL event won't be queued, so no size needed.

    public void close() {
        try {
            _consumerExecutor.shutdownNow();
            _flushScheduler.shutdownNow();
            _senderExecutor.awaitTermination(_waitBeforeShutdown, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            _log.warn("Error when shutting down EventClientImpl", e);
        }
    }

    /**
     * Infinite loop that listens to event from the event queue, dequeue them and send them over once:
     *  - a CENTINEL message has arrived, or
     *  - the queue reached a specific size
     *
     */

    private Runnable runConsumer() {
        Runnable runnable = () -> {
            List<Event> events = new ArrayList<>();
            long accumulated = 0;
            while (!Thread.currentThread().isInterrupted()) {
                WrappedEvent data = _eventsStorageConsumer.pop();
                Event event = data.event();
                Long size = data.size();

                if (event != SENTINEL) {
                    events.add(event);
                    accumulated += size;
                } else if (events.size() < 1) {

                    if (_log.isDebugEnabled()) {
                        _log.debug("No messages to publish.");
                    }

                    continue;
                }
                if (events.size() >= _maxQueueSize ||  accumulated >= MAX_SIZE_BYTES || event == SENTINEL) {

                    // Send over the network
                    if (_log.isDebugEnabled()) {
                        _log.debug(String.format("Sending %d events", events.size()));
                    }

                    // Dispatch
                    List<Event> finalEvents = events; //This is to be able to handle events on Runnable.
                    Runnable r = () -> _eventsSender.sendEvents(finalEvents);
                    _senderExecutor.submit(r);

                    // Clear the queue of events for the next batch.
                    events = new ArrayList<>();
                    accumulated = 0;
                }
            }
        };
        return runnable;
    }

    @VisibleForTesting
    URI getTarget() {
        return _target  ;
    }
}