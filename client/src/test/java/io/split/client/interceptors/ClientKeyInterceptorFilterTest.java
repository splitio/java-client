package io.split.client.interceptors;

import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ClientKeyInterceptorFilterTest {
    @Captor
    private ArgumentCaptor<String> headerNameCaptor;

    @Captor
    private ArgumentCaptor<String> headerValueCaptor;

    @Test
    public void ClientKeyInterceptorFilter() throws IOException, HttpException {
        ClientKeyInterceptorFilter filter = ClientKeyInterceptorFilter.instance("api-token-test-1234");
        HttpRequest req = Mockito.mock(HttpRequest.class);
        HttpContext ctx = Mockito.mock(HttpContext.class);

        filter.process(req, null, ctx);
        Mockito.verify(req, Mockito.times(1)).addHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

        List<String> headerNames = headerNameCaptor.getAllValues();
        List<String> headerValues = headerValueCaptor.getAllValues();

        assertEquals(1, headerNames.size());
        assertEquals(1, headerValues.size());

        assertThat(headerNames, contains(ClientKeyInterceptorFilter.CLIENT_KEY));
        assertThat(headerValues, contains("1234"));
    }
}
