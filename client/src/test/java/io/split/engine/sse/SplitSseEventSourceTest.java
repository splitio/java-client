package io.split.engine.sse;

import io.split.SSEMockServer;
import org.awaitility.Awaitility;
import org.glassfish.grizzly.utils.Pair;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.OutboundSseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
/*
public class SplitSseEventSourceTest {
    @Test
    public void openShouldConnect() throws IOException, URISyntaxException {
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);
        sseServer.start();

        AtomicReference<InboundSseEvent> inboundSseEvent = new AtomicReference<>();
        AtomicReference<SseStatus> sseStatus = new AtomicReference<>();

        SplitSseEventSource splitSseEventSource = new SplitSseEventSource(
                inboundEvent -> { inboundSseEvent.set(inboundEvent); return null; },
                status -> { sseStatus.set(status); return null; });

        Client client = ClientBuilder.newBuilder().readTimeout(70, TimeUnit.SECONDS).build();
        WebTarget target = client.target(new URIBuilder("http://localhost:" + sseServer.getPort())
                .addParameter("v", "1.1")
                .build());

        boolean result = splitSseEventSource.open(target);

        Assert.assertTrue(result);
        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .untilAsserted(() -> Assert.assertEquals(SseStatus.CONNECTED, sseStatus.get()));
        Assert.assertTrue(splitSseEventSource.isOpen());

        try {
            splitSseEventSource.open(target);
        } catch (IllegalStateException ex) {
            Assert.assertEquals("Event Source Already connected.", ex.getMessage());
        }

        sseServer.stop();
    }

    @Test
    public void openShouldNotConnect() throws IOException, URISyntaxException {
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);

        sseServer.start();

        AtomicReference<InboundSseEvent> inboundSseEvent = new AtomicReference<>();
        AtomicReference<SseStatus> sseStatus = new AtomicReference<>();

        SplitSseEventSource splitSseEventSource = new SplitSseEventSource(
                inboundEvent -> { inboundSseEvent.set(inboundEvent); return null; },
                status -> { sseStatus.set(status); return null; });

        Client client = ClientBuilder.newBuilder().readTimeout(70, TimeUnit.SECONDS).build();
        WebTarget target = client.target(new URIBuilder("http://fake:" + sseServer.getPort())
                .addParameter("v", "1.1")
                .build());

        boolean result = splitSseEventSource.open(target);

        Assert.assertFalse(result);
        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .untilAsserted(() -> Assert.assertEquals(SseStatus.NONRETRYABLE_ERROR, sseStatus.get()));
        Assert.assertFalse(splitSseEventSource.isOpen());

        sseServer.stop();
    }

    @Test
    public void closeShouldCloseConnection() throws IOException, URISyntaxException {
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);

        sseServer.start();

        AtomicReference<InboundSseEvent> inboundSseEvent = new AtomicReference<>();
        AtomicReference<SseStatus> sseStatus = new AtomicReference<>();

        SplitSseEventSource splitSseEventSource = new SplitSseEventSource(
                inboundEvent -> { inboundSseEvent.set(inboundEvent); return null; },
                status -> { sseStatus.set(status); return null; });

        Client client = ClientBuilder.newBuilder().readTimeout(70, TimeUnit.SECONDS).build();
        WebTarget target = client.target(new URIBuilder("http://localhost:" + sseServer.getPort())
                .addParameter("v", "1.1")
                .build());

        boolean result = splitSseEventSource.open(target);

        Assert.assertTrue(result);
        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .untilAsserted(() -> Assert.assertEquals(SseStatus.CONNECTED, sseStatus.get()));
        Assert.assertTrue(splitSseEventSource.isOpen());

        splitSseEventSource.close();
        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .untilAsserted(() -> Assert.assertEquals(SseStatus.DISCONNECTED, sseStatus.get()));
        Assert.assertFalse(splitSseEventSource.isOpen());

        sseServer.stop();
    }

    @Test
    public void openShouldReceiveNotification() throws IOException, URISyntaxException, InterruptedException {
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);

        sseServer.start();

        AtomicReference<InboundSseEvent> inboundSseEvent = new AtomicReference<>();
        AtomicReference<SseStatus> sseStatus = new AtomicReference<>();

        SplitSseEventSource splitSseEventSource = new SplitSseEventSource(
                inboundEvent -> { inboundSseEvent.set(inboundEvent); return null; },
                status -> { sseStatus.set(status); return null; });

        Client client = ClientBuilder.newBuilder().readTimeout(70, TimeUnit.SECONDS).build();
        WebTarget target = client.target(new URIBuilder("http://localhost:" + sseServer.getPort())
                .addParameter("v", "1.1")
                .build());

        boolean result = splitSseEventSource.open(target);

        Assert.assertTrue(result);
        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .untilAsserted(() -> Assert.assertEquals(SseStatus.CONNECTED, sseStatus.get()));
        Assert.assertTrue(splitSseEventSource.isOpen());

        OutboundSseEvent sseEvent = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592590436082,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1585948850111}\"}")
                .build();
        eventQueue.push(sseEvent);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .untilAsserted(() -> Assert.assertNotNull(inboundSseEvent));

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .untilAsserted(() -> Assert.assertEquals("message", inboundSseEvent.get().getName()));

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


}
*/
