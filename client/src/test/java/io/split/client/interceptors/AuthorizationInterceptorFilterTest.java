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
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationInterceptorFilterTest {

    @Captor
    private ArgumentCaptor<String> headerNameCaptor;

    @Captor
    private ArgumentCaptor<String> headerValueCaptor;

    @Test
    public void authorizationInterceptorWithValue() throws IOException, HttpException {
        AuthorizationInterceptorFilter filter = AuthorizationInterceptorFilter.instance("api-token-test");
        HttpRequest req = Mockito.mock(HttpRequest.class);
        HttpContext ctx = Mockito.mock(HttpContext.class);

        filter.process(req, null, ctx);
        Mockito.verify(req, Mockito.times(1)).addHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

        List<String> headerNames = headerNameCaptor.getAllValues();
        List<String> headerValues = headerValueCaptor.getAllValues();

        assertEquals(1, headerNames.size());
        assertEquals(1, headerValues.size());

        assertThat(headerNames, contains(AuthorizationInterceptorFilter.AUTHORIZATION_HEADER));
        assertThat(headerValues, contains("Bearer api-token-test"));
    }

    @Test(expected = Exception.class)
    public void authorizationInterceptorWithoutValue() {
        AuthorizationInterceptorFilter filter = AuthorizationInterceptorFilter.instance(null);
    }
}
