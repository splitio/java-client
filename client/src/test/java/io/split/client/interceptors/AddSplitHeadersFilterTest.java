package io.split.client.interceptors;

import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;

import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


@RunWith(MockitoJUnitRunner.class)
public class AddSplitHeadersFilterTest {

    @Captor
    private ArgumentCaptor<String> headerNameCaptor;

    @Captor
    private ArgumentCaptor<String> headerValueCaptor;

    @Test
    public void testHeadersWithIpAndHostname() {
        AddSplitHeadersFilter filter = AddSplitHeadersFilter.instance("abc", true, false);
        HttpRequest req = Mockito.mock(HttpRequest.class);
        HttpContext ctx = Mockito.mock(HttpContext.class);

        filter.process(req, null, ctx);
        Mockito.verify(req, Mockito.times(4)).addHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

        List<String> headerNames = headerNameCaptor.getAllValues();
        List<String> headerValues = headerValueCaptor.getAllValues();

        assertThat(headerNames.size(), is(equalTo(4)));
        assertThat(headerValues.size(), is(equalTo(4)));

        assertThat(headerNames, contains(AddSplitHeadersFilter.AUTHORIZATION_HEADER,
                AddSplitHeadersFilter.CLIENT_VERSION,
                AddSplitHeadersFilter.CLIENT_MACHINE_NAME_HEADER,
                AddSplitHeadersFilter.CLIENT_MACHINE_IP_HEADER));
    }

    @Test
    public void testHeadersWithoutIpAndHostname() {
        AddSplitHeadersFilter filter = AddSplitHeadersFilter.instance("abc", false, false);
        HttpRequest req = Mockito.mock(HttpRequest.class);
        HttpContext ctx = Mockito.mock(HttpContext.class);

        filter.process(req, null, ctx);
        Mockito.verify(req, Mockito.times(2)).addHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

        List<String> headerNames = headerNameCaptor.getAllValues();
        List<String> headerValues = headerValueCaptor.getAllValues();

        assertThat(headerNames.size(), is(equalTo(2)));
        assertThat(headerValues.size(), is(equalTo(2)));

        assertThat(headerNames, contains(AddSplitHeadersFilter.AUTHORIZATION_HEADER,
                AddSplitHeadersFilter.CLIENT_VERSION));

        assertThat(headerNames, not(contains(AddSplitHeadersFilter.CLIENT_MACHINE_NAME_HEADER,
                AddSplitHeadersFilter.CLIENT_MACHINE_IP_HEADER)));
    }

    @Test
    public void testHeadersWithoutIpAndHostnameAndWithClientKey() {
        AddSplitHeadersFilter filter = AddSplitHeadersFilter.instance("ljsdfkjkldfjksldjflksdjflsdjflksd", false, true);
        HttpRequest req = Mockito.mock(HttpRequest.class);
        HttpContext ctx = Mockito.mock(HttpContext.class);

        filter.process(req, null, ctx);
        Mockito.verify(req, Mockito.times(2)).addHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

        List<String> headerNames = headerNameCaptor.getAllValues();
        List<String> headerValues = headerValueCaptor.getAllValues();

        assertEquals(2 ,headerNames.size());
        assertEquals(2, headerValues.size());
        assertThat(headerNames, contains(AddSplitHeadersFilter.CLIENT_VERSION,
                AddSplitHeadersFilter.CLIENT_KEY));
        assertThat(headerNames, not(contains(AddSplitHeadersFilter.CLIENT_MACHINE_NAME_HEADER,
                AddSplitHeadersFilter.CLIENT_MACHINE_IP_HEADER,
                AddSplitHeadersFilter.AUTHORIZATION_HEADER)));
    }

    @Test
    public void testHeadersWithIpAndHostnameAndWithClientKey() {
        AddSplitHeadersFilter filter = AddSplitHeadersFilter.instance("ljsdfkjkldfjksldjflksdjflsdjflksd", true, true);
        HttpRequest req = Mockito.mock(HttpRequest.class);
        HttpContext ctx = Mockito.mock(HttpContext.class);

        filter.process(req, null, ctx);
        Mockito.verify(req, Mockito.times(4)).addHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

        List<String> headerNames = headerNameCaptor.getAllValues();
        List<String> headerValues = headerValueCaptor.getAllValues();

        assertEquals(4 ,headerNames.size());
        assertEquals(4, headerValues.size());
        assertThat(headerNames, contains(AddSplitHeadersFilter.CLIENT_VERSION,
                AddSplitHeadersFilter.CLIENT_MACHINE_NAME_HEADER,
                AddSplitHeadersFilter.CLIENT_MACHINE_IP_HEADER,
                AddSplitHeadersFilter.CLIENT_KEY));
        assertThat(headerNames, not(contains(AddSplitHeadersFilter.AUTHORIZATION_HEADER)));
    }
}
