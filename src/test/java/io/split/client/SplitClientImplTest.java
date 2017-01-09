package io.split.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.split.client.api.Key;
import io.split.client.dtos.DataType;
import io.split.client.dtos.Partition;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.impressions.TreatmentLog;
import io.split.engine.matchers.AllKeysMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.matchers.EqualToMatcher;
import io.split.engine.matchers.GreaterThanOrEqualToMatcher;
import io.split.engine.matchers.WhitelistMatcher;
import io.split.engine.metrics.Metrics;
import io.split.grammar.Treatments;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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

    @Test
    public void null_key_results_in_control() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = new ParsedCondition(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, null, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(splitFetcher, new TreatmentLog.NoopTreatmentLog(),
                new Metrics.NoopMetrics());

        assertThat(client.getTreatment(null, "test1"), is(equalTo(Treatments.CONTROL)));

        verifyZeroInteractions(splitFetcher);
    }

    @Test
    public void null_test_results_in_control() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = new ParsedCondition(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, null, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(splitFetcher, new TreatmentLog.NoopTreatmentLog(),
                new Metrics.NoopMetrics());

        assertThat(client.getTreatment("adil@relateiq.com", null), is(equalTo(Treatments.CONTROL)));

        verifyZeroInteractions(splitFetcher);
    }

    @Test
    public void exceptions_result_in_control() {
        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(anyString())).thenThrow(RuntimeException.class);

        SplitClientImpl client = new SplitClientImpl(splitFetcher, new TreatmentLog.NoopTreatmentLog(),
                new Metrics.NoopMetrics());
        assertThat(client.getTreatment("adil@relateiq.com", "test1"), is(equalTo(Treatments.CONTROL)));

        verify(splitFetcher).fetch("test1");
    }

    @Test
    public void works() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = new ParsedCondition(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, null, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(splitFetcher,
                new TreatmentLog.NoopTreatmentLog(),
                new Metrics.NoopMetrics());

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

        ParsedCondition rollOutToEveryone = new ParsedCondition(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, "user", 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(splitFetcher,
                new TreatmentLog.NoopTreatmentLog(),
                new Metrics.NoopMetrics());

        assertThat(client.getTreatment("pato@codigo.com", test), is(equalTo(Treatments.OFF)));

        verify(splitFetcher).fetch(test);
    }

    @Test
    public void multiple_conditions_work() {
        String test = "test1";

        ParsedCondition adil_is_always_on = new ParsedCondition(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        ParsedCondition pato_is_never_shown = new ParsedCondition(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("pato@codigo.com"))), Lists.newArrayList(partition("off", 100)));
        ParsedCondition trevor_is_always_shown = new ParsedCondition(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("trevor@codigo.com"))), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(adil_is_always_on, pato_is_never_shown, trevor_is_always_shown);
        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, null, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(splitFetcher,
                new TreatmentLog.NoopTreatmentLog()
                , new Metrics.NoopMetrics());

        assertThat(client.getTreatment("adil@codigo.com", test), is(equalTo("on")));
        assertThat(client.getTreatment("pato@codigo.com", test), is(equalTo("off")));
        assertThat(client.getTreatment("trevor@codigo.com", test), is(equalTo("on")));

        verify(splitFetcher, times(3)).fetch(test);
    }


    @Test
    public void killed_test_always_goes_to_default() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = new ParsedCondition(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = new ParsedSplit(test, 123, true, Treatments.OFF, conditions, "user", 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(splitFetcher,
                new TreatmentLog.NoopTreatmentLog(),
                new Metrics.NoopMetrics());

        assertThat(client.getTreatment("adil@codigo.com", test), is(equalTo(Treatments.OFF)));

        verify(splitFetcher).fetch(test);
    }

    @Test
    public void attributes_work() {
        String test = "test1";

        ParsedCondition adil_is_always_on = new ParsedCondition(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        ParsedCondition users_with_age_greater_than_10_are_on = new ParsedCondition(CombiningMatcher.of("age", new GreaterThanOrEqualToMatcher(10, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(adil_is_always_on, users_with_age_greater_than_10_are_on);
        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, null, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(splitFetcher,
                new TreatmentLog.NoopTreatmentLog()
                , new Metrics.NoopMetrics());

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

        ParsedCondition age_equal_to_0_should_be_on = new ParsedCondition(CombiningMatcher.of("age", new EqualToMatcher(0, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, "user", 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(splitFetcher,
                new TreatmentLog.NoopTreatmentLog()
                , new Metrics.NoopMetrics());

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

        ParsedCondition age_equal_to_0_should_be_on = new ParsedCondition(CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, null, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(splitFetcher,
                new TreatmentLog.NoopTreatmentLog()
                , new Metrics.NoopMetrics());

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
    public void labels_are_populated() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = new ParsedCondition(
                CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)),
                Lists.newArrayList(partition("on", 100)),
                "foolabel"
        );

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, null, 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        TreatmentLog treatmentLog = mock(TreatmentLog.class);


        SplitClientImpl client = new SplitClientImpl(
                splitFetcher,
                treatmentLog,
                new Metrics.NoopMetrics()
        );

        assertThat(client.getTreatment("pato@codigo.com", test, ImmutableMap.<String, Object>of("age", -20)), is(equalTo("on")));

        ArgumentCaptor<String> labelCaptor = ArgumentCaptor.forClass(String.class);

        verify(treatmentLog).log(eq("pato@codigo.com"),
                eq("pato@codigo.com"),
                eq(test),
                eq("on"),
                anyLong(),
                labelCaptor.capture(),
                eq(new Long(1L))
        );

        assertThat(labelCaptor.getValue(), is(equalTo("foolabel")));
    }

    @Test
    public void matching_bucketing_keys_work() {
        String test = "test1";


        Set<String> whitelist = new HashSet<>();
        whitelist.add("aijaz");
        ParsedCondition aijaz_should_match = new ParsedCondition(CombiningMatcher.of(new WhitelistMatcher(whitelist)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(aijaz_should_match);
        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, "user", 1);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = new SplitClientImpl(splitFetcher,
                new TreatmentLog.NoopTreatmentLog()
                , new Metrics.NoopMetrics());

        Key bad_key = new Key("adil", "aijaz");
        Key good_key = new Key("aijaz", "adil");

        assertThat(client.getTreatment(bad_key, test, Collections.<String, Object>emptyMap()), is(equalTo("off")));
        assertThat(client.getTreatment(good_key, test, Collections.<String, Object>emptyMap()), is(equalTo("on")));

        verify(splitFetcher, times(2)).fetch(test);
    }

    private Partition partition(String treatment, int size) {
        Partition p = new Partition();
        p.treatment = treatment;
        p.size = size;
        return p;
    }


}
