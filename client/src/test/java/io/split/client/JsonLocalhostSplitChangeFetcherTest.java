package io.split.client;

import io.split.client.dtos.*;
import io.split.client.utils.FileInputStreamProvider;
import io.split.client.utils.InputStreamProvider;
import io.split.client.utils.StaticContentInputStreamProvider;
import io.split.engine.common.FetchOptions;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JsonLocalhostSplitChangeFetcherTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private String TEST_0 = "{\"ff\":{\"d\":[{\"trafficTypeName\":\"user\",\"name\":\"SPLIT_1\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-1780071202,\"seed\":-1442762199,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1675443537882,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"default rule\"}]}],\"s\":-1,\"t\":-1},\"rbs\":{\"d\":[{\"changeNumber\":5,\"name\":\"sample_rule_based_segment\",\"status\":\"ACTIVE\",\"trafficTypeName\":\"user\",\"excluded\":{\"keys\":[\"mauro@split.io\"],\"segments\":[]},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":\"email\"},\"matcherType\":\"ENDS_WITH\",\"negate\":false,\"whitelistMatcherData\":{\"whitelist\":[\"@split.io\"]}}]}}]}],\"s\":-1,\"t\":-1}}";
    private String TEST_1 = "{\"ff\":{\"d\":[{\"trafficTypeName\":\"user\",\"name\":\"SPLIT_1\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-1780071202,\"seed\":-1442762199,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1675443537882,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"default rule\"}]},{\"trafficTypeName\":\"user\",\"name\":\"SPLIT_2\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-1780071202,\"seed\":-1442762199,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1675443537882,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"default rule\"}]}],\"s\":-1,\"t\":-1},\"rbs\":{\"d\":[{\"changeNumber\":5,\"name\":\"sample_rule_based_segment\",\"status\":\"ACTIVE\",\"trafficTypeName\":\"user\",\"excluded\":{\"keys\":[\"mauro@split.io\"],\"segments\":[]},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":\"email\"},\"matcherType\":\"ENDS_WITH\",\"negate\":false,\"whitelistMatcherData\":{\"whitelist\":[\"@split.io\"]}}]}}]}],\"s\":-1,\"t\":-1}}";
    private String TEST_2 = "{\"ff\":{\"d\":[{\"trafficTypeName\":\"user\",\"name\":\"SPLIT_1\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-1780071202,\"seed\":-1442762199,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1675443537882,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"default rule\"}]},{\"trafficTypeName\":\"user\",\"name\":\"SPLIT_2\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-1780071202,\"seed\":-1442762199,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1675443537882,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"default rule\"}]}],\"s\":-1,\"t\":2323},\"rbs\":{\"d\":[{\"changeNumber\":5,\"name\":\"sample_rule_based_segment\",\"status\":\"ACTIVE\",\"trafficTypeName\":\"user\",\"excluded\":{\"keys\":[\"mauro@split.io\"],\"segments\":[]},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":\"email\"},\"matcherType\":\"ENDS_WITH\",\"negate\":false,\"whitelistMatcherData\":{\"whitelist\":[\"@split.io\"]}}]}}]}],\"s\":-1,\"t\":-1}}";
    private String TEST_3 = "{\"ff\":{\"d\":[{\"trafficTypeName\":\"user\",\"name\":\"SPLIT_1\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-1780071202,\"seed\":-1442762199,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1675443537882,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"default rule\"}]}],\"s\":-1,\"t\":2323},\"rbs\":{\"d\":[{\"changeNumber\":5,\"name\":\"sample_rule_based_segment\",\"status\":\"ACTIVE\",\"trafficTypeName\":\"user\",\"excluded\":{\"keys\":[\"mauro@split.io\",\"gaston@split.io\"],\"segments\":[]},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":\"email\"},\"matcherType\":\"ENDS_WITH\",\"negate\":false,\"whitelistMatcherData\":{\"whitelist\":[\"@split.io\"]}}]}}]}],\"s\":-1,\"t\":1122}}";
    private String TEST_4 = "{\"ff\":{\"d\":[{\"trafficTypeName\":\"user\",\"name\":\"SPLIT_1\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-1780071202,\"seed\":-1442762199,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1675443537882,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"default rule\"}]}],\"s\":-1,\"t\":445345},\"rbs\":{\"d\":[{\"changeNumber\":5,\"name\":\"sample_rule_based_segment\",\"status\":\"ACTIVE\",\"trafficTypeName\":\"user\",\"excluded\":{\"keys\":[\"mauro@split.io\",\"gaston@split.io\"],\"segments\":[]},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":\"email\"},\"matcherType\":\"ENDS_WITH\",\"negate\":false,\"whitelistMatcherData\":{\"whitelist\":[\"@split.io\"]}}]}}]}],\"s\":-1,\"t\":5566}}";
    private String TEST_5 = "{\"ff\":{\"d\":[{\"trafficTypeName\":\"user\",\"name\":\"SPLIT_1\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-1780071202,\"seed\":-1442762199,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1675443537882,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"default rule\"}]},{\"trafficTypeName\":\"user\",\"name\":\"SPLIT_2\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-1780071202,\"seed\":-1442762199,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1675443537882,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"default rule\"}]}],\"s\":-1,\"t\":-1},\"rbs\":{\"d\":[{\"changeNumber\":5,\"name\":\"sample_rule_based_segment\",\"status\":\"ACTIVE\",\"trafficTypeName\":\"user\",\"excluded\":{\"keys\":[\"mauro@split.io\"],\"segments\":[]},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":\"email\"},\"matcherType\":\"ENDS_WITH\",\"negate\":false,\"whitelistMatcherData\":{\"whitelist\":[\"@split.io\"]}}]}}]}],\"s\":-1,\"t\":-1}}";

    @Test
    public void testParseSplitChange() throws FileNotFoundException {
        InputStream inputStream = new FileInputStream("src/test/resources/split_init.json");
        InputStreamProvider inputStreamProvider = new StaticContentInputStreamProvider(inputStream);
        JsonLocalhostSplitChangeFetcher localhostSplitChangeFetcher = new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, -1, fetchOptions);

        List<Split> split = splitChange.featureFlags.d;
        Assert.assertEquals(7, split.size());
        Assert.assertEquals(1660326991072L, splitChange.featureFlags.t);
        Assert.assertEquals(-1L, splitChange.featureFlags.s);
    }

    @Test
    public void testSinceAndTillSanitization() throws FileNotFoundException {
        InputStream inputStream = new FileInputStream("src/test/resources/sanitizer/splitChangeTillSanitization.json");
        InputStreamProvider inputStreamProvider = new StaticContentInputStreamProvider(inputStream);
        JsonLocalhostSplitChangeFetcher localhostSplitChangeFetcher = new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, -1, fetchOptions);

        Assert.assertEquals(-1L, splitChange.featureFlags.t);
        Assert.assertEquals(-1L, splitChange.featureFlags.s);

        Assert.assertEquals(-1L, splitChange.ruleBasedSegments.t);
        Assert.assertEquals(-1L, splitChange.ruleBasedSegments.s);

    }

    @Test
    public void testSplitChangeWithoutSplits() throws FileNotFoundException {
        InputStream inputStream = new FileInputStream("src/test/resources/sanitizer/splitChangeWithoutSplits.json");
        InputStreamProvider inputStreamProvider = new StaticContentInputStreamProvider(inputStream);
        JsonLocalhostSplitChangeFetcher localhostSplitChangeFetcher = new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, -1, fetchOptions);

        Assert.assertEquals(0, splitChange.featureFlags.d.size());
        Assert.assertEquals(0, splitChange.ruleBasedSegments.d.size());
    }

    @Test
    public void testSplitChangeSplitsToSanitize() throws FileNotFoundException {
        InputStream inputStream = new FileInputStream("src/test/resources/sanitizer/splitChangeSplitsToSanitize.json");
        InputStreamProvider inputStreamProvider = new StaticContentInputStreamProvider(inputStream);
        JsonLocalhostSplitChangeFetcher localhostSplitChangeFetcher = new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, -1, fetchOptions);

        Assert.assertEquals(1, splitChange.featureFlags.d.size());
        Split split = splitChange.featureFlags.d.get(0);
        Assert.assertEquals(Optional.of(100), Optional.of(split.trafficAllocation));
        Assert.assertEquals(Status.ACTIVE, split.status);
        Assert.assertEquals("control", split.defaultTreatment);
        Assert.assertEquals(ConditionType.ROLLOUT, split.conditions.get(split.conditions.size() - 1).conditionType);

        Assert.assertEquals(1, splitChange.ruleBasedSegments.d.size());
        RuleBasedSegment ruleBasedSegment = splitChange.ruleBasedSegments.d.get(0);
        Assert.assertEquals(Status.ACTIVE, split.status);
        Assert.assertEquals(ConditionType.ROLLOUT, ruleBasedSegment.conditions.get(ruleBasedSegment.conditions.size() - 1).conditionType);
        Assert.assertEquals(new ArrayList<>(), ruleBasedSegment.excluded.segments);
    }

    @Test
    public void testSplitChangeSplitsToSanitizeMatchersNull() throws FileNotFoundException {
        InputStream inputStream = new FileInputStream("src/test/resources/sanitizer/splitChangerMatchersNull.json");
        InputStreamProvider inputStreamProvider = new StaticContentInputStreamProvider(inputStream);
        JsonLocalhostSplitChangeFetcher localhostSplitChangeFetcher = new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, -1, fetchOptions);

        Assert.assertEquals(1, splitChange.featureFlags.d.size());
        Split split = splitChange.featureFlags.d.get(0);
        Assert.assertEquals(Optional.of(100), Optional.of(split.trafficAllocation));
        Assert.assertEquals(Status.ACTIVE, split.status);
        Assert.assertEquals("off", split.defaultTreatment);
        Assert.assertEquals(ConditionType.ROLLOUT, split.conditions.get(split.conditions.size() - 1).conditionType);
    }

    @Test
    public void testSplitChangeSplitsDifferentScenarios() throws IOException {

        File file = folder.newFile("test_0.json");

        byte[] test = TEST_0.getBytes();
        com.google.common.io.Files.write(test, file);

        InputStreamProvider inputStreamProvider = new FileInputStreamProvider(file.getAbsolutePath());
        JsonLocalhostSplitChangeFetcher localhostSplitChangeFetcher = new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        // 0) The CN from storage is -1, till and since are -1, and sha doesn't exist in the hash. It's going to return a split change with updates.
        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, -1, fetchOptions);
        Assert.assertEquals(1, splitChange.featureFlags.d.size());
        Assert.assertEquals(-1, splitChange.featureFlags.t);
        Assert.assertEquals(-1, splitChange.featureFlags.s);
        Assert.assertEquals(1, splitChange.ruleBasedSegments.d.size());
        Assert.assertEquals(-1, splitChange.ruleBasedSegments.t);
        Assert.assertEquals(-1, splitChange.ruleBasedSegments.s);

        test = TEST_1.getBytes();
        com.google.common.io.Files.write(test, file);

        // 1) The CN from storage is -1, till and since are -1, and sha is different than before. It's going to return a split change with updates.
        splitChange = localhostSplitChangeFetcher.fetch(-1L, -1, fetchOptions);
        Assert.assertEquals(2, splitChange.featureFlags.d.size());
        Assert.assertEquals(-1, splitChange.featureFlags.t);
        Assert.assertEquals(-1, splitChange.featureFlags.s);

        test = TEST_2.getBytes();
        com.google.common.io.Files.write(test, file);

        // 2) The CN from storage is -1, till is 2323, and since is -1, and sha is the same as before. It's going to return a split change with the same data.
        splitChange = localhostSplitChangeFetcher.fetch(-1L, -1, fetchOptions);
        Assert.assertEquals(2, splitChange.featureFlags.d.size());
        Assert.assertEquals(-1, splitChange.featureFlags.t);
        Assert.assertEquals(-1, splitChange.featureFlags.s);

        test = TEST_3.getBytes();
        com.google.common.io.Files.write(test, file);

        // 3) The CN from storage is -1, till is 2323, and since is -1, sha is different than before. It's going to return a split change with updates.
        splitChange = localhostSplitChangeFetcher.fetch(-1L, -1L, fetchOptions);
        Assert.assertEquals(1, splitChange.featureFlags.d.size());
        Assert.assertEquals(2323, splitChange.featureFlags.t);
        Assert.assertEquals(-1, splitChange.featureFlags.s);
        Assert.assertEquals(1, splitChange.ruleBasedSegments.d.size());
        Assert.assertEquals(1122, splitChange.ruleBasedSegments.t);
        Assert.assertEquals(-1, splitChange.ruleBasedSegments.s);

        test = TEST_4.getBytes();
        com.google.common.io.Files.write(test, file);

        // 4) The CN from storage is 2323, till is 445345, and since is -1, and sha is the same as before. It's going to return a split change with same data.
        splitChange = localhostSplitChangeFetcher.fetch(2323, 1122, fetchOptions);
        Assert.assertEquals(1, splitChange.featureFlags.d.size());
        Assert.assertEquals(2323, splitChange.featureFlags.t);
        Assert.assertEquals(2323, splitChange.featureFlags.s);
        Assert.assertEquals(1, splitChange.ruleBasedSegments.d.size());
        Assert.assertEquals(1122, splitChange.ruleBasedSegments.t);
        Assert.assertEquals(1122, splitChange.ruleBasedSegments.s);

        test = TEST_5.getBytes();
        com.google.common.io.Files.write(test, file);

        // 5) The CN from storage is 2323, till and since are -1, and sha is different than before. It's going to return a split change with updates.
        splitChange = localhostSplitChangeFetcher.fetch(2323, 1122, fetchOptions);
        Assert.assertEquals(2, splitChange.featureFlags.d.size());
        Assert.assertEquals(2323, splitChange.featureFlags.t);
        Assert.assertEquals(2323, splitChange.featureFlags.s);
        Assert.assertEquals(1, splitChange.ruleBasedSegments.d.size());
        Assert.assertEquals(1122, splitChange.ruleBasedSegments.t);
        Assert.assertEquals(1122, splitChange.ruleBasedSegments.s);
    }

    @Test(expected = IllegalStateException.class)
    public void processTestForException() {
        InputStreamProvider inputStreamProvider = new FileInputStreamProvider("src/test/resources/notExist.json");
        JsonLocalhostSplitChangeFetcher localhostSplitChangeFetcher = new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, -1, fetchOptions);
    }

    @Test
    public void testParseOldSpec() throws FileNotFoundException {
        InputStream inputStream = new FileInputStream("src/test/resources/split_old_spec.json");
        InputStreamProvider inputStreamProvider = new StaticContentInputStreamProvider(inputStream);
        JsonLocalhostSplitChangeFetcher localhostSplitChangeFetcher = new JsonLocalhostSplitChangeFetcher(inputStreamProvider);
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, -1, fetchOptions);

        List<Split> split = splitChange.featureFlags.d;
        Assert.assertEquals(7, split.size());
        Assert.assertEquals(1660326991072L, splitChange.featureFlags.t);
        Assert.assertEquals(-1L, splitChange.featureFlags.s);

        Assert.assertEquals(new ArrayList<>(), splitChange.ruleBasedSegments.d);
        Assert.assertEquals(-1L, splitChange.ruleBasedSegments.t);
        Assert.assertEquals(-1L, splitChange.ruleBasedSegments.s);
    }
}