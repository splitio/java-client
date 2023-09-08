package io.split.client;

import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionListener;
import io.split.client.impressions.ImpressionsManager;
import io.split.integrations.IntegrationsConfig;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class SplitClientConfigTest {

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_feature_refresh_rate_to_less_than_5() {
        SplitClientConfig.builder()
                .featuresRefreshRate(4)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_segment_refresh_rate_to_less_than_30() {
        SplitClientConfig.builder()
                .segmentsRefreshRate(29)
                .build();
    }

    @Test
    public void testImpressionRefreshRateConstraints() {
        SplitClientConfig cfg = SplitClientConfig.builder()
                .impressionsRefreshRate(-1)
                .build(); // OPTIMIZED BY DEFAULT

        assertThat(cfg.impressionsMode(), is(equalTo(ImpressionsManager.Mode.OPTIMIZED)));
        assertThat(cfg.impressionsRefreshRate(), is(equalTo(5 * 60))); // 5 minutes

        cfg = SplitClientConfig.builder()
                .impressionsRefreshRate(0)
                .build(); // OPTIMIZED BY DEFAULT

        assertThat(cfg.impressionsMode(), is(equalTo(ImpressionsManager.Mode.OPTIMIZED)));
        assertThat(cfg.impressionsRefreshRate(), is(equalTo(5 * 60))); // 5 minutes

        cfg = SplitClientConfig.builder()
                .impressionsRefreshRate(1)  // default value
                .build(); // OPTIMIZED BY DEFAULT

        assertThat(cfg.impressionsMode(), is(equalTo(ImpressionsManager.Mode.OPTIMIZED)));
        assertThat(cfg.impressionsRefreshRate(), is(equalTo(60))); // 5 minutes

        cfg = SplitClientConfig.builder()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(-1)
                .build(); // OPTIMIZED BY DEFAULT

        assertThat(cfg.impressionsMode(), is(equalTo(ImpressionsManager.Mode.DEBUG)));
        assertThat(cfg.impressionsRefreshRate(), is(equalTo(60)));

        cfg = SplitClientConfig.builder()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(0)
                .build(); // OPTIMIZED BY DEFAULT

        assertThat(cfg.impressionsMode(), is(equalTo(ImpressionsManager.Mode.DEBUG)));
        assertThat(cfg.impressionsRefreshRate(), is(equalTo(60)));

        cfg = SplitClientConfig.builder()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)  // default value
                .build(); // OPTIMIZED BY DEFAULT

        assertThat(cfg.impressionsMode(), is(equalTo(ImpressionsManager.Mode.DEBUG)));
        assertThat(cfg.impressionsRefreshRate(), is(equalTo(1)));
    }

    @Test
    public void set_impression_refresh_rate_works() {
        SplitClientConfig.builder()
                .impressionsRefreshRate(1)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_events_flush_rate_to_equal_to_1000() {
        SplitClientConfig.builder()
                .eventFlushIntervalInMillis(999)
                .build();
    }

    @Test
    public void events_flush_rate_works() {
        SplitClientConfig.builder()
                .eventFlushIntervalInMillis(1000)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_metrics_refresh_rate_to_less_than_30() {
        SplitClientConfig.builder()
                .metricsRefreshRate(29)
                .build();
    }

    @Test
    public void can_set_refresh_rates_to__30() {
        SplitClientConfig.builder()
                .featuresRefreshRate(30)
                .segmentsRefreshRate(30)
                .impressionsRefreshRate(30)
                .metricsRefreshRate(30)
                .build();
    }

    @Test
    public void config_does_not_crash_if_new_relic_class_not_present() {
        SplitClientConfig cfg = SplitClientConfig.builder()
                .integrations(IntegrationsConfig.builder()
                        .newRelicImpressionListener()
                        .build())
                .build();

        Assert.assertThat(
                cfg.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.SYNC).size(),
                is(equalTo(0)));
    }

    @Test
    public void old_impression_listener_config_still_works() {
        SplitClientConfig cfg = SplitClientConfig.builder()
               .impressionListener(new ImpressionListener() {
                   @Override
                   public void log(Impression impression) { /* noop */ }

                   @Override
                   public void close() { /* noop */ }
               }, 1000)
                .build();

        Assert.assertThat(
                cfg.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.ASYNC).size(),
                is(equalTo(1)));
    }

    @Test
    public void testVersion() {
        SplitClientConfig config = SplitClientConfig.builder()
                .build();

        Assert.assertThat(config.splitSdkVersion, Matchers.not(Matchers.equalTo("undefined")));
        Assert.assertThat(config.splitSdkVersion, Matchers.startsWith("java-"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void authRetryBackoffBaseLessThanAllowed() {
        SplitClientConfig cfg = SplitClientConfig.builder()
                .authRetryBackoffBase(0)
                .build();
    }

    @Test
    public void authRetryBackoffBaseAllowed() {
        SplitClientConfig cfg = SplitClientConfig.builder()
                .authRetryBackoffBase(2)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void streamingReconnectBackoffBaseLessThanAllowed() {
        SplitClientConfig cfg = SplitClientConfig.builder()
                .streamingReconnectBackoffBase(0)
                .build();
    }

    @Test
    public void streamingReconnectBackoffBaseAllowed() {
        SplitClientConfig cfg = SplitClientConfig.builder()
                .streamingReconnectBackoffBase(1)
                .build();
    }

    @Test
    public void checkDefaultRateForFeatureAndSegment() {
        SplitClientConfig config = SplitClientConfig.builder().build();
        Assert.assertEquals(60, config.featuresRefreshRate());
        Assert.assertEquals(60, config.segmentsRefreshRate());
    }

    @Test
    public void checkSetFlagSetsFilter() {
        List<String> sets = Stream.of("test1", "test2", "TEST3", "test-4").collect(Collectors.toList());
        SplitClientConfig config = SplitClientConfig.builder().flagSetsFilter(sets).build();
        Assert.assertNotNull(config.getSetsFilter());
        Assert.assertEquals(3, config.getSetsFilter().size());
    }
}
