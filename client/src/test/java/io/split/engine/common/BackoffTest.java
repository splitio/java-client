package io.split.engine.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BackoffTest {

    @Test
    public void backoffIncreaseInterval() {
        Backoff backoff = new Backoff(1);

        long interval = backoff.interval();
        assertEquals(0L, interval);

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
        assertEquals(0L, interval);

        interval = backoff.interval();
        assertEquals(2L, interval);
    }
}
