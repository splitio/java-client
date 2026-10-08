package io.split.client.lifecycle;

import io.split.client.api.SdkEvent;
import io.split.client.api.SdkEventMetadata;
import io.split.client.api.SdkEventType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SdkEventsManagerTest {

    @Test
    public void listenersOnSameEventFireInRegistrationOrder() {
        SdkEventsManager manager = SdkEventsManager.create(null);
        List<Integer> order = new CopyOnWriteArrayList<>();
        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, null));
        manager.on(SdkEvent.SDK_UPDATE, metadata -> order.add(1));
        manager.on(SdkEvent.SDK_UPDATE, metadata -> order.add(2));
        manager.on(SdkEvent.SDK_UPDATE, metadata -> order.add(3));

        manager.notifyInternalEvent(SdkInternalEvent.FLAGS_UPDATED, SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("a")));

        await().atMost(2, TimeUnit.SECONDS).until(() -> order.size() == 3);
        assertEquals(Arrays.asList(1, 2, 3), order);
        manager.destroy();
    }

    @Test
    public void aThrowingListenerDoesNotBlockOthers() {
        SdkEventsManager manager = SdkEventsManager.create(null);
        AtomicInteger secondCalls = new AtomicInteger();
        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, null));
        manager.on(SdkEvent.SDK_UPDATE, metadata -> {
            throw new RuntimeException("boom");
        });
        manager.on(SdkEvent.SDK_UPDATE, metadata -> secondCalls.incrementAndGet());

        manager.notifyInternalEvent(SdkInternalEvent.FLAGS_UPDATED, SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("a")));

        await().atMost(2, TimeUnit.SECONDS).until(() -> secondCalls.get() == 1);
        manager.destroy();
    }

    @Test
    public void sdkReadyFiresOnceAndReplaysNullToLateSubscriber() {
        SdkEventsManager manager = SdkEventsManager.create(null);
        List<SdkEventMetadata> earlyReceived = new CopyOnWriteArrayList<>();
        manager.on(SdkEvent.SDK_READY, earlyReceived::add);

        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, 123L));
        await().atMost(2, TimeUnit.SECONDS).until(() -> earlyReceived.size() == 1);
        assertThat(earlyReceived.get(0).isInitialCacheLoad(), is(true));

        List<SdkEventMetadata> lateReceived = new CopyOnWriteArrayList<>();
        manager.on(SdkEvent.SDK_READY, lateReceived::add);
        await().atMost(2, TimeUnit.SECONDS).until(() -> lateReceived.size() == 1);
        assertNull(lateReceived.get(0));
        manager.destroy();
    }

    @Test
    public void sdkUpdateNeverReplaysToLateSubscriber() throws InterruptedException {
        SdkEventsManager manager = SdkEventsManager.create(null);
        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, null));
        manager.notifyInternalEvent(SdkInternalEvent.FLAGS_UPDATED, SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("a")));
        // give the first update time to be fully delivered before the late listener registers
        Thread.sleep(200);

        AtomicInteger lateCalls = new AtomicInteger();
        manager.on(SdkEvent.SDK_UPDATE, metadata -> lateCalls.incrementAndGet());
        Thread.sleep(200);
        assertEquals(0, lateCalls.get());

        manager.notifyInternalEvent(SdkInternalEvent.FLAGS_UPDATED, SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("b")));
        await().atMost(2, TimeUnit.SECONDS).until(() -> lateCalls.get() == 1);
        manager.destroy();
    }

    @Test
    public void sdkUpdateRequiresSdkReadyFirst() throws InterruptedException {
        SdkEventsManager manager = SdkEventsManager.create(null);
        AtomicInteger updateCalls = new AtomicInteger();
        manager.on(SdkEvent.SDK_UPDATE, metadata -> updateCalls.incrementAndGet());

        manager.notifyInternalEvent(SdkInternalEvent.FLAGS_UPDATED, SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("a")));
        Thread.sleep(200);
        assertEquals(0, updateCalls.get());

        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, null));
        Thread.sleep(200);
        assertEquals(0, updateCalls.get());

        manager.notifyInternalEvent(SdkInternalEvent.FLAGS_UPDATED, SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("a")));
        await().atMost(2, TimeUnit.SECONDS).until(() -> updateCalls.get() == 1);
        manager.destroy();
    }

    @Test
    public void sdkUpdateMetadataReflectsWhicheverInternalEventFired() {
        SdkEventsManager flagsManager = SdkEventsManager.create(null);
        List<SdkEventMetadata> flagsReceived = new CopyOnWriteArrayList<>();
        flagsManager.notifyInternalEvent(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, null));
        flagsManager.on(SdkEvent.SDK_UPDATE, flagsReceived::add);
        flagsManager.notifyInternalEvent(SdkInternalEvent.FLAGS_UPDATED, SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("a")));
        await().atMost(2, TimeUnit.SECONDS).until(() -> flagsReceived.size() == 1);
        assertEquals(SdkEventType.FLAGS_UPDATE, flagsReceived.get(0).getType());
        assertEquals(Collections.singleton("a"), flagsReceived.get(0).getNames());
        flagsManager.destroy();

        SdkEventsManager segmentsManager = SdkEventsManager.create(null);
        List<SdkEventMetadata> segmentsReceived = new CopyOnWriteArrayList<>();
        segmentsManager.notifyInternalEvent(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, null));
        segmentsManager.on(SdkEvent.SDK_UPDATE, segmentsReceived::add);
        segmentsManager.notifyInternalEvent(SdkInternalEvent.SEGMENTS_UPDATED, SdkEventMetadata.update(SdkEventType.SEGMENTS_UPDATE, Collections.emptyList()));
        await().atMost(2, TimeUnit.SECONDS).until(() -> segmentsReceived.size() == 1);
        assertEquals(SdkEventType.SEGMENTS_UPDATE, segmentsReceived.get(0).getType());
        segmentsManager.destroy();
    }

    @Test
    public void offClearsListenersButDispatcherKeepsWorking() {
        SdkEventsManager manager = SdkEventsManager.create(null);
        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, null));

        AtomicInteger firstListenerCalls = new AtomicInteger();
        manager.on(SdkEvent.SDK_UPDATE, metadata -> firstListenerCalls.incrementAndGet());
        manager.notifyInternalEvent(SdkInternalEvent.FLAGS_UPDATED, SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("a")));
        await().atMost(2, TimeUnit.SECONDS).until(() -> firstListenerCalls.get() == 1);

        manager.off(SdkEvent.SDK_UPDATE);
        AtomicInteger secondListenerCalls = new AtomicInteger();
        manager.on(SdkEvent.SDK_UPDATE, metadata -> secondListenerCalls.incrementAndGet());

        manager.notifyInternalEvent(SdkInternalEvent.FLAGS_UPDATED, SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("b")));
        await().atMost(2, TimeUnit.SECONDS).until(() -> secondListenerCalls.get() == 1);
        assertEquals(1, firstListenerCalls.get());
        manager.destroy();
    }

    @Test
    public void registrationIsNoOpAfterDestroy() throws InterruptedException {
        SdkEventsManager manager = SdkEventsManager.create(null);
        manager.destroy();

        AtomicInteger calls = new AtomicInteger();
        manager.on(SdkEvent.SDK_READY, metadata -> calls.incrementAndGet());
        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, null));
        Thread.sleep(200);
        assertEquals(0, calls.get());
    }

    @Test
    public void notifyAndTriggeredQueryAfterDestroyAreNoOps() {
        SdkEventsManager manager = SdkEventsManager.create(null);
        manager.destroy();

        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, null));
        assertEquals(false, manager.eventAlreadyTriggered(SdkEvent.SDK_READY));
    }

    @Test
    public void offAfterDestroyIsNoOp() {
        SdkEventsManager manager = SdkEventsManager.create(null);
        manager.destroy();

        manager.off(SdkEvent.SDK_UPDATE);
    }

    @Test
    public void inFlightDispatchAfterDestroyIsDroppedSilently() throws Exception {
        SdkEventsManager manager = SdkEventsManager.create(null);
        manager.destroy();

        java.lang.reflect.Method dispatch = SdkEventsManager.class.getDeclaredMethod("dispatch", SdkEvent.class, SdkEventMetadata.class);
        dispatch.setAccessible(true);
        dispatch.invoke(manager, SdkEvent.SDK_UPDATE, null);
    }

    @Test
    public void deliveryAfterExecutorShutdownIsDroppedWithoutThrowing() throws Exception {
        SdkEventsManager manager = SdkEventsManager.create(null);
        AtomicInteger readyCalls = new AtomicInteger();
        manager.on(SdkEvent.SDK_READY, metadata -> readyCalls.incrementAndGet());
        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, null));
        await().atMost(2, TimeUnit.SECONDS).until(() -> readyCalls.get() == 1);

        java.lang.reflect.Field executorField = SdkEventsManager.class.getDeclaredField("deliveryExecutor");
        executorField.setAccessible(true);
        ((java.util.concurrent.ExecutorService) executorField.get(manager)).shutdown();

        // replay path: delivered already contains SDK_READY, so on() submits to the shut-down executor
        manager.on(SdkEvent.SDK_READY, metadata -> { });
        // trigger path: commons calls deliverAsync, which submits to the shut-down executor
        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY_TIMEOUT_REACHED, null);
        Thread.sleep(200);
        manager.destroy();
    }

    @Test
    public void concurrentRegistrationRacingATriggerDeliversExactlyOnceEach() throws InterruptedException {
        SdkEventsManager manager = SdkEventsManager.create(null);
        int listenerCount = 50;
        AtomicInteger totalCalls = new AtomicInteger();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(listenerCount);

        for (int i = 0; i < listenerCount; i++) {
            Thread t = new Thread(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                manager.on(SdkEvent.SDK_READY, metadata -> totalCalls.incrementAndGet());
                doneLatch.countDown();
            });
            t.start();
        }

        startLatch.countDown();
        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, null));
        doneLatch.await(5, TimeUnit.SECONDS);

        await().atMost(3, TimeUnit.SECONDS).until(() -> totalCalls.get() == listenerCount);
        assertEquals(listenerCount, totalCalls.get());
        manager.destroy();
    }

    @Test
    public void sdkReadyTimedOutDeliversNullMetadata() {
        SdkEventsManager manager = SdkEventsManager.create(null);
        List<SdkEventMetadata> received = new CopyOnWriteArrayList<>();
        manager.on(SdkEvent.SDK_READY_TIMED_OUT, received::add);

        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY_TIMEOUT_REACHED, null);

        await().atMost(2, TimeUnit.SECONDS).until(() -> received.size() == 1);
        assertNull(received.get(0));
        manager.destroy();
    }
}
