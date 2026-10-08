package io.split.client.lifecycle;

import io.split.SSEMockServer;
import io.split.SplitMockServer;
import io.split.client.SplitClient;
import io.split.client.SplitClientConfig;
import io.split.client.SplitFactory;
import io.split.client.SplitFactoryBuilder;
import io.split.client.api.SdkEvent;
import io.split.client.api.SdkEventMetadata;
import io.split.client.api.SdkEventType;
import org.glassfish.grizzly.utils.Pair;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.split.client.lifecycle.LifecycleScriptDispatcher.SEGMENT;
import static io.split.client.lifecycle.LifecycleScriptDispatcher.flag;
import static io.split.client.lifecycle.LifecycleScriptDispatcher.flagsBody;
import static io.split.client.lifecycle.LifecycleScriptDispatcher.ruleBasedSegment;
import static io.split.client.lifecycle.LifecycleScriptDispatcher.segmentChange;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * End-to-end tests for the SDK lifecycle events (SDK_READY, SDK_UPDATE, SDK_READY_TIMED_OUT) driving a real standalone
 * {@link SplitFactory} against a scripted mock backend, over both polling and streaming. Localhost and consumer modes
 * are covered by {@link SdkLifecycleEventsModesIntegrationTest}.
 *
 * <p>Out of scope here because they only exist in the client-side SDKs: the "sdkReadyFromCache" scenario and the
 * per-client fan-out scenario (EventsManagerCoordinator).
 */
@RunWith(Parameterized.class)
public class SdkLifecycleEventsIntegrationTest {

    private static final long INITIAL_CN = 100L;
    private static final long UPDATED_CN = 200L;
    private static final long SEGMENT_CN = 10L;
    private static final long SEGMENT_UPDATED_CN = 20L;
    private static final long WAIT_SECONDS = 20L;
    // The segment refresh rate has a 30 second floor, so a polled segment change takes up to that long to arrive.
    private static final long SEGMENT_POLL_WAIT_SECONDS = 70L;
    private static final Duration QUIET_WINDOW = Duration.ofMillis(1500);

    @Parameterized.Parameters(name = "streaming={0}")
    public static Collection<Object[]> modes() {
        return Arrays.asList(new Object[][] {{false}, {true}});
    }

    private final boolean streaming;
    private LifecycleScriptDispatcher backend;
    private SplitMockServer splitServer;
    private SSEMockServer sseServer;
    private SSEMockServer.SseEventQueue sseQueue;
    private SplitFactory factory;

    public SdkLifecycleEventsIntegrationTest(boolean streaming) {
        this.streaming = streaming;
    }

    @Before
    public void startBackend() throws Exception {
        newBackend(0);
    }

    @After
    public void stopEverything() throws Exception {
        if (factory != null) {
            factory.destroy();
        }
        if (sseServer != null) {
            sseServer.stop();
        }
        if (splitServer != null) {
            splitServer.stop();
        }
        // No leaked event-delivery thread once the factory is destroyed (threads that delivered to this test's listeners).
        EventThreads.awaitRecordedThreadsTerminated();
    }

    // Scenario: sdkReady fires once the SDK is ready, and replays to a subscriber that registers after the fact.
    @Test
    public void sdkReadyFiresOnceAndReplaysToLateSubscribers() throws Exception {
        SplitClient client = buildClient(10000);
        Recorder early = new Recorder();
        client.on(SdkEvent.SDK_READY, early);

        client.blockUntilReady();
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> early.size() == 1);

