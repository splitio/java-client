package io.split.client;

import io.split.client.impressions.ImpressionsManager;
import io.split.integrations.IntegrationsConfig;
import junit.framework.TestCase;
import org.junit.Test;

import java.net.URISyntaxException;

public class SplitFactoryImplTest extends TestCase {
    public static final String API_KEY ="29013ionasdasd09u";
    public static final String ENDPOINT = "https://sdk.split-stage.io";
    public static final String EVENTS_ENDPOINT = "https://events.split-stage.io";
    public static final String AUTH_SERVICE = "https://auth.split-stage.io/api/auth";


    @Test
    public void testFactoryInstantiation() throws URISyntaxException {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(10000)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig);

        assertNotNull(splitFactory.client());
        assertNotNull(splitFactory.manager());
    }

    @Test
    public void testFactoryInstantiationWithoutBlockUntilReady() throws URISyntaxException {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig);

        assertNotNull(splitFactory.client());
        assertNotNull(splitFactory.manager());
    }

    @Test
    public void testFactoryInstantiationIntegrationsConfig() throws URISyntaxException {
        IntegrationsConfig integrationsConfig = new IntegrationsConfig.Builder().build();
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(1000)
                .integrations(integrationsConfig)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig);

        assertNotNull(splitFactory.client());
        assertNotNull(splitFactory.manager());
    }

    @Test
    public void testFactoryInstantiationWithProxy() throws URISyntaxException {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(1000)
                .proxyPort(6060)
                .proxyUsername("test")
                .proxyPassword("password")
                .proxyHost(ENDPOINT)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig);

        assertNotNull(splitFactory.client());
        assertNotNull(splitFactory.manager());
    }

    @Test
    public void testFactoryDestroy() throws URISyntaxException {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(10000)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig);
        splitFactory.destroy();

        assertTrue(splitFactory.isDestroyed());
    }

}