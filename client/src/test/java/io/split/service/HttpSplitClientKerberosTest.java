package io.split.service;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import io.split.TestHelper;
import io.split.client.RequestDecorator;
import io.split.client.dtos.*;
import io.split.client.impressions.Impression;
import io.split.client.utils.Json;
import io.split.client.utils.SDKMetadata;
import io.split.client.utils.Utils;
import io.split.engine.common.FetchOptions;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class HttpSplitClientKerberosTest {

    @Test
    public void testGetWithSpecialCharacters() throws URISyntaxException, IOException {
        Map<String, List<String>> responseHeaders = new HashMap<String, List<String>>();
        responseHeaders.put((HttpHeaders.VIA), Arrays.asList("HTTP/1.1 s_proxy_rio1"));

        HttpURLConnection mockHttpURLConnection = Mockito.mock(HttpURLConnection.class);
        when(mockHttpURLConnection.getHeaderFields()).thenReturn(responseHeaders);

        RequestDecorator decorator = new RequestDecorator(null);

        Mockito.when(mockHttpURLConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        ByteArrayInputStream stubInputStream = new ByteArrayInputStream(Files.asCharSource(
                new File("src/test/resources/split-change-special-characters.json"), Charsets.UTF_8).read().getBytes(Charsets.UTF_8));
        when(mockHttpURLConnection.getInputStream()).thenReturn(stubInputStream);

        SplitHttpClientKerberosImpl splitHtpClientKerberos = SplitHttpClientKerberosImpl.create(decorator, "qwerty", metadata());

        Map<String, List<String>> additionalHeaders = Collections.singletonMap("AdditionalHeader",
                Collections.singletonList("add"));

        SplitHttpResponse splitHttpResponse = splitHtpClientKerberos._get(mockHttpURLConnection,
                new FetchOptions.Builder().cacheControlHeaders(true).build(), additionalHeaders);
        SplitChange change = Json.fromJson(splitHttpResponse.body(), SplitChange.class);

        Header[] headers = splitHttpResponse.responseHeaders();
        assertThat(headers[0].getName(), is(equalTo("Via")));
        assertThat(headers[0].getValue(), is(equalTo("[HTTP/1.1 s_proxy_rio1]")));
        Assert.assertNotNull(change);
        Assert.assertEquals(1, change.splits.size());
        Assert.assertNotNull(change.splits.get(0));

        Split split = change.splits.get(0);
        Map<String, String> configs = split.configurations;
        Assert.assertEquals(2, configs.size());
        Assert.assertEquals("{\"test\": \"blue\",\"grüne Straße\": 13}", configs.get("on"));
        Assert.assertEquals("{\"test\": \"blue\",\"size\": 15}", configs.get("off"));
        Assert.assertEquals(2, split.sets.size());
    }

    @Test
    public void testGetParameters() throws URISyntaxException, MalformedURLException {
        URI uri = new URI("https://api.split.io/splitChanges?since=1234567");
        FetchOptions options = new FetchOptions.Builder().cacheControlHeaders(true).build();
        SplitHttpClientKerberosImpl splitHtpClientKerberos = Mockito.mock(SplitHttpClientKerberosImpl.class);
        when(splitHtpClientKerberos.get(uri, options, null)).thenCallRealMethod();

        SplitHttpResponse splitHttpResponse = splitHtpClientKerberos.get(uri, options, null);

        ArgumentCaptor<HttpURLConnection> connectionCaptor = ArgumentCaptor.forClass(HttpURLConnection.class);
        ArgumentCaptor<FetchOptions> optionsCaptor = ArgumentCaptor.forClass(FetchOptions.class);
        ArgumentCaptor<HashMap> headersCaptor = ArgumentCaptor.forClass(HashMap.class);
        verify(splitHtpClientKerberos)._get(connectionCaptor.capture(), optionsCaptor.capture(), headersCaptor.capture());

        assertThat(connectionCaptor.getValue().getRequestMethod(), is(equalTo("GET")));
        assertThat(connectionCaptor.getValue().getURL().toString(), is(equalTo(new URL("https://api.split.io/splitChanges?since=1234567").toString())));

        assertThat(optionsCaptor.getValue().cacheControlHeadersEnabled(), is(equalTo(true)));
    }

    @Test
    public void testGetError() throws URISyntaxException, IOException {
        HttpURLConnection mockHttpURLConnection = Mockito.mock(HttpURLConnection.class);
        RequestDecorator decorator = new RequestDecorator(null);

        Mockito.when(mockHttpURLConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        ByteArrayInputStream stubInputStream = new ByteArrayInputStream(Files.asCharSource(
                new File("/Users/bilalal-shahwany/repos/java/kerberos/java-client/client/src/test/resources/split-change-special-characters.json"), Charsets.UTF_8).read().getBytes(Charsets.UTF_8));
        when(mockHttpURLConnection.getInputStream()).thenReturn(stubInputStream);

        SplitHttpClientKerberosImpl splitHtpClientKerberos = SplitHttpClientKerberosImpl.create(decorator, "qwerty", metadata());
        SplitHttpResponse splitHttpResponse = splitHtpClientKerberos._get(mockHttpURLConnection,
                new FetchOptions.Builder().cacheControlHeaders(true).build(), null);
        Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, (long) splitHttpResponse.statusCode());
    }

    @Test(expected = IllegalStateException.class)
    public void testException() throws URISyntaxException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, IOException {
        URI rootTarget = URI.create("https://api.split.io/splitChanges?since=1234567");
        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("split-change-special-characters.json",
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
        RequestDecorator decorator = null;

        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClientMock, decorator, "qwerty", metadata());
        splitHtpClient.get(rootTarget,
                new FetchOptions.Builder().cacheControlHeaders(true).build(), null);
    }

    @Test
    public void testPost() throws URISyntaxException, IOException, ParseException {
        HttpURLConnection mockHttpURLConnection = Mockito.mock(HttpURLConnection.class);
        RequestDecorator decorator = new RequestDecorator(null);
        Mockito.when(mockHttpURLConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        SplitHttpClientKerberosImpl splitHtpClientKerberos = SplitHttpClientKerberosImpl.create(decorator, "qwerty", metadata());

        // Send impressions
        List<TestImpressions> toSend = Arrays.asList(new TestImpressions("t1", Arrays.asList(
                KeyImpression.fromImpression(new Impression("k1", null, "t1", "on", 123L, "r1", 456L, null)),
                KeyImpression.fromImpression(new Impression("k2", null, "t1", "on", 123L, "r1", 456L, null)),
                KeyImpression.fromImpression(new Impression("k3", null, "t1", "on", 123L, "r1", 456L, null)))),
                new TestImpressions("t2", Arrays.asList(
                        KeyImpression.fromImpression(new Impression("k1", null, "t2", "on", 123L, "r1", 456L, null)),
                        KeyImpression.fromImpression(new Impression("k2", null, "t2", "on", 123L, "r1", 456L, null)),
                        KeyImpression.fromImpression(new Impression("k3", null, "t2", "on", 123L, "r1", 456L, null)))));

        Map<String, List<String>> additionalHeaders = Collections.singletonMap("SplitSDKImpressionsMode",
                Collections.singletonList("OPTIMIZED"));
        when(mockHttpURLConnection.getHeaderFields()).thenReturn(additionalHeaders);

        ByteArrayOutputStream mockOs = Mockito.mock( ByteArrayOutputStream.class);
        when(mockHttpURLConnection.getOutputStream()).thenReturn(mockOs);

        SplitHttpResponse splitHttpResponse = splitHtpClientKerberos._post(mockHttpURLConnection, Utils.toJsonEntity(toSend),
                additionalHeaders);

        // Capture outgoing request and validate it
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(mockOs).write(captor.capture());
        String postBody = EntityUtils.toString(Utils.toJsonEntity(toSend));
        assertThat(captor.getValue(), is(equalTo(postBody.getBytes(StandardCharsets.UTF_8))));

        Header[] headers = splitHttpResponse.responseHeaders();
        assertThat(headers[0].getName(), is(equalTo("SplitSDKImpressionsMode")));
        assertThat(headers[0].getValue(), is(equalTo("[OPTIMIZED]")));

        Assert.assertEquals(200, (long) splitHttpResponse.statusCode());
    }

    @Test
    public void testPotParameters() throws URISyntaxException, IOException {
        URI uri = new URI("https://kubernetesturl.com/split/api/testImpressions/bulk");
        SplitHttpClientKerberosImpl splitHtpClientKerberos = Mockito.mock(SplitHttpClientKerberosImpl.class);
        when(splitHtpClientKerberos.post(uri, null, null)).thenCallRealMethod();

        SplitHttpResponse splitHttpResponse = splitHtpClientKerberos.post(uri, null, null);

        ArgumentCaptor<HttpURLConnection> connectionCaptor = ArgumentCaptor.forClass(HttpURLConnection.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        ArgumentCaptor<HashMap> headersCaptor = ArgumentCaptor.forClass(HashMap.class);
        verify(splitHtpClientKerberos)._post(connectionCaptor.capture(), entityCaptor.capture(), headersCaptor.capture());

        assertThat(connectionCaptor.getValue().getURL().toString(), is(equalTo(new URL("https://kubernetesturl.com/split/api/testImpressions/bulk").toString())));
    }

    @Test(expected = IOException.class)
    public void testPosttException() throws URISyntaxException, IOException {
        HttpURLConnection mockHttpURLConnection = Mockito.mock(HttpURLConnection.class);
        RequestDecorator decorator = new RequestDecorator(null);
        Mockito.when(mockHttpURLConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);

        SplitHttpClientKerberosImpl splitHtpClientKerberos = SplitHttpClientKerberosImpl.create(decorator, "qwerty", metadata());
        SplitHttpResponse splitHttpResponse = splitHtpClientKerberos._post(mockHttpURLConnection,
                Utils.toJsonEntity(Arrays.asList(new String[] { "A", "B", "C", "D" })), null);

        Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, (long) splitHttpResponse.statusCode());
    }

    private SDKMetadata metadata() {
        return new SDKMetadata("java-1.2.3", "1.2.3.4", "someIP");
    }

}
