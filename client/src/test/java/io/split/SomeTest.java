package io.split;

import org.glassfish.grizzly.utils.Pair;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class SomeTest {
    @Test
    public void testSomething() throws IOException, InterruptedException {
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer server = new SSEMockServer(eventQueue, (token, version, channel) -> {
            if (!"1.1".equals(version)) {
                return new Pair<>(new OutboundEvent.Builder().data("wrong version").build(), false);
            }
            return new Pair<>(null, true);
        });

        server.start();
        Client client = ClientBuilder.newBuilder().build();

        WebTarget target = client.target("http://localhost:" + server.getPort())
                .queryParam("v", "1.1");
        SseEventSource sseEventSource = SseEventSource.target(target).build();

        List<String> eventsData = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);
        sseEventSource.register(event -> {
            eventsData.add(event.readData(String.class));
            latch.countDown();
        });
        sseEventSource.open();

        eventQueue.push(new OutboundEvent.Builder().data("someEvent").build());
        eventQueue.push(new OutboundEvent.Builder().data("anotherEvent").build());

        latch.await();

        Assert.assertEquals(2, eventsData.size());
        Assert.assertEquals("someEvent", eventsData.get(0));
        Assert.assertEquals("anotherEvent", eventsData.get(1));

        sseEventSource.close();
    }
}
