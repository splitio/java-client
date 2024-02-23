package io.split.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.split.client.api.Key;
import io.split.client.api.SplitResult;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.DataType;
import io.split.client.dtos.Event;
import io.split.client.dtos.Partition;
import io.split.client.events.EventsStorageProducer;
import io.split.client.events.NoopEventsStorageImp;
import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.interceptors.FlagSetsFilterImpl;
import io.split.storages.SegmentCacheConsumer;
import io.split.storages.SplitCacheConsumer;
import io.split.engine.evaluator.EvaluatorImp;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.matchers.AllKeysMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.matchers.DependencyMatcher;
import io.split.engine.matchers.EqualToMatcher;
import io.split.engine.matchers.GreaterThanOrEqualToMatcher;
import io.split.engine.matchers.collections.ContainsAnyOfSetMatcher;
import io.split.engine.matchers.strings.WhitelistMatcher;
import io.split.grammar.Treatments;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for SplitClientImpl
 *
 * @author adil
 */
public class SplitClientImplTest {

    private static TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);
    private SplitClientConfig config = SplitClientConfig.builder().setBlockUntilReadyTimeout(100).flagSetsFilter(new ArrayList<>(
            Arrays.asList("set1", "set2", "set3"))).build();
    private FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(new HashSet<>(Arrays.asList("set1", "set2", "set3")));

    @Before
    public void updateTelemetryStorage() {
        TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);
    }

    @Test
    public void nullKeyResultsInControl() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        assertEquals(Treatments.CONTROL, client.getTreatment(null, "test1"));

        verifyZeroInteractions(splitCacheConsumer);
    }

    @Test
    public void nullTestResultsInControl() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        assertEquals(Treatments.CONTROL, client.getTreatment("adil@relateiq.com", null));

        verifyZeroInteractions(splitCacheConsumer);
    }

    @Test
    public void exceptionsResultInControl() {
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(anyString())).thenThrow(RuntimeException.class);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        assertEquals(Treatments.CONTROL, client.getTreatment("adil@relateiq.com", "test1"));

        verify(splitCacheConsumer).get("test1");
    }

    @Test
    public void works() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);
        when(gates.isSDKReady()).thenReturn(true);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        int numKeys = 5;
        for (int i = 0; i < numKeys; i++) {
            String randomKey = RandomStringUtils.random(10);
            Assert.assertEquals("on", client.getTreatment(randomKey, test));
        }

        verify(splitCacheConsumer, times(numKeys)).get(test);
        verify(TELEMETRY_STORAGE, times(5)).recordLatency(Mockito.anyObject(), Mockito.anyLong());
    }

    /**
     * There is no config for this treatment
     */
    @Test
    public void worksNullConfig() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        String randomKey = RandomStringUtils.random(10);
        SplitResult result = client.getTreatmentWithConfig(randomKey, test);
        assertEquals(Treatments.ON, result.treatment());
        assertNull(result.config());
        verify(splitCacheConsumer).get(test);
    }

    @Test
    public void worksAndHasConfig() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        // Add config for only one treatment
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.ON, "{\"size\" : 30}");

        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, configurations, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        int numKeys = 5;
        for (int i = 0; i < numKeys; i++) {
            Map<String, Object> attributes = new HashMap<>();
            String randomKey = RandomStringUtils.random(10);
            assertEquals("on", client.getTreatment(randomKey, test));
            assertEquals(configurations.get("on"), client.getTreatmentWithConfig(randomKey, test, attributes).config());
        }

        // Times 2 because we are calling getTreatment twice. Once for getTreatment and one for getTreatmentWithConfig
        verify(splitCacheConsumer, times(numKeys * 2)).get(test);
    }

    @Test
    public void lastConditionIsAlwaysDefault() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        assertEquals(Treatments.OFF, client.getTreatment("pato@codigo.com", test));

        verify(splitCacheConsumer).get(test);
    }

    /**
     * Tests that we retrieve configs from the default treatment
     */
    @Test
    public void lastConditionIsAlwaysDefaultButWithTreatment() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(
                Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        // Add config for only one treatment(default)
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.OFF, "{\"size\" : 30}");

        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                "user", 1, 1, configurations, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        SplitResult result = client.getTreatmentWithConfig("pato@codigo.com", test);
        assertEquals(Treatments.OFF, result.treatment());
        assertEquals("{\"size\" : 30}", result.config());

        verify(splitCacheConsumer).get(test);
    }

    @Test
    public void multipleConditionsWork() {
        String test = "test1";

        ParsedCondition adil_is_always_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        ParsedCondition pato_is_never_shown = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("pato@codigo.com"))), Lists.newArrayList(partition("off", 100)));
        ParsedCondition trevor_is_always_shown = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("trevor@codigo.com"))), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(adil_is_always_on, pato_is_never_shown, trevor_is_always_shown);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);
        when(gates.isSDKReady()).thenReturn(false);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        assertEquals("on", client.getTreatment("adil@codigo.com", test));
        assertEquals("off", client.getTreatment("pato@codigo.com", test));
        assertEquals("on", client.getTreatment("adil@codigo.com", test));

        verify(splitCacheConsumer, times(3)).get(test);
        verify(TELEMETRY_STORAGE, times(3)).recordNonReadyUsage();
    }


    @Test
    public void killedTestAlwaysGoesToDefault() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, true, Treatments.OFF, conditions, "user", 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        assertEquals(Treatments.OFF, client.getTreatment("adil@codigo.com", test));

        verify(splitCacheConsumer).get(test);
    }

    /**
     * when killed, the evaluator follows a slightly different path. So testing that when there is a config.
     */
    @Test
    public void killedTestAlwaysGoesToDefaultHasConfig() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(
                Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        // Add config for only one treatment(default)
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.OFF, "{\"size\" : 30}");

        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, true, Treatments.OFF, conditions,
                "user", 1, 1, configurations, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        SplitResult result = client.getTreatmentWithConfig("adil@codigo.com", test);
        assertEquals(Treatments.OFF, result.treatment());
        assertEquals("{\"size\" : 30}", result.config());

        verify(splitCacheConsumer).get(test);
    }

    @Test
    public void dependencyMatcherOn() {
        String parent = "parent";
        String dependent = "dependent";

        ParsedCondition parent_is_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<ParsedCondition> parent_conditions = Lists.newArrayList(parent_is_on);
        ParsedSplit parentSplit = ParsedSplit.createParsedSplitForTests(parent, 123, false, Treatments.OFF, parent_conditions, null, 1, 1, new HashSet<>());

        ParsedCondition dependent_needs_parent = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new DependencyMatcher(parent, Lists.newArrayList(Treatments.ON))), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<ParsedCondition> dependent_conditions = Lists.newArrayList(dependent_needs_parent);
        ParsedSplit dependentSplit = ParsedSplit.createParsedSplitForTests(dependent, 123, false, Treatments.OFF, dependent_conditions, null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(parent)).thenReturn(parentSplit);
        when(splitCacheConsumer.get(dependent)).thenReturn(dependentSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        assertEquals(Treatments.ON, client.getTreatment("key", parent));
        assertEquals(Treatments.ON, client.getTreatment("key", dependent));
    }

    @Test
    public void dependencyMatcherOff() {
        String parent = "parent";
        String dependent = "dependent";

        ParsedCondition parent_is_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<ParsedCondition> parent_conditions = Lists.newArrayList(parent_is_on);
        ParsedSplit parentSplit = ParsedSplit.createParsedSplitForTests(parent, 123, false, Treatments.OFF, parent_conditions, null, 1, 1, new HashSet<>());

        ParsedCondition dependent_needs_parent = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new DependencyMatcher(parent, Lists.newArrayList(Treatments.OFF))), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<ParsedCondition> dependent_conditions = Lists.newArrayList(dependent_needs_parent);
        ParsedSplit dependentSplit = ParsedSplit.createParsedSplitForTests(dependent, 123, false, Treatments.OFF, dependent_conditions, null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(parent)).thenReturn(parentSplit);
        when(splitCacheConsumer.get(dependent)).thenReturn(dependentSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        assertEquals(Treatments.ON, client.getTreatment("key", parent));
        assertEquals(Treatments.OFF, client.getTreatment("key", dependent));
    }

    @Test
    public void dependencyMatcherControl() {
        String dependent = "dependent";

        ParsedCondition dependent_needs_parent = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new DependencyMatcher("not-exists", Lists.newArrayList(Treatments.OFF))), Lists.newArrayList(partition(Treatments.OFF, 100)));
        List<ParsedCondition> dependent_conditions = Lists.newArrayList(dependent_needs_parent);
        ParsedSplit dependentSplit = ParsedSplit.createParsedSplitForTests(dependent, 123, false, Treatments.ON, dependent_conditions, null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(dependent)).thenReturn(dependentSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        assertEquals(Treatments.ON, client.getTreatment("key", dependent));
    }

    @Test
    public void attributesWork() {
        String test = "test1";

        ParsedCondition adil_is_always_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition(Treatments.ON, 100)));
        ParsedCondition users_with_age_greater_than_10_are_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("age", new GreaterThanOrEqualToMatcher(10, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(adil_is_always_on, users_with_age_greater_than_10_are_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        assertEquals("on", client.getTreatment("adil@codigo.com", test));
        assertEquals("on", client.getTreatment("adil@codigo.com", test, null));
        assertEquals("on", client.getTreatment("adil@codigo.com", test, ImmutableMap.<String, Object>of()));
        assertEquals("on", client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 10)));
        assertEquals("off", client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 9)));

        verify(splitCacheConsumer, times(5)).get(test);
    }

    @Test
    public void attributesWork2() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("age", new EqualToMatcher(0, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        assertEquals("off", client.getTreatment("adil@codigo.com", test));
        assertEquals("off", client.getTreatment("adil@codigo.com", test, null));
        assertEquals("off", client.getTreatment("adil@codigo.com", test, ImmutableMap.<String, Object>of()));

        assertEquals("off", client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 10)));
        assertEquals("on", client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 0)));

        verify(splitCacheConsumer, times(5)).get(test);
    }

    @Test
    public void attributesGreaterThanNegativeNumber() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        assertEquals("off", client.getTreatment("adil@codigo.com", test));
        assertEquals("off", client.getTreatment("adil@codigo.com", test, null));
        assertEquals("off", client.getTreatment("adil@codigo.com", test, ImmutableMap.<String, Object>of()));
        assertEquals("off", client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 10)));
        assertEquals("on", client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", -20)));
        assertEquals("off", client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 20)));
        assertEquals("off", client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", -21)));

        verify(splitCacheConsumer, times(7)).get(test);
    }


    @Test
    public void attributesForSets() {
        String test = "test1";

        ParsedCondition any_of_set = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("products", new ContainsAnyOfSetMatcher(Lists.<String>newArrayList("sms", "video"))), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(any_of_set);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer ,segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        assertEquals("off", client.getTreatment("adil@codigo.com", test));
        assertEquals("off", client.getTreatment("adil@codigo.com", test, null));

        assertEquals("off", client.getTreatment("adil@codigo.com", test, ImmutableMap.<String, Object>of()));
        assertEquals("off", client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("products", Lists.newArrayList())));
        assertEquals("off", client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("products", Lists.newArrayList(""))));
        assertEquals("off", client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("products", Lists.newArrayList("talk"))));
        assertEquals("on", client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("products", Lists.newArrayList("sms"))));
        assertEquals("on", client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("products", Lists.newArrayList("sms", "video"))));
        assertEquals("on", client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("products", Lists.newArrayList("video"))));

        verify(splitCacheConsumer, times(9)).get(test);
    }

    @Test
    public void labelsArePopulated() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = new ParsedCondition(ConditionType.ROLLOUT,
                CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)),
                Lists.newArrayList(partition("on", 100)),
                "foolabel"
        );

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, new HashSet<>());

        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        ImpressionsManager impressionsManager = mock(ImpressionsManager.class);
        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                impressionsManager,
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        Map<String, Object> attributes = ImmutableMap.<String, Object>of("age", -20, "acv", "1000000");
        assertEquals("on", client.getTreatment("pato@codigo.com", test, attributes));

        ArgumentCaptor<List> impressionCaptor = ArgumentCaptor.forClass(List.class);

        verify(impressionsManager).track(impressionCaptor.capture());

        List<Impression> impressions = impressionCaptor.getValue();
        assertNotNull(impressions);
        assertEquals(1, impressions.size());
        Impression impression = impressions.get(0);

        assertEquals("foolabel", impression.appliedRule());

        assertEquals(attributes, impression.attributes());
    }

    @Test
    public void notInSplitIfNoAllocation() {
        trafficAllocation("pato@split.io", 0, 123, "off", "not in split");
    }

    /**
     * This test depends on the underlying hashing algorithm. I have
     * figured out that pato@split.io will be in bucket 9 for seed 123.
     * That is why the test has been set up this way.
     *
     * If the underlying hashing algorithm changes, say to murmur, then we will
     * have to update this test.
     *
     * @author adil
     */
    @Test
    public void notInSplitIf10PercentAllocation() {

        String key = "pato@split.io";
        int i = 0;
        for (; i <= 9; i++) {
            trafficAllocation(key, i, 123, "off", "not in split");
        }

        for (; i <= 100; i++) {
            trafficAllocation(key, i, 123, "on", "in segment all");
        }
    }

    @Test
    public void trafficAllocationOnePercent() {
        //This key, with this seed it should fall in the 1%
        String fallsInOnePercent = "pato193";
        trafficAllocation(fallsInOnePercent, 1, 123, "on", "in segment all");

        //All these others should not be in split
        for (int offset = 0; offset <= 100; offset++) {
            trafficAllocation("pato" + String.valueOf(offset), 1, 123, "off", "not in split");
        }

    }

    @Test
    public void inSplitIf100PercentAllocation() {
        trafficAllocation("pato@split.io", 100, 123, "on", "in segment all");
    }

    @Test
    public void whitelistOverridesTrafficAllocation() {
        trafficAllocation("adil@split.io", 0, 123, "on", "whitelisted user");
    }


    private void trafficAllocation(String key, int trafficAllocation, int trafficAllocationSeed, String expected_treatment_on_or_off, String label) {

        String test = "test1";

        ParsedCondition whitelistCondition = new ParsedCondition(ConditionType.WHITELIST, CombiningMatcher.of(new WhitelistMatcher(
                Lists.newArrayList("adil@split.io"))), Lists.newArrayList(partition("on", 100), partition(
                        "off", 0)), "whitelisted user");
        ParsedCondition rollOutToEveryone = new ParsedCondition(ConditionType.ROLLOUT, CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100), partition("off", 0)), "in segment all");

        List<ParsedCondition> conditions = Lists.newArrayList(whitelistCondition, rollOutToEveryone);

        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, null, 1,
                trafficAllocation, trafficAllocationSeed, 1, null, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        ImpressionsManager impressionsManager = mock(ImpressionsManager.class);
        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                impressionsManager,
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        assertEquals(expected_treatment_on_or_off, client.getTreatment(key, test));

        ArgumentCaptor<List> impressionCaptor = ArgumentCaptor.forClass(List.class);

        verify(impressionsManager).track(impressionCaptor.capture());
        assertNotNull(impressionCaptor.getValue());
        assertEquals(1, impressionCaptor.getValue().size());
        Impression impression = (Impression) impressionCaptor.getValue().get(0);
        assertEquals(label, impression.appliedRule());
    }

    /**
     * Tests that when the key is not in the traffic allocation, it gets the default config when it exists.
     */
    @Test
    public void notInTrafficAllocationDefaultConfig() {

        String test = "test1";
        int trafficAllocation = 0;
        int trafficAllocationSeed = 123;

        // Add config for only one treatment
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.ON, "{\"size\" : 30}");
        configurations.put(Treatments.OFF, "{\"size\" : 30}"); // OFF is default treatment

        ParsedCondition rollOutToEveryone = new ParsedCondition(ConditionType.ROLLOUT, CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100), partition("off", 0)), "in segment all");

        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, null,
                1, trafficAllocation, trafficAllocationSeed, 1, configurations, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        ImpressionsManager impressionsManager = mock(ImpressionsManager.class);


        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                impressionsManager,
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        assertEquals(Treatments.OFF, client.getTreatment("pato@split.io", test));
        SplitResult result = client.getTreatmentWithConfig("pato@split.io", test);
        assertEquals(Treatments.OFF, result.treatment());
        assertEquals("{\"size\" : 30}", result.config());

        ArgumentCaptor<List> impressionCaptor = ArgumentCaptor.forClass(List.class);
        verify(impressionsManager, times(2)).track(impressionCaptor.capture());

        assertNotNull(impressionCaptor.getValue());
        assertEquals(1, impressionCaptor.getValue().size());
        Impression impression = (Impression) impressionCaptor.getValue().get(0);
        assertEquals("not in split", impression.appliedRule());
    }


    @Test
    public void matchingBucketingKeysWork() {
        String test = "test1";


        Set<String> whitelist = new HashSet<>();
        whitelist.add("aijaz");
        ParsedCondition aijaz_should_match = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(whitelist)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(aijaz_should_match);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        Key bad_key = new Key("adil", "aijaz");
        Key good_key = new Key("aijaz", "adil");

        assertEquals("off", client.getTreatment(bad_key, test, Collections.<String, Object>emptyMap()));
        assertEquals("on", client.getTreatment(good_key, test, Collections.<String, Object>emptyMap()));

        verify(splitCacheConsumer, times(2)).get(test);
    }

    @Test
    public void matchingBucketingKeysByFlagSetWork() {
        String test = "test1";

        Set<String> whitelist = new HashSet<>();
        whitelist.add("aijaz");
        ParsedCondition aijaz_should_match = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(whitelist)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(aijaz_should_match);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1, new HashSet<>(Arrays.asList("set1")));

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        HashMap<String, HashSet<String>> flagsBySets = new HashMap<>();
        flagsBySets.put("set1", new HashSet<>(Arrays.asList(test)));
        when(splitCacheConsumer.getNamesByFlagSets(Arrays.asList("set1"))).thenReturn(flagsBySets);

        Map<String, ParsedSplit> fetchManyResult = new HashMap<>();
        fetchManyResult.put(test, parsedSplit);
        when(splitCacheConsumer.fetchMany(Arrays.asList(test))).thenReturn(fetchManyResult);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        Key bad_key = new Key("adil", "aijaz");
        Key good_key = new Key("aijaz", "adil");

        assertEquals("off", client.getTreatmentsByFlagSet(bad_key, "set1", Collections.emptyMap()).get(test));
        assertEquals("on", client.getTreatmentsByFlagSet(good_key, "set1", Collections.emptyMap()).get(test));
    }

    @Test
    public void matchingBucketingKeysByFlagSetsWork() {
        String test = "test1";

        Set<String> whitelist = new HashSet<>();
        whitelist.add("aijaz");
        ParsedCondition aijaz_should_match = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(whitelist)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(aijaz_should_match);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1, new HashSet<>(Arrays.asList("set1")));

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        HashMap<String, HashSet<String>> flagsBySets = new HashMap<>();
        flagsBySets.put("set1", new HashSet<>(Arrays.asList(test)));
        when(splitCacheConsumer.getNamesByFlagSets(Arrays.asList("set1"))).thenReturn(flagsBySets);

        Map<String, ParsedSplit> fetchManyResult = new HashMap<>();
        fetchManyResult.put(test, parsedSplit);
        when(splitCacheConsumer.fetchMany(Arrays.asList(test))).thenReturn(fetchManyResult);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        Key bad_key = new Key("adil", "aijaz");
        Key good_key = new Key("aijaz", "adil");

        assertEquals("off", client.getTreatmentsByFlagSets(bad_key, Arrays.asList("set1"), Collections.emptyMap()).get(test));
        assertEquals("on", client.getTreatmentsByFlagSets(good_key, Arrays.asList("set1"), Collections.emptyMap()).get(test));
    }

    @Test
    public void impressionMetadataIsPropagated() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = new ParsedCondition(ConditionType.ROLLOUT,
                CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)),
                Lists.newArrayList(partition("on", 100)),
                "foolabel"
        );

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        ImpressionsManager impressionsManager = mock(ImpressionsManager.class);
        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                impressionsManager,
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        Map<String, Object> attributes = ImmutableMap.<String, Object>of("age", -20, "acv", "1000000");

        assertEquals("on", client.getTreatment("pato@codigo.com", test, attributes));

        ArgumentCaptor<List> impressionCaptor = ArgumentCaptor.forClass(List.class);

        verify(impressionsManager).track(impressionCaptor.capture());


        assertNotNull(impressionCaptor.getValue());
        assertEquals(1, impressionCaptor.getValue().size());
        Impression impression = (Impression) impressionCaptor.getValue().get(0);

        assertEquals("foolabel", impression.appliedRule());
        assertEquals(attributes, impression.attributes());
    }

    private Partition partition(String treatment, int size) {
        Partition p = new Partition();
        p.treatment = treatment;
        p.size = size;
        return p;
    }

    @Test
    public void blockUntilReadyDoesNotTimeWhenSdkIsReady() throws TimeoutException, InterruptedException {
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SDKReadinessGates ready = mock(SDKReadinessGates.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(ready.waitUntilInternalReady(100)).thenReturn(true);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                mock(ImpressionsManager.class),
                NoopEventsStorageImp.create(),
                config,
                ready,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        client.blockUntilReady();
    }

    @Test(expected = TimeoutException.class)
    public void blockUntilReadyTimesWhenSdkIsNotReady() throws TimeoutException, InterruptedException {
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SDKReadinessGates ready = mock(SDKReadinessGates.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(ready.waitUntilInternalReady(100)).thenReturn(false);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                mock(ImpressionsManager.class),
                NoopEventsStorageImp.create(),
                config,
                ready,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        client.blockUntilReady();
    }

    @Test
    public void trackWithValidParameters() {
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(gates.isSDKReady()).thenReturn(false);
        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        assertTrue(client.track("validKey", "valid_traffic_type", "valid_event"));

        String validEventSize = new String(new char[80]).replace('\0', 'a');
        String validKeySize = new String(new char[250]).replace('\0', 'a');
        assertTrue(client.track(validKeySize, "valid_traffic_type", validEventSize, 10));
        verify(TELEMETRY_STORAGE, times(2)).recordLatency(Mockito.anyObject(), Mockito.anyLong());
    }

    @Test
    public void trackWithInvalidEventTypeIds() {
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        
        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        Assert.assertFalse(client.track("validKey", "valid_traffic_type", ""));
        Assert.assertFalse(client.track("validKey", "valid_traffic_type", null));
        Assert.assertFalse(client.track("validKey", "valid_traffic_type", "invalid#char"));

        String invalidEventSize = new String(new char[81]).replace('\0', 'a');
        Assert.assertFalse(client.track("validKey", "valid_traffic_type", invalidEventSize));
    }

    @Test
    public void trackWithInvalidTrafficTypeNames() {
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        
        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        Assert.assertFalse(client.track("validKey", "", "valid"));
        Assert.assertFalse(client.track("validKey", null, "valid"));
    }

    @Test
    public void trackWithInvalidKeys() {
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        
        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        Assert.assertFalse(client.track("", "valid_traffic_type", "valid"));
        Assert.assertFalse(client.track(null, "valid_traffic_type", "valid"));

        String invalidKeySize = new String(new char[251]).replace('\0', 'a');
        Assert.assertFalse(client.track(invalidKeySize, "valid_traffic_type", "valid"));
    }

    @Test
    public void getTreatmentWithInvalidKeys() {
        String test = "split";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        Assert.assertNotEquals(Treatments.CONTROL, client.getTreatment("valid", "split"));
        assertEquals(Treatments.CONTROL, client.getTreatment("", "split"));
        assertEquals(Treatments.CONTROL, client.getTreatment(null, "split"));

        String invalidKeySize = new String(new char[251]).replace('\0', 'a');
        assertEquals(Treatments.CONTROL, client.getTreatment(invalidKeySize, "split"));

        assertEquals(Treatments.CONTROL, client.getTreatment("valid", ""));
        assertEquals(Treatments.CONTROL, client.getTreatment("valid", null));

        String matchingKey = new String(new char[250]).replace('\0', 'a');
        String bucketingKey = new String(new char[250]).replace('\0', 'a');
        Key key = new Key(matchingKey, bucketingKey);
        Assert.assertNotEquals(Treatments.CONTROL, client.getTreatment(key, "split", null));

        key = new Key("valid", "");
        assertEquals(Treatments.CONTROL, client.getTreatment(key, "split", null));

        key = new Key("", "valid");
        assertEquals(Treatments.CONTROL, client.getTreatment(key, "split", null));

        matchingKey = new String(new char[251]).replace('\0', 'a');
        bucketingKey = new String(new char[250]).replace('\0', 'a');
        key = new Key(matchingKey, bucketingKey);
        assertEquals(Treatments.CONTROL, client.getTreatment(key, "split", null));

        matchingKey = new String(new char[250]).replace('\0', 'a');
        bucketingKey = new String(new char[251]).replace('\0', 'a');
        key = new Key(matchingKey, bucketingKey);
        assertEquals(Treatments.CONTROL, client.getTreatment(key, "split", null));
    }

    @Test
    public void trackWithProperties() {
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        EventsStorageProducer eventClientMock = Mockito.mock(EventsStorageProducer.class);
        Mockito.when(eventClientMock.track((Event) Mockito.any(), Mockito.anyInt())).thenReturn(true);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                eventClientMock,
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        HashMap<String, Object> properties = new HashMap<>();
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

        properties.put("ok_property", 123);
        properties.put("some_property", new Object());
        assertTrue(client.track("key1", "user", "purchase", properties));
        verify(eventClientMock).track(eventArgumentCaptor.capture(), Mockito.anyInt());
        Event captured = eventArgumentCaptor.getValue();
        assertEquals(2, captured.properties.size());
        assertEquals(123, captured.properties.get("ok_property"));
        assertNull(captured.properties.get("some_property"));

        properties.clear();
        Mockito.reset(eventClientMock);
        Mockito.when(eventClientMock.track((Event) Mockito.any(), Mockito.anyInt())).thenReturn(true);
        properties.put("ok_property", 123);
        properties.put("some_property", Arrays.asList(1, 2, 3));
        assertTrue(client.track("key1", "user", "purchase", properties));
        eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventClientMock).track(eventArgumentCaptor.capture(), Mockito.anyInt());
        captured = eventArgumentCaptor.getValue();
        assertEquals(2, captured.properties.size());
        assertEquals(123, captured.properties.get("ok_property"));
        assertNull(captured.properties.get("some_property"));

        properties.clear();
        Mockito.reset(eventClientMock);
        Mockito.when(eventClientMock.track((Event) Mockito.any(), Mockito.anyInt())).thenReturn(true);
        properties.put("ok_property", 123);
        properties.put("some_property", new HashMap<String, Number>());
        assertTrue(client.track("key1", "user", "purchase", properties));
        eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventClientMock).track(eventArgumentCaptor.capture(), Mockito.anyInt());
        captured = eventArgumentCaptor.getValue();
        assertEquals(2, captured.properties.size());
        assertEquals(123, captured.properties.get("ok_property"));
        assertNull(captured.properties.get("some_property"));

        properties.clear();
        Mockito.reset(eventClientMock);
        Mockito.when(eventClientMock.track((Event) Mockito.any(), Mockito.anyInt())).thenReturn(true);
        properties.put("ok_property", 123);
        assertTrue(client.track("key1", "user", "purchase", 123, properties));
        eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventClientMock).track(eventArgumentCaptor.capture(), Mockito.anyInt());
        captured = eventArgumentCaptor.getValue();
        assertEquals(123.0, captured.value, 0);
        assertEquals("user", captured.trafficTypeName);
        assertEquals("purchase", captured.eventTypeId);
        assertEquals("key1", captured.key);
        assertEquals(1, captured.properties.size());
        assertEquals(123, captured.properties.get("ok_property"));

        properties.clear();
        Mockito.reset(eventClientMock);
        Mockito.when(eventClientMock.track((Event) Mockito.any(), Mockito.anyInt())).thenReturn(true);
        properties.put("prop1", 1);
        properties.put("prop2", 2L);
        properties.put("prop3", 7.56);
        properties.put("prop4", "something");
        properties.put("prop5", true);
        properties.put("prop6", null);
        assertTrue(client.track("key1", "user", "purchase", properties));
        eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventClientMock).track(eventArgumentCaptor.capture(), Mockito.anyInt());
        captured = eventArgumentCaptor.getValue();
        assertEquals(6, captured.properties.size());
        assertEquals(1, captured.properties.get("prop1"));
        assertEquals(2L, captured.properties.get("prop2"));
        assertEquals(7.56, captured.properties.get("prop3"));
        assertEquals("something", captured.properties.get("prop4"));
        assertTrue((Boolean) captured.properties.get("prop5"));
        assertNull(captured.properties.get("prop6"));

        // 110 props of 300 bytes should be enough to make the event fail.
        properties.clear();
        for (int i = 0; i < 110; ++i) {
            properties.put(new String(new char[300]).replace('\0', 'a') + i ,
                    new String(new char[300]).replace('\0', 'a') + i);
        }
        Assert.assertFalse(client.track("key1", "user", "purchase", properties));
    }

    @Test
    public void clientCannotPerformActionsWhenDestroyed() throws InterruptedException, URISyntaxException, TimeoutException, IOException {
        String test = "split";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitFactory mockFactory = new SplitFactory() {
            private boolean destroyed = false;

            @Override
            public SplitClient client() { return null; }

            @Override
            public SplitManager manager() { return null; }

            @Override
            public void destroy() { destroyed = true; }

            @Override
            public boolean isDestroyed() { return destroyed; }
        };

        SplitClientImpl client = new SplitClientImpl(
                mockFactory,
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        assertEquals(Treatments.ON, client.getTreatment("valid", "split"));
        assertTrue(client.track("validKey", "valid_traffic_type", "valid_event"));

        client.destroy();

        assertEquals(Treatments.CONTROL, client.getTreatment("valid", "split"));
        Assert.assertFalse(client.track("validKey", "valid_traffic_type", "valid_event"));
    }

    @Test
    public void worksAndHasConfigTryKetTreatmentWithKey() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        // Add config for only one treatment
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.ON, "{\"size\" : 30}");

        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, configurations, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        int numKeys = 5;
        for (int i = 0; i < numKeys; i++) {
            Map<String, Object> attributes = new HashMap<>();
            String randomKey = RandomStringUtils.random(10);
            Key key = new Key(randomKey, "BucketingKey");
            assertEquals("on", client.getTreatment(randomKey, test));
            assertEquals("{\"size\" : 30}", client.getTreatmentWithConfig(key, test, attributes).config());
        }

        // Times 2 because we are calling getTreatment twice. Once for getTreatment and one for getTreatmentWithConfig
        verify(splitCacheConsumer, times(numKeys * 2)).get(test);
    }

    @Test
    public void worksAndHasConfigByFlagSetTryKetTreatmentWithKey() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        // Add config for only one treatment
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.ON, "{\"size\" : 30}");

        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, configurations, new HashSet<>(Arrays.asList("set1")));

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        HashMap<String, HashSet<String>> flagsBySets = new HashMap<>();
        flagsBySets.put("set1", new HashSet<>(Arrays.asList(test)));
        when(splitCacheConsumer.getNamesByFlagSets(Arrays.asList("set1"))).thenReturn(flagsBySets);

        Map<String, ParsedSplit> fetchManyResult = new HashMap<>();
        fetchManyResult.put(test, parsedSplit);
        when(splitCacheConsumer.fetchMany(Arrays.asList(test))).thenReturn(fetchManyResult);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        int numKeys = 5;
        for (int i = 0; i < numKeys; i++) {
            Map<String, Object> attributes = new HashMap<>();
            String randomKey = RandomStringUtils.random(10);
            Key key = new Key(randomKey, "BucketingKey");
            assertEquals("on", client.getTreatmentsByFlagSet(randomKey, "set1", new HashMap<>()).get(test));
            assertEquals("{\"size\" : 30}", client.getTreatmentsWithConfigByFlagSet(key, "set1", attributes).get(test).config());
        }
    }

    @Test
    public void worksAndHasConfigByFlagSetsTryKetTreatmentWithKey() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        // Add config for only one treatment
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.ON, "{\"size\" : 30}");

        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, configurations, new HashSet<>(Arrays.asList("set1")));

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        HashMap<String, HashSet<String>> flagsBySets = new HashMap<>();
        flagsBySets.put("set1", new HashSet<>(Arrays.asList(test)));
        when(splitCacheConsumer.getNamesByFlagSets(Arrays.asList("set1"))).thenReturn(flagsBySets);

        Map<String, ParsedSplit> fetchManyResult = new HashMap<>();
        fetchManyResult.put(test, parsedSplit);
        when(splitCacheConsumer.fetchMany(Arrays.asList(test))).thenReturn(fetchManyResult);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        int numKeys = 5;
        for (int i = 0; i < numKeys; i++) {
            Map<String, Object> attributes = new HashMap<>();
            String randomKey = RandomStringUtils.random(10);
            Key key = new Key(randomKey, "BucketingKey");
            assertEquals("on", client.getTreatmentsByFlagSets(randomKey, Arrays.asList("set1"), new HashMap<>()).get(test));
            assertEquals("{\"size\" : 30}", client.getTreatmentsWithConfigByFlagSets(key, Arrays.asList("set1"), attributes).get(test).config());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void blockUntilReadyException() throws TimeoutException, InterruptedException {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, new HashSet<>());

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(test)).thenReturn(parsedSplit);

        SplitClientConfig config = SplitClientConfig.builder().setBlockUntilReadyTimeout(-100).build();
        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        client.blockUntilReady();
    }

    @Test
    public void nullKeyResultsInControlGetTreatments() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, new HashSet<>());
        Map<String, ParsedSplit> splits = new HashMap<>();
        splits.put(test, parsedSplit);
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.fetchMany(Collections.singletonList(test))).thenReturn(splits);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        assertEquals(Treatments.CONTROL, client.getTreatments(null, Collections.singletonList("test1")).get("test1"));

        verifyZeroInteractions(splitCacheConsumer);
    }

    @Test
    public void nullSplitsResultsInEmptyGetTreatments() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, new HashSet<>());
        Map<String, ParsedSplit> splits = new HashMap<>();
        splits.put(test, parsedSplit);
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.fetchMany(Collections.singletonList(test))).thenReturn(splits);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        assertEquals(0, client.getTreatments("key", null).size());

        verifyZeroInteractions(splitCacheConsumer);
    }

    @Test
    public void exceptionsResultInControlGetTreatments() {
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.fetchMany(anyList())).thenThrow(RuntimeException.class);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        Map<String, String> result = client.getTreatments("adil@relateiq.com", Arrays.asList("test1", "test2"));
        assertEquals(2, result.values().size());
        assertEquals(Treatments.CONTROL, result.get("test1"));
        assertEquals(Treatments.CONTROL, result.get("test2"));

        verify(splitCacheConsumer).fetchMany(anyList());
    }

    @Test
    public void getTreatmentsWorks() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, new HashSet<>());
        Map<String, ParsedSplit> splits = new HashMap<>();
        splits.put(test, parsedSplit);
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.fetchMany(anyList())).thenReturn(splits);
        when(gates.isSDKReady()).thenReturn(true);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        Map<String, String> result = client.getTreatments("randomKey", Arrays.asList(test, "test2"));
        assertEquals("on", result.get(test));
        assertEquals(Treatments.CONTROL, result.get("test2"));

        verify(TELEMETRY_STORAGE, times(1)).recordLatency(Mockito.anyObject(), Mockito.anyLong());
    }

    @Test
    public void emptySplitsResultsInNullGetTreatments() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, new HashSet<>());
        Map<String, ParsedSplit> splits = new HashMap<>();
        splits.put(test, parsedSplit);
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.fetchMany(Collections.singletonList(test))).thenReturn(splits);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        Map<String, String> result = client.getTreatments("key", new ArrayList<>());
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyZeroInteractions(splitCacheConsumer);
    }

    @Test
    public void exceptionsResultInControlTreatments() {
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.get(anyString())).thenThrow(RuntimeException.class);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        Map<String, String> result = client.getTreatments("adil@relateiq.com", Arrays.asList("test1"));
        assertEquals(1, result.size());
        assertEquals(Treatments.CONTROL, result.get("test1"));
    }

    @Test
    public void worksTreatments() {
        String test = "test1";
        String test2 = "test2";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, new HashSet<>());
        ParsedSplit parsedSplit2 = ParsedSplit.createParsedSplitForTests(test2, 123, false, Treatments.OFF, conditions,
                null, 1, 1, new HashSet<>());
        Map<String, ParsedSplit> parsedSplits = new HashMap<>();
        parsedSplits.put(test, parsedSplit);
        parsedSplits.put(test2, parsedSplit2);

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.fetchMany(anyList())).thenReturn(parsedSplits);
        when(gates.isSDKReady()).thenReturn(true);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        Map<String, String> result = client.getTreatments("anyKey", Arrays.asList(test, test2));
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("on", result.get(test));
        assertEquals("on", result.get(test2));

        verify(splitCacheConsumer, times(1)).fetchMany(anyList());
        verify(TELEMETRY_STORAGE, times(1)).recordLatency(Mockito.anyObject(), Mockito.anyLong());
    }

    @Test
    public void worksOneControlTreatments() {
        String test = "test1";
        String test2 = "test2";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, new HashSet<>());
        Map<String, ParsedSplit> parsedSplits = new HashMap<>();
        parsedSplits.put(test, parsedSplit);

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.fetchMany(anyList())).thenReturn(parsedSplits);
        when(gates.isSDKReady()).thenReturn(true);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        Map<String, String> result = client.getTreatments("anyKey", Arrays.asList(test, test2));
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("on", result.get(test));
        assertEquals("control", result.get(test2));

        verify(splitCacheConsumer, times(1)).fetchMany(anyList());
        verify(TELEMETRY_STORAGE, times(1)).recordLatency(Mockito.anyObject(), Mockito.anyLong());
    }

    @Test
    public void treatmentsWorksAndHasConfig() {
        String test = "test1";
        String test2 = "test2";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        // Add config for only one treatment
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.ON, "{\"size\" : 30}");
        configurations.put(Treatments.CONTROL, "{\"size\" : 30}");


        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, configurations, new HashSet<>());
        Map<String, ParsedSplit> parsedSplits = new HashMap<>();
        parsedSplits.put(test, parsedSplit);
        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.fetchMany(anyList())).thenReturn(parsedSplits);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        Map<String, Object> attributes = new HashMap<>();
        Map<String, SplitResult> result = client.getTreatmentsWithConfig("randomKey", Arrays.asList(test, test2, "", null), attributes);
        assertEquals(2, result.size());
        assertEquals(configurations.get("on"), result.get(test).config());
        assertNull(result.get(test2).config());
        assertEquals("control", result.get(test2).treatment());

        verify(splitCacheConsumer, times(1)).fetchMany(anyList());
    }

    @Test
    public void testTreatmentsByFlagSet() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, new HashSet<>(Arrays.asList("set1", "set2")));

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        Map<String, ParsedSplit> fetchManyResult = new HashMap<>();
        fetchManyResult.put(test, parsedSplit);
        when(splitCacheConsumer.fetchMany(new ArrayList<>(Arrays.asList(test)))).thenReturn(fetchManyResult);
        List<String> sets = new ArrayList<>(Arrays.asList("set1"));
        Map<String, HashSet<String>> flagsBySets = new HashMap<>();
        flagsBySets.put("set1", new HashSet<>(Arrays.asList(test)));
        when(splitCacheConsumer.getNamesByFlagSets(sets)).thenReturn(flagsBySets);
        when(gates.isSDKReady()).thenReturn(true);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );

        int numKeys = 5;
        Map<String, String> getTreatmentResult;
        for (int i = 0; i < numKeys; i++) {
            String randomKey = RandomStringUtils.random(10);
            getTreatmentResult = client.getTreatmentsByFlagSet(randomKey, "set1", null);
            assertEquals("on", getTreatmentResult.get(test));
        }
        verify(splitCacheConsumer, times(numKeys)).fetchMany(new ArrayList<>(Arrays.asList(test)));
        verify(TELEMETRY_STORAGE, times(5)).recordLatency(Mockito.anyObject(), Mockito.anyLong());
    }

    @Test
    public void testTreatmentsByFlagSetInvalid() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, new HashSet<>(Arrays.asList("set1", "set2")));

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        List<String> sets = new ArrayList<>();
        when(gates.isSDKReady()).thenReturn(true);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        assertTrue(client.getTreatmentsByFlagSet(RandomStringUtils.random(10), "", null).isEmpty());
    }

    @Test
    public void testTreatmentsByFlagSets() {
        String test = "test1";
        String test2 = "test2";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, new HashSet<>(Arrays.asList("set1", "set2")));
        ParsedSplit parsedSplit2 = ParsedSplit.createParsedSplitForTests(test2, 123, false, Treatments.OFF, conditions,
                null, 1, 1, new HashSet<>(Arrays.asList("set3", "set4")));

        SDKReadinessGates gates = mock(SDKReadinessGates.class);
        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);

        Map<String, ParsedSplit> fetchManyResult = new HashMap<>();
        fetchManyResult.put(test, parsedSplit);
        fetchManyResult.put(test2, parsedSplit2);
        when(splitCacheConsumer.fetchMany(new ArrayList<>(Arrays.asList(test2, test)))).thenReturn(fetchManyResult);

        List<String> sets = new ArrayList<>(Arrays.asList("set3", "set1"));
        Map<String, HashSet<String>> flagsBySets = new HashMap<>();
        flagsBySets.put("set1", new HashSet<>(Arrays.asList(test)));
        flagsBySets.put("set3", new HashSet<>(Arrays.asList(test2)));

        when(splitCacheConsumer.getNamesByFlagSets(sets)).thenReturn(flagsBySets);
        when(gates.isSDKReady()).thenReturn(true);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        int numKeys = 5;
        Map<String, String> getTreatmentResult;
        for (int i = 0; i < numKeys; i++) {
            String randomKey = RandomStringUtils.random(10);
            getTreatmentResult = client.getTreatmentsByFlagSets(randomKey, Arrays.asList("set1", "set3"), null);
            assertEquals("on", getTreatmentResult.get(test));
            assertEquals("on", getTreatmentResult.get(test2));
        }
        verify(splitCacheConsumer, times(numKeys)).fetchMany(new ArrayList<>(Arrays.asList(test2, test)));
        verify(TELEMETRY_STORAGE, times(5)).recordLatency(Mockito.anyObject(), Mockito.anyLong());
    }

    @Test
    public void treatmentsWorksAndHasConfigFlagSet() {
        String test = "test1";
        String test2 = "test2";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        // Add config for only one treatment
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.ON, "{\"size\" : 30}");
        configurations.put(Treatments.CONTROL, "{\"size\" : 30}");


        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, configurations, new HashSet<>(Arrays.asList("set1")));
        Map<String, ParsedSplit> parsedSplits = new HashMap<>();
        parsedSplits.put(test, parsedSplit);

        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.fetchMany(anyList())).thenReturn(parsedSplits);

        List<String> sets = new ArrayList<>(Arrays.asList("set1"));
        Map<String, HashSet<String>> flagsBySets = new HashMap<>();
        flagsBySets.put("set1", new HashSet<>(Arrays.asList(test, test2)));
        when(splitCacheConsumer.getNamesByFlagSets(sets)).thenReturn(flagsBySets);

        SDKReadinessGates gates = mock(SDKReadinessGates.class);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        Map<String, Object> attributes = new HashMap<>();
        Map<String, SplitResult> result = client.getTreatmentsWithConfigByFlagSet("randomKey", "set1", attributes);
        assertEquals(2, result.size());
        assertEquals(configurations.get("on"), result.get(test).config());
        assertNull(result.get(test2).config());
        assertEquals("control", result.get(test2).treatment());

        verify(splitCacheConsumer, times(1)).fetchMany(anyList());
    }

    @Test
    public void treatmentsWorksAndHasConfigFlagSets() {
        String test = "test1";
        String test2 = "test2";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        // Add config for only one treatment
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.ON, "{\"size\" : 30}");
        configurations.put(Treatments.CONTROL, "{\"size\" : 30}");


        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions,
                null, 1, 1, configurations, new HashSet<>(Arrays.asList("set1")));
        Map<String, ParsedSplit> parsedSplits = new HashMap<>();
        parsedSplits.put(test, parsedSplit);

        SplitCacheConsumer splitCacheConsumer = mock(SplitCacheConsumer.class);
        SegmentCacheConsumer segmentCacheConsumer = mock(SegmentCacheConsumer.class);
        when(splitCacheConsumer.fetchMany(anyList())).thenReturn(parsedSplits);

        List<String> sets = new ArrayList<>(Arrays.asList("set1"));
        Map<String, HashSet<String>> flagsBySets = new HashMap<>();
        flagsBySets.put("set1", new HashSet<>(Arrays.asList(test, test2)));
        when(splitCacheConsumer.getNamesByFlagSets(sets)).thenReturn(flagsBySets);

        SDKReadinessGates gates = mock(SDKReadinessGates.class);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitCacheConsumer,
                new ImpressionsManager.NoOpImpressionsManager(),
                NoopEventsStorageImp.create(),
                config,
                gates,
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE,
                flagSetsFilter
        );
        Map<String, Object> attributes = new HashMap<>();
        Map<String, SplitResult> result = client.getTreatmentsWithConfigByFlagSets("randomKey", new ArrayList<>(Arrays.asList("set1")), attributes);
        assertEquals(2, result.size());
        assertEquals(configurations.get("on"), result.get(test).config());
        assertNull(result.get(test2).config());
        assertEquals("control", result.get(test2).treatment());

        verify(splitCacheConsumer, times(1)).fetchMany(anyList());
    }
}