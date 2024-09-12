package io.split.httpmodules.okhttp;

import com.sun.tools.javac.util.StringUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import io.split.client.CustomHeaderDecorator;
import io.split.client.RequestDecorator;
import io.split.client.dtos.*;
import io.split.client.impressions.Impression;
import io.split.client.utils.Json;
import io.split.client.utils.SDKMetadata;
import io.split.client.utils.Utils;
import io.split.client.dtos.SplitHttpResponse.Header;
import io.split.engine.common.FetchOptions;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.*;
import okhttp3.HttpUrl;
import okhttp3.Headers;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.HttpURLConnection;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;

public class OkHttpClientImplTest {

    @Test
    public void testGetWithSpecialCharacters() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        BufferedReader br = new BufferedReader(new FileReader("src/test/resources/split-change-special-characters.json"));
        String body;
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            body = sb.toString();
        } finally {
            br.close();
        }
/*
        server.enqueue(new MockResponse().setBody(body).addHeader("via", "HTTP/1.1 s_proxy_rio1"));
        server.start();
        HttpUrl baseUrl = server.url("/v1/");
        URI rootTarget = baseUrl.uri();

        OkHttpClientImpl okHttpClientImpl = mock(OkHttpClientImpl.class);
        OkHttpClient client = new OkHttpClient.Builder().build();
        PowerMockito.doReturn(client).when(okHttpClientImpl).initializeClient(null, "bilal", false,
                0, 0);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setHttpClient(null, "bilal", false,
                0, 0);
        okHttpClientImpl.setHttpClient(null, "bilal", false,
        0, 0);

        Map<String, List<String>> additionalHeaders = Collections.singletonMap("AdditionalHeader",
                Collections.singletonList("add"));
        FetchOptions fetchOptions = new FetchOptions.Builder().cacheControlHeaders(true).build();
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).get(rootTarget, fetchOptions, additionalHeaders);
        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder();
        requestBuilder.url(rootTarget.toString());
        PowerMockito.doReturn(requestBuilder).when(okHttpClientImpl).getRequestBuilder();
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setBasicHeaders(requestBuilder);
        Whitebox.setInternalState(okHttpClientImpl, "_metadata", metadata());
        Whitebox.setInternalState(okHttpClientImpl, "_apikey", "qwerty");
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setAdditionalAndDecoratedHeaders(requestBuilder, additionalHeaders);
        RequestDecorator requestDecorator = new RequestDecorator(null);
//        Whitebox.setInternalState(okHttpClientImpl, "_requestDecorator", requestDecorator);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).getResponseHeaders(any());
        PowerMockito.doReturn(requestBuilder.build()).when(okHttpClientImpl).getRequest(requestBuilder);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).getRequest(requestBuilder);

        SplitHttpResponse splitHttpResponse = okHttpClientImpl.get(rootTarget, fetchOptions, additionalHeaders);

        RecordedRequest request = server.takeRequest();
        server.shutdown();
        Headers requestHeaders = request.getHeaders();

        assertThat(splitHttpResponse.statusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        Assert.assertEquals("/v1/", request.getPath());
        assertThat(requestHeaders.get("Authorization"), is(equalTo("Bearer qwerty"))) ;
        assertThat(requestHeaders.get("SplitSDKClientKey"), is(equalTo("erty")));
        assertThat(requestHeaders.get("SplitSDKVersion"), is(equalTo("java-1.2.3")));
        assertThat(requestHeaders.get("SplitSDKMachineIP"), is(equalTo("1.2.3.4")));
        assertThat(requestHeaders.get("SplitSDKMachineName"), is(equalTo("someIP")));
        assertThat(requestHeaders.get("AdditionalHeader"), is(equalTo("add")));

        SplitChange change = Json.fromJson(splitHttpResponse.body(), SplitChange.class);

        Header[] headers = splitHttpResponse.responseHeaders();
        assertThat(headers[1].getName(), is(equalTo("via")));
        assertThat(headers[1].getValues().get(0), is(equalTo("HTTP/1.1 s_proxy_rio1")));
        assertThat(splitHttpResponse.statusCode(), is(equalTo(200)));
        Assert.assertNotNull(change);
        Assert.assertEquals(1, change.splits.size());
        Assert.assertNotNull(change.splits.get(0));

        Split split = change.splits.get(0);
        Map<String, String> configs = split.configurations;
        Assert.assertEquals(2, configs.size());
        Assert.assertEquals("{\"test\": \"blue\",\"grüne Straße\": 13}", configs.get("on"));
        Assert.assertEquals("{\"test\": \"blue\",\"size\": 15}", configs.get("off"));
        Assert.assertEquals(2, split.sets.size());
        okHttpClientImpl.close();
    }

    @Test
    public void testGetErrors() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("").setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR));
        server.start();
        HttpUrl baseUrl = server.url("/v1/");
        URI rootTarget = baseUrl.uri();

        OkHttpClientImpl okHttpClientImpl = mock(OkHttpClientImpl.class);
        OkHttpClient client = new OkHttpClient.Builder().build();
        PowerMockito.doReturn(client).when(okHttpClientImpl).initializeClient(null, "bilal", false,
                0, 0);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setHttpClient(null, "bilal", false,
                0, 0);
        okHttpClientImpl.setHttpClient(null, "bilal", false,
                0, 0);

        Map<String, List<String>> additionalHeaders = Collections.singletonMap("AdditionalHeader",
                Collections.singletonList("add"));
        FetchOptions fetchOptions = new FetchOptions.Builder().cacheControlHeaders(true).build();
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).get(rootTarget, fetchOptions, additionalHeaders);
        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder();
        requestBuilder.url(rootTarget.toString());
        PowerMockito.doReturn(requestBuilder).when(okHttpClientImpl).getRequestBuilder();
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setBasicHeaders(requestBuilder);
        Whitebox.setInternalState(okHttpClientImpl, "_metadata", metadata());
        Whitebox.setInternalState(okHttpClientImpl, "_apikey", "qwerty");
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setAdditionalAndDecoratedHeaders(requestBuilder, additionalHeaders);
        RequestDecorator requestDecorator = new RequestDecorator(null);
//        Whitebox.setInternalState(okHttpClientImpl, "_requestDecorator", requestDecorator);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).getResponseHeaders(any());

        SplitHttpResponse splitHttpResponse = okHttpClientImpl.get(rootTarget,
                fetchOptions, additionalHeaders);

        RecordedRequest request = server.takeRequest();
        server.shutdown();
        assertThat(splitHttpResponse.statusCode(), is(equalTo(HttpURLConnection.HTTP_INTERNAL_ERROR)));
        okHttpClientImpl.close();
    }

    @Test
    public void testGetParameters() throws IOException, InterruptedException {
        class MyCustomHeaders implements CustomHeaderDecorator {
            public MyCustomHeaders() {}
            @Override
            public Map<String, List<String>> getHeaderOverrides(RequestContext context) {
                Map<String, List<String>> additionalHeaders = context.headers();
                additionalHeaders.put("first", Arrays.asList("1"));
                additionalHeaders.put("second", Arrays.asList("2.1", "2.2"));
                additionalHeaders.put("third", Arrays.asList("3"));
                return additionalHeaders;
            }
        }
        MockWebServer server = new MockWebServer();
        BufferedReader br = new BufferedReader(new FileReader("src/test/resources/split-change-special-characters.json"));
        String body;
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            body = sb.toString();
        } finally {
            br.close();
        }

        server.enqueue(new MockResponse().setBody(body).addHeader("via", "HTTP/1.1 s_proxy_rio1"));
        server.start();
        HttpUrl baseUrl = server.url("/splitChanges?since=1234567");
        URI rootTarget = baseUrl.uri();
        RequestDecorator requestDecorator = new RequestDecorator(new MyCustomHeaders());

        OkHttpClientImpl okHttpClientImpl = mock(OkHttpClientImpl.class);
        OkHttpClient client = new OkHttpClient.Builder().build();
        PowerMockito.doReturn(client).when(okHttpClientImpl).initializeClient(null, "bilal", false,
                0, 0);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setHttpClient(null, "bilal", false,
                0, 0);
        okHttpClientImpl.setHttpClient(null, "bilal", false,
                0, 0);

        FetchOptions fetchOptions = new FetchOptions.Builder().cacheControlHeaders(true).build();
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).get(rootTarget, fetchOptions, null);
        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder();
        requestBuilder.url(rootTarget.toString());
        PowerMockito.doReturn(requestBuilder).when(okHttpClientImpl).getRequestBuilder();
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setBasicHeaders(requestBuilder);
        Whitebox.setInternalState(okHttpClientImpl, "_metadata", metadata());
        Whitebox.setInternalState(okHttpClientImpl, "_apikey", "qwerty");
//        Whitebox.setInternalState(okHttpClientImpl, "_requestDecorator", requestDecorator);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setAdditionalAndDecoratedHeaders(requestBuilder, null);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).getResponseHeaders(any());
        FetchOptions options = new FetchOptions.Builder().cacheControlHeaders(true).build();

        SplitHttpResponse splitHttpResponse = okHttpClientImpl.get(rootTarget, options, null);

        RecordedRequest request = server.takeRequest();
        server.shutdown();
        Headers requestHeaders = request.getHeaders();

        assertThat(requestHeaders.get("Cache-Control"), is(equalTo("no-cache")));
//        assertThat(requestHeaders.get("first"), is(equalTo("1")));
//        assertThat(requestHeaders.values("second"), is(equalTo(Arrays.asList("2.1","2.2"))));
//        assertThat(requestHeaders.get("third"), is(equalTo("3")));
        Assert.assertEquals("/splitChanges?since=1234567", request.getPath());
        assertThat(request.getMethod(), is(equalTo("GET")));
    }

    @Test(expected = IllegalStateException.class)
    public void testException() throws URISyntaxException, IOException {
        URI rootTarget = new URI("https://api.split.io/splitChanges?since=1234567");
        RequestDecorator requestDecorator = null;

        OkHttpClientImpl okHttpClientImpl = mock(OkHttpClientImpl.class);
        OkHttpClient client = new OkHttpClient.Builder().build();
        PowerMockito.doReturn(client).when(okHttpClientImpl).initializeClient(null, "bilal", false,
                0, 0);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setHttpClient(null, "bilal", false,
                0, 0);
        okHttpClientImpl.setHttpClient(null, "bilal", false,
                0, 0);

        FetchOptions fetchOptions = new FetchOptions.Builder().cacheControlHeaders(true).build();
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).get(rootTarget, fetchOptions, null);
        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder();
        requestBuilder.url(rootTarget.toString());
        PowerMockito.doReturn(requestBuilder).when(okHttpClientImpl).getRequestBuilder();
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setBasicHeaders(requestBuilder);
        Whitebox.setInternalState(okHttpClientImpl, "_metadata", metadata());
        Whitebox.setInternalState(okHttpClientImpl, "_apikey", "qwerty");
        Whitebox.setInternalState(okHttpClientImpl, "_requestDecorator", requestDecorator);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setAdditionalAndDecoratedHeaders(requestBuilder, null);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).getResponseHeaders(any());
        FetchOptions options = new FetchOptions.Builder().cacheControlHeaders(true).build();

        SplitHttpResponse splitHttpResponse = okHttpClientImpl.get(rootTarget,
                    new FetchOptions.Builder().cacheControlHeaders(true).build(), null);
    }



    @Test
    public void testPost() throws IOException,  InterruptedException {
        MockWebServer server = new MockWebServer();

        server.enqueue(new MockResponse().addHeader("via", "HTTP/1.1 s_proxy_rio1"));
        server.start();
        HttpUrl baseUrl = server.url("/impressions");
        URI rootTarget = baseUrl.uri();
        RequestDecorator requestDecorator = new RequestDecorator(null);

        OkHttpClientImpl okHttpClientImpl = mock(OkHttpClientImpl.class);
        OkHttpClient client = new OkHttpClient.Builder().build();
        PowerMockito.doReturn(client).when(okHttpClientImpl).initializeClient(null, "bilal", false,
                0, 0);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setHttpClient(null, "bilal", false,
                0, 0);
        okHttpClientImpl.setHttpClient(null, "bilal", false,
                0, 0);
        Map<String, List<String>> additionalHeaders = Collections.singletonMap("SplitSDKImpressionsMode",
                Collections.singletonList("OPTIMIZED"));

        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder();
        requestBuilder.url(rootTarget.toString());
        PowerMockito.doReturn(requestBuilder).when(okHttpClientImpl).getRequestBuilder();
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setBasicHeaders(requestBuilder);
        Whitebox.setInternalState(okHttpClientImpl, "_metadata", metadata());
        Whitebox.setInternalState(okHttpClientImpl, "_apikey", "qwerty");
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setAdditionalAndDecoratedHeaders(requestBuilder, additionalHeaders);
//        Whitebox.setInternalState(okHttpClientImpl, "_requestDecorator", requestDecorator);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).getResponseHeaders(any());
        // Send impressions
        List<TestImpressions> toSend = Arrays.asList(new TestImpressions("t1", Arrays.asList(
                KeyImpression.fromImpression(new Impression("k1", null, "t1", "on", 123L, "r1", 456L, null)),
                KeyImpression.fromImpression(new Impression("k2", null, "t1", "on", 123L, "r1", 456L, null)),
                KeyImpression.fromImpression(new Impression("k3", null, "t1", "on", 123L, "r1", 456L, null)))),
                new TestImpressions("t2", Arrays.asList(
                        KeyImpression.fromImpression(new Impression("k1", null, "t2", "on", 123L, "r1", 456L, null)),
                        KeyImpression.fromImpression(new Impression("k2", null, "t2", "on", 123L, "r1", 456L, null)),
                        KeyImpression.fromImpression(new Impression("k3", null, "t2", "on", 123L, "r1", 456L, null)))));
        String data = Json.toJson(toSend);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).post(rootTarget, data,
                additionalHeaders);

        SplitHttpResponse splitHttpResponse = okHttpClientImpl.post(rootTarget, data,
                additionalHeaders);

        RecordedRequest request = server.takeRequest();
        server.shutdown();
        Headers requestHeaders = request.getHeaders();

        Assert.assertEquals("POST /impressions HTTP/1.1", request.getRequestLine());
        Assert.assertEquals(data, request.getBody().readUtf8());
        assertThat(requestHeaders.get("Authorization"), is(equalTo("Bearer qwerty"))) ;
        assertThat(requestHeaders.get("SplitSDKClientKey"), is(equalTo("erty")));
        assertThat(requestHeaders.get("SplitSDKVersion"), is(equalTo("java-1.2.3")));
        assertThat(requestHeaders.get("SplitSDKMachineIP"), is(equalTo("1.2.3.4")));
        assertThat(requestHeaders.get("SplitSDKMachineName"), is(equalTo("someIP")));
        assertThat(requestHeaders.get("SplitSDKImpressionsMode"), is(equalTo("OPTIMIZED")));

        Header[] headers = splitHttpResponse.responseHeaders();
        assertThat(headers[1].getName(), is(equalTo("via")));
        assertThat(headers[1].getValues().get(0), is(equalTo("HTTP/1.1 s_proxy_rio1")));
        assertThat(splitHttpResponse.statusCode(), is(equalTo(200)));
    }

    @Test
    public void testPostErrors() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("").setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR));
        server.start();
        HttpUrl baseUrl = server.url("/v1/");
        URI rootTarget = baseUrl.uri();
        RequestDecorator requestDecorator = new RequestDecorator(null);

        OkHttpClientImpl okHttpClientImpl = mock(OkHttpClientImpl.class);
        OkHttpClient client = new OkHttpClient.Builder().build();
        PowerMockito.doReturn(client).when(okHttpClientImpl).initializeClient(null, "bilal", false,
                0, 0);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setHttpClient(null, "bilal", false,
                0, 0);
        okHttpClientImpl.setHttpClient(null, "bilal", false,
                0, 0);
        Map<String, List<String>> additionalHeaders = Collections.singletonMap("SplitSDKImpressionsMode",
                Collections.singletonList("OPTIMIZED"));

        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder();
        requestBuilder.url(rootTarget.toString());
        PowerMockito.doReturn(requestBuilder).when(okHttpClientImpl).getRequestBuilder();
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setBasicHeaders(requestBuilder);
        Whitebox.setInternalState(okHttpClientImpl, "_metadata", metadata());
        Whitebox.setInternalState(okHttpClientImpl, "_apikey", "qwerty");
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setAdditionalAndDecoratedHeaders(requestBuilder, additionalHeaders);
//        Whitebox.setInternalState(okHttpClientImpl, "_requestDecorator", requestDecorator);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).getResponseHeaders(any());

        String data = Json.toJson("<>");
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).post(rootTarget, data,
                additionalHeaders);

        SplitHttpResponse splitHttpResponse = okHttpClientImpl.post(rootTarget, data,
                additionalHeaders);

        RecordedRequest request = server.takeRequest();
        server.shutdown();
        assertThat(splitHttpResponse.statusCode(), is(equalTo(HttpURLConnection.HTTP_INTERNAL_ERROR)));
        okHttpClientImpl.close();
    }

    @Test(expected = IllegalStateException.class)
    public void testPosttException() throws URISyntaxException, IOException {
        URI rootTarget = new URI("https://kubernetesturl.com/split/api/testImpressions/bulk");

        OkHttpClientImpl okHttpClientImpl = mock(OkHttpClientImpl.class);
        OkHttpClient client = new OkHttpClient.Builder().build();
        PowerMockito.doReturn(client).when(okHttpClientImpl).initializeClient(null, "bilal", false,
                0, 0);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setHttpClient(null, "bilal", false,
                0, 0);
        okHttpClientImpl.setHttpClient(null, "bilal", false,
                0, 0);
        Map<String, List<String>> additionalHeaders = Collections.singletonMap("SplitSDKImpressionsMode",
                Collections.singletonList("OPTIMIZED"));

        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder();
        requestBuilder.url(rootTarget.toString());
        PowerMockito.doReturn(requestBuilder).when(okHttpClientImpl).getRequestBuilder();
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setBasicHeaders(requestBuilder);
        Whitebox.setInternalState(okHttpClientImpl, "_metadata", metadata());
        Whitebox.setInternalState(okHttpClientImpl, "_apikey", "qwerty");
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).setAdditionalAndDecoratedHeaders(requestBuilder, additionalHeaders);
        Whitebox.setInternalState(okHttpClientImpl, "_requestDecorator", requestDecorator);
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).getResponseHeaders(any());

        String data = Json.toJson("<>");
        PowerMockito.doCallRealMethod().when(okHttpClientImpl).post(rootTarget, data,
                additionalHeaders);

        SplitHttpResponse splitHttpResponse = okHttpClientImpl.post(rootTarget, data,
                additionalHeaders);
*/
    }

    private SDKMetadata metadata() {
        return new SDKMetadata("java-1.2.3", "1.2.3.4", "someIP");
    }


}
