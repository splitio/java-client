package io.split.client;

import io.split.client.lifecycle.EventThreads;
import io.split.client.lifecycle.SdkLifecycleEventsIntegrationTest;
import io.split.client.api.SdkEvent;
import io.split.storages.enums.OperationMode;
import org.junit.After;
import org.junit.Test;
import pluggable.CustomStorageWrapper;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lifecycle events in the modes that have no sync engine producing updates: localhost and consumer. Both emit
 * SDK_READY (with the metadata of their mode) and neither ever emits SDK_UPDATE.
 */
public class SdkLifecycleEventsModesIntegrationTest {

    private static final long WAIT_SECONDS = 20L;
    private static final Duration QUIET_WINDOW = Duration.ofMillis(1500);

    private SplitFactoryImpl factory;

    @After
    public void destroyFactory() {
        if (factory != null) {
            factory.destroy();
        }
        EventThreads.awaitRecordedThreadsTerminated();
    }

    // Scenario: sdkReady fires once and replays to late subscribers (localhost mode).
    @Test
    public void localhostFiresSdkReadyOnceReplaysAndNeverRaisesAnUpdate() throws Exception {
        SplitClientConfig config = SplitClientConfig.builder()
                .splitFile("src/test/resources/splits_localhost.json")
                .setBlockUntilReadyTimeout(10000)
                .build();
        factory = new SplitFactoryImpl(config);
        SplitClient client = factory.client();
        SdkLifecycleEventsIntegrationTest.Recorder ready = new SdkLifecycleEventsIntegrationTest.Recorder();
        SdkLifecycleEventsIntegrationTest.Recorder updates = new SdkLifecycleEventsIntegrationTest.Recorder();
        client.on(SdkEvent.SDK_READY, ready);
        client.on(SdkEvent.SDK_UPDATE, updates);

        client.blockUntilReady();
        // localhost loads its file while the factory is constructed, so this subscriber may be a replay (no metadata)
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> ready.size() == 1);

        SdkLifecycleEventsIntegrationTest.Recorder late = new SdkLifecycleEventsIntegrationTest.Recorder();
        client.on(SdkEvent.SDK_READY, late);
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> late.size() == 1);
        assertNull(late.get(0));

        await().during(QUIET_WINDOW).atMost(WAIT_SECONDS, TimeUnit.SECONDS)
                .until(() -> ready.size() == 1 && updates.size() == 0);
    }

    // Scenario: destroy stops events and clears handlers (localhost mode).
    @Test
    public void localhostDestroyDropsHandlers() throws Exception {
        SplitClientConfig config = SplitClientConfig.builder()
                .splitFile("src/test/resources/splits_localhost.json")
                .setBlockUntilReadyTimeout(10000)
                .build();
        factory = new SplitFactoryImpl(config);
        SplitClient client = factory.client();
        client.blockUntilReady();
        factory.destroy();

        SdkLifecycleEventsIntegrationTest.Recorder afterDestroy = new SdkLifecycleEventsIntegrationTest.Recorder();
        client.on(SdkEvent.SDK_READY, afterDestroy);
        await().during(QUIET_WINDOW).atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> afterDestroy.size() == 0);
        assertTrue(factory.isDestroyed());
    }

    // Consumer mode emits SDK_READY (not from cache) but never SDK_UPDATE.
    @Test
    public void consumerFiresSdkReadyOnceWithoutCacheMetadataAndNeverRaisesAnUpdate() throws Exception {
        CustomStorageWrapper wrapper = mock(CustomStorageWrapper.class);
        when(wrapper.connect()).thenReturn(true);
        SplitClientConfig config = SplitClientConfig.builder()
                .operationMode(OperationMode.CONSUMER)
                .customStorageWrapper(wrapper)
                .setBlockUntilReadyTimeout(10000)
                .build();
        factory = new SplitFactoryImpl("fake-api-token", config, wrapper);
        SplitClient client = factory.client();
        SdkLifecycleEventsIntegrationTest.Recorder ready = new SdkLifecycleEventsIntegrationTest.Recorder();
        SdkLifecycleEventsIntegrationTest.Recorder updates = new SdkLifecycleEventsIntegrationTest.Recorder();
        client.on(SdkEvent.SDK_READY, ready);
        client.on(SdkEvent.SDK_UPDATE, updates);

        client.blockUntilReady();
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> ready.size() == 1);
        assertFalse(ready.get(0).isInitialCacheLoad());

        SdkLifecycleEventsIntegrationTest.Recorder late = new SdkLifecycleEventsIntegrationTest.Recorder();
        client.on(SdkEvent.SDK_READY, late);
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> late.size() == 1);
        await().during(QUIET_WINDOW).atMost(WAIT_SECONDS, TimeUnit.SECONDS)
                .until(() -> ready.size() == 1 && updates.size() == 0);
        assertEquals(1, late.size());
    }
}
