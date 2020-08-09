package io.split.engine.sse;

import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class SplitSseEventSource {
    private static final Logger _log = LoggerFactory.getLogger(EventSourceClient.class);
    private static final String SERVER_SENT_EVENTS = "text/event-stream";
    private final AtomicReference<SseState> _state = new AtomicReference<>(SseState.CLOSED);
    private final Function<InboundSseEvent, Void> _eventCallback;
    private final ScheduledExecutorService _executor;
    private CountDownLatch _firstContactSignal;
    private final Function<SseStatus, Void> _sseStatusHandler;
    private EventInput _eventInput;


    public SplitSseEventSource(Function<InboundSseEvent, Void> eventCallback, Function<SseStatus, Void> sseStatusHandler) {
        _executor = Executors.newSingleThreadScheduledExecutor();
        _eventCallback = eventCallback;
        _sseStatusHandler = sseStatusHandler;
    }


    public boolean open(WebTarget target) {
        if (isOpen()) {
            throw new IllegalStateException("Event Source Already connected.");
        }

        _firstContactSignal = new CountDownLatch(1);
        _executor.execute(() -> run(target));
        awaitFirstContact();
        return isOpen();
    }

    private void run(WebTarget target) {
        try {
            // Initialization
            try {
                final Invocation.Builder request = target.request(SERVER_SENT_EVENTS);
                _eventInput = request.get(EventInput.class);
                if (_eventInput != null && !_eventInput.isClosed()) {
                    _sseStatusHandler.apply(SseStatus.CONNECTED);
                    _state.set(SseState.OPEN);
                }
            } finally {
                if (_firstContactSignal != null) {
                    // release the signal regardless of event source state or connection request outcome
                    _firstContactSignal.countDown();
                }
            }

            // Processing incoming messages
            while (isOpen() && !Thread.currentThread().isInterrupted() && null != _eventInput && !_eventInput.isClosed()) {
                InboundEvent e = _eventInput.read();
                if (null == e  && isOpen()) {
                    _sseStatusHandler.apply(SseStatus.RETRYABLE_ERROR);
                    return;
                }
                _eventCallback.apply(e);
            }

            // Notify graceful disconnection
            _sseStatusHandler.apply(SseStatus.DISCONNECTED);

        } catch (WebApplicationException wae) {
            _log.warn(wae.getMessage());
            if (wae.getResponse().getStatus() >= 400 && wae.getResponse().getStatus() < 500) {
                _sseStatusHandler.apply(SseStatus.NONRETRYABLE_ERROR);
            } else {
                _sseStatusHandler.apply(SseStatus.RETRYABLE_ERROR);
            }
        } catch (Exception exc) {
            // Unexpected exception: disable streaming completely
            _log.warn(exc.getMessage());
            _sseStatusHandler.apply(SseStatus.NONRETRYABLE_ERROR);
        } finally {
            if (_eventInput != null) {
                _eventInput.close();
            }
            _state.set(SseState.CLOSED);
            _log.debug("SSE connection finished.");
        }
    }

    public boolean isOpen() {
        return _state.get() == SseState.OPEN;
    }

    public void close() {
        if (!isOpen()) {
            _log.warn("SplitSseEventSource already closed.");
            return;
        }

        _state.set(SseState.CLOSED);
        _eventInput.close();
        _log.debug(String.format("SplitSseEventSource.close final state: %s", _state.get()));
    }

    public enum SseState {
        OPEN,
        CLOSED
    }

    private void awaitFirstContact() {
        _log.debug("Awaiting first contact signal.");
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
}
