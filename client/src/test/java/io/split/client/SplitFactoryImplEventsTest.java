package io.split.client;

import io.split.client.api.SdkEvent;
import io.split.client.api.SdkEventMetadata;
import io.split.client.lifecycle.SdkEventsManager;
import io.split.client.lifecycle.SdkInternalEvent;
import io.split.storages.enums.OperationMode;
import org.junit.Test;
import pluggable.CustomStorageWrapper;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class SplitFactoryImplEventsTest {

    private static SdkEventsManager eventsManagerOf(Object target) throws Exception {
        Field field = target.getClass().getDeclaredField("_eventsManager");
        field.setAccessible(true);
        return (SdkEventsManager) field.get(target);
    }

    private static SplitFactoryImpl localhostFactory() throws Exception {
        return new SplitFactoryImpl("localhost", SplitClientConfig.builder().setBlockUntilReadyTimeout(10000).build());
    }

    @Test
    public void localhostFactoryBuildsOneManagerSharedWithTheClient() throws Exception {
        SplitFactoryImpl factory = localhostFactory();
        try {
            SdkEventsManager factoryManager = eventsManagerOf(factory);
            assertNotNull(factoryManager);
            assertSame(factoryManager, eventsManagerOf(factory.client()));
        } finally {
            factory.destroy();
        }
    }

    @Test
    public void destroyStopsDeliveryAndRegistrationAfterwardsIsNoOp() throws Exception {
        SplitFactoryImpl factory = localhostFactory();
        SdkEventsManager manager = eventsManagerOf(factory);
        AtomicInteger calls = new AtomicInteger();
        manager.on(SdkEvent.SDK_READY, metadata -> calls.incrementAndGet());

        factory.destroy();

        manager.on(SdkEvent.SDK_READY, metadata -> calls.incrementAndGet());
        manager.notifyInternalEvent(SdkInternalEvent.SDK_READY, SdkEventMetadata.ready(true, null));
        Thread.sleep(200);
        assertTrue(factory.isDestroyed());
        assertFalse(manager.eventAlreadyTriggered(SdkEvent.SDK_READY));
        org.junit.Assert.assertEquals(0, calls.get());
    }

    @Test
    public void destroyTwiceDoesNotThrow() throws Exception {
        SplitFactoryImpl factory = localhostFactory();
        factory.destroy();
        factory.destroy();
        assertTrue(factory.isDestroyed());
    }

    @Test
    public void localhostFactoryDeliversSdkReadyExactlyOnce() throws Exception {
        SplitFactoryImpl factory = new SplitFactoryImpl(SplitClientConfig.builder()
                .splitFile("src/test/resources/splits_localhost.json")
                .setBlockUntilReadyTimeout(10000)
                .build());
        try {
            AtomicInteger calls = new AtomicInteger();
            eventsManagerOf(factory).on(SdkEvent.SDK_READY, metadata -> calls.incrementAndGet());
            org.awaitility.Awaitility.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(() -> calls.get() == 1);
            Thread.sleep(300);
            org.junit.Assert.assertEquals(1, calls.get());
        } finally {
            factory.destroy();
        }
    }

    @Test
    public void consumerFactoryDeliversSdkReadyExactlyOnce() throws Exception {
        CustomStorageWrapper wrapper = mock(CustomStorageWrapper.class);
        org.mockito.Mockito.when(wrapper.connect()).thenReturn(true);
        SplitClientConfig config = SplitClientConfig.builder()
                .operationMode(OperationMode.CONSUMER)
                .customStorageWrapper(wrapper)
                .build();
        SplitFactoryImpl factory = new SplitFactoryImpl("token", config, wrapper);
        try {
            AtomicInteger calls = new AtomicInteger();
            eventsManagerOf(factory).on(SdkEvent.SDK_READY, metadata -> calls.incrementAndGet());
            org.awaitility.Awaitility.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(() -> calls.get() == 1);
            Thread.sleep(300);
            org.junit.Assert.assertEquals(1, calls.get());
        } finally {
            factory.destroy();
        }
    }

    @Test
    public void consumerFactoryBuildsAManagerAndDestroysIt() throws Exception {
        CustomStorageWrapper wrapper = mock(CustomStorageWrapper.class);
        SplitClientConfig config = SplitClientConfig.builder()
                .operationMode(OperationMode.CONSUMER)
                .customStorageWrapper(wrapper)
                .build();
        SplitFactoryImpl factory = new SplitFactoryImpl("token", config, wrapper);
        SdkEventsManager manager = eventsManagerOf(factory);
        assertNotNull(manager);
        assertSame(manager, eventsManagerOf(factory.client()));

        factory.destroy();
        factory.destroy();
        assertTrue(factory.isDestroyed());
        assertFalse(manager.eventAlreadyTriggered(SdkEvent.SDK_READY));
    }
}
