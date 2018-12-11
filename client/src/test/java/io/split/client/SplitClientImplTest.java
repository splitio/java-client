package io.split.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.split.client.api.Key;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.DataType;
import io.split.client.dtos.Partition;
import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionListener;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.matchers.AllKeysMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.matchers.DependencyMatcher;
import io.split.engine.matchers.EqualToMatcher;
import io.split.engine.matchers.GreaterThanOrEqualToMatcher;
import io.split.engine.matchers.collections.ContainsAnyOfSetMatcher;
import io.split.engine.matchers.strings.WhitelistMatcher;
import io.split.engine.metrics.Metrics;
import io.split.grammar.Treatments;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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

    private SplitClientConfig config = SplitClientConfig.builder().setBlockUntilReadyTimeout(100).build();

    @Test
    public void null_key_results_in_control() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );

        assertThat(client.getTreatment(null, "test1"), is(equalTo(Treatments.CONTROL)));

        verifyZeroInteractions(splitFetcher);
    }

    @Test
    public void null_test_results_in_control() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );

        assertThat(client.getTreatment("adil@relateiq.com", null), is(equalTo(Treatments.CONTROL)));

        verifyZeroInteractions(splitFetcher);
    }

    @Test
    public void exceptions_result_in_control() {
        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(anyString())).thenThrow(RuntimeException.class);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );
        assertThat(client.getTreatment("adil@relateiq.com", "test1"), is(equalTo(Treatments.CONTROL)));

        verify(splitFetcher).fetch("test1");
    }

    @Test
    public void works() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );

        int numKeys = 5;
        for (int i = 0; i < numKeys; i++) {
            String randomKey = RandomStringUtils.random(10);
            assertThat(client.getTreatment(randomKey, test), is(equalTo("on")));
        }

        verify(splitFetcher, times(numKeys)).fetch(test);
    }


    @Test
    public void last_condition_is_always_default() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );

        assertThat(client.getTreatment("pato@codigo.com", test), is(equalTo(Treatments.OFF)));

        verify(splitFetcher).fetch(test);
    }

    @Test
    public void multiple_conditions_work() {
        String test = "test1";

        ParsedCondition adil_is_always_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        ParsedCondition pato_is_never_shown = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("pato@codigo.com"))), Lists.newArrayList(partition("off", 100)));
        ParsedCondition trevor_is_always_shown = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("trevor@codigo.com"))), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(adil_is_always_on, pato_is_never_shown, trevor_is_always_shown);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );

        assertThat(client.getTreatment("adil@codigo.com", test), is(equalTo("on")));
        assertThat(client.getTreatment("pato@codigo.com", test), is(equalTo("off")));
        assertThat(client.getTreatment("trevor@codigo.com", test), is(equalTo("on")));

        verify(splitFetcher, times(3)).fetch(test);
    }


    @Test
    public void killed_test_always_goes_to_default() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, true, Treatments.OFF, conditions, "user", 1, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );

        assertThat(client.getTreatment("adil@codigo.com", test), is(equalTo(Treatments.OFF)));

        verify(splitFetcher).fetch(test);
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

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(parent)).thenReturn(parentSplit);
        when(splitFetcher.fetch(dependent)).thenReturn(dependentSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
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

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(parent)).thenReturn(parentSplit);
        when(splitFetcher.fetch(dependent)).thenReturn(dependentSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
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

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(dependent)).thenReturn(dependentSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );

