package io.split.engine.experiments;

import io.split.Spec;
import io.split.client.JsonLocalhostSplitChangeFetcher;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.interceptors.FlagSetsFilterImpl;
import io.split.client.utils.FileInputStreamProvider;
import io.split.client.utils.InputStreamProvider;
import io.split.engine.common.FetchOptions;
import io.split.storages.RuleBasedSegmentCacheProducer;
import io.split.storages.SplitCacheProducer;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.storages.memory.RuleBasedSegmentCacheInMemoryImp;
import io.split.telemetry.storage.NoopTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

public class SplitFetcherImpTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final TelemetryStorage TELEMETRY_STORAGE_NOOP = Mockito.mock(NoopTelemetryStorage.class);
    private static final String TEST_FLAG_SETS = "{\"ff\":{\"d\":[{\"trafficTypeName\":\"client\",\"name\":\"workm\",\"trafficAllocation\":100,\"trafficAllocationSeed\":147392224,\"seed\":524417105,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"on\",\"changeNumber\":1602796638344,\"algo\":2,\"configurations\":{},\"sets\":[\"set_1\",\"set_2\"],\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"new_segment\"},\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":100},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"in segment new_segment\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":0},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"default rule\"}]},{\"trafficTypeName\":\"client\",\"name\":\"workm_set_3\",\"trafficAllocation\":100,\"trafficAllocationSeed\":147392224,\"seed\":524417105,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"on\",\"changeNumber\":1602796638344,\"algo\":2,\"configurations\":{},\"sets\":[\"set_3\"],\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"new_segment\"},\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":100},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"in segment new_segment\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":0},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"default rule\"}]}],\"s\":-1,\"t\":1602796638344},\"rbs\":{\"d\":[],\"t\":-1,\"s\":-1}}";

    // TODO: enable tests when JSONLocalhost support spec 1.3

    @Ignore
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

    @Ignore
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

    @Ignore
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