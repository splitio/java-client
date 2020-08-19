package io.split.engine.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BackoffTest {

    @Test
    public void backoffIncreaseInterval() {
        Backoff backoff = new Backoff(1);

        long interval = backoff.interval();
        assertEquals(1L, interval);

        interval = backoff.interval();
        assertEquals(2L, interval);

        interval = backoff.interval();
        assertEquals(4L, interval);

        interval = backoff.interval();
        assertEquals(8L, interval);

        interval = backoff.interval();
        assertEquals(16L, interval);

        backoff.reset();
        interval = backoff.interval();
        assertEquals(1L, interval);

        interval = backoff.interval();
        assertEquals(2L, interval);
    }
    @Test
    public void backoffIncreaseIntervalMaxAllowed() {
        long max_allowed = 1800;
        Backoff backoff = new Backoff(450);

        long interval = backoff.interval();
        assertEquals(450L, interval);

        interval = backoff.interval();
        assertEquals(900L, interval);

        interval = backoff.interval();
        assertEquals(max_allowed, interval);

        interval = backoff.interval();
        assertEquals(max_allowed, interval);

        interval = backoff.interval();
        assertEquals(max_allowed, interval);
    }
}
