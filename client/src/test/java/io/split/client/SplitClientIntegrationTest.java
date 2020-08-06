package io.split.client;

import io.split.SSEMockServer;
import io.split.SplitMockServer;
import org.glassfish.grizzly.utils.Pair;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.sse.OutboundSseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

public class SplitClientIntegrationTest {
    @Test
    public void example() throws IOException, TimeoutException, InterruptedException, URISyntaxException {
        SplitMockServer splitServer = new SplitMockServer();
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = new SSEMockServer(eventQueue, (token, version, channel) -> {
            if (!"1.1".equals(version)) {
                return new Pair<>(new OutboundEvent.Builder().data("wrong version").build(), false);
            }
            return new Pair<>(null, true);
        });

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

        OutboundSseEvent sseEvent1 = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592590436082,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1585948850111}\"}")
                .build();
        eventQueue.push(sseEvent1);

        Thread.sleep(1000);

        String result1 = client.getTreatment("admin", "push_test");
        Assert.assertEquals("after_notification_received", result1);

        result = client.getTreatment("test_in_segment", "push_test");
        Assert.assertEquals("on_rollout", result);

        OutboundSseEvent sseEvent2 = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592591696052,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_segments\",\"data\":\"{\\\"type\\\":\\\"SEGMENT_UPDATE\\\",\\\"changeNumber\\\":1585948850112,\\\"segmentName\\\":\\\"segment3\\\"}\"}")
                .build();
        eventQueue.push(sseEvent2);

        Thread.sleep(1000);

        result = client.getTreatment("test_in_segment", "push_test");
        Assert.assertEquals("in_segment_match", result);

        OutboundSseEvent sseEvent3 = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592591081575,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_KILL\\\",\\\"changeNumber\\\":1585948850112,\\\"defaultTreatment\\\":\\\"split_killed\\\",\\\"splitName\\\":\\\"push_test\\\"}\"}")
                .build();
        eventQueue.push(sseEvent3);

        Thread.sleep(1000);

        result = client.getTreatment("admin", "push_test");
        Assert.assertEquals("split_killed", result);

        client.destroy();
        splitServer.stop();
        sseServer.stop();
    }
}
