package io.split.client.events;

import io.split.TestHelper;
import io.split.client.RequestDecorator;
import io.split.client.dtos.Event;
import io.split.service.SplitHttpClient;
import io.split.service.SplitHttpClientImpl;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class EventsSenderTest {

    private static final TelemetryRuntimeProducer TELEMETRY_RUNTIME_CONSUMER = Mockito.mock(TelemetryRuntimeProducer.class);
    private static final CloseableHttpClient CLOSEABLE_HTTP_CLIENT = Mockito.mock(CloseableHttpClient.class);

    @Test
    public void testDefaultURL() throws URISyntaxException {
        SplitHttpClient SPLIT_HTTP_CLIENT = SplitHttpClientImpl.create(CLOSEABLE_HTTP_CLIENT, new RequestDecorator(null));
        URI rootTarget = URI.create("https://api.split.io");
        EventsSender fetcher = EventsSender.create(SPLIT_HTTP_CLIENT, rootTarget, TELEMETRY_RUNTIME_CONSUMER);
        Assert.assertEquals("https://api.split.io/api/events/bulk", fetcher.getBulkEndpoint().toString());
    }

    @Test
    public void testCustomURLNoPathNoBackslash() throws URISyntaxException {
        SplitHttpClient SPLIT_HTTP_CLIENT = SplitHttpClientImpl.create(CLOSEABLE_HTTP_CLIENT, new RequestDecorator(null));
        URI rootTarget = URI.create("https://kubernetesturl.com");
        EventsSender fetcher = EventsSender.create(SPLIT_HTTP_CLIENT, rootTarget, TELEMETRY_RUNTIME_CONSUMER);
        Assert.assertEquals("https://kubernetesturl.com/api/events/bulk", fetcher.getBulkEndpoint().toString());
    }

    @Test
    public void testCustomURLAppendingPath() throws URISyntaxException {
        SplitHttpClient SPLIT_HTTP_CLIENT = SplitHttpClientImpl.create(CLOSEABLE_HTTP_CLIENT, new RequestDecorator(null));
        URI rootTarget = URI.create("https://kubernetesturl.com/split/");
        EventsSender fetcher = EventsSender.create(SPLIT_HTTP_CLIENT, rootTarget, TELEMETRY_RUNTIME_CONSUMER);
        Assert.assertEquals("https://kubernetesturl.com/split/api/events/bulk", fetcher.getBulkEndpoint().toString());
    }

    @Test
    public void testCustomURLAppendingPathNoBackslash() throws URISyntaxException {
        SplitHttpClient SPLIT_HTTP_CLIENT = SplitHttpClientImpl.create(CLOSEABLE_HTTP_CLIENT, new RequestDecorator(null));
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        EventsSender fetcher = EventsSender.create(SPLIT_HTTP_CLIENT, rootTarget, TELEMETRY_RUNTIME_CONSUMER);
        Assert.assertEquals("https://kubernetesturl.com/split/api/events/bulk", fetcher.getBulkEndpoint().toString());
    }
    @Test
    public void testHttpError() throws URISyntaxException, IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = TestHelper.mockHttpClient("", HttpStatus.SC_BAD_REQUEST);
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null));
        EventsSender sender = EventsSender.create(splitHtpClient, rootTarget, TELEMETRY_RUNTIME_CONSUMER);
        // should not raise exception
        sender.sendEvents(new ArrayList<>());
    }
}