        Recorder late = new Recorder();
        client.on(SdkEvent.SDK_READY, late);
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> late.size() == 1);
        assertNull("a replayed SDK_READY carries no metadata", late.get(0));

        // later syncs (and later registrations) never fire it again
        publishFlagChange(flagsBody(INITIAL_CN, UPDATED_CN, flag("f1", UPDATED_CN, true), -1, -1, ""));
        await().during(QUIET_WINDOW).atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> early.size() == 1);
        assertEquals(1, late.size());
    }

    // Scenario: sdkUpdate fires only after sdkReady (the initial load is not an update).
    @Test
    public void sdkUpdateIsNotRaisedByTheInitialLoadOnlyByLaterChanges() throws Exception {
        SplitClient client = buildClient(10000);
        Recorder ready = new Recorder();
        Recorder updates = new Recorder();
        client.on(SdkEvent.SDK_READY, ready);
        client.on(SdkEvent.SDK_UPDATE, updates);

        client.blockUntilReady();
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> ready.size() == 1);
        await().during(QUIET_WINDOW).atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> updates.size() == 0);

        publishFlagChange(flagsBody(INITIAL_CN, UPDATED_CN, flag("f1", UPDATED_CN, true), -1, -1, ""));
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> updates.size() == 1);
    }

    // Scenario: sdkUpdate does not replay to a subscriber that registers after an update already happened.
    @Test
    public void sdkUpdateDoesNotReplayToLateSubscribers() throws Exception {
        SplitClient client = buildClient(10000);
        Recorder first = new Recorder();
        client.on(SdkEvent.SDK_UPDATE, first);
        client.blockUntilReady();

        publishFlagChange(flagsBody(INITIAL_CN, UPDATED_CN, flag("f1", UPDATED_CN, true), -1, -1, ""));
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> first.size() == 1);

        Recorder late = new Recorder();
        client.on(SdkEvent.SDK_UPDATE, late);
        await().during(QUIET_WINDOW).atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> late.size() == 0);
        assertEquals(1, first.size());
    }

    // Scenario: sdkReadyTimedOut fires when the SDK does not become ready within the timeout.
    @Test
    public void sdkReadyTimedOutFiresWhenTheSdkIsNotReadyInTime() throws Exception {
        stopBackend();
        newBackend(4000);
        SplitClient client = buildClient(300);
        Recorder timedOut = new Recorder();
        Recorder ready = new Recorder();
        client.on(SdkEvent.SDK_READY_TIMED_OUT, timedOut);
        client.on(SdkEvent.SDK_READY, ready);

        try {
            client.blockUntilReady();
            fail("the backend is slower than the timeout, blockUntilReady must time out");
        } catch (TimeoutException expected) {
            // expected
        }

        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> timedOut.size() == 1);
        assertNull(timedOut.get(0));
        assertEquals(0, ready.size());

        Recorder lateTimedOut = new Recorder();
        client.on(SdkEvent.SDK_READY_TIMED_OUT, lateTimedOut);
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> lateTimedOut.size() == 1);
    }

    // Scenario: sdkReadyTimedOut is suppressed when sdkReady wins.
    @Test
    public void sdkReadyTimedOutIsSuppressedWhenTheSdkIsReady() throws Exception {
        SplitClient client = buildClient(10000);
        Recorder ready = new Recorder();
        Recorder timedOut = new Recorder();
        client.on(SdkEvent.SDK_READY, ready);
        client.on(SdkEvent.SDK_READY_TIMED_OUT, timedOut);

        client.blockUntilReady();
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> ready.size() == 1);

        // already ready: another wait neither times out nor raises the event
        client.blockUntilReady();
        await().during(QUIET_WINDOW).atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> timedOut.size() == 0);
    }

    // Scenario: listeners on one event are invoked sequentially, in registration order, with error isolation.
    @Test
    public void listenersRunSequentiallyInRegistrationOrderAndAThrowingOneDoesNotBlockTheRest() throws Exception {
        SplitClient client = buildClient(10000);
        List<String> order = new CopyOnWriteArrayList<>();
        List<String> threads = new CopyOnWriteArrayList<>();
        client.on(SdkEvent.SDK_UPDATE, metadata -> {
            order.add("first");
            threads.add(Thread.currentThread().getName());
            EventThreads.record();
        });
        client.on(SdkEvent.SDK_UPDATE, metadata -> {
            order.add("throws");
            throw new IllegalStateException("listener failure under test");
        });
        client.on(SdkEvent.SDK_UPDATE, metadata -> {
            order.add("third");
            threads.add(Thread.currentThread().getName());
            EventThreads.record();
        });
        client.blockUntilReady();

        publishFlagChange(flagsBody(INITIAL_CN, UPDATED_CN, flag("f1", UPDATED_CN, true), -1, -1, ""));

        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> order.size() == 3);
        assertEquals(Arrays.asList("first", "throws", "third"), order);
        assertEquals("listeners share the single delivery thread", threads.get(0), threads.get(1));
        assertFalse("not delivered on the test thread", threads.get(0).equals(Thread.currentThread().getName()));
    }

    // Scenario: the metadata is exposed to handlers.
    @Test
    public void metadataIsExposedToHandlers() throws Exception {
        SplitClient client = buildClient(10000);
        Recorder ready = new Recorder();
        Recorder updates = new Recorder();
        client.on(SdkEvent.SDK_READY, ready);
        client.on(SdkEvent.SDK_UPDATE, updates);
        client.blockUntilReady();
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> ready.size() == 1);
        assertTrue(ready.get(0).isInitialCacheLoad());

        publishFlagChange(flagsBody(INITIAL_CN, UPDATED_CN, flag("f1", UPDATED_CN, true), -1, -1, ""));
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> updates.size() == 1);

        assertEquals(SdkEventType.FLAGS_UPDATE, updates.get(0).getType());
        assertEquals(Collections.singleton("f1"), updates.get(0).getNames());
    }

    // Scenario: destroy stops events and clears the handlers.
    @Test
    public void destroyStopsEventsAndClearsHandlers() throws Exception {
        SplitClient client = buildClient(10000);
        Recorder updates = new Recorder();
        client.on(SdkEvent.SDK_UPDATE, updates);
        client.blockUntilReady();

        factory.destroy();

        // events published after destroy never reach the dropped handlers
        publishFlagChange(flagsBody(INITIAL_CN, UPDATED_CN, flag("f1", UPDATED_CN, true), -1, -1, ""));
        Recorder afterDestroy = new Recorder();
        client.on(SdkEvent.SDK_UPDATE, afterDestroy);
        client.on(SdkEvent.SDK_READY, afterDestroy);
        client.off(SdkEvent.SDK_UPDATE);
        await().during(QUIET_WINDOW).atMost(WAIT_SECONDS, TimeUnit.SECONDS)
                .until(() -> updates.size() == 0 && afterDestroy.size() == 0);
        assertTrue(factory.isDestroyed());
    }

    // Appendix 4, case D: a polled response carrying flags and rule-based segments raises exactly one SDK_UPDATE,
    // typed FLAGS_UPDATE.
    @Test
    public void flagsAndRuleBasedSegmentsInOneResponseRaiseExactlyOneFlagsUpdate() throws Exception {
        SplitClient client = buildClient(10000);
        Recorder updates = new Recorder();
        client.on(SdkEvent.SDK_UPDATE, updates);
        client.blockUntilReady();

        publishFlagChange(flagsBody(INITIAL_CN, UPDATED_CN, flag("f2", UPDATED_CN, false),
                -1, UPDATED_CN, ruleBasedSegment("rbs1", UPDATED_CN)));

        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> updates.size() >= 1);
        await().during(QUIET_WINDOW).atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> updates.size() == 1);
        assertEquals(SdkEventType.FLAGS_UPDATE, updates.get(0).getType());
        assertEquals(Collections.singleton("f2"), updates.get(0).getNames());
    }

    // Appendix 4, case E: a polled segment change raises exactly one SDK_UPDATE, typed SEGMENTS_UPDATE.
    @Test
    public void aSegmentChangeRaisesExactlyOneSegmentsUpdate() throws Exception {
        SplitClient client = buildClient(10000);
        Recorder updates = new Recorder();
        client.on(SdkEvent.SDK_UPDATE, updates);
        client.blockUntilReady();

        backend.scriptSegment(SEGMENT_CN, segmentChange(SEGMENT_CN, SEGMENT_UPDATED_CN, "b", ""));
        if (streaming) {
            pushSse("xxxx_xxxx_segments", "{\\\"type\\\":\\\"SEGMENT_UPDATE\\\",\\\"changeNumber\\\":" + SEGMENT_UPDATED_CN
                    + ",\\\"segmentName\\\":\\\"" + SEGMENT + "\\\"}");
        }

        await().atMost(SEGMENT_POLL_WAIT_SECONDS, TimeUnit.SECONDS).until(() -> updates.size() >= 1);
        await().during(QUIET_WINDOW).atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> updates.size() == 1);
        assertEquals(SdkEventType.SEGMENTS_UPDATE, updates.get(0).getType());
        assertTrue(updates.get(0).getNames().isEmpty());
    }

    // A throwing listener does not affect other listeners or destabilise the SDK.
    @Test
    public void aThrowingListenerDoesNotDestabiliseTheSdk() throws Exception {
        SplitClient client = buildClient(10000);
        Recorder healthyReady = new Recorder();
        Recorder healthyUpdate = new Recorder();
        client.on(SdkEvent.SDK_READY, metadata -> {
            throw new IllegalStateException("ready listener failure under test");
        });
        client.on(SdkEvent.SDK_READY, healthyReady);
        client.on(SdkEvent.SDK_UPDATE, metadata -> {
            throw new IllegalStateException("update listener failure under test");
        });
        client.on(SdkEvent.SDK_UPDATE, healthyUpdate);

        client.blockUntilReady();
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> healthyReady.size() == 1);
        assertEquals("on", client.getTreatment("a", "f1"));

        publishFlagChange(flagsBody(INITIAL_CN, UPDATED_CN, flag("f1", UPDATED_CN, true), -1, -1, ""));
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> healthyUpdate.size() == 1);

        // delivery keeps working for subsequent events, and evaluation is unaffected
        publishFlagChange(UPDATED_CN, 300L, flagsBody(UPDATED_CN, 300L, flag("f1", 300L, true), -1, -1, ""));
        await().atMost(WAIT_SECONDS, TimeUnit.SECONDS).until(() -> healthyUpdate.size() == 2);
        assertEquals("on", client.getTreatment("a", "f1"));
    }

    private void newBackend(long headersDelayMillis) throws Exception {
        backend = new LifecycleScriptDispatcher(
                flagsBody(-1, INITIAL_CN, flag("f1", INITIAL_CN, true), -1, -1, ""),
                segmentChange(-1, SEGMENT_CN, "a", ""),
                headersDelayMillis);
        splitServer = new SplitMockServer(backend);
        splitServer.start();
        if (streaming) {
            sseQueue = new SSEMockServer.SseEventQueue();
            sseServer = new SSEMockServer(sseQueue, (token, version, channel) -> {
                if (!"1.1".equals(version)) {
                    return new Pair<>(new OutboundEvent.Builder().data("wrong version").build(), false);
                }
                return new Pair<>(null, true);
            });
            sseServer.start();
        }
    }

    private void stopBackend() throws Exception {
        if (sseServer != null) {
            sseServer.stop();
            sseServer = null;
        }
        splitServer.stop();
    }

    private SplitClient buildClient(int blockUntilReadyTimeoutMillis) throws Exception {
        SplitClientConfig.Builder builder = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(blockUntilReadyTimeoutMillis)
                .endpoint(splitServer.getUrl(), splitServer.getUrl())
                .authServiceURL(String.format("%s/api/auth/enabled", splitServer.getUrl()))
                .featuresRefreshRate(5)
                .segmentsRefreshRate(30)
                .streamingEnabled(streaming);
        if (streaming) {
            builder.streamingServiceURL("http://localhost:" + sseServer.getPort());
        }
        factory = SplitFactoryBuilder.build("fake-api-token", builder.build());
        return factory.client();
    }

    private void publishFlagChange(String body) throws Exception {
        publishFlagChange(INITIAL_CN, UPDATED_CN, body);
    }

    /** Makes {@code body} the answer to "changes since {@code since}" and, when streaming, announces it. */
    private void publishFlagChange(long since, long till, String body) throws Exception {
        backend.scriptFlags(since, body);
        if (streaming) {
            pushSse("xxxx_xxxx_splits", "{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":" + till + "}");
        }
    }

    private void pushSse(String channel, String innerJson) {
        sseQueue.push(new OutboundEvent.Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592590436082,\"encoding\":\"json\",\"channel\":\""
                        + channel + "\",\"data\":\"" + innerJson + "\"}")
                .build());
    }

    /** Collects every invocation; a replayed SDK_READY / SDK_READY_TIMED_OUT records {@code null}. */
    public static final class Recorder implements io.split.client.api.SdkEventListener {
        private final List<SdkEventMetadata> received = new CopyOnWriteArrayList<>();

        @Override
        public void handle(SdkEventMetadata metadata) {
            EventThreads.record();
            received.add(metadata);
        }

        public int size() {
            return received.size();
        }

        public SdkEventMetadata get(int index) {
            return received.get(index);
        }
    }
}
