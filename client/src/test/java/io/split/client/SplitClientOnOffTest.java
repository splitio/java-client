package io.split.client;

import io.split.client.api.SdkEvent;
import io.split.client.api.SdkEventListener;
import io.split.client.api.SdkEventMetadata;
import io.split.client.api.SdkEventType;
import io.split.client.dtos.FallbackTreatmentCalculator;
import io.split.client.events.EventsStorageProducer;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.lifecycle.SdkEventsManager;
import io.split.client.lifecycle.SdkInternalEvent;
import io.split.engine.SDKReadinessGates;
import io.split.engine.evaluator.Evaluator;
import io.split.storages.SplitCacheConsumer;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class SplitClientOnOffTest {

    private SdkEventsManager _manager;
    private SplitClientImpl _client;

    @Before
    public void setUp() {
        _manager = SdkEventsManager.create(null);
        TelemetryStorage telemetry = mock(TelemetryStorage.class);
        _client = new SplitClientImpl(mock(SplitFactory.class), mock(SplitCacheConsumer.class), mock(ImpressionsManager.class),
                mock(EventsStorageProducer.class), SplitClientConfig.builder().build(), new SDKReadinessGates(),
                mock(Evaluator.class), telemetry, telemetry, mock(FlagSetsFilter.class),
                mock(FallbackTreatmentCalculator.class), _manager);
    }

    @After
    public void tearDown() {
        _manager.destroy();
    }

    private void becomeReady() {
        _manager.notifyInternalEvent(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, null));
    }

    private void flagUpdate(String name) {
        _manager.notifyInternalEvent(SdkInternalEvent.FLAGS_UPDATED,
                SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Collections.singleton(name)));
    }

    @Test
    public void multipleListenersAndDuplicatesAreAllInvokedInOrder() {
        becomeReady();
        List<String> calls = new CopyOnWriteArrayList<>();
        SdkEventListener shared = metadata -> calls.add("shared");
        _client.on(SdkEvent.SDK_UPDATE, shared);
        _client.on(SdkEvent.SDK_UPDATE, shared);
        _client.on(SdkEvent.SDK_UPDATE, metadata -> calls.add("other"));

        flagUpdate("f1");

        await().atMost(2, TimeUnit.SECONDS).until(() -> calls.size() == 3);
        assertEquals(Arrays.asList("shared", "shared", "other"), calls);
    }

    @Test
    public void runnableOverloadRunsWithoutMetadata() {
        becomeReady();
        AtomicInteger calls = new AtomicInteger();
        _client.on(SdkEvent.SDK_UPDATE, calls::incrementAndGet);

        flagUpdate("f1");

        await().atMost(2, TimeUnit.SECONDS).until(() -> calls.get() == 1);
    }

    @Test
    public void listenerReceivesTheUpdateMetadata() {
        becomeReady();
        List<SdkEventMetadata> received = new CopyOnWriteArrayList<>();
        _client.on(SdkEvent.SDK_UPDATE, received::add);

        flagUpdate("f1");

        await().atMost(2, TimeUnit.SECONDS).until(() -> received.size() == 1);
        assertEquals(SdkEventType.FLAGS_UPDATE, received.get(0).getType());
        assertEquals(Collections.singleton("f1"), received.get(0).getNames());
    }

    @Test
    public void lateSdkReadySubscriberIsReplayedWithNullMetadata() {
        becomeReady();
        await().atMost(2, TimeUnit.SECONDS).until(() -> _manager.eventAlreadyTriggered(SdkEvent.SDK_READY));
        List<SdkEventMetadata> received = new CopyOnWriteArrayList<>();
        _client.on(SdkEvent.SDK_READY, received::add);

        await().atMost(2, TimeUnit.SECONDS).until(() -> received.size() == 1);
        assertNull(received.get(0));
    }

    @Test
    public void offRemovesOnlyTheGivenEvent() throws InterruptedException {
        AtomicInteger updates = new AtomicInteger();
        AtomicInteger ready = new AtomicInteger();
        _client.on(SdkEvent.SDK_UPDATE, updates::incrementAndGet);
        _client.on(SdkEvent.SDK_READY, ready::incrementAndGet);

        _client.off(SdkEvent.SDK_UPDATE);
        becomeReady();
        await().atMost(2, TimeUnit.SECONDS).until(() -> ready.get() == 1);
        flagUpdate("f1");

        Thread.sleep(200);
        assertEquals(0, updates.get());
    }

    @Test
    public void registeringAfterDestroyIsASilentNoOp() throws InterruptedException {
        _manager.destroy();
        AtomicInteger calls = new AtomicInteger();

        _client.on(SdkEvent.SDK_READY, calls::incrementAndGet);
        _client.on(SdkEvent.SDK_READY, metadata -> calls.incrementAndGet());
        _client.off(SdkEvent.SDK_READY);
        becomeReady();

        Thread.sleep(200);
        assertEquals(0, calls.get());
    }

    @Test
    public void nullArgumentsAreIgnoredWithoutThrowing() throws InterruptedException {
        becomeReady();
        AtomicInteger calls = new AtomicInteger();

        _client.on(null, calls::incrementAndGet);
        _client.on(null, (SdkEventListener) metadata -> calls.incrementAndGet());
        _client.on(SdkEvent.SDK_UPDATE, (Runnable) null);
        _client.on(SdkEvent.SDK_UPDATE, (SdkEventListener) null);
        _client.off(null);
        flagUpdate("f1");

        Thread.sleep(200);
        assertEquals(0, calls.get());
    }

    @Test
    public void clientWithoutAnEventsManagerIgnoresRegistrations() {
        TelemetryStorage telemetry = mock(TelemetryStorage.class);
        SplitClientImpl client = new SplitClientImpl(mock(SplitFactory.class), mock(SplitCacheConsumer.class),
                mock(ImpressionsManager.class), mock(EventsStorageProducer.class), SplitClientConfig.builder().build(),
                new SDKReadinessGates(), mock(Evaluator.class), telemetry, telemetry, mock(FlagSetsFilter.class),
                mock(FallbackTreatmentCalculator.class));

        client.on(SdkEvent.SDK_READY, () -> { });
        client.off(SdkEvent.SDK_READY);
    }

    @Test
    public void noCommonsTypeIsReachableFromThePublicApi() {
        for (Method method : SplitClient.class.getMethods()) {
            assertFalse(method.getName(), method.getReturnType().getName().startsWith("io.harness"));
            for (Class<?> parameter : method.getParameterTypes()) {
                assertFalse(method.getName(), parameter.getName().startsWith("io.harness"));
            }
        }
    }
}
