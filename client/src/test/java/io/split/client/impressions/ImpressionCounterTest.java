package io.split.client.impressions;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class ImpressionCounterTest {

    @Test
    public void testTruncateTimeFrame() {
        assertThat(ImpressionCounter.truncateTimeframe(new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 10, 53, 12).getTimeInMillis()),
                is(equalTo(new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 10, 0, 0).getTimeInMillis())));
        assertThat(ImpressionCounter.truncateTimeframe(new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 10, 0, 0).getTimeInMillis()),
                is(equalTo(new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 10, 0, 0).getTimeInMillis())));
        assertThat(ImpressionCounter.truncateTimeframe(new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 10, 53, 0 ).getTimeInMillis()),
                is(equalTo(new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 10, 0, 0).getTimeInMillis())));
        assertThat(ImpressionCounter.truncateTimeframe(new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 10, 0, 12).getTimeInMillis()),
                is(equalTo(new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 10, 0, 0).getTimeInMillis())));
        assertThat(ImpressionCounter.truncateTimeframe(new GregorianCalendar(1970, Calendar.JANUARY, 0, 0, 0, 0).getTimeInMillis()),
                is(equalTo(new GregorianCalendar(1970, Calendar.JANUARY, 0, 0, 0, 0).getTimeInMillis())));
    }

    @Test
    public void testMakeKey() {
        assertThat(ImpressionCounter.makeKey("someFeature", new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 10, 5, 23).getTimeInMillis()),
                is(equalTo("someFeature::1599051600000")));
        assertThat(ImpressionCounter.makeKey("", new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 10, 5, 23).getTimeInMillis()),
                is(equalTo("::1599051600000")));
        assertThat(ImpressionCounter.makeKey(null, new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 10, 5, 23).getTimeInMillis()),
                is(equalTo("null::1599051600000")));
        assertThat(ImpressionCounter.makeKey(null, 0L), is(equalTo("null::0")));
    }

    @Test
    public void testBasicUsage() {
        final ImpressionCounter counter = new ImpressionCounter();
        final long timestamp = new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 10, 10, 12).getTimeInMillis();
        counter.inc("feature1", timestamp, 1);
        counter.inc("feature1", timestamp + 1, 1);
        counter.inc("feature1", timestamp + 2, 1);
        counter.inc("feature2", timestamp + 3, 2);
        counter.inc("feature2", timestamp + 4, 2);
        Map<String, Integer> counted = counter.popAll();
        assertThat(counted.size(), is(equalTo(2)));
        assertThat(counted.get(ImpressionCounter.makeKey("feature1", timestamp)), is(equalTo(3)));
        assertThat(counted.get(ImpressionCounter.makeKey("feature2", timestamp)), is(equalTo(4)));
        assertThat(counter.popAll().size(), is(equalTo(0)));

        final long nextHourTimestamp = new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 11, 10, 12).getTimeInMillis();
        counter.inc("feature1", timestamp, 1);
        counter.inc("feature1", timestamp + 1, 1);
        counter.inc("feature1", timestamp + 2, 1);
        counter.inc("feature2", timestamp + 3, 2);
        counter.inc("feature2", timestamp + 4, 2);
        counter.inc("feature1", nextHourTimestamp, 1);
        counter.inc("feature1", nextHourTimestamp + 1, 1);
        counter.inc("feature1", nextHourTimestamp + 2, 1);
        counter.inc("feature2", nextHourTimestamp + 3, 2);
        counter.inc("feature2", nextHourTimestamp + 4, 2);
        counted = counter.popAll();
        assertThat(counted.size(), is(equalTo(4)));
        assertThat(counted.get(ImpressionCounter.makeKey("feature1", timestamp)), is(equalTo(3)));
        assertThat(counted.get(ImpressionCounter.makeKey("feature2", timestamp)), is(equalTo(4)));
        assertThat(counted.get(ImpressionCounter.makeKey("feature1", nextHourTimestamp)), is(equalTo(3)));
        assertThat(counted.get(ImpressionCounter.makeKey("feature2", nextHourTimestamp)), is(equalTo(4)));
        assertThat(counter.popAll().size(), is(equalTo(0)));
    }

    @Test
    public void manyConcurrentCalls() throws InterruptedException {
        final int iterations = 10000000;
        final long timestamp = new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 10, 10, 12).getTimeInMillis();
        final long nextHourTimestamp = new GregorianCalendar(2020, Calendar.SEPTEMBER, 2, 11, 10, 12).getTimeInMillis();
        ImpressionCounter counter = new ImpressionCounter();
        Thread t1 = new Thread(() -> {
            int times = iterations;
            while (times-- > 0) {
                counter.inc("feature1", timestamp, 1);
                counter.inc("feature2", timestamp, 1);
                counter.inc("feature1", nextHourTimestamp, 2);
                counter.inc("feature2", nextHourTimestamp, 2);
            }
        });
        Thread t2 = new Thread(() -> {
            int times = iterations;
            while (times-- > 0) {
                counter.inc("feature1", timestamp, 2);
                counter.inc("feature2", timestamp, 2);
                counter.inc("feature1", nextHourTimestamp, 1);
                counter.inc("feature2", nextHourTimestamp, 1);
            }
        });

        t1.setDaemon(true); t2.setDaemon(true);
        t1.start(); t2.start();
        t1.join(); t2.join();

        HashMap<String, Integer> counted = counter.popAll();
        assertThat(counted.size(), is(equalTo(4)));
        assertThat(counted.get(ImpressionCounter.makeKey("feature1", timestamp)), is(equalTo(iterations * 3)));
        assertThat(counted.get(ImpressionCounter.makeKey("feature2", timestamp)), is(equalTo(iterations * 3)));
        assertThat(counted.get(ImpressionCounter.makeKey("feature1", nextHourTimestamp)), is(equalTo(iterations * 3)));
        assertThat(counted.get(ImpressionCounter.makeKey("feature2", nextHourTimestamp)), is(equalTo(iterations * 3)));
    }
}
