package io.split.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
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
    private SplitClientConfig config = SplitClientConfig.builder().setBlockUntilReadyTimeout(100).build();

    @Before
    public void updateTelemetryStorage() {
        TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);
    }

    @Test
    public void null_key_results_in_control() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        assertThat(client.getTreatment(null, "test1"), is(equalTo(Treatments.CONTROL)));

        verifyZeroInteractions(splitCacheConsumer);
    }

    @Test
    public void null_test_results_in_control() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        assertThat(client.getTreatment("adil@relateiq.com", null), is(equalTo(Treatments.CONTROL)));

        verifyZeroInteractions(splitCacheConsumer);
    }

    @Test
    public void exceptions_result_in_control() {
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );
        assertThat(client.getTreatment("adil@relateiq.com", "test1"), is(equalTo(Treatments.CONTROL)));

        verify(splitCacheConsumer).get("test1");
    }

    @Test
    public void works() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        int numKeys = 5;
        for (int i = 0; i < numKeys; i++) {
            String randomKey = RandomStringUtils.random(10);
            assertThat(client.getTreatment(randomKey, test), is(equalTo("on")));
        }

        verify(splitCacheConsumer, times(numKeys)).get(test);
        verify(TELEMETRY_STORAGE, times(5)).recordLatency(Mockito.anyObject(), Mockito.anyLong());
    }

    /**
     * There is no config for this treatment
     */
    @Test
    public void works_null_config() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );


        String randomKey = RandomStringUtils.random(10);
        SplitResult result = client.getTreatmentWithConfig(randomKey, test);
        assertThat(result.treatment(), is(equalTo(Treatments.ON)));
        assertThat(result.config(), is(nullValue()));

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

        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, configurations);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        int numKeys = 5;
        for (int i = 0; i < numKeys; i++) {
            Map<String, Object> attributes = new HashMap<>();
            String randomKey = RandomStringUtils.random(10);
            assertThat(client.getTreatment(randomKey, test), is(equalTo("on")));
            assertThat(client.getTreatmentWithConfig(randomKey, test, attributes).config(), is(equalTo(configurations.get("on"))));
        }

        // Times 2 because we are calling getTreatment twice. Once for getTreatment and one for getTreatmentWithConfig
        verify(splitCacheConsumer, times(numKeys * 2)).get(test);
    }

    @Test
    public void last_condition_is_always_default() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        assertThat(client.getTreatment("pato@codigo.com", test), is(equalTo(Treatments.OFF)));

        verify(splitCacheConsumer).get(test);
    }

    /**
     * Tests that we retrieve configs from the default treatment
     */
    @Test
    public void last_condition_is_always_default_but_with_treatment() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        // Add config for only one treatment(default)
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.OFF, "{\"size\" : 30}");

        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1, configurations);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        SplitResult result = client.getTreatmentWithConfig("pato@codigo.com", test);
        assertThat(result.treatment(), is(equalTo(Treatments.OFF)));
        assertThat(result.config(), is(equalTo("{\"size\" : 30}")));

        verify(splitCacheConsumer).get(test);
    }

    @Test
    public void multiple_conditions_work() {
        String test = "test1";

        ParsedCondition adil_is_always_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        ParsedCondition pato_is_never_shown = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("pato@codigo.com"))), Lists.newArrayList(partition("off", 100)));
        ParsedCondition trevor_is_always_shown = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("trevor@codigo.com"))), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(adil_is_always_on, pato_is_never_shown, trevor_is_always_shown);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        assertThat(client.getTreatment("adil@codigo.com", test), is(equalTo("on")));
        assertThat(client.getTreatment("pato@codigo.com", test), is(equalTo("off")));
        assertThat(client.getTreatment("trevor@codigo.com", test), is(equalTo("on")));

        verify(splitCacheConsumer, times(3)).get(test);
        verify(TELEMETRY_STORAGE, times(3)).recordNonReadyUsage();
    }


    @Test
    public void killed_test_always_goes_to_default() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, true, Treatments.OFF, conditions, "user", 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        assertThat(client.getTreatment("adil@codigo.com", test), is(equalTo(Treatments.OFF)));

        verify(splitCacheConsumer).get(test);
    }

    /**
     * when killed, the evaluator follows a slightly different path. So testing that when there is a config.
     */
    @Test
    public void killed_test_always_goes_to_default_has_config() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        // Add config for only one treatment(default)
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.OFF, "{\"size\" : 30}");

        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, true, Treatments.OFF, conditions, "user", 1, 1, configurations);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        SplitResult result = client.getTreatmentWithConfig("adil@codigo.com", test);
        assertThat(result.treatment(), is(equalTo(Treatments.OFF)));
        assertThat(result.config(), is(equalTo("{\"size\" : 30}")));

        verify(splitCacheConsumer).get(test);
    }

    @Test
    public void dependency_matcher_on() {
        String parent = "parent";
        String dependent = "dependent";

        ParsedCondition parent_is_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<ParsedCondition> parent_conditions = Lists.newArrayList(parent_is_on);
        ParsedSplit parentSplit = ParsedSplit.createParsedSplitForTests(parent, 123, false, Treatments.OFF, parent_conditions, null, 1, 1);

        ParsedCondition dependent_needs_parent = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new DependencyMatcher(parent, Lists.newArrayList(Treatments.ON))), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<ParsedCondition> dependent_conditions = Lists.newArrayList(dependent_needs_parent);
        ParsedSplit dependentSplit = ParsedSplit.createParsedSplitForTests(dependent, 123, false, Treatments.OFF, dependent_conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        assertThat(client.getTreatment("key", parent), is(equalTo(Treatments.ON)));
        assertThat(client.getTreatment("key", dependent), is(equalTo(Treatments.ON)));
    }

    @Test
    public void dependency_matcher_off() {
        String parent = "parent";
        String dependent = "dependent";

        ParsedCondition parent_is_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<ParsedCondition> parent_conditions = Lists.newArrayList(parent_is_on);
        ParsedSplit parentSplit = ParsedSplit.createParsedSplitForTests(parent, 123, false, Treatments.OFF, parent_conditions, null, 1, 1);

        ParsedCondition dependent_needs_parent = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new DependencyMatcher(parent, Lists.newArrayList(Treatments.OFF))), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<ParsedCondition> dependent_conditions = Lists.newArrayList(dependent_needs_parent);
        ParsedSplit dependentSplit = ParsedSplit.createParsedSplitForTests(dependent, 123, false, Treatments.OFF, dependent_conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        assertThat(client.getTreatment("key", parent), is(equalTo(Treatments.ON)));
        assertThat(client.getTreatment("key", dependent), is(equalTo(Treatments.OFF)));
    }

    @Test
    public void dependency_matcher_control() {
        String dependent = "dependent";

        ParsedCondition dependent_needs_parent = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new DependencyMatcher("not-exists", Lists.newArrayList(Treatments.OFF))), Lists.newArrayList(partition(Treatments.OFF, 100)));
        List<ParsedCondition> dependent_conditions = Lists.newArrayList(dependent_needs_parent);
        ParsedSplit dependentSplit = ParsedSplit.createParsedSplitForTests(dependent, 123, false, Treatments.ON, dependent_conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        assertThat(client.getTreatment("key", dependent), is(equalTo(Treatments.ON)));
    }

    @Test
    public void attributes_work() {
        String test = "test1";

        ParsedCondition adil_is_always_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition(Treatments.ON, 100)));
        ParsedCondition users_with_age_greater_than_10_are_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("age", new GreaterThanOrEqualToMatcher(10, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(adil_is_always_on, users_with_age_greater_than_10_are_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        assertThat(client.getTreatment("adil@codigo.com", test), is(equalTo("on")));
        assertThat(client.getTreatment("adil@codigo.com", test, null), is(equalTo("on")));
        assertThat(client.getTreatment("adil@codigo.com", test, ImmutableMap.<String, Object>of()), is(equalTo("on")));

        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 10)), is(equalTo("on")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 9)), is(equalTo("off")));

        verify(splitCacheConsumer, times(5)).get(test);
    }

    @Test
    public void attributes_work_2() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("age", new EqualToMatcher(0, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        assertThat(client.getTreatment("adil@codigo.com", test), is(equalTo("off")));
        assertThat(client.getTreatment("adil@codigo.com", test, null), is(equalTo("off")));
        assertThat(client.getTreatment("adil@codigo.com", test, ImmutableMap.<String, Object>of()), is(equalTo("off")));

        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 10)), is(equalTo("off")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 0)), is(equalTo("on")));

        verify(splitCacheConsumer, times(5)).get(test);
    }

    @Test
    public void attributes_greater_than_negative_number() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        assertThat(client.getTreatment("adil@codigo.com", test), is(equalTo("off")));
        assertThat(client.getTreatment("adil@codigo.com", test, null), is(equalTo("off")));
        assertThat(client.getTreatment("adil@codigo.com", test, ImmutableMap.<String, Object>of()), is(equalTo("off")));

        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 10)), is(equalTo("off")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", -20)), is(equalTo("on")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 20)), is(equalTo("off")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", -21)), is(equalTo("off")));

        verify(splitCacheConsumer, times(7)).get(test);
    }


    @Test
    public void attributes_for_sets() {
        String test = "test1";

        ParsedCondition any_of_set = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("products", new ContainsAnyOfSetMatcher(Lists.<String>newArrayList("sms", "video"))), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(any_of_set);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer ,segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        assertThat(client.getTreatment("adil@codigo.com", test), is(equalTo("off")));
        assertThat(client.getTreatment("adil@codigo.com", test, null), is(equalTo("off")));
        assertThat(client.getTreatment("adil@codigo.com", test, ImmutableMap.<String, Object>of()), is(equalTo("off")));

        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("products", Lists.newArrayList())), is(equalTo("off")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("products", Lists.newArrayList(""))), is(equalTo("off")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("products", Lists.newArrayList("talk"))), is(equalTo("off")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("products", Lists.newArrayList("sms"))), is(equalTo("on")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("products", Lists.newArrayList("sms", "video"))), is(equalTo("on")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("products", Lists.newArrayList("video"))), is(equalTo("on")));

        verify(splitCacheConsumer, times(9)).get(test);
    }

    @Test
    public void labels_are_populated() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = new ParsedCondition(ConditionType.ROLLOUT,
                CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)),
                Lists.newArrayList(partition("on", 100)),
                "foolabel"
        );

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        Map<String, Object> attributes = ImmutableMap.<String, Object>of("age", -20, "acv", "1000000");
        assertThat(client.getTreatment("pato@codigo.com", test, attributes), is(equalTo("on")));

        ArgumentCaptor<List> impressionCaptor = ArgumentCaptor.forClass(List.class);

        verify(impressionsManager).track(impressionCaptor.capture());

        List<Impression> impressions = impressionCaptor.getValue();
        assertNotNull(impressions);
        assertEquals(1, impressions.size());
        Impression impression = impressions.get(0);

        assertThat(impression.appliedRule(), is(equalTo("foolabel")));

        assertThat(impression.attributes(), is(attributes));
    }

    @Test
    public void not_in_split_if_no_allocation() {
        traffic_allocation("pato@split.io", 0, 123, "off", "not in split");
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
    public void not_in_split_if_10_percent_allocation() {

        String key = "pato@split.io";
        int i = 0;
        for (; i <= 9; i++) {
            traffic_allocation(key, i, 123, "off", "not in split");
        }

        for (; i <= 100; i++) {
            traffic_allocation(key, i, 123, "on", "in segment all");
        }
    }

    @Test
    public void traffic_allocation_one_percent() {
        //This key, with this seed it should fall in the 1%
        String fallsInOnePercent = "pato193";
        traffic_allocation(fallsInOnePercent, 1, 123, "on", "in segment all");

        //All these others should not be in split
        for (int offset = 0; offset <= 100; offset++) {
            traffic_allocation("pato" + String.valueOf(offset), 1, 123, "off", "not in split");
        }

    }

    @Test
    public void in_split_if_100_percent_allocation() {
        traffic_allocation("pato@split.io", 100, 123, "on", "in segment all");
    }

    @Test
    public void whitelist_overrides_traffic_allocation() {
        traffic_allocation("adil@split.io", 0, 123, "on", "whitelisted user");
    }


    private void traffic_allocation(String key, int trafficAllocation, int trafficAllocationSeed, String expected_treatment_on_or_off, String label) {

        String test = "test1";

        ParsedCondition whitelistCondition = new ParsedCondition(ConditionType.WHITELIST, CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@split.io"))), Lists.newArrayList(partition("on", 100), partition("off", 0)), "whitelisted user");
        ParsedCondition rollOutToEveryone = new ParsedCondition(ConditionType.ROLLOUT, CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100), partition("off", 0)), "in segment all");

        List<ParsedCondition> conditions = Lists.newArrayList(whitelistCondition, rollOutToEveryone);

        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, null, 1, trafficAllocation, trafficAllocationSeed, 1, null);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        assertThat(client.getTreatment(key, test), is(equalTo(expected_treatment_on_or_off)));

        ArgumentCaptor<List> impressionCaptor = ArgumentCaptor.forClass(List.class);

        verify(impressionsManager).track(impressionCaptor.capture());
        assertNotNull(impressionCaptor.getValue());
        assertEquals(1, impressionCaptor.getValue().size());
        Impression impression = (Impression) impressionCaptor.getValue().get(0);
        assertThat(impression.appliedRule(), is(equalTo(label)));
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

        ParsedCondition rollOutToEveryone = new ParsedCondition(ConditionType.ROLLOUT, CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100), partition("off", 0)), "in segment all");

        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, null, 1, trafficAllocation, trafficAllocationSeed, 1, configurations);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        assertThat(client.getTreatment("pato@split.io", test), is(equalTo(Treatments.OFF)));

        SplitResult result = client.getTreatmentWithConfig("pato@split.io", test);
        assertThat(result.treatment(), is(equalTo(Treatments.OFF)));
        assertThat(result.config(), is(equalTo("{\"size\" : 30}")));

        ArgumentCaptor<List> impressionCaptor = ArgumentCaptor.forClass(List.class);
        verify(impressionsManager, times(2)).track(impressionCaptor.capture());

        assertNotNull(impressionCaptor.getValue());
        assertEquals(1, impressionCaptor.getValue().size());
        Impression impression = (Impression) impressionCaptor.getValue().get(0);
        assertThat(impression.appliedRule(), is(equalTo("not in split")));
    }


    @Test
    public void matching_bucketing_keys_work() {
        String test = "test1";


        Set<String> whitelist = new HashSet<>();
        whitelist.add("aijaz");
        ParsedCondition aijaz_should_match = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(whitelist)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(aijaz_should_match);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        Key bad_key = new Key("adil", "aijaz");
        Key good_key = new Key("aijaz", "adil");

        assertThat(client.getTreatment(bad_key, test, Collections.<String, Object>emptyMap()), is(equalTo("off")));
        assertThat(client.getTreatment(good_key, test, Collections.<String, Object>emptyMap()), is(equalTo("on")));

        verify(splitCacheConsumer, times(2)).get(test);
    }

    @Test
    public void impression_metadata_is_propagated() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = new ParsedCondition(ConditionType.ROLLOUT,
                CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)),
                Lists.newArrayList(partition("on", 100)),
                "foolabel"
        );

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        Map<String, Object> attributes = ImmutableMap.<String, Object>of("age", -20, "acv", "1000000");

        assertThat(client.getTreatment("pato@codigo.com", test, attributes), is(equalTo("on")));

        ArgumentCaptor<List> impressionCaptor = ArgumentCaptor.forClass(List.class);

        verify(impressionsManager).track(impressionCaptor.capture());


        assertNotNull(impressionCaptor.getValue());
        assertEquals(1, impressionCaptor.getValue().size());
        Impression impression = (Impression) impressionCaptor.getValue().get(0);

        assertThat(impression.appliedRule(), is(equalTo("foolabel")));
        assertThat(impression.attributes(), is(equalTo(attributes)));
    }

    private Partition partition(String treatment, int size) {
        Partition p = new Partition();
        p.treatment = treatment;
        p.size = size;
        return p;
    }

    @Test
    public void block_until_ready_does_not_time_when_sdk_is_ready() throws TimeoutException, InterruptedException {
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        client.blockUntilReady();
    }

    @Test(expected = TimeoutException.class)
    public void block_until_ready_times_when_sdk_is_not_ready() throws TimeoutException, InterruptedException {
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        client.blockUntilReady();
    }

    @Test
    public void track_with_valid_parameters() {
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        Assert.assertThat(client.track("validKey", "valid_traffic_type", "valid_event"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(true)));

        String validEventSize = new String(new char[80]).replace('\0', 'a');
        String validKeySize = new String(new char[250]).replace('\0', 'a');
        Assert.assertThat(client.track(validKeySize, "valid_traffic_type", validEventSize, 10),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(true)));
        verify(TELEMETRY_STORAGE, times(2)).recordLatency(Mockito.anyObject(), Mockito.anyLong());

    }

    @Test
    public void track_with_invalid_event_type_ids() {
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        Assert.assertThat(client.track("validKey", "valid_traffic_type", ""),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(false)));

        Assert.assertThat(client.track("validKey", "valid_traffic_type", null),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(false)));

        Assert.assertThat(client.track("validKey", "valid_traffic_type", "invalid#char"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(false)));

        String invalidEventSize = new String(new char[81]).replace('\0', 'a');
        Assert.assertThat(client.track("validKey", "valid_traffic_type", invalidEventSize),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(false)));

    }

    @Test
    public void track_with_invalid_traffic_type_names() {
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        Assert.assertThat(client.track("validKey", "", "valid"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(false)));

        Assert.assertThat(client.track("validKey", null, "valid"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(false)));
    }

    @Test
    public void track_with_invalid_keys() {
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        Assert.assertThat(client.track("", "valid_traffic_type", "valid"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(false)));

        Assert.assertThat(client.track(null, "valid_traffic_type", "valid"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(false)));

        String invalidKeySize = new String(new char[251]).replace('\0', 'a');
        Assert.assertThat(client.track(invalidKeySize, "valid_traffic_type", "valid"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(false)));
    }

    @Test
    public void getTreatment_with_invalid_keys() {
        String test = "split";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        Assert.assertThat(client.getTreatment("valid", "split"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.not(Treatments.CONTROL)));

        Assert.assertThat(client.getTreatment("", "split"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(Treatments.CONTROL)));

        Assert.assertThat(client.getTreatment(null, "split"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(Treatments.CONTROL)));

        String invalidKeySize = new String(new char[251]).replace('\0', 'a');
        Assert.assertThat(client.getTreatment(invalidKeySize, "split"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(Treatments.CONTROL)));

        Assert.assertThat(client.getTreatment("valid", ""),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(Treatments.CONTROL)));

        Assert.assertThat(client.getTreatment("valid", null),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(Treatments.CONTROL)));

        String matchingKey = new String(new char[250]).replace('\0', 'a');
        String bucketingKey = new String(new char[250]).replace('\0', 'a');
        Key key = new Key(matchingKey, bucketingKey);
        Assert.assertThat(client.getTreatment(key, "split", null),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.not(Treatments.CONTROL)));

        key = new Key("valid", "");
        Assert.assertThat(client.getTreatment(key, "split", null),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(Treatments.CONTROL)));

        key = new Key("", "valid");
        Assert.assertThat(client.getTreatment(key, "split", null),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(Treatments.CONTROL)));

        matchingKey = new String(new char[251]).replace('\0', 'a');
        bucketingKey = new String(new char[250]).replace('\0', 'a');
        key = new Key(matchingKey, bucketingKey);
        Assert.assertThat(client.getTreatment(key, "split", null),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.is(Treatments.CONTROL)));

        matchingKey = new String(new char[250]).replace('\0', 'a');
        bucketingKey = new String(new char[251]).replace('\0', 'a');
        key = new Key(matchingKey, bucketingKey);
        Assert.assertThat(client.getTreatment(key, "split", null),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.is(Treatments.CONTROL)));
    }

    @Test
    public void track_with_properties() {
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        HashMap<String, Object> properties = new HashMap<>();
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

        properties.put("ok_property", 123);
        properties.put("some_property", new Object());
        Assert.assertThat(client.track("key1", "user", "purchase", properties),
                org.hamcrest.Matchers.is(true));
        verify(eventClientMock).track(eventArgumentCaptor.capture(), Mockito.anyInt());
        Event captured = eventArgumentCaptor.getValue();
        Assert.assertThat(captured.properties.size(), org.hamcrest.Matchers.is(2));
        Assert.assertThat((Integer) captured.properties.get("ok_property"), org.hamcrest.Matchers.is(123));
        Assert.assertThat(captured.properties.get("some_property"), org.hamcrest.Matchers.nullValue());

        properties.clear();
        Mockito.reset(eventClientMock);
        Mockito.when(eventClientMock.track((Event) Mockito.any(), Mockito.anyInt())).thenReturn(true);
        properties.put("ok_property", 123);
        properties.put("some_property", Arrays.asList(1, 2, 3));
        Assert.assertThat(client.track("key1", "user", "purchase", properties),
                org.hamcrest.Matchers.is(true));
        eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventClientMock).track(eventArgumentCaptor.capture(), Mockito.anyInt());
        captured = eventArgumentCaptor.getValue();
        Assert.assertThat(captured.properties.size(), org.hamcrest.Matchers.is(2));
        Assert.assertThat((Integer) captured.properties.get("ok_property"), org.hamcrest.Matchers.is(123));
        Assert.assertThat(captured.properties.get("some_property"), org.hamcrest.Matchers.nullValue());

        properties.clear();
        Mockito.reset(eventClientMock);
        Mockito.when(eventClientMock.track((Event) Mockito.any(), Mockito.anyInt())).thenReturn(true);
        properties.put("ok_property", 123);
        properties.put("some_property", new HashMap<String, Number>());
        Assert.assertThat(client.track("key1", "user", "purchase", properties),
                org.hamcrest.Matchers.is(true));
        eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventClientMock).track(eventArgumentCaptor.capture(), Mockito.anyInt());
        captured = eventArgumentCaptor.getValue();
        Assert.assertThat(captured.properties.size(), org.hamcrest.Matchers.is(2));
        Assert.assertThat((Integer) captured.properties.get("ok_property"), org.hamcrest.Matchers.is(123));
        Assert.assertThat(captured.properties.get("some_property"), org.hamcrest.Matchers.nullValue());

        properties.clear();
        Mockito.reset(eventClientMock);
        Mockito.when(eventClientMock.track((Event) Mockito.any(), Mockito.anyInt())).thenReturn(true);
        properties.put("ok_property", 123);
        Assert.assertThat(client.track("key1", "user", "purchase", 123, properties),
                org.hamcrest.Matchers.is(true));
        eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventClientMock).track(eventArgumentCaptor.capture(), Mockito.anyInt());
        captured = eventArgumentCaptor.getValue();
        Assert.assertThat(captured.value, org.hamcrest.Matchers.is(123.0));
        Assert.assertThat(captured.trafficTypeName,org.hamcrest.Matchers.is("user"));
        Assert.assertThat(captured.eventTypeId,org.hamcrest.Matchers.is("purchase"));
        Assert.assertThat(captured.key,org.hamcrest.Matchers.is("key1"));
        Assert.assertThat(captured.properties.size(), org.hamcrest.Matchers.is(1));
        Assert.assertThat((Integer) captured.properties.get("ok_property"), org.hamcrest.Matchers.is(123));

        properties.clear();
        Mockito.reset(eventClientMock);
        Mockito.when(eventClientMock.track((Event) Mockito.any(), Mockito.anyInt())).thenReturn(true);
        properties.put("prop1", 1);
        properties.put("prop2", 2L);
        properties.put("prop3", 7.56);
        properties.put("prop4", "something");
        properties.put("prop5", true);
        properties.put("prop6", null);
        Assert.assertThat(client.track("key1", "user", "purchase", properties),
                org.hamcrest.Matchers.is(true));
        eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventClientMock).track(eventArgumentCaptor.capture(), Mockito.anyInt());
        captured = eventArgumentCaptor.getValue();
        Assert.assertThat(captured.properties.size(), org.hamcrest.Matchers.is(6));
        Assert.assertThat((Integer) captured.properties.get("prop1"), org.hamcrest.Matchers.is(1));
        Assert.assertThat((Long) captured.properties.get("prop2"), org.hamcrest.Matchers.is(2L));
        Assert.assertThat((Double) captured.properties.get("prop3"), org.hamcrest.Matchers.is(7.56));
        Assert.assertThat((String) captured.properties.get("prop4"), org.hamcrest.Matchers.is("something"));
        Assert.assertThat((Boolean) captured.properties.get("prop5"), org.hamcrest.Matchers.is(true));
        Assert.assertThat(captured.properties.get("prop6"), org.hamcrest.Matchers.nullValue());

        // 110 props of 300 bytes should be enough to make the event fail.
        properties.clear();
        for (int i = 0; i < 110; ++i) {
            properties.put(new String(new char[300]).replace('\0', 'a') + i ,
                    new String(new char[300]).replace('\0', 'a') + i);
        }
        Assert.assertThat(client.track("key1", "user", "purchase", properties), org.hamcrest.Matchers.is(false));
    }

    @Test
    public void client_cannot_perform_actions_when_destroyed() throws InterruptedException, URISyntaxException, TimeoutException, IOException {
        String test = "split";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        Assert.assertThat(client.getTreatment("valid", "split"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.not(Treatments.CONTROL)));

        Assert.assertThat(client.track("validKey", "valid_traffic_type", "valid_event"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(true)));

        client.destroy();

        Assert.assertThat(client.getTreatment("valid", "split"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(Treatments.CONTROL)));

        Assert.assertThat(client.track("validKey", "valid_traffic_type", "valid_event"),
                org.hamcrest.Matchers.is(org.hamcrest.Matchers.equalTo(false)));
    }

    @Test
    public void worksAndHasConfigTryKetTreatmentWithKey() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        // Add config for only one treatment
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.ON, "{\"size\" : 30}");

        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, configurations);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        int numKeys = 5;
        for (int i = 0; i < numKeys; i++) {
            Map<String, Object> attributes = new HashMap<>();
            String randomKey = RandomStringUtils.random(10);
            Key key = new Key(randomKey, "BucketingKey");
            assertThat(client.getTreatment(randomKey, test), is(equalTo("on")));
            assertThat(client.getTreatmentWithConfig(key, test, attributes).config(), is(equalTo(configurations.get("on"))));
        }

        // Times 2 because we are calling getTreatment twice. Once for getTreatment and one for getTreatmentWithConfig
        verify(splitCacheConsumer, times(numKeys * 2)).get(test);
    }

    @Test(expected = IllegalArgumentException.class)
    public void blockUntilReadyException() throws TimeoutException, InterruptedException {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        client.blockUntilReady();
    }

    @Test
    public void null_key_results_in_control_getTreatments() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );


        assertEquals(Treatments.CONTROL, client.getTreatments(null, Collections.singletonList("test1")).get("test1"));

        verifyZeroInteractions(splitCacheConsumer);
    }

    @Test
    public void null_splits_results_in_null_getTreatments() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );


        assertNull(client.getTreatments("key", null));

        verifyZeroInteractions(splitCacheConsumer);
    }

    @Test
    public void exceptions_result_in_control_getTreatments() {
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );
        Map<String, String> result = client.getTreatments("adil@relateiq.com", Arrays.asList("test1", "test2"));
        assertEquals(2, result.values().size());
        assertEquals(Treatments.CONTROL, result.get("test1"));
        assertEquals(Treatments.CONTROL, result.get("test2"));

        verify(splitCacheConsumer).fetchMany(anyList());
    }



    @Test
    public void getTreatments_works() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );
        Map<String, String> result = client.getTreatments("randomKey", Arrays.asList(test, "test2"));
        assertEquals("on", result.get(test));
        assertEquals(Treatments.CONTROL, result.get("test2"));

        verify(TELEMETRY_STORAGE, times(1)).recordLatency(Mockito.anyObject(), Mockito.anyLong());
    }

    @Test
    public void empty_splits_results_in_null_getTreatments() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );


        Map<String, String> result = client.getTreatments("key", new ArrayList<>());
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyZeroInteractions(splitCacheConsumer);
    }

    @Test
    public void exceptions_result_in_control_treatments() {
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );
        Map<String, String> result = client.getTreatments("adil@relateiq.com", Arrays.asList("test1"));
        assertEquals(1, result.size());
        assertEquals(Treatments.CONTROL, result.get("test1"));
    }

    @Test
    public void works_treatments() {
        String test = "test1";
        String test2 = "test2";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);
        ParsedSplit parsedSplit2 = ParsedSplit.createParsedSplitForTests(test2, 123, false, Treatments.OFF, conditions, null, 1, 1);
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
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
    public void works_one_control_treatments() {
        String test = "test1";
        String test2 = "test2";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
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
    public void treatments_worksAndHasConfig() {
        String test = "test1";
        String test2 = "test2";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);

        // Add config for only one treatment
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.ON, "{\"size\" : 30}");
        configurations.put(Treatments.CONTROL, "{\"size\" : 30}");


        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, configurations);
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
                new EvaluatorImp(splitCacheConsumer, segmentCacheConsumer), TELEMETRY_STORAGE, TELEMETRY_STORAGE
        );

        int numKeys = 5;
        Map<String, Object> attributes = new HashMap<>();
        Map<String, SplitResult> result = client.getTreatmentsWithConfig("randomKey", Arrays.asList(test, test2), attributes);
        assertEquals(configurations.get("on"), result.get(test).config());
        assertNull(result.get(test2).config());
        assertEquals("control", result.get(test2).treatment());


        verify(splitCacheConsumer, times(1)).fetchMany(anyList());
    }

}
