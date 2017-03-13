package io.split.client.metrics;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@Ignore
public class LogarithmicSearchLatencyTrackerTest {

    LogarithmicSearchLatencyTracker tracker;

    @Before
    public void before() {
        tracker = new LogarithmicSearchLatencyTracker();
    }

    /**
     * Latencies of <=1 millis or <= 1000 micros correspond to the first bucket (index 0)
     */
    @Test
    public void testLessThanFirstBucket() {

        tracker.addLatencyMicros(750);
        tracker.addLatencyMicros(450);
        assertThat(tracker.getLatency(0), is(equalTo(2L)));

        tracker.addLatencyMillis(0);
        assertThat(tracker.getLatency(0), is(equalTo(3L)));
    }

    /**
     * Latencies of 1 millis or <= 1000 micros correspond to the first bucket (index 0)
     */
    @Test
    public void testFirstBucket() {

        tracker.addLatencyMicros(1000);
        assertThat(tracker.getLatency(0), is(equalTo(1L)));

        tracker.addLatencyMillis(1);
        assertThat(tracker.getLatency(0), is(equalTo(2L)));
    }

    /**
     * Latencies of 7481 millis or 7481828 micros correspond to the last bucket (index 22)
     */
    @Test
    public void testLastBucket() {

        tracker.addLatencyMicros(7481828);
        assertThat(tracker.getLatency(22), is(equalTo(1L)));

        tracker.addLatencyMillis(7481);
        assertThat(tracker.getLatency(22), is(equalTo(2L)));
    }

    /**
     * Latencies of more than 7481 millis or 7481828 micros correspond to the last bucket (index 22)
     */
    @Test
    public void testGreaterThanLastBucket() {

        tracker.addLatencyMicros(7481830);
        assertThat(tracker.getLatency(22), is(equalTo(1L)));

        tracker.addLatencyMicros(7999999);
        assertThat(tracker.getLatency(22), is(equalTo(2L)));

        tracker.addLatencyMillis(7482);
        assertThat(tracker.getLatency(22), is(equalTo(3L)));

        tracker.addLatencyMillis(8000);
        assertThat(tracker.getLatency(22), is(equalTo(4L)));
    }

    /**
     * Latencies between 11,392 and 17,086 are in the 8th bucket.
     */
    @Test
    public void test8ThBucket() {

        tracker.addLatencyMicros(11392);
        assertThat(tracker.getLatency(7), is(equalTo(1L)));

        tracker.addLatencyMicros(17086);
        assertThat(tracker.getLatency(7), is(equalTo(2L)));

        tracker.addLatencyMillis(18);
        assertThat(tracker.getLatency(7), is(equalTo(3L)));
    }

    /**
     * Latencies between 656,842 and 985,261 are in the 18th bucket.
     */
    @Test
    public void test17ThBucket() {

        tracker.addLatencyMicros(656842);
        assertThat(tracker.getLatency(17), is(equalTo(1L)));

        tracker.addLatencyMicros(985261);
        assertThat(tracker.getLatency(17), is(equalTo(2L)));

        tracker.addLatencyMillis(985);
        assertThat(tracker.getLatency(17), is(equalTo(3L)));
    }

}