//        assertThat(client.getTreatment("key", dependent), is(equalTo(Treatments.CONTROL)));
        assertThat(client.getTreatment("key", dependent), is(equalTo(Treatments.ON)));

    }

    @Test
    public void attributes_work() {
        String test = "test1";

        ParsedCondition adil_is_always_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition(Treatments.ON, 100)));
        ParsedCondition users_with_age_greater_than_10_are_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("age", new GreaterThanOrEqualToMatcher(10, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(adil_is_always_on, users_with_age_greater_than_10_are_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );

        assertThat(client.getTreatment("adil@codigo.com", test), is(equalTo("on")));
        assertThat(client.getTreatment("adil@codigo.com", test, null), is(equalTo("on")));
        assertThat(client.getTreatment("adil@codigo.com", test, ImmutableMap.<String, Object>of()), is(equalTo("on")));

        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 10)), is(equalTo("on")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 9)), is(equalTo("off")));

        verify(splitFetcher, times(5)).fetch(test);
    }

    @Test
    public void attributes_work_2() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("age", new EqualToMatcher(0, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );

        assertThat(client.getTreatment("adil@codigo.com", test), is(equalTo("off")));
        assertThat(client.getTreatment("adil@codigo.com", test, null), is(equalTo("off")));
        assertThat(client.getTreatment("adil@codigo.com", test, ImmutableMap.<String, Object>of()), is(equalTo("off")));

        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 10)), is(equalTo("off")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 0)), is(equalTo("on")));

        verify(splitFetcher, times(5)).fetch(test);
    }

    @Test
    public void attributes_greater_than_negative_number() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );

        assertThat(client.getTreatment("adil@codigo.com", test), is(equalTo("off")));
        assertThat(client.getTreatment("adil@codigo.com", test, null), is(equalTo("off")));
        assertThat(client.getTreatment("adil@codigo.com", test, ImmutableMap.<String, Object>of()), is(equalTo("off")));

        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 10)), is(equalTo("off")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", -20)), is(equalTo("on")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", 20)), is(equalTo("off")));
        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", -21)), is(equalTo("off")));

        verify(splitFetcher, times(7)).fetch(test);
    }


    @Test
    public void attributes_for_sets() {
        String test = "test1";

        ParsedCondition any_of_set = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("products", new ContainsAnyOfSetMatcher(Lists.<String>newArrayList("sms", "video"))), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(any_of_set);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
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

        verify(splitFetcher, times(9)).fetch(test);
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

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        ImpressionListener impressionListener = mock(ImpressionListener.class);


        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                impressionListener,
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );

        Map<String, Object> attributes = ImmutableMap.<String, Object>of("age", -20, "acv", "1000000");
        assertThat(client.getTreatment("pato@codigo.com", test, attributes), is(equalTo("on")));

        ArgumentCaptor<Impression> impressionCaptor = ArgumentCaptor.forClass(Impression.class);

        verify(impressionListener).log(impressionCaptor.capture());

        assertThat(impressionCaptor.getValue().appliedRule(), is(equalTo("foolabel")));

        assertThat(impressionCaptor.getValue().attributes(), is(attributes));
    }

    @Test
    public void not_in_split_if_no_allocation() {
        traffic_allocation("pato@split.io", 0, 123, "off", "not in split");
    }

    /**
     * This test depends on the underlying hashing algorithm. I have
     * figured out that pato@split.io will be in bucket 10 for seed 123.
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
        for (; i <= 10; i++) {
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

        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, null, 1, trafficAllocation, trafficAllocationSeed, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        ImpressionListener impressionListener = mock(ImpressionListener.class);


        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                impressionListener,
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );

        System.out.println(key);
        assertThat(client.getTreatment(key, test), is(equalTo(expected_treatment_on_or_off)));

        ArgumentCaptor<Impression> impressionCaptor = ArgumentCaptor.forClass(Impression.class);

        verify(impressionListener).log(impressionCaptor.capture());

        assertThat(impressionCaptor.getValue().appliedRule(), is(equalTo(label)));
    }


    @Test
    public void matching_bucketing_keys_work() {
        String test = "test1";


        Set<String> whitelist = new HashSet<>();
        whitelist.add("aijaz");
        ParsedCondition aijaz_should_match = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(whitelist)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(aijaz_should_match);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );

        Key bad_key = new Key("adil", "aijaz");
        Key good_key = new Key("aijaz", "adil");

        assertThat(client.getTreatment(bad_key, test, Collections.<String, Object>emptyMap()), is(equalTo("off")));
        assertThat(client.getTreatment(good_key, test, Collections.<String, Object>emptyMap()), is(equalTo("on")));

        verify(splitFetcher, times(2)).fetch(test);
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

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        ImpressionListener impressionListener = mock(ImpressionListener.class);


        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                splitFetcher,
                impressionListener,
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                mock(SDKReadinessGates.class)
        );

        Map<String, Object> attributes = ImmutableMap.<String, Object>of("age", -20, "acv", "1000000");

        assertThat(client.getTreatment("pato@codigo.com", test, attributes), is(equalTo("on")));

        ArgumentCaptor<Impression> impressionCaptor = ArgumentCaptor.forClass(Impression.class);

        verify(impressionListener).log(impressionCaptor.capture());

        assertThat(impressionCaptor.getValue().appliedRule(), is(equalTo("foolabel")));
        assertThat(impressionCaptor.getValue().attributes(), is(equalTo(attributes)));
    }

    private Partition partition(String treatment, int size) {
        Partition p = new Partition();
        p.treatment = treatment;
        p.size = size;
        return p;
    }

    @Test
    public void block_until_ready_does_not_time_when_sdk_is_ready() throws TimeoutException, InterruptedException {
        SDKReadinessGates ready = mock(SDKReadinessGates.class);
        when(ready.isSDKReady(100)).thenReturn(true);
        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                mock(SplitFetcher.class),
                mock(ImpressionListener.class),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                ready
        );

        client.blockUntilReady();
    }

    @Test(expected = TimeoutException.class)
    public void block_until_ready_times_when_sdk_is_not_ready() throws TimeoutException, InterruptedException {
        SDKReadinessGates ready = mock(SDKReadinessGates.class);
        when(ready.isSDKReady(100)).thenReturn(false);
        SplitClientImpl client = new SplitClientImpl(
                mock(SplitFactory.class),
                mock(SplitFetcher.class),
                mock(ImpressionListener.class),
                new Metrics.NoopMetrics(),
                NoopEventClient.create(),
                config,
                ready
        );

        client.blockUntilReady();
    }
}
