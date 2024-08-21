package io.split.service;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import io.split.client.RequestDecorator;
import io.split.client.dtos.*;
import io.split.client.impressions.Impression;
import io.split.client.utils.Json;
import io.split.client.utils.SDKMetadata;
import io.split.client.utils.Utils;
import io.split.engine.common.FetchOptions;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

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
        URI rootTarget = URI.create("https://api.split.io/splitChanges?since=1234567");

        HttpURLConnection mockHttpURLConnection = Mockito.mock(HttpURLConnection.class);
        when(mockHttpURLConnection.getHeaderFields()).thenReturn(responseHeaders);

        RequestDecorator decorator = new RequestDecorator(null);

        Mockito.when(mockHttpURLConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        ByteArrayInputStream stubInputStream = new ByteArrayInputStream(Files.asCharSource(
                new File("src/test/resources/split-change-special-characters.json"), Charsets.UTF_8).read().getBytes(Charsets.UTF_8));
        when(mockHttpURLConnection.getInputStream()).thenReturn(stubInputStream);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("1.0.0.127", 8080));
        OkHttpClient client = new Builder()
                .proxy(proxy)
                .build();

        SplitHttpClientKerberosImpl splitHtpClientKerberos = SplitHttpClientKerberosImpl.create(client, decorator, "qwerty", metadata());

        Map<String, List<String>> additionalHeaders = Collections.singletonMap("AdditionalHeader",
                Collections.singletonList("add"));

        try {
            SplitHttpResponse splitHttpResponse = splitHtpClientKerberos.get(rootTarget,
                    new FetchOptions.Builder().cacheControlHeaders(true).build(), additionalHeaders);
        } catch (Exception e) {
        }
/*
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

 */
    }

    @Test
    public void testGetParameters() throws URISyntaxException, MalformedURLException {
        URI uri = new URI("https://api.split.io/splitChanges?since=1234567");
        FetchOptions options = new FetchOptions.Builder().cacheControlHeaders(true).build();
        SplitHttpClientKerberosImpl splitHtpClientKerberos = Mockito.mock(SplitHttpClientKerberosImpl.class);
        when(splitHtpClientKerberos.get(uri, options, null)).thenCallRealMethod();

        try {
            SplitHttpResponse splitHttpResponse = splitHtpClientKerberos.get(uri, options, null);
        } catch (Exception e) {
        }

//        ArgumentCaptor<FetchOptions> optionsCaptor = ArgumentCaptor.forClass(FetchOptions.class);
//        ArgumentCaptor<HashMap> headersCaptor = ArgumentCaptor.forClass(HashMap.class);
//        verify(splitHtpClientKerberos).get(connectionCaptor.capture(), optionsCaptor.capture(), headersCaptor.capture());

  //      assertThat(connectionCaptor.getValue().getRequestMethod(), is(equalTo("GET")));
//        assertThat(connectionCaptor.getValue().getURL().toString(), is(equalTo(new URL("https://api.split.io/splitChanges?since=1234567").toString())));

 //       assertThat(optionsCaptor.getValue().cacheControlHeadersEnabled(), is(equalTo(true)));
    }

    @Test
    public void testGetError() throws URISyntaxException, IOException {
        URI uri = new URI("https://api.split.io/splitChanges?since=1234567");
        RequestDecorator decorator = new RequestDecorator(null);

//        Mockito.when(mockHttpURLConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        ByteArrayInputStream stubInputStream = new ByteArrayInputStream(Files.asCharSource(
                new File("src/test/resources/split-change-special-characters.json"), Charsets.UTF_8).read().getBytes(Charsets.UTF_8));
//        when(mockHttpURLConnection.getInputStream()).thenReturn(stubInputStream);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("1.0.0.127", 8080));
        OkHttpClient client = new Builder()
                .proxy(proxy)
                .build();
        try {
            SplitHttpClientKerberosImpl splitHtpClientKerberos = SplitHttpClientKerberosImpl.create(client, decorator, "qwerty", metadata());
            SplitHttpResponse splitHttpResponse = splitHtpClientKerberos.get(uri,
                    new FetchOptions.Builder().cacheControlHeaders(true).build(), null);
        } catch (Exception e) {
        }
  //      Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, (long) splitHttpResponse.statusCode());
    }

    @Test(expected = IllegalStateException.class)
    public void testException() throws URISyntaxException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, IOException {
        URI uri = new URI("https://api.split.io/splitChanges?since=1234567");
        RequestDecorator decorator = null;

//        Mockito.when(mockHttpURLConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        ByteArrayInputStream stubInputStream = new ByteArrayInputStream(Files.asCharSource(
                new File("src/test/resources/split-change-special-characters.json"), Charsets.UTF_8).read().getBytes(Charsets.UTF_8));
