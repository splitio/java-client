package io.split.client.impressions;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.split.client.dtos.ImpressionCount;
import io.split.client.dtos.KeyImpression;
import io.split.client.dtos.TestImpressions;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpImpressionsSenderTest {

    @Test
    public void testDefaultURL() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        HttpImpressionsSender fetcher = HttpImpressionsSender.create(httpClient, rootTarget, ImpressionsManager.Mode.DEBUG);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://api.split.io/api/testImpressions/bulk")));
    }

    @Test
    public void testCustomURLNoPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        HttpImpressionsSender fetcher = HttpImpressionsSender.create(httpClient, rootTarget, ImpressionsManager.Mode.DEBUG);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/api/testImpressions/bulk")));
    }

    @Test
    public void testCustomURLAppendingPath() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split/");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        HttpImpressionsSender fetcher = HttpImpressionsSender.create(httpClient, rootTarget, ImpressionsManager.Mode.DEBUG);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/testImpressions/bulk")));
    }

    @Test
    public void testCustomURLAppendingPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        HttpImpressionsSender fetcher = HttpImpressionsSender.create(httpClient, rootTarget, ImpressionsManager.Mode.DEBUG);
        Assert.assertThat(fetcher.getTarget().toString(), Matchers.is(Matchers.equalTo("https://kubernetesturl.com/split/api/testImpressions/bulk")));
    }

    @Test
    public void testImpressionCountsEndpointOptimized() throws URISyntaxException, IOException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");

        // Setup response mock
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(httpClient.execute(Mockito.any())).thenReturn(response);

        // Send counters
        HttpImpressionsSender sender = HttpImpressionsSender.create(httpClient, rootTarget, ImpressionsManager.Mode.OPTIMIZED);
        HashMap<ImpressionCounter.Key, Integer> toSend = new HashMap<>();
        toSend.put(new ImpressionCounter.Key("test1", 0), 4);
        toSend.put(new ImpressionCounter.Key("test2", 0), 5);
        sender.postCounters(toSend);

        // Capture outgoing request and validate it
        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(captor.capture());
        HttpUriRequest request = captor.getValue();
        assertThat(request.getURI(), is(equalTo(URI.create("https://kubernetesturl.com/split/api/testImpressions/count"))));
        assertThat(request.getAllHeaders().length, is(0));
        assertThat(request, instanceOf(HttpPost.class));
        HttpPost asPostRequest = (HttpPost) request;
        InputStreamReader reader = new InputStreamReader(asPostRequest.getEntity().getContent());
        Gson gson = new Gson();
        ImpressionCount payload = gson.fromJson(reader, ImpressionCount.class);
        assertThat(payload.perFeature.size(), is(equalTo(2)));
        assertThat(payload.perFeature, contains(new ImpressionCount.CountPerFeature("test1", 0, 4),
                new ImpressionCount.CountPerFeature("test2", 0, 5)));
    }

    @Test
    public void testImpressionCountsEndpointDebug() throws URISyntaxException, IOException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");

        // Setup response mock
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(httpClient.execute(Mockito.any())).thenReturn(response);

        // Send counters
        HttpImpressionsSender sender = HttpImpressionsSender.create(httpClient, rootTarget, ImpressionsManager.Mode.DEBUG);
        HashMap<ImpressionCounter.Key, Integer> toSend = new HashMap<>();
        toSend.put(new ImpressionCounter.Key("test1", 0), 4);
        toSend.put(new ImpressionCounter.Key("test2", 0), 5);
        sender.postCounters(toSend);

        // Assert that the HTTP client was not called
        verify(httpClient, Mockito.never()).execute(Mockito.any());
    }

    @Test
    public void testImpressionBulksEndpoint() throws URISyntaxException, IOException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");

        // Setup response mock
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(httpClient.execute(Mockito.any())).thenReturn(response);
        HttpImpressionsSender sender = HttpImpressionsSender.create(httpClient, rootTarget, ImpressionsManager.Mode.OPTIMIZED);

        // Send impressions
        List<TestImpressions> toSend = Arrays.asList(new TestImpressions("t1", Arrays.asList(
                KeyImpression.fromImpression(new Impression("k1", null, "t1", "on", 123L, "r1", 456L, null)),
                KeyImpression.fromImpression(new Impression("k2", null, "t1", "on", 123L, "r1", 456L, null)),
                KeyImpression.fromImpression(new Impression("k3", null, "t1", "on", 123L, "r1", 456L, null))
            )), new TestImpressions("t2", Arrays.asList(
                KeyImpression.fromImpression(new Impression("k1", null, "t2", "on", 123L, "r1", 456L, null)),
                KeyImpression.fromImpression(new Impression("k2", null, "t2", "on", 123L, "r1", 456L, null)),
                KeyImpression.fromImpression(new Impression("k3", null, "t2", "on", 123L, "r1", 456L, null))
        )));
        sender.postImpressionsBulk(toSend);

        // Capture outgoing request and validate it
        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(captor.capture());
        HttpUriRequest request = captor.getValue();
        assertThat(request.getURI(), is(equalTo(URI.create("https://kubernetesturl.com/split/api/testImpressions/bulk"))));
        assertThat(request.getAllHeaders().length, is(1));
        assertThat(request.getFirstHeader("SplitSDKImpressionsMode").getValue(), is(equalTo("OPTIMIZED")));
        assertThat(request, instanceOf(HttpPost.class));
        HttpPost asPostRequest = (HttpPost) request;
        InputStreamReader reader = new InputStreamReader(asPostRequest.getEntity().getContent());
        Gson gson = new Gson();
        List<TestImpressions> payload = gson.fromJson(reader, new TypeToken<List<TestImpressions>>() { }.getType());
        assertThat(payload.size(), is(equalTo(2)));

        // Do the same flow for imrpessionsMode = debug
        Mockito.reset(httpClient, response, statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(httpClient.execute(Mockito.any())).thenReturn(response);
        sender = HttpImpressionsSender.create(httpClient, rootTarget, ImpressionsManager.Mode.DEBUG);
        sender.postImpressionsBulk(toSend);
        captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(captor.capture());
        request = captor.getValue();
        assertThat(request.getAllHeaders().length, is(1));
        assertThat(request.getFirstHeader("SplitSDKImpressionsMode").getValue(), is(equalTo("DEBUG")));
    }

}
