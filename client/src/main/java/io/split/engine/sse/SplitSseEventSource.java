package io.split.engine.sse;

import org.glassfish.jersey.media.sse.EventInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class SplitSseEventSource {
    private static final Logger _log = LoggerFactory.getLogger(EventSourceClient.class);
    private static final String SERVER_SENT_EVENTS = "text/event-stream";

    private final AtomicReference<SplitSseEventSource.State> state = new AtomicReference<>(SplitSseEventSource.State.READY);

    private final Function<InboundSseEvent, Void> _eventCallback;
    private final Function<Boolean, Void> _disconnectedCallback;
    private WebTarget _target;
    private CountDownLatch firstContactSignal;
    private EventInput eventInput;
    private ExecutorService _executor;

    public SplitSseEventSource(Function<InboundSseEvent, Void> eventCallback, Function<Boolean, Void> disconnectedCallback) {
        _eventCallback = eventCallback;
        _disconnectedCallback = disconnectedCallback;
    }

    public void setTarget(WebTarget target){
        _target = target;
    }

    public void open() {
        firstContactSignal = new CountDownLatch(1);

        _executor = Executors.newSingleThreadExecutor();
        _executor.submit(this::run);
        awaitFirstContact();
    }

    private void run() {
        try {
            if (state.get() == State.OPEN) {
                throw new IllegalStateException("Event Source Already connected.");
            }

            final Invocation.Builder request = _target.request(SERVER_SENT_EVENTS);

            try {
                eventInput = request.get(EventInput.class);
            } finally {
                if (firstContactSignal != null) {
                    // release the signal regardless of event source state or connection request outcome
                    firstContactSignal.countDown();
                }
            }

            state.set(State.OPEN);

            while (!Thread.currentThread().isInterrupted()) {
                if (eventInput == null || eventInput.isClosed()) {
                    _log.debug("SplitSseEventSource - Connection lost.");
                    if (state.compareAndSet(State.OPEN, State.CLOSED)) {
                        _disconnectedCallback.apply(true);
                    }
                    break;
                } else {
                    _eventCallback.apply(eventInput.read());
                }
            }
        } catch (WebApplicationException wae) {
            _log.debug("Unable to connect. Reconnect: false");
        } catch (Exception exc) {
            _log.debug("Unable to connect - closing the event source. Reconnect: false");
        } finally {
            if (state.compareAndSet(State.OPEN, State.CLOSED)) {
                _log.debug("Connection lost. Reconnect: false.");
                _disconnectedCallback.apply(false);
            }
        }
    }

    public boolean isOpen() {
        return state.get() == SplitSseEventSource.State.OPEN;
    }

    public void close() {
        if (!isOpen()) {
            return;
        }

        state.set(State.CLOSED);
        eventInput.close();
        _executor.shutdown();
    }

    private void awaitFirstContact() {
        try {
            if (firstContactSignal == null) {
                return;
            }

            try {
                firstContactSignal.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        } finally {
            _log.debug("First contact signal released.");
        }
    }

    public enum State {
        READY,
        OPEN,
        CLOSED
    }
}