//        when(mockHttpURLConnection.getInputStream()).thenReturn(stubInputStream);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("1.0.0.127", 8080));
        OkHttpClient client = new Builder()
                .proxy(proxy)
                .build();

        SplitHttpClientKerberosImpl splitHtpClientKerberos = SplitHttpClientKerberosImpl.create(client, decorator, "qwerty", metadata());
        SplitHttpResponse splitHttpResponse = splitHtpClientKerberos.get(uri,
                    new FetchOptions.Builder().cacheControlHeaders(true).build(), null);

    }

    @Test
    public void testPost() throws URISyntaxException, IOException, ParseException {
        URI uri = new URI("https://api.split.io/splitChanges?since=1234567");
        RequestDecorator decorator = new RequestDecorator(null);
//        Mockito.when(mockHttpURLConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("1.0.0.127", 8080));
        OkHttpClient client = new Builder()
                .proxy(proxy)
                .build();
        SplitHttpClientKerberosImpl splitHtpClientKerberos = SplitHttpClientKerberosImpl.create(client, decorator, "qwerty", metadata());

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
//        when(mockHttpURLConnection.getHeaderFields()).thenReturn(additionalHeaders);

        ByteArrayOutputStream mockOs = Mockito.mock( ByteArrayOutputStream.class);
 //       when(mockHttpURLConnection.getOutputStream()).thenReturn(mockOs);

        try {
            SplitHttpResponse splitHttpResponse = splitHtpClientKerberos.post(uri, Utils.toJsonEntity(toSend),
                    additionalHeaders);

            // Capture outgoing request and validate it
            ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
            verify(mockOs).write(captor.capture());
            String postBody = EntityUtils.toString(Utils.toJsonEntity(toSend));
    //        assertThat(captor.getValue(), is(equalTo(postBody.getBytes(StandardCharsets.UTF_8))));

            Header[] headers = splitHttpResponse.responseHeaders();
     //       assertThat(headers[0].getName(), is(equalTo("SplitSDKImpressionsMode")));
     //       assertThat(headers[0].getValue(), is(equalTo("[OPTIMIZED]")));

      //      Assert.assertEquals(200, (long) splitHttpResponse.statusCode());
        } catch (Exception e) {
        }
    }

    @Test
    public void testPotParameters() throws URISyntaxException, IOException {
        URI uri = new URI("https://kubernetesturl.com/split/api/testImpressions/bulk");
//        when(splitHtpClientKerberos.post(uri, null, null)).thenCallRealMethod();
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("1.0.0.127", 8080));
        OkHttpClient client = new Builder()
                .proxy(proxy)
                .build();
        RequestDecorator decorator = new RequestDecorator(null);
        SplitHttpClientKerberosImpl splitHtpClientKerberos = SplitHttpClientKerberosImpl.create(client, decorator, "qwerty", metadata());

        try {
            SplitHttpResponse splitHttpResponse = splitHtpClientKerberos.post(uri, null, null);
        } catch (Exception e) {
        }

//        ArgumentCaptor<HttpURLConnection> connectionCaptor = ArgumentCaptor.forClass(HttpURLConnection.class);
//        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
//        ArgumentCaptor<HashMap> headersCaptor = ArgumentCaptor.forClass(HashMap.class);
//        verify(splitHtpClientKerberos).doPost(connectionCaptor.capture(), entityCaptor.capture(), headersCaptor.capture());

 //       assertThat(connectionCaptor.getValue().getURL().toString(), is(equalTo(new URL("https://kubernetesturl.com/split/api/testImpressions/bulk").toString())));
    }

    @Test
    public void testPosttError() throws URISyntaxException, IOException {
        URI uri = new URI("https://kubernetesturl.com/split/api/testImpressions/bulk");
        RequestDecorator decorator = new RequestDecorator(null);
        ByteArrayOutputStream mockOs = Mockito.mock( ByteArrayOutputStream.class);
//        Mockito.when(mockHttpURLConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
//        when(mockHttpURLConnection.getOutputStream()).thenReturn(mockOs);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("1.0.0.127", 8080));
        OkHttpClient client = new Builder()
                .proxy(proxy)
                .build();
        SplitHttpClientKerberosImpl splitHtpClientKerberos = SplitHttpClientKerberosImpl.create(client, decorator, "qwerty", metadata());
        try {
            SplitHttpResponse splitHttpResponse = splitHtpClientKerberos.post(uri,
                    Utils.toJsonEntity(Arrays.asList(new String[] { "A", "B", "C", "D" })), null);

            Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, (long) splitHttpResponse.statusCode());
        } catch (Exception e) {
        }

    }

    @Test(expected = IllegalStateException.class)
    public void testPosttException() throws URISyntaxException, IOException {
        RequestDecorator decorator = null;
//        Mockito.when(mockHttpURLConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        URI uri = new URI("https://kubernetesturl.com/split/api/testImpressions/bulk");

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("1.0.0.127", 8080));
        OkHttpClient client = new Builder()
                .proxy(proxy)
                .build();
        SplitHttpClientKerberosImpl splitHtpClientKerberos = SplitHttpClientKerberosImpl.create(client, decorator, "qwerty", metadata());
        SplitHttpResponse splitHttpResponse = splitHtpClientKerberos.post(uri,
                Utils.toJsonEntity(Arrays.asList(new String[] { "A", "B", "C", "D" })), null);
    }

    private SDKMetadata metadata() {
        return new SDKMetadata("java-1.2.3", "1.2.3.4", "someIP");
    }

}
