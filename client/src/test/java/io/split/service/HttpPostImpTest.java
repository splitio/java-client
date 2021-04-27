package io.split.service;

import io.split.TestHelper;
import junit.framework.TestCase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

public class HttpPostImpTest{

    private static final String URL = "www.split.io";
    private static int SUCCESSS_CODE = 200;
    private static int ERROR_CODE = 400;

    @Test
    public void testPostWith200() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        CloseableHttpClient client =TestHelper.mockHttpClient(URL, SUCCESSS_CODE);
        HttpPostImp httpPostImp = new HttpPostImp(client);
        httpPostImp.post(URI.create(URL), new Object(), "Metrics");
        Mockito.verify(client, Mockito.times(1)).execute(Mockito.any());
    }

    @Test
    public void testPostWith400() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        CloseableHttpClient client =TestHelper.mockHttpClient(URL, ERROR_CODE);
        HttpPostImp httpPostImp = new HttpPostImp(client);
        httpPostImp.post(URI.create(URL), new Object(), "Metrics");
        Mockito.verify(client, Mockito.times(1)).execute(Mockito.any());
    }
}