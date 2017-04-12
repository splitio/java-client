package io.split.client.metrics;

import com.google.common.collect.Lists;
import io.split.client.dtos.Counter;
import io.split.client.dtos.Latency;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by adilaijaz on 9/23/15.
 */
public class CachedMetricsTest {

    private static final class MyDTOMetrics implements DTOMetrics {

        private List<Latency> latencies = Lists.newArrayList();
        private List<Counter> counters = Lists.newArrayList();

        @Override
        public void time(Latency dto) {
            latencies.add(dto);
        }

        @Override
        public void count(Counter dto) {
            counters.add(dto);
        }
    }

    @Test
    public void count() {
        MyDTOMetrics metrics = new MyDTOMetrics();

        CachedMetrics cachedMetrics = new CachedMetrics(metrics, 2);

        cachedMetrics.count("foo", 4);
        cachedMetrics.count("foo", 5);
        cachedMetrics.count("foo", 6);
        cachedMetrics.count("foo", 7);
        cachedMetrics.count("foo", 8);

        Counter counter = new Counter();
        counter.name = "foo";
        counter.delta = 9L;

        assertThat(metrics.counters.size(), is(equalTo(2)));
        assertThat(metrics.counters.get(0).name, is(equalTo("foo")));
        assertThat(metrics.counters.get(0).delta, is(equalTo(9L)));

        assertThat(metrics.counters.get(1).name, is(equalTo("foo")));
        assertThat(metrics.counters.get(1).delta, is(equalTo(13L)));
    }

    @Test
    public void latency() throws Exception {
        MyDTOMetrics delegate = new MyDTOMetrics();
        CachedMetrics cachedMetrics = new CachedMetrics(delegate, 15L);

        cachedMetrics.time("foo", 4);
        cachedMetrics.time("foo", 5);
        cachedMetrics.time("foo", 6);
        cachedMetrics.time("foo", 7);
        Thread.sleep(30);
        cachedMetrics.time("foo", 8);

        List<Long> latencies = Lists.newArrayList(0L, 0L, 0L, 0L, 2L, 2L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);

        assertThat(delegate.latencies.get(0).name, is(equalTo("foo")));
        assertThat(delegate.latencies.get(0).latencies, is(equalTo(latencies)));
    }

}
