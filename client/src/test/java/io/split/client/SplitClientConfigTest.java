package io.split.client;

import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionListener;
import io.split.integrations.IntegrationsConfig;
import org.junit.Assert;
import org.junit.Test;

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

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_impression_refresh_rate_to_equal_to_0() {
        SplitClientConfig.builder()
                .impressionsRefreshRate(0)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_impression_refresh_rate_to_less_than_0() {
        SplitClientConfig.builder()
                .impressionsRefreshRate(-1)
                .build();
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

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_zero_capacity_on_impression_listener() throws InterruptedException {
        SplitClientConfig.builder()
                .integrations(IntegrationsConfig.builder()
                        .impressionsListener(new ImpressionListener.NoopImpressionListener(), 0)
                        .build())
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
}
