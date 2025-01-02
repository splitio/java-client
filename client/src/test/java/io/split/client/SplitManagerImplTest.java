package io.split.client;

import com.google.common.collect.Lists;
import io.split.client.api.SplitView;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.client.utils.Json;
import io.split.engine.ConditionsTestUtil;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitParser;
import io.split.engine.matchers.AllKeysMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.grammar.Treatments;
import io.split.storages.SplitCacheConsumer;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SplitManagerImplTest {

    private SplitClientConfig config = SplitClientConfig.builder().setBlockUntilReadyTimeout(100).build();
    private static TelemetryStorage TELEMETRY_STORAGE = mock(InMemoryTelemetryStorage.class);

    @Before
    public void updateTelemetryStorage() {
        TELEMETRY_STORAGE = mock(InMemoryTelemetryStorage.class);
    }
    @Test
    public void splitCallWithNonExistentSplit() {
        String nonExistent = "nonExistent";
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        when(splitCacheConsumer.get(nonExistent)).thenReturn(null);

        SplitManagerImpl splitManager = new SplitManagerImpl(splitCacheConsumer,
                mock(SplitClientConfig.class),
                mock(SDKReadinessGates.class), TELEMETRY_STORAGE);
        Assert.assertNull(splitManager.split("nonExistent"));
    }

    @Test
    public void splitCallWithExistentSplit() {
        String existent = "existent";

        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        ParsedSplit response = ParsedSplit.createParsedSplitForTests("FeatureName", 123, true, "off", Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1, new HashSet<>(), false);
        when(splitCacheConsumer.get(existent)).thenReturn(response);

        SplitManagerImpl splitManager = new SplitManagerImpl(splitCacheConsumer,
                mock(SplitClientConfig.class),
                mock(SDKReadinessGates.class), TELEMETRY_STORAGE);
        SplitView theOne = splitManager.split(existent);
        Assert.assertEquals(response.feature(), theOne.name);
        Assert.assertEquals(response.changeNumber(), theOne.changeNumber);
        Assert.assertEquals(response.killed(), theOne.killed);
        Assert.assertEquals(response.trafficTypeName(), theOne.trafficType);
        Assert.assertEquals(1, theOne.treatments.size());
        Assert.assertEquals("off", theOne.treatments.get(0));
        Assert.assertEquals(0, theOne.configs.size());
        Assert.assertEquals("off", theOne.defaultTreatment);
    }

    @Test
    public void splitCallWithExistentSplitAndConfigs() {
        String existent = "existent";
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);

        // Add config for only one treatment(default)
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.OFF, "{\"size\" : 30}");

        ParsedSplit response = ParsedSplit.createParsedSplitForTests("FeatureName", 123, true, "off", Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1, configurations, new HashSet<>(), false);
        when(splitCacheConsumer.get(existent)).thenReturn(response);

        SplitManagerImpl splitManager = new SplitManagerImpl(splitCacheConsumer,
                mock(SplitClientConfig.class),
                mock(SDKReadinessGates.class), TELEMETRY_STORAGE);
        SplitView theOne = splitManager.split(existent);

        Assert.assertEquals(response.feature(), theOne.name);
        Assert.assertEquals(response.changeNumber(), theOne.changeNumber);
        Assert.assertEquals(response.killed(), theOne.killed);
        Assert.assertEquals(response.trafficTypeName(), theOne.trafficType);
        Assert.assertEquals(1, theOne.treatments.size());
        Assert.assertEquals("off", theOne.treatments.get(0));
        Assert.assertEquals("{\"size\" : 30}", theOne.configs.get("off"));
    }

    @Test
    public void splitsCallWithNoSplit() {
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        when(splitCacheConsumer.getAll()).thenReturn(Lists.<ParsedSplit>newArrayList());
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        when(gates.isSDKReady()).thenReturn(false);
        SplitManagerImpl splitManager = new SplitManagerImpl(splitCacheConsumer,
                mock(SplitClientConfig.class),
                gates, TELEMETRY_STORAGE);
        Assert.assertTrue(splitManager.splits().isEmpty());
        verify(TELEMETRY_STORAGE, times(1)).recordNonReadyUsage();
    }

    @Test
    public void splitsCallWithSplit() {
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        List<ParsedSplit> parsedSplits = Lists.newArrayList();
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        when(gates.isSDKReady()).thenReturn(false);
        ParsedSplit response = ParsedSplit.createParsedSplitForTests("FeatureName", 123, true, "off", Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1, new HashSet<>(), false);
        parsedSplits.add(response);

        when(splitCacheConsumer.getAll()).thenReturn(parsedSplits);
        SplitManagerImpl splitManager = new SplitManagerImpl(splitCacheConsumer,
                mock(SplitClientConfig.class),
                gates, TELEMETRY_STORAGE);
        List<SplitView> splits = splitManager.splits();
        Assert.assertEquals(1, splits.size());
        Assert.assertEquals(response.feature(), splits.get(0).name);
        Assert.assertEquals(response.changeNumber(), response.changeNumber());
        Assert.assertEquals(response.killed(), splits.get(0).killed);
        Assert.assertEquals(response.trafficTypeName(), splits.get(0).trafficType);
        Assert.assertEquals(1, splits.get(0).treatments.size());
        Assert.assertEquals("off", splits.get(0).treatments.get(0));
        verify(TELEMETRY_STORAGE, times(1)).recordNonReadyUsage();
    }

    @Test
    public void splitNamesCallWithNoSplit() {
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        when(splitCacheConsumer.getAll()).thenReturn(Lists.<ParsedSplit>newArrayList());
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        when(gates.isSDKReady()).thenReturn(false);
        SplitManagerImpl splitManager = new SplitManagerImpl(splitCacheConsumer,
                mock(SplitClientConfig.class),
                gates, TELEMETRY_STORAGE);
        Assert.assertTrue(splitManager.splitNames().isEmpty());
        verify(TELEMETRY_STORAGE, times(1)).recordNonReadyUsage();
    }

    @Test
    public void splitNamesCallWithSplit() {
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        List<String> parsedSplits = new ArrayList<>();
        parsedSplits.add("FeatureName");

        when(splitCacheConsumer.splitNames()).thenReturn(parsedSplits);
        SplitManagerImpl splitManager = new SplitManagerImpl(splitCacheConsumer,
                mock(SplitClientConfig.class),
                mock(SDKReadinessGates.class), TELEMETRY_STORAGE);
        List<String> splitNames = splitManager.splitNames();
        Assert.assertEquals(1, splitNames.size());
        Assert.assertEquals("FeatureName",splitNames.get(0));
    }

    @Test
    public void blockUntilReadyDoesNotTimeWhenSdkIsReady() throws TimeoutException, InterruptedException {
        SDKReadinessGates ready = mock(SDKReadinessGates.class);
        when(ready.waitUntilInternalReady(100)).thenReturn(true);
        SplitManagerImpl splitManager = new SplitManagerImpl(mock(SplitCacheConsumer.class),
                config,
                ready, TELEMETRY_STORAGE);

        splitManager.blockUntilReady();
    }

    @Test(expected = TimeoutException.class)
    public void blockUntilReadyTimesWhenSdkIsNotReady() throws TimeoutException, InterruptedException {
        SDKReadinessGates ready = mock(SDKReadinessGates.class);
        when(ready.waitUntilInternalReady(100)).thenReturn(false);

        SplitManagerImpl splitManager = new SplitManagerImpl(mock(SplitCacheConsumer.class),
                config,
                ready, TELEMETRY_STORAGE);

        splitManager.blockUntilReady();
        verify(TELEMETRY_STORAGE, times(1)).recordBURTimeout();
    }

    @Test
    public void splitCallWithExistentSets() {
        String existent = "existent";
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        ParsedSplit response = ParsedSplit.createParsedSplitForTests("FeatureName", 123, true, "off",
                Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1, new HashSet<>(Arrays.asList("set1", "set2", "set3")), false);
        when(splitCacheConsumer.get(existent)).thenReturn(response);

        SplitManagerImpl splitManager = new SplitManagerImpl(splitCacheConsumer,
                mock(SplitClientConfig.class),
                mock(SDKReadinessGates.class), TELEMETRY_STORAGE);
        SplitView theOne = splitManager.split(existent);
        Assert.assertEquals(response.flagSets().size(), theOne.sets.size());
    }

    @Test
    public void splitCallWithEmptySets() {
        String existent = "existent";
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        ParsedSplit response = ParsedSplit.createParsedSplitForTests("FeatureName", 123, true, "off",
                Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1, null, false);
        when(splitCacheConsumer.get(existent)).thenReturn(response);

        SplitManagerImpl splitManager = new SplitManagerImpl(splitCacheConsumer,
                mock(SplitClientConfig.class),
                mock(SDKReadinessGates.class), TELEMETRY_STORAGE);
        SplitView theOne = splitManager.split(existent);
        Assert.assertEquals(0, theOne.sets.size());
    }

    private ParsedCondition getTestCondition(String treatment) {
        return ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(ConditionsTestUtil.partition(treatment, 10)));
    }

    @Test
    public void ImpressionToggleParseTest() throws IOException {
        SplitParser parser = new SplitParser();
        String splits = new String(Files.readAllBytes(Paths.get("src/test/resources/splits_imp_toggle.json")), StandardCharsets.UTF_8);
        SplitChange change = Json.fromJson(splits, SplitChange.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        for (Split split : change.splits) {
            ParsedSplit parsedSplit = parser.parse(split);
            when(splitCacheConsumer.get(split.name)).thenReturn(parsedSplit);
        }
        SplitManagerImpl splitManager = new SplitManagerImpl(splitCacheConsumer,
                mock(SplitClientConfig.class),
                mock(SDKReadinessGates.class), TELEMETRY_STORAGE);

        SplitView splitView = splitManager.split("without_impression_toggle");
        assertFalse(splitView.impressionsDisabled);
        splitView = splitManager.split("impression_toggle_on");
        assertFalse(splitView.impressionsDisabled);
        splitView = splitManager.split("impression_toggle_off");
        assertTrue(splitView.impressionsDisabled);
    }
}