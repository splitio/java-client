package io.split.client;

import io.split.client.api.SdkEvent;
import io.split.client.lifecycle.SdkEventsManager;
import io.split.client.lifecycle.SdkEventsNotifier;
import io.split.client.lifecycle.SdkInternalEvent;
import io.split.engine.SDKReadinessGates;
import io.split.engine.evaluator.Evaluator;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.dtos.FallbackTreatmentCalculator;
import io.split.client.events.EventsStorageProducer;
import io.split.storages.SplitCacheConsumer;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class SplitTimeoutEventsTest {

    private static final SplitClientConfig CONFIG = SplitClientConfig.builder().setBlockUntilReadyTimeout(50).build();

    private static SplitClientImpl client(SDKReadinessGates gates, SdkEventsManager manager) {
        TelemetryStorage telemetry = mock(TelemetryStorage.class);
        return new SplitClientImpl(mock(SplitFactory.class), mock(SplitCacheConsumer.class), mock(ImpressionsManager.class),
                mock(EventsStorageProducer.class), CONFIG, gates, mock(Evaluator.class), telemetry, telemetry,
                mock(FlagSetsFilter.class), mock(FallbackTreatmentCalculator.class), manager);
    }

    @Test
    public void clientBlockUntilReadyTimeoutEmitsTimedOutOnceWithNullMetadata() throws Exception {
        SdkEventsManager manager = SdkEventsManager.create(null);
        AtomicInteger timedOut = new AtomicInteger();
        AtomicInteger ready = new AtomicInteger();
        manager.on(SdkEvent.SDK_READY_TIMED_OUT, metadata -> {
            assertNull(metadata);
            timedOut.incrementAndGet();
        });
        manager.on(SdkEvent.SDK_READY, metadata -> ready.incrementAndGet());
        SplitClientImpl client = client(new SDKReadinessGates(), manager);

        for (int i = 0; i < 2; i++) {
            try {
                client.blockUntilReady();
                org.junit.Assert.fail("expected TimeoutException");
            } catch (TimeoutException expected) {
                // behaviour unchanged
            }
        }

        await().atMost(2, TimeUnit.SECONDS).until(() -> timedOut.get() == 1);
        Thread.sleep(200);
        assertEquals(1, timedOut.get());
        assertEquals(0, ready.get());
        manager.destroy();
    }

    @Test
    public void readinessWinningTheRaceSuppressesTimedOut() throws Exception {
        SdkEventsManager manager = SdkEventsManager.create(null);
        AtomicInteger timedOut = new AtomicInteger();
        AtomicInteger ready = new AtomicInteger();
        manager.on(SdkEvent.SDK_READY_TIMED_OUT, metadata -> timedOut.incrementAndGet());
        manager.on(SdkEvent.SDK_READY, metadata -> ready.incrementAndGet());

        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY, io.split.client.api.SdkEventMetadata.ready(true, null));
        await().atMost(2, TimeUnit.SECONDS).until(() -> ready.get() == 1);
        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY_TIMEOUT_REACHED, null);

        Thread.sleep(300);
        assertEquals(1, ready.get());
        assertEquals(0, timedOut.get());
        manager.destroy();
    }

    @Test
    public void managerBlockUntilReadyTimeoutNotifiesTimeoutReached() throws Exception {
        SdkEventsNotifier notifier = mock(SdkEventsNotifier.class);
        SplitManagerImpl manager = new SplitManagerImpl(mock(SplitCacheConsumer.class), CONFIG, new SDKReadinessGates(),
                mock(TelemetryStorage.class), notifier);

        try {
            manager.blockUntilReady();
            org.junit.Assert.fail("expected TimeoutException");
        } catch (TimeoutException expected) {
            // behaviour unchanged
        }

        verify(notifier).notify(eq(SdkInternalEvent.SDK_READY_TIMEOUT_REACHED), eq(null));
    }

    @Test
    public void blockUntilReadyThatSucceedsNotifiesNothing() throws Exception {
        SDKReadinessGates gates = new SDKReadinessGates();
        gates.sdkInternalReady();
        SdkEventsNotifier notifier = mock(SdkEventsNotifier.class);
        new SplitManagerImpl(mock(SplitCacheConsumer.class), CONFIG, gates, mock(TelemetryStorage.class), notifier)
                .blockUntilReady();
        verifyZeroInteractions(notifier);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonPositiveTimeoutStillThrowsIllegalArgument() throws Exception {
        SdkEventsNotifier notifier = mock(SdkEventsNotifier.class);
        try {
            new SplitManagerImpl(mock(SplitCacheConsumer.class), SplitClientConfig.builder().build(), new SDKReadinessGates(),
                    mock(TelemetryStorage.class), notifier).blockUntilReady();
        } finally {
            verify(notifier, never()).notify(any(SdkInternalEvent.class), any(io.split.client.api.SdkEventMetadata.class));
        }
    }
}
