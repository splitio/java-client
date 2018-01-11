package io.split.client;

import io.split.client.dtos.Event;
import io.split.client.utils.GenericClientUtil;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.MIN_PRIORITY;

/**
 * Responsible for sending events added via .track() to Split collection services
 */
public class EventClientImpl implements EventClient {

    private final BlockingQueue<Event> _eventQueue;
    private final int _maxQueueSize;
    private final long _flushIntervalMillis;

    private final ExecutorService _senderExecutor;
    private final ExecutorService _consumerExecutor;

    private final ScheduledExecutorService _flushScheduler;

    static final Event CENTINEL = new Event();
    private static final Logger _log = LoggerFactory.getLogger(EventClientImpl.class);
    private final CloseableHttpClient _httpclient;
    private final URI _eventsTarget;
    private final int _waitBeforeShutdown;

    ThreadFactory eventClientThreadFactory(final String name) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Thread.currentThread().setPriority(MIN_PRIORITY);
                        r.run();
                    }
                }, name);
            }
        };
    }


    public static EventClient create(CloseableHttpClient httpclient, URI eventsRootTarget, int maxQueueSize, long flushIntervalMillis, int waitBeforeShutdown) throws URISyntaxException {
        return new EventClientImpl(new LinkedBlockingQueue<Event>(), httpclient, eventsRootTarget, maxQueueSize, flushIntervalMillis, waitBeforeShutdown);
    }

    EventClientImpl(BlockingQueue<Event> eventQueue, CloseableHttpClient httpclient, URI eventsRootTarget, int maxQueueSize,
                    long flushIntervalMillis, int waitBeforeShutdown) throws URISyntaxException {

        _httpclient = httpclient;

        _eventsTarget = new URIBuilder(eventsRootTarget).setPath("/api/events/bulk").build();

        _eventQueue = eventQueue;
        _waitBeforeShutdown = waitBeforeShutdown;

        _maxQueueSize = maxQueueSize;
        _flushIntervalMillis = flushIntervalMillis;

        _senderExecutor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(50),
                eventClientThreadFactory("eventclient-sender"),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        _log.warn("Executor queue full. Dropping events.");
                    }
                });

        _consumerExecutor = Executors.newSingleThreadExecutor(eventClientThreadFactory("eventclient-consumer"));
        _consumerExecutor.submit(new Consumer());

        _flushScheduler = Executors.newScheduledThreadPool(1, eventClientThreadFactory("eventclient-flush"));
        _flushScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                flush();
            }
        }, _flushIntervalMillis, _flushIntervalMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * the existence of this message in the queue triggers a send event in the consumer thread.
     */
    public void flush() {
        track(CENTINEL);
    }

    public boolean track(Event event) {
        try {
            if (event == null) {
                return false;
            }
            _eventQueue.put(event);
        } catch (InterruptedException e) {
            _log.warn("Interruption when adding event withed while adding message %s.", event);
            return false;
        }
        return true;
    }

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
    class Consumer implements Runnable {
        @Override
        public void run() {
            List<Event> events = new ArrayList<>();

            try {
                while (true) {
                    Event event = _eventQueue.take();

                    if (event != CENTINEL) {
                        events.add(event);
                    } else if (events.size() < 1) {

                        if (_log.isDebugEnabled()) {
                            _log.debug("No messages to publish.");
                        }

                        continue;
                    }

                    if (events.size() >= _maxQueueSize || event == CENTINEL) {

                        // Send over the network
                        if (_log.isDebugEnabled()) {
                            _log.debug(String.format("Sending %d events", events.size()));
                        }

                        // Dispatch
                        _senderExecutor.submit(EventSenderTask.create(_httpclient, _eventsTarget, events));

                        // Clear the queue of events for the next batch.
                        events = new ArrayList<>();
                    }
                }
            } catch (InterruptedException e) {
                _log.debug("Consumer thread was interrupted. Exiting...");
            }
        }
    }

    static class EventSenderTask implements Runnable {

        private final List<Event> _data;
        private final URI _endpoint;
        private final CloseableHttpClient _client;

        static EventSenderTask create(CloseableHttpClient httpclient, URI eventsTarget, List<Event> events) {
            return new EventSenderTask(httpclient, eventsTarget, events);
        }

        EventSenderTask(CloseableHttpClient httpclient, URI eventsTarget, List<Event> events) {
            _client = httpclient;
            _data = events;
            _endpoint = eventsTarget;
        }

        @Override
        public void run() {
            GenericClientUtil.process(_data, _endpoint, _client);
        }
    }
}