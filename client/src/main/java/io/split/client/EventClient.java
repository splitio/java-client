package io.split.client;

import io.split.client.dtos.Event;
import io.split.client.utils.Utils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.MIN_PRIORITY;

/**
 * Responsible for sending events added via .track() to Split collection services
 */
public class EventClient {

    private final BlockingQueue<Event> _eventQueue;
    private final int _maxQueueSize;
    private final long _flushIntervalMillis;

    private final ExecutorService _senderExecutor;
    private final ExecutorService _consumerExecutor;

    private final ScheduledExecutorService _flushScheduler;

    static final Event CENTINEL = new Event();
    private static final Logger _log = LoggerFactory.getLogger(EventClient.class);
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
        return new EventClient(new LinkedBlockingQueue<Event>(), httpclient, eventsRootTarget, maxQueueSize, flushIntervalMillis, waitBeforeShutdown);
    }

    EventClient(BlockingQueue<Event> eventQueue, CloseableHttpClient httpclient, URI eventsRootTarget, int maxQueueSize,
                long flushIntervalMillis, int waitBeforeShutdown) throws URISyntaxException {

        _httpclient = httpclient;

        _eventsTarget = new URIBuilder(eventsRootTarget).setPath("/api/events/bulk").build();

        _eventQueue = eventQueue;
        _waitBeforeShutdown = waitBeforeShutdown;

        _maxQueueSize = maxQueueSize;
        _flushIntervalMillis = flushIntervalMillis;

        _senderExecutor = Executors.newSingleThreadExecutor(eventClientThreadFactory("eventclient-sender"));

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
     * the existing of this message in the queue triggers a send event in the consumer thread.
     */
    public void flush() {
        track(CENTINEL);
    }

    public void track(Event event) {
        try {
            if (event == null) {
                return;
            }
            _eventQueue.put(event);
        } catch (InterruptedException e) {
            _log.warn("Interruption when adding event withed while adding message %s.", event);
        }
    }

    public void close() {
        try {
            _consumerExecutor.shutdownNow();
            _flushScheduler.shutdownNow();
            _senderExecutor.awaitTermination(_waitBeforeShutdown, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            _log.warn("Error when shutting down EventClient", e);
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
            CloseableHttpResponse response = null;

            try {
                StringEntity entity = Utils.toJsonEntity(_data);

                HttpPost request = new HttpPost(_endpoint);
                request.setEntity(entity);

                response = _client.execute(request);

                int status = response.getStatusLine().getStatusCode();

                if (status < 200 || status >= 300) {
                    _log.warn("Posting events returned with error. status: " + status);
                }

            } catch (Throwable t) {
                _log.warn("Posting events returned with error");
            } finally {
                Utils.forceClose(response);
            }
        }
    }

}

