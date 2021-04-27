package io.split.service;

import io.split.TestHelper;
import junit.framework.TestCase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
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
        HttpPostImp httpPostImp = new HttpPostImp(client);
        httpPostImp.post(URI.create(URL), new Object(), "Metrics");
        Mockito.verify(client, Mockito.times(1)).execute(Mockito.any());
    }

    @Test
    public void testPostWith400() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        CloseableHttpClient client =TestHelper.mockHttpClient(URL, HttpStatus.SC_CLIENT_ERROR);
        HttpPostImp httpPostImp = new HttpPostImp(client);
        httpPostImp.post(URI.create(URL), new Object(), "Metrics");
        Mockito.verify(client, Mockito.times(1)).execute(Mockito.any());
    }
}