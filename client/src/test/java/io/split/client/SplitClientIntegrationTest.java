package io.split.client;

import io.split.SSEMockServer;
import io.split.SplitMockServer;
import io.split.client.api.SplitView;
import org.awaitility.Awaitility;
import org.glassfish.grizzly.utils.Pair;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.sse.OutboundSseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SplitClientIntegrationTest {
    // TODO: review this test.
    @Test
    @Ignore
    public void getTreatmentWithStreamingEnabled() throws IOException, TimeoutException, InterruptedException, URISyntaxException {
        SplitMockServer splitServer = new SplitMockServer();
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);

        splitServer.start();
        sseServer.start();

        SplitClientConfig config = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .endpoint(splitServer.getUrl(), splitServer.getUrl())
                .authServiceURL(String.format("%s/api/auth/enabled", splitServer.getUrl()))
                .streamingServiceURL("http://localhost:" + sseServer.getPort())
                .streamingEnabled(true)
                .build();

        SplitFactory factory = SplitFactoryBuilder.build("fake-api-token", config);
        SplitClient client = factory.client();
        client.blockUntilReady();

        String result = client.getTreatment("admin", "push_test");
        Assert.assertEquals("on_whitelist", result);

        // SPLIT_UPDATED should fetch -> changeNumber > since
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

        OutboundSseEvent sseEvent2 = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592591696052,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_segments\",\"data\":\"{\\\"type\\\":\\\"SEGMENT_UPDATE\\\",\\\"changeNumber\\\":1585948850111,\\\"segmentName\\\":\\\"segment3\\\"}\"}")
                .build();
        eventQueue.push(sseEvent2);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "in_segment_match".equals(client.getTreatment("test_in_segment", "push_test")));

        // SEGMENT_UPDATE should not fetch -> changeNumber < since
        OutboundSseEvent sseEvent5 = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592591696052,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_segments\",\"data\":\"{\\\"type\\\":\\\"SEGMENT_UPDATE\\\",\\\"changeNumber\\\":1585948850109,\\\"segmentName\\\":\\\"segment3\\\"}\"}")
                .build();
        eventQueue.push(sseEvent5);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "in_segment_match".equals(client.getTreatment("test_in_segment", "push_test")));

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
    public void getTreatmentWithStreamingEnabledAndAuthDisabled() throws IOException, TimeoutException, InterruptedException, URISyntaxException {
        SplitMockServer splitServer = new SplitMockServer();
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
    public void getTreatmentWithStreamingDisabled() throws IOException, TimeoutException, InterruptedException, URISyntaxException {
        SplitMockServer splitServer = new SplitMockServer();
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
    public void managerSplitsWithStreamingEnabled() throws IOException, TimeoutException, InterruptedException, URISyntaxException {
        SplitMockServer splitServer = new SplitMockServer();
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
    public void splitClientOccupancyNotifications() throws IOException, TimeoutException, InterruptedException, URISyntaxException {
        SplitMockServer splitServer = new SplitMockServer();
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
    public void splitClientControlNotifications() throws IOException, TimeoutException, InterruptedException, URISyntaxException {
        SplitMockServer splitServer = new SplitMockServer();
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
    public void splitClientMultiFactory() throws IOException, TimeoutException, InterruptedException, URISyntaxException {
        SplitMockServer splitServer = new SplitMockServer();

        SSEMockServer.SseEventQueue eventQueue1 = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer1 = buildSSEMockServer(eventQueue1);

        SSEMockServer.SseEventQueue eventQueue2 = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer2 = buildSSEMockServer(eventQueue2);

        SSEMockServer.SseEventQueue eventQueue3 = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer3 = buildSSEMockServer(eventQueue3);

        SSEMockServer.SseEventQueue eventQueue4 = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer4 = buildSSEMockServer(eventQueue4);

        splitServer.start();
        sseServer1.start();
        sseServer2.start();
        sseServer3.start();
        sseServer4.start();

        SplitClientConfig config1 = buildSplitClientConfig("enabled", splitServer.getUrl(), sseServer1.getPort(), true, 20);
        SplitFactory factory1 = SplitFactoryBuilder.build("fake-api-token-1", config1);
        SplitClient client1 = factory1.client();
        client1.blockUntilReady();

        SplitClientConfig config2 = buildSplitClientConfig("enabled", splitServer.getUrl(), sseServer2.getPort(), true, 20);
        SplitFactory factory2 = SplitFactoryBuilder.build("fake-api-token-2", config2);
        SplitClient client2 = factory2.client();
        client2.blockUntilReady();

        SplitClientConfig config3 = buildSplitClientConfig("enabled", splitServer.getUrl(), sseServer3.getPort(), true, 20);
        SplitFactory factory3 = SplitFactoryBuilder.build("fake-api-token-3", config3);
        SplitClient client3 = factory3.client();
        client3.blockUntilReady();

        SplitClientConfig config4 = buildSplitClientConfig("disabled", splitServer.getUrl(), sseServer4.getPort(), true, 50);
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

        OutboundSseEvent sseEventSplitUpdate = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592590436082,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1585948850111}\"}")
                .build();
        eventQueue1.push(sseEventSplitUpdate);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "after_notification_received".equals(client1.getTreatment("admin", "push_test")));

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
                .until(() -> "after_notification_received".equals(client1.getTreatment("admin", "push_test")));

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "on_whitelist".equals(client2.getTreatment("admin", "push_test")));

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "after_notification_received".equals(client3.getTreatment("admin", "push_test")));

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "on_whitelist".equals(client4.getTreatment("admin", "push_test")));

        OutboundSseEvent sseEventSplitUpdate3 = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592590436082,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1585948850112}\"}")
                .build();
        eventQueue3.push(sseEventSplitUpdate3);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "after_notification_received".equals(client1.getTreatment("admin", "push_test")));

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "on_whitelist".equals(client2.getTreatment("admin", "push_test")));

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "split_killed".equals(client3.getTreatment("admin", "push_test")));

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .until(() -> "on_whitelist".equals(client4.getTreatment("admin", "push_test")));

        client1.destroy();
        client2.destroy();
        client3.destroy();
        client4.destroy();
        splitServer.stop();
        sseServer1.stop();
        sseServer2.stop();
        sseServer3.stop();
        sseServer4.stop();
    }

    // TODO: review this test.
    @Test
    @Ignore
    public void keepAlive() throws IOException, TimeoutException, InterruptedException, URISyntaxException {
        SplitMockServer splitServer = new SplitMockServer();
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
        Thread.sleep(80000);

        // must reconnect and after the second syncAll the result must be different
        Awaitility.await()
                .atMost(1L, TimeUnit.MINUTES)
                .untilAsserted(() -> Assert.assertEquals("split_killed", client.getTreatment("admin", "push_test")));

        client.destroy();
        splitServer.stop();
        sseServer.stop();
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
