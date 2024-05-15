package io.split.service;

import io.split.TestHelper;
import io.split.client.RequestDecorator;
import io.split.client.utils.SDKMetadata;
import io.split.telemetry.domain.enums.HttpParamsWrapper;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

public class HttpPostImpTest {

    private static final String URL = "www.split.io";

    @Test
    public void testPostWith200() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException,
            IOException, URISyntaxException {
        CloseableHttpClient client = TestHelper.mockHttpClient(URL, HttpStatus.SC_OK);
        SplitHttpClient splitHttpClient = SplitHttpClientImpl.create(client, new RequestDecorator(null), "qwerty",
                metadata());
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        HttpPostImp httpPostImp = new HttpPostImp(splitHttpClient, telemetryStorage);
        httpPostImp.post(URI.create(URL), new Object(), "Metrics", HttpParamsWrapper.TELEMETRY);
        Mockito.verify(client, Mockito.times(1)).execute(Mockito.any());
        Assert.assertNotEquals(0, telemetryStorage.getLastSynchronization().getTelemetry());
        Assert.assertEquals(1, telemetryStorage.popHTTPLatencies().getTelemetry().stream().mapToInt(Long::intValue).sum());
    }

    @Test
    public void testPostWith400() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException,
            IOException, URISyntaxException {
        CloseableHttpClient client = TestHelper.mockHttpClient(URL, HttpStatus.SC_CLIENT_ERROR);
        SplitHttpClient splitHttpClient = SplitHttpClientImpl.create(client, new RequestDecorator(null), "qwerty",
                metadata());
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        HttpPostImp httpPostImp = new HttpPostImp(splitHttpClient, telemetryStorage);
        httpPostImp.post(URI.create(URL), new Object(), "Metrics", HttpParamsWrapper.TELEMETRY);
        Mockito.verify(client, Mockito.times(1)).execute(Mockito.any());
        Assert.assertEquals(1, telemetryStorage.popHTTPErrors().getTelemetry().get(Long.valueOf(HttpStatus.SC_CLIENT_ERROR)).intValue());
    }

    private SDKMetadata metadata() {
        return new SDKMetadata("java-1.2.3", "1.2.3.4", "someIP");
    }

}
