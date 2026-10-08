package io.split.client.lifecycle;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * Leak check for the event-delivery thread. Other tests sharing the JVM may leave their own event threads running (or
 * start one lazily at any moment), so instead of scanning all threads a test records the threads that delivered events
 * to its own listeners and afterwards asserts that exactly those have terminated.
 */
public final class EventThreads {

    private static final Set<Thread> DELIVERED_ON = Collections.synchronizedSet(new HashSet<Thread>());

    private EventThreads() {
    }

    /** Call from a listener: remembers the thread the event was delivered on. */
    public static void record() {
        DELIVERED_ON.add(Thread.currentThread());
    }

    public static void awaitRecordedThreadsTerminated() {
        try {
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                synchronized (DELIVERED_ON) {
                    for (Thread t : DELIVERED_ON) {
                        if (t.isAlive()) {
                            return false;
                        }
                    }
                    return true;
                }
            });
        } finally {
            DELIVERED_ON.clear();
        }
    }
}
