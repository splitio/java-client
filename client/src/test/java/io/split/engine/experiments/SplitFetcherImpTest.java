package io.split.engine.experiments;

import io.split.SplitMockServer;
import io.split.client.*;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.interceptors.FlagSetsFilterImpl;
import io.split.client.interceptors.GzipDecoderResponseInterceptor;
import io.split.client.interceptors.GzipEncoderRequestInterceptor;
import io.split.client.utils.*;
import io.split.engine.common.FetchOptions;
import io.split.service.SplitHttpClient;
import io.split.service.SplitHttpClientImpl;
import io.split.storages.RuleBasedSegmentCache;
import io.split.storages.RuleBasedSegmentCacheProducer;
import io.split.storages.SplitCache;
import io.split.storages.SplitCacheProducer;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.storages.memory.RuleBasedSegmentCacheInMemoryImp;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.NoopTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import io.split.telemetry.storage.TelemetryStorageProducer;
import okhttp3.mockwebserver.MockResponse;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class SplitFetcherImpTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final TelemetryStorage TELEMETRY_STORAGE_NOOP = Mockito.mock(NoopTelemetryStorage.class);
    private static final String TEST_FLAG_SETS = "{\"ff\":{\"d\":[{\"trafficTypeName\":\"client\",\"name\":\"workm\",\"trafficAllocation\":100,\"trafficAllocationSeed\":147392224,\"seed\":524417105,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"on\",\"changeNumber\":1602796638344,\"algo\":2,\"configurations\":{},\"sets\":[\"set_1\",\"set_2\"],\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"new_segment\"},\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":100},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"in segment new_segment\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":0},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"default rule\"}]},{\"trafficTypeName\":\"client\",\"name\":\"workm_set_3\",\"trafficAllocation\":100,\"trafficAllocationSeed\":147392224,\"seed\":524417105,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"on\",\"changeNumber\":1602796638344,\"algo\":2,\"configurations\":{},\"sets\":[\"set_3\"],\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"new_segment\"},\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":100},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"in segment new_segment\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":0},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"default rule\"}]}],\"s\":-1,\"t\":1602796638344},\"rbs\":{\"d\":[],\"t\":-1,\"s\":-1}}";

    @Test
    public void testFetchingSplitsAndRuleBasedSegments() throws Exception {
        MockResponse response = new MockResponse().setBody("{" +
        "\"ff\":{" +
            "\"t\":1675095324253," +
            "\"s\":-1," +
            "\"d\": [{" +
                "\"changeNumber\": 123," +
                "\"trafficTypeName\": \"user\"," +
                "\"name\": \"some_name\"," +
                "\"trafficAllocation\": 100," +
                "\"trafficAllocationSeed\": 123456," +
                "\"seed\": 321654," +
                "\"status\": \"ACTIVE\"," +
                "\"killed\": false," +
                "\"defaultTreatment\": \"off\"," +
                "\"algo\": 2," +
                "\"conditions\": [{" +
                    "\"partitions\": [{\"treatment\": \"on\", \"size\": 50},{\"treatment\": \"off\", \"size\": 50}]," +
                    "\"contitionType\": \"WHITELIST\"," +
                    "\"label\": \"some_label\"," +
                    "\"matcherGroup\": {" +
                        "\"matchers\": [{\"matcherType\": \"WHITELIST\",\"whitelistMatcherData\": {\"whitelist\": [\"k1\", \"k2\", \"k3\"]},\"negate\": false}]," +
                        "\"combiner\": \"AND\"}" +
                "},{" +
                    "\"conditionType\": \"ROLLOUT\"," +
                    "\"matcherGroup\": {\"combiner\": \"AND\"," +
                        "\"matchers\": [{\"keySelector\": {\"trafficType\": \"user\"},\"matcherType\": \"IN_RULE_BASED_SEGMENT\",\"negate\": false,\"userDefinedSegmentMatcherData\": {\"segmentName\": \"sample_rule_based_segment\"}}]" +
                    "}," +
                    "\"partitions\": [{\"treatment\": \"on\",\"size\": 100},{\"treatment\": \"off\",\"size\": 0}]," +
                    "\"label\": \"in rule based segment sample_rule_based_segment\"" +
                "}]," +
                "\"sets\": [\"set1\", \"set2\"]}]" +
            "}," +
        "\"rbs\":  {" +
            "\"t\": 1585948850111," +
            "\"s\": -1," +
            "\"d\": [" +
            "{" +
                "\"changeNumber\": 5," +
                "\"name\": \"sample_rule_based_segment\"," +
                "\"status\": \"ACTIVE\"," +
                "\"trafficTypeName\": \"user\"," +
                "\"excluded\":{" +
                    "\"keys\":[\"mauro@split.io\",\"gaston@split.io\"]," +
                    "\"segments\":[]" +
                "}," +
                "\"conditions\": [" +
                "{" +
                    "\"matcherGroup\": {" +
                        "\"combiner\": \"AND\"," +
                        "\"matchers\": [" +
                        "{" +
                            "\"keySelector\": {" +
                                "\"trafficType\": \"user\"," +
                                "\"attribute\": \"email\"" +
                            "}," +
                            "\"matcherType\": \"ENDS_WITH\"," +
                            "\"negate\": false," +
                            "\"whitelistMatcherData\": {" +
                            "\"whitelist\": [" +
                                "\"@split.io\"" +
                "]}}]}}]}]}}");
        MockResponse response2 = new MockResponse().setBody("{\"ff\":{\"d\": [], \"s\":1675095324253, \"t\":1685095324253}, \"rbs\":{\"d\":[],\"s\":1585948850111,\"t\":1585948850111}}");
        MockResponse response3 = new MockResponse().setBody("{\"ff\":{\"d\": [], \"s\":1685095324253, \"t\":1695095324253}, \"rbs\":{\"d\":[],\"s\":1585948850111,\"t\":1585948850111}}");
        MockResponse response4 = new MockResponse().setBody("{\"ff\":{\"d\": [], \"s\":1695095324253, \"t\":1775095324253}, \"rbs\":{\"d\":[],\"s\":1585948850111,\"t\":1585948850111}}");
        MockResponse response5 = new MockResponse().setBody("{\"ff\":{\"d\": [], \"s\":1775095324253, \"t\":1775095324253}, \"rbs\":{\"d\":[],\"s\":1585948850111,\"t\":1585948850111}}");
        Queue responses = new LinkedList<>();
        responses.add(response);
        Queue responses2 = new LinkedList<>();
        responses2.add(response2);
        Queue responses3 = new LinkedList<>();
        responses3.add(response3);
        Queue responses4 = new LinkedList<>();
        responses4.add(response4);
        Queue responses5 = new LinkedList<>();
        responses5.add(response5);
        SplitMockServer splitServer = new SplitMockServer(CustomDispatcher2.builder()
                .path(CustomDispatcher2.SPLIT_FETCHER_1, responses)
                .path(CustomDispatcher2.SPLIT_FETCHER_2, responses2)
                .path(CustomDispatcher2.SPLIT_FETCHER_3, responses3)
                .path(CustomDispatcher2.SPLIT_FETCHER_4, responses4)
                .path(CustomDispatcher2.SPLIT_FETCHER_5, responses5)
                .build());
        splitServer.start();

        SplitClientConfig config = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .endpoint(splitServer.getUrl(), splitServer.getUrl())
                .featuresRefreshRate(20)
                .segmentsRefreshRate(30)
                .streamingEnabled(false)
                .build();

        SplitParser splitParser = new SplitParser();
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();
        RuleBasedSegmentCache ruleBasedSegmentCache = new RuleBasedSegmentCacheInMemoryImp();
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(config.getSetsFilter());
        SplitCache splitCache = new InMemoryCacheImp(flagSetsFilter);
        RequestDecorator _requestDecorator = new RequestDecorator(config.customHeaderDecorator());
        SDKMetadata _sdkMetadata = new SDKMetadata("1.1.1", "ip", "machineName");
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(config.connectionTimeout()))
                .setCookieSpec(StandardCookieSpec.STRICT)
                .build();
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        TelemetryStorageProducer _telemetryStorageProducer = telemetryStorage;

        HttpClientBuilder httpClientbuilder = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .addRequestInterceptorLast(new GzipEncoderRequestInterceptor())
                .addResponseInterceptorLast((new GzipDecoderResponseInterceptor()));
        SplitHttpClient _splitHttpClient = SplitHttpClientImpl.create(httpClientbuilder.build(),
                _requestDecorator,
                "apiToken",
                _sdkMetadata);
        URI _rootTarget = URI.create(config.endpoint());
        SplitChangeFetcher splitChangeFetcher = HttpSplitChangeFetcher.create(_splitHttpClient, _rootTarget,
                _telemetryStorageProducer, config.isSdkEndpointOverridden());
        SplitFetcherImp splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCache, _telemetryStorageProducer,
                flagSetsFilter, ruleBasedSegmentParser, ruleBasedSegmentCache);

        splitFetcher.forceRefresh(new FetchOptions.Builder().cacheControlHeaders(false).build());
        splitServer.stop();
        Assert.assertEquals("some_name", splitCache.get("some_name").feature());
        Assert.assertEquals("sample_rule_based_segment", ruleBasedSegmentCache.get("sample_rule_based_segment").ruleBasedSegment());
    }

    @Test
    public void testLocalHost() {
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(new HashSet<>());
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp(flagSetsFilter);
        RuleBasedSegmentCacheProducer ruleBasedSegmentCacheProducer = new RuleBasedSegmentCacheInMemoryImp();
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();

        InputStreamProvider inputStreamProvider = new FileInputStreamProvider("src/test/resources/split_init.json");
        SplitChangeFetcher splitChangeFetcher = new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
        SplitParser splitParser = new SplitParser();
        FetchOptions fetchOptions = new FetchOptions.Builder().build();
        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheProducer, TELEMETRY_STORAGE_NOOP, flagSetsFilter,
                ruleBasedSegmentParser, ruleBasedSegmentCacheProducer);

        FetchResult fetchResult = splitFetcher.forceRefresh(fetchOptions);

        Assert.assertEquals(1, fetchResult.getSegments().size());
    }

    @Test
    public void testLocalHostFlagSets() throws IOException {
        File file = folder.newFile("test_0.json");

        byte[] test = TEST_FLAG_SETS.getBytes();
        com.google.common.io.Files.write(test, file);

        InputStreamProvider inputStreamProvider = new FileInputStreamProvider(file.getAbsolutePath());
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(new HashSet<>(Arrays.asList("set_1")));
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp(flagSetsFilter);
        RuleBasedSegmentCacheProducer ruleBasedSegmentCacheProducer = new RuleBasedSegmentCacheInMemoryImp();
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();

        SplitChangeFetcher splitChangeFetcher = new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
        SplitParser splitParser = new SplitParser();
        FetchOptions fetchOptions = new FetchOptions.Builder().build();
        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheProducer, TELEMETRY_STORAGE_NOOP, flagSetsFilter,
                ruleBasedSegmentParser, ruleBasedSegmentCacheProducer);

        FetchResult fetchResult = splitFetcher.forceRefresh(fetchOptions);

        Assert.assertEquals(1, fetchResult.getSegments().size());
    }

    @Test
    public void testLocalHostFlagSetsNotIntersect() throws IOException {
        File file = folder.newFile("test_0.json");

        byte[] test = TEST_FLAG_SETS.getBytes();
        com.google.common.io.Files.write(test, file);

        InputStreamProvider inputStreamProvider = new FileInputStreamProvider(file.getAbsolutePath());
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(new HashSet<>(Arrays.asList("set_4")));
        SplitCacheProducer splitCacheProducer = new InMemoryCacheImp(flagSetsFilter);
        RuleBasedSegmentCacheProducer ruleBasedSegmentCacheProducer = new RuleBasedSegmentCacheInMemoryImp();
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();

        SplitChangeFetcher splitChangeFetcher = new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
        SplitParser splitParser = new SplitParser();
        FetchOptions fetchOptions = new FetchOptions.Builder().build();
        SplitFetcher splitFetcher = new SplitFetcherImp(splitChangeFetcher, splitParser, splitCacheProducer, TELEMETRY_STORAGE_NOOP, flagSetsFilter,
            ruleBasedSegmentParser, ruleBasedSegmentCacheProducer);

        FetchResult fetchResult = splitFetcher.forceRefresh(fetchOptions);

        Assert.assertEquals(0, fetchResult.getSegments().size());
    }
}