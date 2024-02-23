package io.split.client;

import io.split.SSEMockServer;
import io.split.SplitMockServer;
import io.split.client.api.SplitView;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.utils.CustomDispatcher;
import io.split.storages.enums.OperationMode;
import io.split.storages.enums.StorageMode;
import io.split.storages.pluggable.CustomStorageWrapperImp;
import io.split.storages.pluggable.domain.EventConsumer;
import io.split.storages.pluggable.domain.ImpressionConsumer;
import okhttp3.mockwebserver.MockResponse;
import org.awaitility.Awaitility;
import org.glassfish.grizzly.utils.Pair;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.sse.OutboundSseEvent;
import java.io.IOException;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class SplitClientIntegrationTest {

    @Test
    public void getTreatmentWithStreamingEnabled() throws Exception {
        MockResponse response = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850109, \"till\":1585948850109}");
        MockResponse response2 = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850110, \"till\":1585948850110}");
        MockResponse response3 = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850111, \"till\":1585948850111}");
        Queue responses = new LinkedList<>();
        responses.add(response);
        Queue responses2 = new LinkedList<>();
        responses2.add(response2);
        Queue responses3 = new LinkedList<>();
        responses3.add(response3);
        SplitMockServer splitServer = new SplitMockServer(CustomDispatcher.builder()
                .path(CustomDispatcher.SINCE_1585948850109, responses)
                .path(CustomDispatcher.SINCE_1585948850110, responses2)
                .path(CustomDispatcher.SINCE_1585948850111, responses3)
                .build());
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);

        splitServer.start();
        sseServer.start();

        SplitClientConfig config = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .endpoint(splitServer.getUrl(), splitServer.getUrl())
                .authServiceURL(String.format("%s/api/auth/enabled", splitServer.getUrl()))
                .streamingServiceURL("http://localhost:" + sseServer.getPort())
                .featuresRefreshRate(20)
                .segmentsRefreshRate(30)
                .streamingEnabled(true)
                .build();

        SplitFactory factory = SplitFactoryBuilder.build("fake-api-token", config);
        SplitClient client = factory.client();
        client.blockUntilReady();

        String result = client.getTreatment("admin", "push_test");
        Assert.assertEquals("on_whitelist", result);

        // SPLIT_UPDATED should fetch -> changeNumber > since

        OutboundSseEvent sseEventWithPublishers = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"timestamp\":1588254668328,\"encoding\":\"json\",\"channel\":\"[?occupancy=metrics.publishers]control_pri\",\"data\":\"{\\\"metrics\\\":{\\\"publishers\\\":2}}\",\"name\":\"[meta]occupancy\"}")
                .build();
        eventQueue.push(sseEventWithPublishers);

        OutboundSseEvent sseEventWithoutPublishers = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"timestamp\":1588254668328,\"encoding\":\"json\",\"channel\":\"[?occupancy=metrics.publishers]control_pri\",\"data\":\"{\\\"metrics\\\":{\\\"publishers\\\":0}}\",\"name\":\"[meta]occupancy\"}")
                .build();
        eventQueue.push(sseEventWithoutPublishers);

        OutboundSseEvent sseEvent1 = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592590436082,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1585948850111}\"}")
                .build();
        eventQueue.push(sseEvent1);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "after_notification_received".equals(client.getTreatment("admin", "push_test")));

        // SPLIT_UPDATED should not fetch -> changeNumber < since
        OutboundSseEvent sseEvent4 = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592590436082,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1585948850109}\"}")
                .build();
        eventQueue.push(sseEvent4);


        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "after_notification_received".equals(client.getTreatment("admin", "push_test"))
                    && "on_rollout".equals(client.getTreatment("test_in_segment", "push_test")));

        eventQueue.push(sseEventWithPublishers);
        eventQueue.push(sseEventWithoutPublishers);

        // SPLIT_KILL should fetch.
        OutboundSseEvent sseEvent3 = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592591081575,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_KILL\\\",\\\"changeNumber\\\":1585948850112,\\\"defaultTreatment\\\":\\\"split_killed\\\",\\\"splitName\\\":\\\"push_test\\\"}\"}")
                .build();
        eventQueue.push(sseEvent3);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "split_killed".equals(client.getTreatment("admin", "push_test")));

        client.destroy();
        splitServer.stop();
        sseServer.stop();
    }

    @Test
    public void getTreatmentWithStreamingEnabledAndAuthDisabled() throws Exception {
        MockResponse response = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850109, \"till\":1585948850109}");
        Queue responses = new LinkedList<>();
        responses.add(response);
        SplitMockServer splitServer = new SplitMockServer(CustomDispatcher.builder()
                .path(CustomDispatcher.SINCE_1585948850109, responses)
                .build());
        splitServer.start();

        SplitClientConfig config = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .endpoint(splitServer.getUrl(), splitServer.getUrl())
                .authServiceURL(String.format("%s/api/auth/disabled", splitServer.getUrl()))
                .streamingEnabled(true)
                .build();

        SplitFactory factory = SplitFactoryBuilder.build("fake-api-token", config);
        SplitClient client = factory.client();
        client.blockUntilReady();

        String result = client.getTreatment("admin", "push_test");
        Assert.assertEquals("on_whitelist", result);

        client.destroy();
        splitServer.stop();
    }

    @Test
    public void getTreatmentWithStreamingDisabled() throws Exception {
        MockResponse response = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850109, \"till\":1585948850109}");
        Queue responses = new LinkedList<>();
        responses.add(response);
        SplitMockServer splitServer = new SplitMockServer(CustomDispatcher.builder()
                .path(CustomDispatcher.SINCE_1585948850109, responses)
                .build());
        splitServer.start();

        SplitClientConfig config = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .endpoint(splitServer.getUrl(), splitServer.getUrl())
                .authServiceURL(String.format("%s/api/auth/enabled", splitServer.getUrl()))
                .streamingEnabled(false)
                .featuresRefreshRate(5)
                .build();

        SplitFactory factory = SplitFactoryBuilder.build("fake-api-token", config);
        SplitClient client = factory.client();
        client.blockUntilReady();

        String result = client.getTreatment("admin", "push_test");
        Assert.assertEquals("on_whitelist", result);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "split_killed".equals(client.getTreatment("admin", "push_test")));

        client.destroy();
        splitServer.stop();
    }

    @Test
    public void managerSplitsWithStreamingEnabled() throws Exception {
        MockResponse response = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850109, \"till\":1585948850109}");
        Queue responses = new LinkedList<>();
        responses.add(response);
        SplitMockServer splitServer = new SplitMockServer(CustomDispatcher.builder()
                .path(CustomDispatcher.SINCE_1585948850109, responses)
                .build());
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);

        splitServer.start();
        sseServer.start();

        SplitClientConfig config = buildSplitClientConfig("enabled", splitServer.getUrl(), sseServer.getPort(), true, 50);

        SplitFactory factory = SplitFactoryBuilder.build("fake-api-token", config);
        SplitManager manager = factory.manager();
        manager.blockUntilReady();

        List<SplitView> results = manager.splits();
        Assert.assertEquals(4, results.size());
        Assert.assertEquals(3, results.stream().filter(r -> !r.killed).toArray().length);

        // SPLIT_KILL should fetch.
        OutboundSseEvent sseEventSplitKill = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592591081575,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_KILL\\\",\\\"changeNumber\\\":1585948850112,\\\"defaultTreatment\\\":\\\"split_killed\\\",\\\"splitName\\\":\\\"push_test\\\"}\"}")
                .build();
        eventQueue.push(sseEventSplitKill);

        Awaitility.await()
                .atMost(2L, TimeUnit.MINUTES)
                .until(() -> 2 == manager.splits().stream().filter(r -> !r.killed).toArray().length);

        splitServer.stop();
        sseServer.stop();
    }

    @Test
    public void splitClientOccupancyNotifications() throws Exception {
        MockResponse response = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850109, \"till\":1585948850109}");
        MockResponse response2 = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850110, \"till\":1585948850110}");
        MockResponse response3 = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850111, \"till\":1585948850111}");
        Queue responses = new LinkedList<>();
        responses.add(response);
        Queue responses2 = new LinkedList<>();
        responses2.add(response2);
        Queue responses3 = new LinkedList<>();
        responses3.add(response3);
        SplitMockServer splitServer = new SplitMockServer(CustomDispatcher.builder()
                .path(CustomDispatcher.SINCE_1585948850109, responses)
                .path(CustomDispatcher.SINCE_1585948850110, responses2)
                .path(CustomDispatcher.SINCE_1585948850111, responses3)
                .build());
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);

        splitServer.start();
        sseServer.start();

        SplitClientConfig config = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .endpoint(splitServer.getUrl(), splitServer.getUrl())
                .authServiceURL(String.format("%s/api/auth/enabled", splitServer.getUrl()))
                .streamingServiceURL("http://localhost:" + sseServer.getPort())
                .featuresRefreshRate(20)
                .streamingEnabled(true)
                .build();

        SplitFactory factory = SplitFactoryBuilder.build("fake-api-token", config);
        SplitClient client = factory.client();
        client.blockUntilReady();

        String result = client.getTreatment("admin", "push_test");
        Assert.assertEquals("on_whitelist", result);

        OutboundSseEvent sseEventWithPublishers = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"222\",\"timestamp\":1588254668328,\"encoding\":\"json\",\"channel\":\"[?occupancy=metrics.publishers]control_pri\",\"data\":\"{\\\"metrics\\\":{\\\"publishers\\\":2}}\",\"name\":\"[meta]occupancy\"}")
                .build();
        eventQueue.push(sseEventWithPublishers);

        OutboundSseEvent sseEventWithoutPublishers = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"222\",\"timestamp\":1588254668328,\"encoding\":\"json\",\"channel\":\"[?occupancy=metrics.publishers]control_pri\",\"data\":\"{\\\"metrics\\\":{\\\"publishers\\\":0}}\",\"name\":\"[meta]occupancy\"}")
                .build();
        eventQueue.push(sseEventWithoutPublishers);

        OutboundSseEvent sseEventSplitKill = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592590436082,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1585948850112}\"}")
                .build();
        eventQueue.push(sseEventSplitKill);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "after_notification_received".equals(client.getTreatment("admin", "push_test")));

        eventQueue.push(sseEventWithPublishers);
        eventQueue.push(sseEventSplitKill);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "split_killed".equals(client.getTreatment("admin", "push_test")));

        client.destroy();
        splitServer.stop();
        sseServer.stop();
    }

    @Test
    public void splitClientControlNotifications() throws Exception {
        MockResponse response = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850109, \"till\":1585948850109}");
        MockResponse response2 = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850110, \"till\":1585948850110}");
        MockResponse response3 = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850111, \"till\":1585948850111}");
        Queue responses = new LinkedList<>();
        responses.add(response);
        Queue responses2 = new LinkedList<>();
        responses2.add(response2);
        Queue responses3 = new LinkedList<>();
        responses3.add(response3);
        SplitMockServer splitServer = new SplitMockServer(CustomDispatcher.builder()
                .path(CustomDispatcher.SINCE_1585948850109, responses)
                .path(CustomDispatcher.SINCE_1585948850110, responses2)
                .path(CustomDispatcher.SINCE_1585948850111, responses3)
                .build());
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);

        splitServer.start();
        sseServer.start();

        SplitClientConfig config = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .endpoint(splitServer.getUrl(), splitServer.getUrl())
                .authServiceURL(String.format("%s/api/auth/enabled", splitServer.getUrl()))
                .streamingServiceURL("http://localhost:" + sseServer.getPort())
                .featuresRefreshRate(20)
                .streamingEnabled(true)
                .build();

        SplitFactory factory = SplitFactoryBuilder.build("fake-api-token", config);
        SplitClient client = factory.client();
        client.blockUntilReady();

        String result = client.getTreatment("admin", "push_test");
        Assert.assertEquals("on_whitelist", result);

        // STREAMING_PAUSE pause streaming and start periodic fetching.
        OutboundSseEvent sseEventPause = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"2222\",\"clientId\":\"3333\",\"timestamp\":1588254699236,\"encoding\":\"json\",\"channel\":\"[?occupancy=metrics.publishers]control_pri\",\"data\":\"{\\\"type\\\":\\\"CONTROL\\\",\\\"controlType\\\":\\\"STREAMING_PAUSED\\\"}\"}")
                .build();
        eventQueue.push(sseEventPause);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "after_notification_received".equals(client.getTreatment("admin", "push_test")));

        OutboundSseEvent sseEventSplitUpdate = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592590436082,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1585948850112}\"}")
                .build();
        eventQueue.push(sseEventSplitUpdate);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "after_notification_received".equals(client.getTreatment("admin", "push_test")));

        OutboundSseEvent sseEventSplitUpdate2 = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592590436082,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1585948850113}\"}")
                .build();
        eventQueue.push(sseEventSplitUpdate2);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "after_notification_received".equals(client.getTreatment("admin", "push_test")));

        OutboundSseEvent sseEventResumed = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"2222\",\"clientId\":\"3333\",\"timestamp\":1588254699236,\"encoding\":\"json\",\"channel\":\"[?occupancy=metrics.publishers]control_pri\",\"data\":\"{\\\"type\\\":\\\"CONTROL\\\",\\\"controlType\\\":\\\"STREAMING_RESUMED\\\"}\"}")
                .build();
        eventQueue.push(sseEventResumed);

        OutboundSseEvent sseEventSplitUpdate3 = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592590436082,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1585948850112}\"}")
                .build();
        eventQueue.push(sseEventSplitUpdate3);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "split_killed".equals(client.getTreatment("admin", "push_test")));

        client.destroy();
        splitServer.stop();
        sseServer.stop();
    }

    @Test
    public void splitClientMultiFactory() throws Exception {
        MockResponse response = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850109, \"till\":1585948850109}");
        Queue responses = new LinkedList<>();
        responses.add(response);
        responses.add(response);
        responses.add(response);
        responses.add(response);
        responses.add(response);
        responses.add(response);
        responses.add(response);
        responses.add(response);

        SplitMockServer splitServer1 = new SplitMockServer(CustomDispatcher.builder()
                .path(CustomDispatcher.SINCE_1585948850109, responses)
                .build());
        SplitMockServer splitServer2 = new SplitMockServer(CustomDispatcher.builder()
                .path(CustomDispatcher.SINCE_1585948850109, responses)
                .build());
        SplitMockServer splitServer3 = new SplitMockServer(CustomDispatcher.builder()
                .path(CustomDispatcher.SINCE_1585948850109, responses)
                .build());
        SplitMockServer splitServer4 = new SplitMockServer(CustomDispatcher.builder()
                .path(CustomDispatcher.SINCE_1585948850109, responses)
                .build());

        SSEMockServer.SseEventQueue eventQueue1 = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer1 = buildSSEMockServer(eventQueue1);

        SSEMockServer.SseEventQueue eventQueue2 = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer2 = buildSSEMockServer(eventQueue2);

        SSEMockServer.SseEventQueue eventQueue3 = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer3 = buildSSEMockServer(eventQueue3);

        SSEMockServer.SseEventQueue eventQueue4 = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer4 = buildSSEMockServer(eventQueue4);

        splitServer1.start();
        splitServer2.start();
        splitServer3.start();
        splitServer4.start();
        sseServer1.start();
        sseServer2.start();
        sseServer3.start();
        sseServer4.start();

        SplitClientConfig config1 = buildSplitClientConfig("enabled", splitServer1.getUrl(), sseServer1.getPort(), true, 20);
        SplitFactory factory1 = SplitFactoryBuilder.build("fake-api-token-1", config1);
        SplitClient client1 = factory1.client();
        client1.blockUntilReady();

        SplitClientConfig config2 = buildSplitClientConfig("enabled", splitServer2.getUrl(), sseServer2.getPort(), true, 20);
        SplitFactory factory2 = SplitFactoryBuilder.build("fake-api-token-2", config2);
        SplitClient client2 = factory2.client();
        client2.blockUntilReady();

        SplitClientConfig config3 = buildSplitClientConfig("enabled", splitServer3.getUrl(), sseServer3.getPort(), true, 20);
        SplitFactory factory3 = SplitFactoryBuilder.build("fake-api-token-3", config3);
        SplitClient client3 = factory3.client();
        client3.blockUntilReady();

        SplitClientConfig config4 = buildSplitClientConfig("disabled", splitServer4.getUrl(), sseServer4.getPort(), true, 100);
        SplitFactory factory4 = SplitFactoryBuilder.build("fake-api-token-4", config4);
        SplitClient client4 = factory4.client();
        client4.blockUntilReady();

        String result1 = client1.getTreatment("admin", "push_test");
        Assert.assertEquals("on_whitelist", result1);

        String result2 = client2.getTreatment("admin", "push_test");
        Assert.assertEquals("on_whitelist", result2);

        String result3 = client3.getTreatment("admin", "push_test");
        Assert.assertEquals("on_whitelist", result3);

        String result4 = client4.getTreatment("admin", "push_test");
        Assert.assertEquals("on_whitelist", result4);


        OutboundSseEvent sseEventInitial = new OutboundEvent
                .Builder()
                .comment("initializing")
                .id("fakeid")
                .name("message")
                .data("{\"id\":\"222\",\"timestamp\":1588254668328,\"encoding\":\"json\",\"channel\":\"[?occupancy=metrics.publishers]control_pri\",\"data\":\"{\\\"metrics\\\":{\\\"publishers\\\":2}}\",\"name\":\"[meta]occupancy\"}")
                .build();
        OutboundSseEvent sseEventSplitUpdate = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592590436082,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1585948850111}\"}")
                .build();
        eventQueue1.push(sseEventInitial);
        eventQueue2.push(sseEventInitial);
        eventQueue3.push(sseEventInitial);
        eventQueue4.push(sseEventInitial);

        Thread.sleep(10000);
        eventQueue1.push(sseEventSplitUpdate);

        Awaitility.await()
                .atMost(100L, TimeUnit.SECONDS)
                .until(() -> "split_killed".equals(client1.getTreatment("admin", "push_test")));


        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "on_whitelist".equals(client2.getTreatment("admin", "push_test")));

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "on_whitelist".equals(client3.getTreatment("admin", "push_test")));

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "on_whitelist".equals(client4.getTreatment("admin", "push_test")));

        eventQueue3.push(sseEventSplitUpdate);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "split_killed".equals(client1.getTreatment("admin", "push_test")));

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "on_whitelist".equals(client2.getTreatment("admin", "push_test")));

        Awaitility.await()
                .atMost(100L, TimeUnit.SECONDS)
                .until(() -> "split_killed".equals(client3.getTreatment("admin", "push_test")));

        Awaitility.await()
                .atMost(100L, TimeUnit.SECONDS)
                .until(() -> "on_whitelist".equals(client4.getTreatment("admin", "push_test")));

        client1.destroy();
        client2.destroy();
        client3.destroy();
        client4.destroy();
        splitServer1.stop();
        splitServer2.stop();
        splitServer3.stop();
        splitServer4.stop();
        sseServer1.stop();
        sseServer2.stop();
        sseServer3.stop();
        sseServer4.stop();
    }

    @Test
    public void keepAlive() throws Exception {
        MockResponse response = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850109, \"till\":1585948850109}");
        Queue responses = new LinkedList<>();
        responses.add(response);

        SplitMockServer splitServer = new SplitMockServer(CustomDispatcher.builder()
                .path(CustomDispatcher.SINCE_1585948850109, responses)
                .build());

        //plitMockServer splitServer = new SplitMockServer(CustomDispatcher.builder().build());
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);

        splitServer.start();
        sseServer.start();

        SplitClientConfig config = buildSplitClientConfig("enabled", splitServer.getUrl(), sseServer.getPort(), true, 50);
        SplitFactory factory = SplitFactoryBuilder.build("fake-api-token-1", config);
        SplitClient client = factory.client();
        client.blockUntilReady();

        String result = client.getTreatment("admin", "push_test");
        Assert.assertEquals("on_whitelist", result);

        // wait to check keep alive notification.
        Thread.sleep(50000);

        // must reconnect and after the second syncAll the result must be different
        Awaitility.await()
                .atMost(1L, TimeUnit.MINUTES)
                .untilAsserted(() -> Assert.assertEquals("split_killed", client.getTreatment("admin", "push_test")));

        client.destroy();
        splitServer.stop();
        sseServer.stop();
    }

    @Test
    public void testConnectionClosedByRemoteHostIsProperlyHandled() throws Exception {
        MockResponse response = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850109, \"till\":1585948850109}");
        Queue responses = new LinkedList<>();
        responses.add(response);
        SplitMockServer splitServer = new SplitMockServer(CustomDispatcher.builder()
                .path(CustomDispatcher.SINCE_1585948850109, responses)
                .build());
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);

        splitServer.start();
        sseServer.start();

        SplitClientConfig config = buildSplitClientConfig("enabled", splitServer.getUrl(), sseServer.getPort(), true, 100);
        SplitFactory factory = SplitFactoryBuilder.build("fake-api-token-1", config);
        SplitClient client = factory.client();
        client.blockUntilReady();

        OutboundSseEvent sseEventInitial = new OutboundEvent
                .Builder()
                .comment("initializing")
                .id("fakeid")
                .name("message")
                .data("{\"id\":\"222\",\"timestamp\":1588254668328,\"encoding\":\"json\",\"channel\":\"[?occupancy=metrics.publishers]control_pri\",\"data\":\"{\\\"metrics\\\":{\\\"publishers\\\":2}}\",\"name\":\"[meta]occupancy\"}")
                .build();

        eventQueue.push(sseEventInitial);

        String result = client.getTreatment("admin", "push_test");
        Assert.assertEquals("on_whitelist", result);
        Thread.sleep(1000);
        eventQueue.push(SSEMockServer.CONNECTION_CLOSED_BY_REMOTE_HOST);
        Thread.sleep(1000);
        result = client.getTreatment("admin", "push_test");
        Assert.assertNotEquals("on_whitelist", result);
    }

    @Test
    public void testConnectionClosedIsProperlyHandled() throws Exception {
        MockResponse response = new MockResponse().setBody("{\"splits\": [], \"since\":1585948850109, \"till\":1585948850109}");
        Queue responses = new LinkedList<>();
        responses.add(response);
        SplitMockServer splitServer = new SplitMockServer(CustomDispatcher.builder()
                .path(CustomDispatcher.SINCE_1585948850109, responses)
                .build());
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);

        splitServer.start();
        sseServer.start();

        SplitClientConfig config = buildSplitClientConfig("enabled", splitServer.getUrl(), sseServer.getPort(), true, 5);
        SplitFactory factory = SplitFactoryBuilder.build("fake-api-token-1", config);
        SplitClient client = factory.client();
        client.blockUntilReady();

        OutboundSseEvent sseEventInitial = new OutboundEvent
                .Builder()
                .comment("initializing")
                .id("fakeid")
                .build();

        eventQueue.push(sseEventInitial);

        String result = client.getTreatment("admin", "push_test");
        Assert.assertEquals("on_whitelist", result);
        Thread.sleep(1000);
        sseServer.stop();
        Thread.sleep(1000);
        result = client.getTreatment("admin", "push_test");
        Assert.assertNotEquals("on_whitelist", result);
    }

    @Test
    public void testPluggableMode() throws IOException, URISyntaxException {
        CustomStorageWrapperImp customStorageWrapper = new CustomStorageWrapperImp();
        SplitClientConfig config = SplitClientConfig.builder()
            .enableDebug()
            .impressionsMode(ImpressionsManager.Mode.DEBUG)
            .impressionsRefreshRate(1)
            .setBlockUntilReadyTimeout(10000)
            .streamingEnabled(true)
            .operationMode(OperationMode.CONSUMER)
            .customStorageWrapper(customStorageWrapper)
            .build();
        SplitFactory splitFactory = SplitFactoryBuilder.build("fake-api-token", config);
        SplitClient client = splitFactory.client();
        try {
            client.blockUntilReady();
            SplitManager splitManager = splitFactory.manager();
            HashMap<String, Object> properties = new HashMap<>();
            properties.put("number_property", 123);
            properties.put("object_property", new Object());

            client.track("key", "tt", "importantEventType");
            client.track("keyValue", "tt", "importantEventType", 12L);
            client.track("keyProperties", "tt", "importantEventType", 12L, properties);
            List<EventConsumer> events = customStorageWrapper.getEvents();
            List<SplitView> splits = splitManager.splits();

            Assert.assertEquals(3, events.size());
            Assert.assertTrue(events.stream().anyMatch(e -> "key".equals(e.getEventDto().key) && "tt".equals(e.getEventDto().trafficTypeName)));
            Assert.assertTrue(events.stream().anyMatch(e -> "keyValue".equals(e.getEventDto().key) && e.getEventDto().value == 12L));
            Assert.assertTrue(events.stream().anyMatch(e -> "keyProperties".equals(e.getEventDto().key) && e.getEventDto().properties != null));

            Assert.assertEquals(2, splits.size());
            Assert.assertTrue(splits.stream().anyMatch(sw -> "first.name".equals(sw.name)));
            Assert.assertTrue(splits.stream().anyMatch(sw -> "second.name".equals(sw.name)));
            Assert.assertEquals("on", client.getTreatment("key", "first.name"));
            Assert.assertEquals("off", client.getTreatmentWithConfig("FakeKey", "second.name").treatment());
            Assert.assertEquals("control", client.getTreatment("FakeKey", "noSplit"));

            List<ImpressionConsumer> impressions = customStorageWrapper.getImps();
            Assert.assertEquals(2, impressions.size());
            Assert.assertTrue(impressions.stream().anyMatch(imp -> "first.name".equals(imp.getKeyImpression().feature) && "on".equals(imp.getKeyImpression().treatment)));
            Assert.assertTrue(impressions.stream().anyMatch(imp -> "second.name".equals(imp.getKeyImpression().feature) && "off".equals(imp.getKeyImpression().treatment)));

            Map<String, Long> latencies = customStorageWrapper.getLatencies();

            List<String> keys = new ArrayList<>(latencies.keySet());

            String key1 = keys.stream().filter(key -> key.contains("track/")).collect(Collectors.toList()).get(0);
            String key2 = keys.stream().filter(key -> key.contains("getTreatment/")).collect(Collectors.toList()).get(0);
            String key3 = keys.stream().filter(key -> key.contains("getTreatmentWithConfig/")).collect(Collectors.toList()).get(0);

            Assert.assertEquals(Optional.of(3L), Optional.ofNullable(latencies.get(key1)));
            Assert.assertEquals(Optional.of(1L), Optional.of(latencies.get(key2)));
            Assert.assertEquals(Optional.of(1L), Optional.of(latencies.get(key3)));

            Thread.sleep(500);
            Assert.assertNotNull(customStorageWrapper.getConfig());
            String key = customStorageWrapper.getConfig().keySet().stream().collect(Collectors.toList()).get(0);
            Assert.assertTrue(customStorageWrapper.getConfig().get(key).contains(StorageMode.PLUGGABLE.name()));

        } catch (TimeoutException | InterruptedException e) {
        }
    }

    @Test
    public void getTreatmentFlagSetWithPolling() throws Exception {
        MockResponse response = new MockResponse().setBody("{\"splits\":[{\"trafficTypeName\":\"client\",\"name\":\"workm\",\"trafficAllocation\":100,\"trafficAllocationSeed\":147392224,\"seed\":524417105,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"on\",\"changeNumber\":1602796638344,\"algo\":2,\"configurations\":{},\"sets\":[\"set1\",\"set2\"],\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"new_segment\"},\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":100},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"in segment new_segment\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":0},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"default rule\"}]},{\"trafficTypeName\":\"client\",\"name\":\"workm_set_3\",\"trafficAllocation\":100,\"trafficAllocationSeed\":147392224,\"seed\":524417105,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"on\",\"changeNumber\":1602796638344,\"algo\":2,\"configurations\":{},\"sets\":[\"set3\"],\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"new_segment\"},\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":100},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"in segment new_segment\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":0},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"default rule\"}]}],\"since\":-1,\"till\":1602796638344}");
        MockResponse responseFlag = new MockResponse().setBody("{\"splits\": [], \"since\":1602796638344, \"till\":1602796638344}");
        MockResponse segmentResponse = new MockResponse().setBody("{\"name\":\"new_segment\",\"added\":[\"user-1\"],\"removed\":[\"user-2\",\"user-3\"],\"since\":-1,\"till\":-1}");
        Queue responses = new LinkedList<>();
        responses.add(response);
        Queue responsesFlags = new LinkedList<>();
        responsesFlags.add(responseFlag);
        Queue segmentResponses = new LinkedList<>();
        segmentResponses.add(segmentResponse);
        SplitMockServer splitServer = new SplitMockServer(CustomDispatcher.builder()
                .path(CustomDispatcher.INITIAL_FLAGS_BY_SETS, responses)
                .path(CustomDispatcher.SINCE_1602796638344, responsesFlags)
                .path(CustomDispatcher.SEGMENT_BY_FLAG_SET, segmentResponses)
                .build());
        splitServer.start();

        SplitClientConfig config = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .endpoint(splitServer.getUrl(), splitServer.getUrl())
                .authServiceURL(String.format("%s/api/auth/enabled", splitServer.getUrl()))
                .streamingEnabled(false)
                .flagSetsFilter(Arrays.asList("set2", "set1"))
                .featuresRefreshRate(5)
                .build();

        SplitFactory factory = SplitFactoryBuilder.build("fake-api-token", config);
        SplitClient client = factory.client();
        client.blockUntilReady();

        String result = client.getTreatment("admin", "workm");
        Assert.assertEquals("on", result);
        Assert.assertEquals("on", client.getTreatmentsByFlagSet("admin", "set1", null).get("workm"));

        client.destroy();
        splitServer.stop();
    }

    private SSEMockServer buildSSEMockServer(SSEMockServer.SseEventQueue eventQueue) {
        return new SSEMockServer(eventQueue, (token, version, channel) -> {
            if (!"1.1".equals(version)) {
                return new Pair<>(new OutboundEvent.Builder().data("wrong version").build(), false);
            }
            return new Pair<>(null, true);
        });
    }

    private SplitClientConfig buildSplitClientConfig(String authUrl, String splitServerUrl, int sseServerPort, boolean streamingEnabled, int featuresRefreshRate) {
        return SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .endpoint(splitServerUrl, splitServerUrl)
                .authServiceURL(String.format("%s/api/auth/%s", splitServerUrl, authUrl))
                .streamingServiceURL("http://localhost:" + sseServerPort)
                .featuresRefreshRate(featuresRefreshRate)
                .streamingEnabled(streamingEnabled)
                .build();
    }
}