package io.split.engine.sse;

import io.split.engine.common.Backoff;
import org.glassfish.jersey.media.sse.EventInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class SplitSseEventSource {
    private static final Logger _log = LoggerFactory.getLogger(EventSourceClient.class);
    private static final String SERVER_SENT_EVENTS = "text/event-stream";
    private static final double MAX_SECONDS_BACKOFF_ALLOWED = 1800;

    private final AtomicReference<SseState> _state = new AtomicReference<>(SseState.CLOSED);

    private final Function<InboundSseEvent, Void> _eventCallback;
    private final Function<Boolean, Void> _disconnectedCallback;
    private final Function<String, Void> _reconnectCallback;
    private final Backoff _backoff;
    private WebTarget _target;
    private CountDownLatch _firstContactSignal;
    private EventInput _eventInput;
    private ScheduledExecutorService _executor;

    public SplitSseEventSource(Function<InboundSseEvent, Void> eventCallback, Function<Boolean, Void> disconnectedCallback, Function<String, Void> reconnectCallback, Backoff backoff) {
        _eventCallback = eventCallback;
        _disconnectedCallback = disconnectedCallback;
        _reconnectCallback = reconnectCallback;
        _backoff = backoff;
    }

    public void setTarget(WebTarget target){
        _target = target;
    }

    public void open() {
        _eventInput = null;
        double interval = _backoff.interval();
        _log.debug(String.format("Sse Backoff interval: %s", interval));

        if (interval > MAX_SECONDS_BACKOFF_ALLOWED) {
            _disconnectedCallback.apply(false);
            _state.set(SseState.CLOSED);
            return;
        }

        _firstContactSignal = new CountDownLatch(1);
        _executor = Executors.newSingleThreadScheduledExecutor();
        _executor.schedule(this::run, (long)interval, TimeUnit.SECONDS);
        awaitFirstContact();
    }

    private void run() {
        try {
            if (isOpen()) {
                throw new IllegalStateException("Event Source Already connected.");
            }

            final Invocation.Builder request = _target.request(SERVER_SENT_EVENTS);

            try {
                _eventInput = request.get(EventInput.class);
                _state.set(SseState.OPEN);
                _log.debug(String.format("SplitSseEventSource.run state: %s", _state.get()));
                _backoff.reset();
            } finally {
                if (_firstContactSignal != null) {
                    // release the signal regardless of event source state or connection request outcome
                    _firstContactSignal.countDown();
                }
            }

            while (isOpen() && !Thread.currentThread().isInterrupted()) {
                if (_eventInput == null || _eventInput.isClosed()) {
                    if (isOpen()) {
                        _log.debug("SplitSseEventSource - Connection lost. Reconnect true");
                        internalReconnect();
                    }
                    break;
                } else {
                    _eventCallback.apply(_eventInput.read());
                }
            }

        } catch (WebApplicationException wae) {
            _log.debug(String.format("Unable to connect. Reconnect: true. %s - %s", wae.getResponse(), wae.getMessage()));
            close(true);
        } catch (Exception exc) {
            _log.warn(exc.getMessage());
            close();
        } finally {
            _state.set(SseState.CLOSED);
            Thread.currentThread().interrupt();
        }
    }

    public boolean isOpen() {
        return _state.get() == SseState.OPEN;
    }

    private void close(boolean reconnect) {
        _log.debug(String.format("SplitSseEventSource.close state: %s", _state.get()));
        if (!isOpen()) {
            _log.warn("SplitSseEventSource already closed.");
            return;
        }

        _state.set(SseState.CLOSED);
        _disconnectedCallback.apply(reconnect);
        _eventInput.close();
        _executor.shutdown();
        _log.debug(String.format("SplitSseEventSource.close final state: %s", _state.get()));
    }

    public void close() {
        close(false);
    }

    private void internalReconnect() {
        _state.set(SseState.CLOSED);
        _eventInput.close();
        _executor.shutdown();
        _reconnectCallback.apply("Reconnect - Connection lost.");
    }

    private void awaitFirstContact() {
        try {
            if (_firstContactSignal == null) {
                return;
            }

            try {
                _firstContactSignal.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        } finally {
            _log.debug("First contact signal released.");
        }
    }

    public enum SseState {
        OPEN,
        CLOSED
    }
}
