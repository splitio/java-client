package io.split.client.lifecycle;

import io.harness.events.EventHandler;
import io.harness.events.EventsManager;
import io.harness.events.EventsManagers;
import io.split.client.api.SdkEvent;
import io.split.client.api.SdkEventListener;
import io.split.client.api.SdkEventMetadata;
import io.split.client.utils.SplitExecutorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Wraps the commons {@code io.harness.events} library to expose {@link SdkEvent} registration/notification in terms
 * of {@code Sdk*} types only. No {@code io.harness.events} type appears in any public method of this class.
 *
 * <p>Registers exactly one dispatcher per {@link SdkEvent} with the underlying commons manager, because commons
 * stores multiple handlers for the same event in a {@code HashSet} (unordered) and delivers with no per-handler
 * error isolation. This class owns its own per-event ordered listener list and its own try/catch per listener,
 * so registration-order delivery and error isolation are guaranteed regardless of commons' internals.
 *
 * <p>Delivery runs on a dedicated single-thread executor, never on the caller's thread and never on commons'
 * own internal processing thread: commons calls our {@code EventDelivery} synchronously from the caller's thread
 * when replaying to a late subscriber, and serializes all internal-event processing for every event on one
 * background thread. Handing delivery off to our own executor keeps a slow listener from blocking either.
 *
 * <p>This layer is purely additive to readiness. {@code SDKReadinessGates} stays the authoritative source for
 * {@code isSDKReady()}, deliberately not {@link #eventAlreadyTriggered(SdkEvent)}: the commons implementation waits on
 * its internal process queue with no timeout, which must never happen on the evaluation path.
 */
public final class SdkEventsManager {

    private static final Logger LOG = LoggerFactory.getLogger(SdkEventsManager.class);

    private final EventsManager<SdkEvent, SdkInternalEvent, SdkEventMetadata> commonsManager;
    private final ExecutorService deliveryExecutor;
    private final Object registrationLock = new Object();
    private final Map<SdkEvent, List<SdkEventListener>> listeners = new HashMap<>();
    private final Set<SdkEvent> delivered = new HashSet<>();
    private boolean destroyed = false;

    public static SdkEventsManager create(ThreadFactory threadFactory) {
        return new SdkEventsManager(threadFactory);
    }

    SdkEventsManager(ThreadFactory threadFactory) {
        for (SdkEvent event : SdkEvent.values()) {
            listeners.put(event, new ArrayList<SdkEventListener>());
        }
        deliveryExecutor = SplitExecutorFactory.buildExecutorService(threadFactory, "SPLIT-EventsManager-%d");
        commonsManager = EventsManagers.create(SdkEventsConfig.build(), this::deliverAsync);
        for (SdkEvent event : SdkEvent.values()) {
            commonsManager.register(event, this::dispatch);
        }
    }

    /**
     * Registers a listener for {@code event}. If the event is once-only (SDK_READY, SDK_READY_TIMED_OUT) and has
     * already fired, the listener is delivered {@code null} metadata asynchronously instead of being registered
     * for future delivery (there won't be one). No-op after {@link #destroy()}.
     */
    public void on(SdkEvent event, SdkEventListener listener) {
        checkNotNull(event);
        checkNotNull(listener);
        boolean replay;
        synchronized (registrationLock) {
            if (destroyed) {
                return;
            }
            if (delivered.contains(event)) {
                replay = true;
            } else {
                listeners.get(event).add(listener);
                replay = false;
            }
        }
        if (replay) {
            submitQuietly(() -> safeInvoke(listener, event, null));
        }
    }

    /**
     * Removes all listeners registered for {@code event}. Does not affect whether the event can fire again in
     * the future, or whether new listeners can be registered afterward.
     */
    public void off(SdkEvent event) {
        checkNotNull(event);
        synchronized (registrationLock) {
            if (destroyed) {
                return;
            }
            listeners.get(event).clear();
        }
        // Deliberately not calling the commons manager's unregister(event): that would remove our permanent
        // per-event dispatcher, and this event could never fire again for the lifetime of this manager.
    }

    public void notifyInternalEvent(SdkInternalEvent event, SdkEventMetadata metadata) {
        if (isDestroyed()) {
            return;
        }
        commonsManager.notifyInternalEvent(event, metadata);
    }

    public boolean eventAlreadyTriggered(SdkEvent event) {
        if (isDestroyed()) {
            return false;
        }
        return commonsManager.eventAlreadyTriggered(event);
    }

    /**
     * Stops this manager: no further events fire, {@link #on(SdkEvent, SdkEventListener)} becomes a no-op, and all
     * registered listeners are dropped.
     */
    public void destroy() {
        synchronized (registrationLock) {
            if (destroyed) {
                return;
            }
            destroyed = true;
            listeners.clear();
        }
        commonsManager.destroy();
        deliveryExecutor.shutdown();
    }

    private boolean isDestroyed() {
        synchronized (registrationLock) {
            return destroyed;
        }
    }

    private void deliverAsync(EventHandler<SdkEvent, SdkEventMetadata> handler, SdkEvent event, SdkEventMetadata metadata) {
        submitQuietly(() -> {
            try {
                handler.handle(event, metadata);
            } catch (Throwable t) {
                LOG.warn("Unexpected error delivering " + event, t);
            }
        });
    }

    // destroy() can shut the executor down while a delivery is in flight; dropping it is the intended outcome.
    private void submitQuietly(Runnable task) {
        try {
            deliveryExecutor.submit(task);
        } catch (RejectedExecutionException e) {
            LOG.debug("Delivery dropped: events manager was destroyed");
        }
    }

    private void dispatch(SdkEvent event, SdkEventMetadata metadata) {
        List<SdkEventListener> snapshot;
        synchronized (registrationLock) {
            if (destroyed) {
                return;
            }
            // Readiness won the race: a timeout that was notified after SDK_READY was dispatched is dropped.
            if (event == SdkEvent.SDK_READY_TIMED_OUT && delivered.contains(SdkEvent.SDK_READY)) {
                return;
            }
            snapshot = new ArrayList<>(listeners.get(event));
            if (isOnceOnly(event)) {
                delivered.add(event);
            }
        }
        for (SdkEventListener listener : snapshot) {
            safeInvoke(listener, event, metadata);
        }
    }

    private void safeInvoke(SdkEventListener listener, SdkEvent event, SdkEventMetadata metadata) {
        try {
            listener.handle(metadata);
        } catch (Throwable t) {
            LOG.warn("SdkEventListener threw an exception handling " + event, t);
        }
    }

    private static boolean isOnceOnly(SdkEvent event) {
        return event == SdkEvent.SDK_READY || event == SdkEvent.SDK_READY_TIMED_OUT;
    }
}
