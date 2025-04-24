package io.split.client;

import io.split.Spec;
import io.split.TestHelper;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.client.utils.Json;
import io.split.client.utils.SDKMetadata;
import io.split.engine.common.FetchOptions;
import io.split.engine.metrics.Metrics;
import io.split.service.SplitHttpClient;
import io.split.service.SplitHttpClientImpl;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.telemetry.storage.TelemetryStorage;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

public class HttpSplitChangeFetcherTest {
    private static final TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);

    @Test
    public void testDefaultURL() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        Metrics.NoopMetrics metrics = new Metrics.NoopMetrics();
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null), "qwerty",
                metadata());

        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget, TELEMETRY_STORAGE, false);
        Assert.assertEquals("https://api.split.io/api/splitChanges", fetcher.getTarget().toString());
    }

    @Test
    public void testCustomURLNoPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null), "qwerty",
                metadata());

        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget, TELEMETRY_STORAGE, false);
        Assert.assertEquals("https://kubernetesturl.com/split/api/splitChanges", fetcher.getTarget().toString());
    }

    @Test
    public void testCustomURLAppendingPath() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split/");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null), "qwerty",
                metadata());
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget, TELEMETRY_STORAGE, false);
        Assert.assertEquals("https://kubernetesturl.com/split/api/splitChanges", fetcher.getTarget().toString());
    }

    @Test
    public void testCustomURLAppendingPathNoBackslash() throws URISyntaxException {
        URI rootTarget = URI.create("https://kubernetesturl.com/split");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClient, new RequestDecorator(null), "qwerty",
                metadata());
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget, TELEMETRY_STORAGE, false);
        Assert.assertEquals("https://kubernetesturl.com/split/api/splitChanges", fetcher.getTarget().toString());
    }

    @Test
    public void testFetcherWithSpecialCharacters() throws URISyntaxException, InvocationTargetException,
            NoSuchMethodException, IllegalAccessException, IOException {
        URI rootTarget = URI.create("https://api.split.io");

        CloseableHttpClient httpClientMock = TestHelper.mockHttpClient("split-change-special-characters.json",
                HttpStatus.SC_OK);
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClientMock, new RequestDecorator(null),
                "qwerty",
                metadata());

        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget, TELEMETRY_STORAGE, false);

        SplitChange change = fetcher.fetch(1234567, -1, new FetchOptions.Builder().cacheControlHeaders(true).build());

        Assert.assertNotNull(change);
        Assert.assertEquals(1, change.featureFlags.d.size());
        Assert.assertNotNull(change.featureFlags.d.get(0));

        Split split = change.featureFlags.d.get(0);
        Map<String, String> configs = split.configurations;
        Assert.assertEquals(2, configs.size());
        Assert.assertEquals("{\"test\": \"blue\",\"grüne Straße\": 13}", configs.get("on"));
        Assert.assertEquals("{\"test\": \"blue\",\"size\": 15}", configs.get("off"));
        Assert.assertEquals(2, split.sets.size());
    }

    @Test
    public void testFetcherWithCDNBypassOption() throws IOException, URISyntaxException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        URI rootTarget = URI.create("https://api.split.io");

        HttpEntity entityMock = Mockito.mock(HttpEntity.class);
        when(entityMock.getContent())
                .thenReturn(new ByteArrayInputStream("{\"ff\":{\"t\": 1,\"s\": -1,\"d\": []},\"rbs\":{\"t\": -1,\"s\": -1,\"d\": []}}".
                        getBytes(StandardCharsets.UTF_8)));
        ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
        when(response.getCode()).thenReturn(200);
        when(response.getEntity()).thenReturn(entityMock);
        when(response.getHeaders()).thenReturn(new Header[0]);

        ArgumentCaptor<ClassicHttpRequest> requestCaptor = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        when(httpClientMock.execute(requestCaptor.capture()))
                .thenReturn(TestHelper.classicResponseToCloseableMock(response));
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClientMock, new RequestDecorator(null),
                "qwerty", metadata());

        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget,
                Mockito.mock(TelemetryRuntimeProducer.class), false);

        fetcher.fetch(-1, -1, new FetchOptions.Builder().targetChangeNumber(123).build());
        // TODO: Fix the test with integration tests update
//        fetcher.fetch(-1, -1, new FetchOptions.Builder().build());
        List<ClassicHttpRequest> captured = requestCaptor.getAllValues();
        Assert.assertEquals(captured.size(), 1);
        Assert.assertTrue(captured.get(0).getUri().toString().contains("till=123"));
