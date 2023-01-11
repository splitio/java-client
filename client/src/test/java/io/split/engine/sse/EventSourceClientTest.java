package io.split.engine.sse;

import io.split.SSEMockServer;
import io.split.engine.sse.client.SSEClient;
import io.split.engine.sse.dtos.ErrorNotification;
import io.split.engine.sse.dtos.SplitChangeNotification;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.awaitility.Awaitility;
import org.glassfish.grizzly.utils.Pair;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.sse.OutboundSseEvent;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class EventSourceClientTest {
    private NotificationParser _notificationParser;
    private NotificationProcessor _notificationProcessor;
    private PushStatusTracker _pushStatusTracker;

    @Before
    public void setUp() {
        _notificationParser = Mockito.mock(NotificationParser.class);
        _notificationProcessor = Mockito.mock(NotificationProcessor.class);
        _pushStatusTracker = Mockito.mock(PushStatusTracker.class);
    }

    @Test
    public void startShouldConnect() throws IOException {
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(InMemoryTelemetryStorage.class);
        sseServer.start();

        EventSourceClient eventSourceClient = new EventSourceClientImp("http://localhost:" + sseServer.getPort(), _notificationParser, _notificationProcessor, _pushStatusTracker, buildHttpClient(), telemetryRuntimeProducer);

        boolean result = eventSourceClient.start("channel-test","token-test");

        Assert.assertTrue(result);

        Mockito.verify(_pushStatusTracker, Mockito.times(1)).handleSseStatus(SSEClient.StatusMessage.CONNECTED);
    }

    @Test
    public void startShouldReconnect() throws IOException {
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(InMemoryTelemetryStorage.class);
        sseServer.start();
        EventSourceClient eventSourceClient = new EventSourceClientImp("http://fake:" + sseServer.getPort(), _notificationParser, _notificationProcessor, _pushStatusTracker, buildHttpClient(), telemetryRuntimeProducer);

        boolean result = eventSourceClient.start("channel-test","token-test");

        Assert.assertFalse(result);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .untilAsserted(() -> Mockito.verify(_pushStatusTracker, Mockito.times(1)).handleSseStatus(SSEClient.StatusMessage.RETRYABLE_ERROR));
    }

    @Test
    public void startAndReceiveNotification() throws IOException {
        SSEMockServer.SseEventQueue eventQueue = new SSEMockServer.SseEventQueue();
        SSEMockServer sseServer = buildSSEMockServer(eventQueue);
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(InMemoryTelemetryStorage.class);
        sseServer.start();
        EventSourceClient eventSourceClient = new EventSourceClientImp("http://localhost:" + sseServer.getPort(), _notificationParser, _notificationProcessor, _pushStatusTracker, buildHttpClient(), telemetryRuntimeProducer);

        boolean result = eventSourceClient.start("channel-test","token-test");

        Assert.assertTrue(result);

        Mockito.verify(_pushStatusTracker, Mockito.times(1)).handleSseStatus(SSEClient.StatusMessage.CONNECTED);

        OutboundSseEvent sseEvent = new OutboundEvent
                .Builder()
                .name("message")
                .data("{\"id\":\"22\",\"clientId\":\"22\",\"timestamp\":1592590436082,\"encoding\":\"json\",\"channel\":\"xxxx_xxxx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1585948850111}\"}")
                .build();
        eventQueue.push(sseEvent);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .untilAsserted(() -> Mockito.verify(_notificationParser, Mockito.times(1)).parseMessage(Mockito.anyString()));

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .untilAsserted(() -> Mockito.verify(_notificationProcessor, Mockito.times(1)).process(Mockito.any(SplitChangeNotification.class)));

        OutboundSseEvent sseEventError = new OutboundEvent
                .Builder()
                .name("error")
                .data("{\"message\":\"Token expired\",\"code\":40142,\"statusCode\":401,\"href\":\"https://help.io/error/40142\"}")
                .build();
        eventQueue.push(sseEventError);

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .untilAsserted(() -> Mockito.verify(_notificationParser, Mockito.times(1)).parseError(Mockito.anyString()));

        Awaitility.await()
                .atMost(50L, TimeUnit.SECONDS)
                .untilAsserted(() -> Mockito.verify(_pushStatusTracker, Mockito.times(1)).handleIncomingAblyError(Mockito.any(ErrorNotification.class)));

        Mockito.verify(_pushStatusTracker, Mockito.times(1)).handleSseStatus(SSEClient.StatusMessage.FIRST_EVENT);
    }

    private SSEMockServer buildSSEMockServer(SSEMockServer.SseEventQueue eventQueue) {
        return new SSEMockServer(eventQueue, (token, version, channel) -> {
            if (!"1.1".equals(version)) {
                return new Pair<>(new OutboundEvent.Builder().data("wrong version").build(), false);
            }
            return new Pair<>(null, true);
        });
    }

    private static CloseableHttpClient buildHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(70000))
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(1);
        cm.setDefaultMaxPerRoute(1);

        return HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
