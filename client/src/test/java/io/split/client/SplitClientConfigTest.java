package io.split.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.dtos.RequestContext;
import io.split.client.dtos.FallbackTreatmentsConfiguration;
import io.split.client.dtos.FallbackTreatment;
import io.split.client.dtos.ProxyConfiguration;
import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionListener;
import io.split.client.impressions.ImpressionsManager;
import io.split.integrations.IntegrationsConfig;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsRefreshRate(65)
                .build();
        Assert.assertEquals(65, config.impressionsRefreshRate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotSetEventsFlushRateToEqualTo1000() {
        SplitClientConfig.builder()
                .eventFlushIntervalInMillis(999)
                .build();
    }

    @Test
    public void eventsFlushRateWorks() {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventFlushIntervalInMillis(1000)
                .build();
        Assert.assertEquals(1000, config.eventSendIntervalInMillis());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotSetMetricsRefreshRateToLessThan30() {
        SplitClientConfig.builder()
                .metricsRefreshRate(29)
                .build();
    }

    @Test
    public void canSetRefreshRatesTo30() {
        SplitClientConfig cfg = SplitClientConfig.builder()
                .featuresRefreshRate(30)
                .segmentsRefreshRate(30)
                .impressionsRefreshRate(65)
                .metricsRefreshRate(65)
                .build();
        Assert.assertEquals(30, cfg.featuresRefreshRate());
        Assert.assertEquals(30, cfg.segmentsRefreshRate());
        Assert.assertEquals(65, cfg.impressionsRefreshRate());
        Assert.assertEquals(65, cfg.metricsRefreshRate());
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
        Assert.assertEquals(1, cfg.streamingReconnectBackoffBase());
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

    @Test
    public void IntegrationConfigSyncNotNull() {
        SplitClientConfig config = SplitClientConfig.builder().integrations(IntegrationsConfig.builder()
                .impressionsListener(Mockito.mock(ImpressionListener.class), 500, IntegrationsConfig.Execution.SYNC)
                .build()).build();
        Assert.assertNotNull(config.integrationsConfig());
        Assert.assertEquals(1, config.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.SYNC).size());
        Assert.assertEquals(0, config.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.ASYNC).size());
    }

    @Test
    public void IntegrationConfigAsyncNotNull() {
        SplitClientConfig config = SplitClientConfig.builder().integrations(IntegrationsConfig.builder()
                .impressionsListener(Mockito.mock(ImpressionListener.class), 500, IntegrationsConfig.Execution.ASYNC)
                .build()).build();
        Assert.assertNotNull(config.integrationsConfig());
        Assert.assertEquals(0, config.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.SYNC).size());
        Assert.assertEquals(1, config.integrationsConfig().getImpressionsListeners(IntegrationsConfig.Execution.ASYNC).size());
    }

    @Test
    public void checkUserCustomdHeaderDecorator() {
        CustomHeaderDecorator ucd = new CustomHeaderDecorator() {
            @Override
            public Map<String, List<String>> getHeaderOverrides(RequestContext context) {
                return null;
            }
        };
        SplitClientConfig config = SplitClientConfig.builder().customHeaderDecorator(ucd).build();
        Assert.assertNotNull(config.customHeaderDecorator());
        Assert.assertEquals(ucd, config.customHeaderDecorator());

        SplitClientConfig config2 = SplitClientConfig.builder().build();
        Assert.assertNull(config2.customHeaderDecorator());
    }

    @Test
    public void checkProxyParams() throws MalformedURLException, FileNotFoundException {
        SplitClientConfig config = SplitClientConfig.builder()
                .proxyConfiguration(new ProxyConfiguration.Builder()
                        .url(new URL("https://proxy-host:8888"))
                        .build())
                .build();
        Assert.assertEquals("proxy-host", config.proxyConfiguration().getHost().getHostName());
        Assert.assertEquals(8888, config.proxyConfiguration().getHost().getPort());
        Assert.assertEquals("https", config.proxyConfiguration().getHost().getSchemeName());

        config = SplitClientConfig.builder()
                .proxyConfiguration(new ProxyConfiguration.Builder()
                        .url(new URL("https://proxy-host:888"))
                        .credentialsProvider(new io.split.client.dtos.BasicCredentialsProvider() {
                            @Override
                            public String getUsername() {
                                return "user";
                            }

                            @Override
                            public String getPassword() {
                                return "pass";
                            }
                        })
                        .build())
                .build();
        io.split.client.dtos.BasicCredentialsProvider basicAuth = (io.split.client.dtos.BasicCredentialsProvider) config.proxyConfiguration().getProxyCredentialsProvider();
        Assert.assertEquals("user", basicAuth.getUsername());
        Assert.assertEquals("pass", basicAuth.getPassword());

        io.split.client.dtos.BearerCredentialsProvider bearerCredentialsProvider = new io.split.client.dtos.BearerCredentialsProvider() {
            @Override
            public String getToken() {
                return "my-token";
            }
        };

        config = SplitClientConfig.builder()
                .proxyConfiguration(new ProxyConfiguration.Builder()
                        .url(new URL("https://proxy-host:888"))
                        .credentialsProvider(bearerCredentialsProvider)
                        .build())
                .build();
        Assert.assertEquals(bearerCredentialsProvider, config.proxyConfiguration().getProxyCredentialsProvider());
        FileInputStream p12File = new FileInputStream("src/test/resources/keyStore.p12");
        config = SplitClientConfig.builder()
                .proxyConfiguration(new ProxyConfiguration.Builder()
                        .url(new URL("https://proxy-host:888"))
                        .mtls(p12File, "pass-key")
                        .build())
                .build();
        Assert.assertEquals(p12File, config.proxyConfiguration().getP12File());
        Assert.assertEquals("pass-key", config.proxyConfiguration().getPassKey());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotUseInvalidHttpScheme() throws MalformedURLException {
        SplitClientConfig.builder()
                .proxyConfiguration(new ProxyConfiguration.Builder()
                        .url(new URL("ftp://proxy-host:888"))
                        .build())
                .build();
    }

    @Test(expected = MalformedURLException.class)
    public void cannotUseInvalidUrl() throws MalformedURLException {
        SplitClientConfig.builder()
                .proxyConfiguration(new ProxyConfiguration.Builder()
                        .url(new URL(""))
                        .build())
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustUseUrl() throws MalformedURLException {
        SplitClientConfig.builder()
                .proxyConfiguration(new ProxyConfiguration.Builder()
                        .build())
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustUseP12FileWithProxyMtls() throws MalformedURLException {
        SplitClientConfig.builder()
                .proxyConfiguration(new ProxyConfiguration.Builder()
                        .url(new URL("https://proxy-host:888"))
                        .mtls(null, "pass-key")
                        .build())
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustUseP12PassKeyWithProxyMtls() throws MalformedURLException, FileNotFoundException {
        SplitClientConfig.builder()
                .proxyConfiguration(new ProxyConfiguration.Builder()
                        .url(new URL("https://proxy-host:888"))
                        .mtls(new FileInputStream("src/test/resources/keyStore.p12"), null)
                        .build())
                .build();
    }

    @Test
    public void fallbackTreatmentCheckRegex() {
        SplitClientConfig config = SplitClientConfig.builder()
                .fallbackTreatments(new FallbackTreatmentsConfiguration(new FallbackTreatment("12#2"), null))
                .build();
        Assert.assertEquals(null, config.fallbackTreatments().getGlobalFallbackTreatment().getTreatment());

        config = SplitClientConfig.builder()
                .fallbackTreatments(new FallbackTreatmentsConfiguration(null, new HashMap<String, FallbackTreatment>() {{ put("flag", new FallbackTreatment("12#2")); }} ))
                .build();
        Assert.assertEquals(0, config.fallbackTreatments().getByFlagFallbackTreatment().size());

        config = SplitClientConfig.builder()
                .fallbackTreatments(new FallbackTreatmentsConfiguration(
                        new FallbackTreatment("on"),
                        new HashMap<String, FallbackTreatment>() {{ put("flag", new FallbackTreatment("off")); }} ))
                .build();
        Assert.assertEquals("on", config.fallbackTreatments().getGlobalFallbackTreatment().getTreatment());
        Assert.assertEquals("off", config.fallbackTreatments().getByFlagFallbackTreatment().get("flag").getTreatment());
    }
}