//        Assert.assertFalse(captured.get(1).getUri().toString().contains("till="));
    }

    @Test
    public void testRandomNumberGeneration() throws URISyntaxException {
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClientMock, new RequestDecorator(null),
                "qwerty", metadata());
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget,
                Mockito.mock(TelemetryRuntimeProducer.class), false);

        Set<Long> seen = new HashSet<>();
        long min = (long) Math.pow(2, 63) * (-1);
        final long total = 10000000;
        for (long x = 0; x < total; x++) {
            long r = fetcher.makeRandomTill();
            Assert.assertTrue(r < 0 && r > min);
            seen.add(r);
        }

        Assert.assertTrue(seen.size() >= (total * 0.9999));
    }

    @Test(expected = IllegalStateException.class)
    public void testURLTooLong() throws IOException, URISyntaxException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        URI rootTarget = URI.create("https://api.split.io");

        HttpEntity entityMock = Mockito.mock(HttpEntity.class);
        when(entityMock.getContent())
                .thenReturn(new ByteArrayInputStream("{\"till\": 1}".getBytes(StandardCharsets.UTF_8)));
        ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
        when(response.getCode()).thenReturn(414);
        when(response.getEntity()).thenReturn(entityMock);
        when(response.getHeaders()).thenReturn(new Header[0]);
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        ArgumentCaptor<ClassicHttpRequest> requestCaptor = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        when(httpClientMock.execute(requestCaptor.capture()))
                .thenReturn(TestHelper.classicResponseToCloseableMock(response));

        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClientMock, new RequestDecorator(null),
                "qwerty", metadata());
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget,
                Mockito.mock(TelemetryRuntimeProducer.class), false);
        List<String> sets = new ArrayList<String>();
        for (Integer i = 0; i < 100; i++) {
            sets.add("set" + i.toString());
        }
        String result = sets.stream()
                .map(n -> String.valueOf(n))
                .collect(Collectors.joining(",", "", ""));
        fetcher.fetch(-1, -1, new FetchOptions.Builder().flagSetsFilter(result).cacheControlHeaders(false).build());
    }

    @Test
    public void testSwitchingToOldSpec() throws URISyntaxException, InvocationTargetException,
            NoSuchMethodException, IllegalAccessException, IOException, NoSuchFieldException, InterruptedException {
        Spec.SPEC_VERSION = Spec.SPEC_1_3;
        URI rootTarget = URI.create("https://api.split.io");
        CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
        HttpEntity entityMock = Mockito.mock(HttpEntity.class);
        when(entityMock.getContent())
                .thenReturn(new ByteArrayInputStream("{\"till\": -1, \"since\": -1, \"splits\": []}".getBytes(StandardCharsets.UTF_8)));
        HttpEntity entityMock2 = Mockito.mock(HttpEntity.class);
        when(entityMock2.getContent())
                .thenReturn(new ByteArrayInputStream("{\"till\": 123, \"since\": 122, \"splits\": [{\"name\":\"some\"}, {\"name\":\"some2\"}]}".getBytes(StandardCharsets.UTF_8)));
        HttpEntity entityMock3 = Mockito.mock(HttpEntity.class);
        when(entityMock3.getContent())
                .thenReturn(new ByteArrayInputStream("{\"till\": 123, \"since\": 122, \"splits\": [{\"name\":\"some\"}, {\"name\":\"some2\"}]}".getBytes(StandardCharsets.UTF_8)));
        HttpEntity entityMock4 = Mockito.mock(HttpEntity.class);
        when(entityMock4.getContent())
                .thenReturn(new ByteArrayInputStream("{\"ff\":{\"t\": 123, \"s\": 122, \"d\": [{\"name\":\"some\"}, {\"name\":\"some2\"}]}, \"rbs\":{\"t\": -1, \"s\": -1, \"d\": []}}".getBytes(StandardCharsets.UTF_8)));
        ClassicHttpResponse response1 = Mockito.mock(ClassicHttpResponse.class);
        when(response1.getCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        when(response1.getEntity()).thenReturn(entityMock);
        when(response1.getHeaders()).thenReturn(new Header[0]);

        ClassicHttpResponse response2 = Mockito.mock(ClassicHttpResponse.class);
        when(response2.getCode()).thenReturn(HttpStatus.SC_OK);
        when(response2.getEntity()).thenReturn(entityMock2);
        when(response2.getHeaders()).thenReturn(new Header[0]);

        ClassicHttpResponse response3 = Mockito.mock(ClassicHttpResponse.class);
        when(response3.getCode()).thenReturn(HttpStatus.SC_OK);
        when(response3.getEntity()).thenReturn(entityMock3);
        when(response3.getHeaders()).thenReturn(new Header[0]);

        ClassicHttpResponse response4 = Mockito.mock(ClassicHttpResponse.class);
        when(response4.getCode()).thenReturn(HttpStatus.SC_OK);
        when(response4.getEntity()).thenReturn(entityMock4);
        when(response4.getHeaders()).thenReturn(new Header[0]);

        ArgumentCaptor<ClassicHttpRequest> requestCaptor = ArgumentCaptor.forClass(ClassicHttpRequest.class);

        when(httpClientMock.execute(requestCaptor.capture()))
                .thenReturn(TestHelper.classicResponseToCloseableMock(response1))
                .thenReturn(TestHelper.classicResponseToCloseableMock(response2))
                .thenReturn(TestHelper.classicResponseToCloseableMock(response1))
                .thenReturn(TestHelper.classicResponseToCloseableMock(response3))
                .thenReturn(TestHelper.classicResponseToCloseableMock(response4));

        SplitHttpClient splitHtpClient = SplitHttpClientImpl.create(httpClientMock, new RequestDecorator(null),
                "qwerty", metadata());
        HttpSplitChangeFetcher fetcher = HttpSplitChangeFetcher.create(splitHtpClient, rootTarget,
                Mockito.mock(TelemetryRuntimeProducer.class), true);

        SplitChange change = fetcher.fetch(-1, -1, new FetchOptions.Builder().cacheControlHeaders(true).build());

        Assert.assertEquals(Spec.SPEC_1_1, Spec.SPEC_VERSION);
        List<ClassicHttpRequest> captured = requestCaptor.getAllValues();
        Assert.assertEquals(captured.size(), 2);
        Assert.assertTrue(captured.get(0).getUri().toString().contains("s=1.3"));
        Assert.assertTrue(captured.get(1).getUri().toString().contains("s=1.1"));
        Assert.assertEquals(122, change.featureFlags.s);
        Assert.assertEquals(123, change.featureFlags.t);
        Assert.assertEquals(2, change.featureFlags.d.size());
        Assert.assertEquals(Json.fromJson("{\"name\":\"some\"}", Split.class).name, change.featureFlags.d.get(0).name);
        Assert.assertEquals(Json.fromJson("{\"name\":\"some2\"}", Split.class).name, change.featureFlags.d.get(1).name);
        Assert.assertEquals(0, change.ruleBasedSegments.d.size());
        Assert.assertEquals(-1, change.ruleBasedSegments.s);
        Assert.assertEquals(-1, change.ruleBasedSegments.t);

        // Set proxy interval to low number to force check for spec 1.3
        Field proxyInterval = fetcher.getClass().getDeclaredField("PROXY_CHECK_INTERVAL_MILLISECONDS_SS");
        proxyInterval.setAccessible(true);
        proxyInterval.set(fetcher, 5);
        Thread.sleep(1000);
        change = fetcher.fetch(-1, -1, new FetchOptions.Builder().cacheControlHeaders(true).build());

        Assert.assertEquals(Spec.SPEC_1_1, Spec.SPEC_VERSION);
        Assert.assertTrue(captured.get(2).getUri().toString().contains("s=1.3"));
        Assert.assertTrue(captured.get(3).getUri().toString().contains("s=1.1"));
        Assert.assertEquals(122, change.featureFlags.s);
        Assert.assertEquals(123, change.featureFlags.t);
        Assert.assertEquals(2, change.featureFlags.d.size());
        Assert.assertEquals(Json.fromJson("{\"name\":\"some\"}", Split.class).name, change.featureFlags.d.get(0).name);
        Assert.assertEquals(Json.fromJson("{\"name\":\"some2\"}", Split.class).name, change.featureFlags.d.get(1).name);

        // test if proxy is upgraded and spec 1.3 now works.
        Thread.sleep(1000);
        change = fetcher.fetch(-1, -1, new FetchOptions.Builder().cacheControlHeaders(true).build());
        Assert.assertEquals(Spec.SPEC_1_3, Spec.SPEC_VERSION);
        Assert.assertTrue(captured.get(4).getUri().toString().contains("s=1.3"));
        Assert.assertEquals(122, change.featureFlags.s);
        Assert.assertEquals(123, change.featureFlags.t);
        Assert.assertEquals(2, change.featureFlags.d.size());
        Assert.assertEquals(Json.fromJson("{\"name\":\"some\"}", Split.class).name, change.featureFlags.d.get(0).name);
        Assert.assertEquals(Json.fromJson("{\"name\":\"some2\"}", Split.class).name, change.featureFlags.d.get(1).name);
    }

    private SDKMetadata metadata() {
        return new SDKMetadata("java-1.2.3", "1.2.3.4", "someIP");
    }

}
