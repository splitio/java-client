package io.split.client;

import io.split.client.impressions.ImpressionsManager;
import io.split.engine.segments.SegmentSynchronizationTaskImp;
import io.split.integrations.IntegrationsConfig;
import io.split.telemetry.storage.TelemetryStorage;
import junit.framework.TestCase;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;

public class SplitFactoryImplTest extends TestCase {
    public static final String API_KEY ="29013ionasdasd09u";
    public static final String ENDPOINT = "https://sdk.split-stage.io";
    public static final String EVENTS_ENDPOINT = "https://events.split-stage.io";
    public static final String AUTH_SERVICE = "https://auth.split-stage.io/api/auth";


    @Test
    public void testFactoryInstantiation() throws Exception {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(10000)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig);

        assertNotNull(splitFactory.client());
        assertNotNull(splitFactory.manager());
    }

    @Test
    public void testFactoryInstantiationWithoutBlockUntilReady() throws Exception {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig);

        assertNotNull(splitFactory.client());
        assertNotNull(splitFactory.manager());
    }

    @Test
    public void testFactoryInstantiationIntegrationsConfig() throws Exception {
        IntegrationsConfig integrationsConfig = new IntegrationsConfig.Builder().build();
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(1000)
                .integrations(integrationsConfig)
                .build();
        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig);

        assertNotNull(splitFactory.client());
        assertNotNull(splitFactory.manager());
    }

    @Test
    public void testFactoryInstantiationWithProxy() throws Exception {
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
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
    public void testFactoryDestroy() throws Exception {
        TelemetryStorage telemetryStorage = Mockito.mock(TelemetryStorage.class);
        SplitClientConfig splitClientConfig = SplitClientConfig.builder()
                .enableDebug()
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .impressionsRefreshRate(1)
                .endpoint(ENDPOINT,EVENTS_ENDPOINT)
                .telemetryURL(SplitClientConfig.TELEMETRY_ENDPOINT)
                .authServiceURL(AUTH_SERVICE)
                .setBlockUntilReadyTimeout(10000)
                .build();

        SplitFactoryImpl splitFactory = new SplitFactoryImpl(API_KEY, splitClientConfig);
        //Before destroy we replace telemetryStorage via reflection.
        Field factoryDestroy = SplitFactoryImpl.class.getDeclaredField("_telemetryStorage");
        factoryDestroy.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(factoryDestroy, factoryDestroy.getModifiers() & ~Modifier.FINAL);

        factoryDestroy.set(splitFactory, telemetryStorage);
        splitFactory.destroy();

        assertTrue(splitFactory.isDestroyed());
        Mockito.verify(telemetryStorage, Mockito.times(1)).recordSessionLength(Mockito.anyLong());
    }

}