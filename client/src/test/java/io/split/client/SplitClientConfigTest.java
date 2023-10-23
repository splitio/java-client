package io.split.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionListener;
import io.split.client.impressions.ImpressionsManager;
import io.split.integrations.IntegrationsConfig;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class SplitClientConfigTest {

    @Test(expected = IllegalArgumentException.class)
    public void cannotSetFeatureRefreshRateToLessThan5() {
        SplitClientConfig.builder()
                .featuresRefreshRate(4)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotSetSegmentRefreshRateToLessThan30() {
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
    public void setImpressionRefreshRateWorks() {
        SplitClientConfig.builder()
                .impressionsRefreshRate(1)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotSetEventsFlushRateToEqualTo1000() {
        SplitClientConfig.builder()
                .eventFlushIntervalInMillis(999)
                .build();
    }

    @Test
    public void eventsFlushRateWorks() {
        SplitClientConfig.builder()
                .eventFlushIntervalInMillis(1000)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotSetMetricsRefreshRateToLessTha30() {
        SplitClientConfig.builder()
                .metricsRefreshRate(29)
                .build();
    }

    @Test
    public void canSetRefreshRatesTo30() {
        SplitClientConfig.builder()
                .featuresRefreshRate(30)
                .segmentsRefreshRate(30)
                .impressionsRefreshRate(30)
                .metricsRefreshRate(30)
                .build();
    }

    @Test
    public void configDoesNotCrashIfNewRelicClassNotPresent() {
        SplitClientConfig cfg = SplitClientConfig.builder()
                .integrations(IntegrationsConfig.builder()
                        .newRelicImpressionListener()
                        .build())
                .build();

        Assert.assertEquals(0, cfg.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.SYNC).size());
    }

    @Test
    public void oldImpressionListenerConfigStillWorks() {
        SplitClientConfig cfg = SplitClientConfig.builder()
               .impressionListener(new ImpressionListener() {
                   @Override
                   public void log(Impression impression) { /* noop */ }

                   @Override
                   public void close() { /* noop */ }
               }, 1000)
                .build();

        Assert.assertEquals(1, cfg.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.ASYNC).size());
    }

    @Test
    public void testVersion() {
        SplitClientConfig config = SplitClientConfig.builder()
                .build();

        Assert.assertNotEquals("undefined", config.splitSdkVersion);
        Assert.assertTrue(config.splitSdkVersion.startsWith("java-"));
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
    public void checkDefaultRateForFeatureAndSegment(){
        SplitClientConfig config = SplitClientConfig.builder().build();
        Assert.assertEquals(60, config.featuresRefreshRate());
        Assert.assertEquals(60, config.segmentsRefreshRate());
    }

    @Test
    public void threadFactoryNull() {
        SplitClientConfig config = SplitClientConfig.builder().build();
        Assert.assertNull(config.getThreadFactory());
    }

    @Test
    public void threadFactoryNotNull() {
        SplitClientConfig config = SplitClientConfig.builder().threadFactory(new ThreadFactoryBuilder().build()).build();
        Assert.assertNotNull(config.getThreadFactory());
    }
}