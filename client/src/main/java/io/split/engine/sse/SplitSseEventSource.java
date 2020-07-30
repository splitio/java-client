package io.split.engine.sse;

import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.LocalizationMessages;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class SplitSseEventSource {
    public static final String SERVER_SENT_EVENTS = "text/event-stream";

    private final AtomicReference<SplitSseEventSource.State> state = new AtomicReference<>(SplitSseEventSource.State.READY);
    private final CountDownLatch firstContactSignal;

    private final WebTarget _target;
    private final ExecutorService _executor;
    private final Function<InboundSseEvent, Void> _eventCallback;
    private final Function<String, Void> _failureCallback;
    private Future<?> _task;

    public SplitSseEventSource(WebTarget target, Function<InboundSseEvent, Void> eventCallback, Function<String, Void> failureCallback) {
        _target = target;
        _executor = Executors.newSingleThreadExecutor();
        _eventCallback = eventCallback;
        _failureCallback = failureCallback;

        this.firstContactSignal = new CountDownLatch(1);
    }

    public void open() {
        _task = _executor.submit(this::run);
        awaitFirstContact();
    }

    private void run() {
        try {
            if (state.get() != State.READY) {
                switch (state.get()) {
                    case CLOSED:
                        throw new IllegalStateException("Event Source Already close.");
                    case OPEN:
                        throw new IllegalStateException("Event Source Already connected.");
                }
            }

            final Invocation.Builder request = _target.request(SERVER_SENT_EVENTS);

            EventInput eventInput = null;
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
                    state.set(SplitSseEventSource.State.CLOSED);
                    _failureCallback.apply("se cerro la conexion");
                    break;
                } else {
                    _eventCallback.apply(eventInput.read());
                }
            }
        }  catch (Exception exc) {
            state.set(SplitSseEventSource.State.CLOSED);
            _failureCallback.apply(exc.getMessage());
        } finally {
            if (state.compareAndSet(State.OPEN, State.CLOSED)) {
                _failureCallback.apply("se cerro la conexion");
            }
        }
    }

    public boolean isOpen() {
        return state.get() == SplitSseEventSource.State.OPEN;
    }

    public void close() {
        _task.cancel(true);
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
            //LOGGER.debugLog("First contact signal released.");
        }
    }

    public enum State {
        /**
         * Ready to connect.
         */
        READY,

        /**
         * Connection established, events can be received.
         */
        OPEN,

        /**
         * Closed, won't receive any events.
         */
        CLOSED
    }
}
