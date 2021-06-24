package io.split.service;

import io.split.TestHelper;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.HttpParamsWrapper;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.telemetry.storage.TelemetryStorage;
import junit.framework.TestCase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

public class HttpPostImpTest{

    private static final String URL = "www.split.io";

    @Test
    public void testPostWith200() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        CloseableHttpClient client =TestHelper.mockHttpClient(URL, HttpStatus.SC_OK);
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        HttpPostImp httpPostImp = new HttpPostImp(client, telemetryStorage);
        httpPostImp.post(URI.create(URL), new Object(), "Metrics", HttpParamsWrapper.TELEMETRY);
        Mockito.verify(client, Mockito.times(1)).execute(Mockito.any());
        Assert.assertNotEquals(0, telemetryStorage.getLastSynchronization().get_telemetry());
        Assert.assertEquals(1, telemetryStorage.popHTTPLatencies().get_telemetry().stream().mapToInt(Long::intValue).sum());
    }

    @Test
    public void testPostWith400() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        CloseableHttpClient client =TestHelper.mockHttpClient(URL, HttpStatus.SC_CLIENT_ERROR);
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        HttpPostImp httpPostImp = new HttpPostImp(client, telemetryStorage);
        httpPostImp.post(URI.create(URL), new Object(), "Metrics", HttpParamsWrapper.TELEMETRY);
        Mockito.verify(client, Mockito.times(1)).execute(Mockito.any());

        Assert.assertEquals(1, telemetryStorage.popHTTPErrors().get_telemetry().get(Long.valueOf(HttpStatus.SC_CLIENT_ERROR)).intValue());
    }
}