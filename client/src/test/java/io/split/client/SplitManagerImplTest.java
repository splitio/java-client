package io.split.client;

import com.google.common.collect.Lists;
import io.split.client.api.SplitView;
import io.split.engine.ConditionsTestUtil;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.matchers.AllKeysMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.grammar.Treatments;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SplitManagerImplTest {

    private SplitClientConfig config = SplitClientConfig.builder().setBlockUntilReadyTimeout(100).build();

    @Test
    public void splitCallWithNonExistentSplit() {
        String nonExistent = "nonExistent";
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        Mockito.when(splitFetcher.fetch(nonExistent)).thenReturn(null);

        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher,
                Mockito.mock(SplitClientConfig.class),
                Mockito.mock(SDKReadinessGates.class));
        assertThat(splitManager.split("nonExistent"), is(nullValue()));
    }

    @Test
    public void splitCallWithExistentSplit() {
        String existent = "existent";
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);

        ParsedSplit response = ParsedSplit.createParsedSplitForTests("FeatureName", 123, true, "off", Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1);
        Mockito.when(splitFetcher.fetch(existent)).thenReturn(response);

        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher,
                Mockito.mock(SplitClientConfig.class),
                Mockito.mock(SDKReadinessGates.class));
        SplitView theOne = splitManager.split(existent);
        assertThat(theOne.name, is(equalTo(response.feature())));
        assertThat(theOne.changeNumber, is(equalTo(response.changeNumber())));
        assertThat(theOne.killed, is(equalTo(response.killed())));
        assertThat(theOne.trafficType, is(equalTo(response.trafficTypeName())));
        assertThat(theOne.treatments.size(), is(equalTo(1)));
        assertThat(theOne.treatments.get(0), is(equalTo("off")));
        assertThat(theOne.configs.size(), is(0));
    }

    @Test
    public void splitCallWithExistentSplitAndConfigs() {
        String existent = "existent";
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);

        // Add config for only one treatment(default)
        Map<String, String> configurations = new HashMap<>();
        configurations.put(Treatments.OFF, "{\"size\" : 30}");

        ParsedSplit response = ParsedSplit.createParsedSplitForTests("FeatureName", 123, true, "off", Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1, configurations);
        Mockito.when(splitFetcher.fetch(existent)).thenReturn(response);

        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher,
                Mockito.mock(SplitClientConfig.class),
                Mockito.mock(SDKReadinessGates.class));
        SplitView theOne = splitManager.split(existent);
        assertThat(theOne.name, is(equalTo(response.feature())));
        assertThat(theOne.changeNumber, is(equalTo(response.changeNumber())));
        assertThat(theOne.killed, is(equalTo(response.killed())));
        assertThat(theOne.trafficType, is(equalTo(response.trafficTypeName())));
        assertThat(theOne.treatments.size(), is(equalTo(1)));
        assertThat(theOne.treatments.get(0), is(equalTo("off")));
        assertThat(theOne.configs.get("off"), is(equalTo("{\"size\" : 30}")));
    }

    @Test
    public void splitsCallWithNoSplit() {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        Mockito.when(splitFetcher.fetchAll()).thenReturn(Lists.<ParsedSplit>newArrayList());
        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher,
                Mockito.mock(SplitClientConfig.class),
                Mockito.mock(SDKReadinessGates.class));
        assertThat(splitManager.splits(), is(empty()));
    }

    @Test
    public void splitsCallWithSplit() {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        List<ParsedSplit> parsedSplits = Lists.newArrayList();
        ParsedSplit response = ParsedSplit.createParsedSplitForTests("FeatureName", 123, true, "off", Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1);
        parsedSplits.add(response);

        Mockito.when(splitFetcher.fetchAll()).thenReturn(parsedSplits);
        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher,
                Mockito.mock(SplitClientConfig.class),
                Mockito.mock(SDKReadinessGates.class));
        List<SplitView> splits = splitManager.splits();
        assertThat(splits.size(), is(equalTo(1)));
        assertThat(splits.get(0).name, is(equalTo(response.feature())));
        assertThat(splits.get(0).changeNumber, is(equalTo(response.changeNumber())));
        assertThat(splits.get(0).killed, is(equalTo(response.killed())));
        assertThat(splits.get(0).trafficType, is(equalTo(response.trafficTypeName())));
        assertThat(splits.get(0).treatments.size(), is(equalTo(1)));
        assertThat(splits.get(0).treatments.get(0), is(equalTo("off")));
    }

    @Test
    public void splitNamesCallWithNoSplit() {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        Mockito.when(splitFetcher.fetchAll()).thenReturn(Lists.<ParsedSplit>newArrayList());
        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher,
                Mockito.mock(SplitClientConfig.class),
                Mockito.mock(SDKReadinessGates.class));
        assertThat(splitManager.splitNames(), is(empty()));
    }

    @Test
    public void splitNamesCallWithSplit() {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        List<ParsedSplit> parsedSplits = Lists.newArrayList();
        ParsedSplit response = ParsedSplit.createParsedSplitForTests("FeatureName", 123, true, "off", Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1);
        parsedSplits.add(response);

        Mockito.when(splitFetcher.fetchAll()).thenReturn(parsedSplits);
        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher,
                Mockito.mock(SplitClientConfig.class),
                Mockito.mock(SDKReadinessGates.class));
        List<String> splitNames = splitManager.splitNames();
        assertThat(splitNames.size(), is(equalTo(1)));
        assertThat(splitNames.get(0), is(equalTo(response.feature())));
    }

    @Test
    public void block_until_ready_does_not_time_when_sdk_is_ready() throws TimeoutException, InterruptedException {
        SDKReadinessGates ready = mock(SDKReadinessGates.class);
        when(ready.isSDKReady(100)).thenReturn(true);
        SplitManagerImpl splitManager = new SplitManagerImpl(mock(SplitFetcher.class),
                config,
                ready);

        splitManager.blockUntilReady();
    }

    @Test(expected = TimeoutException.class)
    public void block_until_ready_times_when_sdk_is_not_ready() throws TimeoutException, InterruptedException {
        SDKReadinessGates ready = mock(SDKReadinessGates.class);
        when(ready.isSDKReady(100)).thenReturn(false);

        SplitManagerImpl splitManager = new SplitManagerImpl(mock(SplitFetcher.class),
                config,
                ready);

        splitManager.blockUntilReady();
    }

    private ParsedCondition getTestCondition(String treatment) {
        return ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(ConditionsTestUtil.partition(treatment, 10)));
    }

}
