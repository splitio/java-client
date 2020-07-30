package io.split.engine.sse;

import org.glassfish.jersey.media.sse.EventInput;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.InboundSseEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class SplitSseEventSource {
    public static final String SERVER_SENT_EVENTS = "text/event-stream";

    private final AtomicReference<SplitSseEventSource.State> state = new AtomicReference<>(SplitSseEventSource.State.READY);

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
    }

    public void open() {
        _task = _executor.submit(this::run);
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
            //EventInput eventInput = request.get(EventInput.class);
            Response response = request.get();

            state.set(State.OPEN);

            while (!Thread.currentThread().isInterrupted()) {
                /*if (eventInput == null || eventInput.isClosed()) {
                    state.set(SplitSseEventSource.State.CLOSED);
                    _failureCallback.apply("se cerro la conexion");
                    break;
                } else {
                    _eventCallback.apply(eventInput.read());
                }*/
            }
        }  catch (Exception exc) {
            state.set(SplitSseEventSource.State.CLOSED);
            _failureCallback.apply(exc.getMessage());
        } finally {
            System.out.println("CHAUUU");
            state.set(SplitSseEventSource.State.CLOSED);
        }
    }

    public boolean isOpen() {
        return state.get() == SplitSseEventSource.State.OPEN;
    }

    public void close() {
        _task.cancel(true);
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
