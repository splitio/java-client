package io.split.client;

import com.google.common.collect.Lists;
import io.split.client.api.SplitView;
import io.split.engine.ConditionsTestUtil;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.matchers.AllKeysMatcher;
import io.split.engine.matchers.CombiningMatcher;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class SplitManagerImplTest {

    @Test
    public void splitCallWithNonExistentSplit() {
        String nonExistent = "nonExistent";
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        Mockito.when(splitFetcher.fetch(nonExistent)).thenReturn(null);
        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher);
        assertThat(splitManager.split("nonExistent"), is(nullValue()));
    }

    @Test
    public void splitCallWithExistentSplit() {
        String existent = "existent";
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);

        ParsedSplit response = new ParsedSplit("FeatureName", 123, true, "off", Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1);
        Mockito.when(splitFetcher.fetch(existent)).thenReturn(response);

        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher);
        SplitView theOne = splitManager.split(existent);
        assertThat(theOne.name, is(equalTo(response.feature())));
        assertThat(theOne.changeNumber, is(equalTo(response.changeNumber())));
        assertThat(theOne.killed, is(equalTo(response.killed())));
        assertThat(theOne.trafficType, is(equalTo(response.trafficTypeName())));
        assertThat(theOne.treatments.size(), is(equalTo(1)));
        assertThat(theOne.treatments.get(0), is(equalTo("off")));
    }

    @Test
    public void splitsCallWithNoSplit() {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        Mockito.when(splitFetcher.fetchAll()).thenReturn(Lists.<ParsedSplit>newArrayList());
        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher);
        assertThat(splitManager.splits(), is(empty()));
    }

    @Test
    public void splitsCallWithSplit() {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        List<ParsedSplit> parsedSplits = Lists.newArrayList();
        ParsedSplit response = new ParsedSplit("FeatureName", 123, true, "off", Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1);
        parsedSplits.add(response);

        Mockito.when(splitFetcher.fetchAll()).thenReturn(parsedSplits);
        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher);
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
        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher);
        assertThat(splitManager.splitNames(), is(empty()));
    }

    @Test
    public void splitNamesCallWithSplit() {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        List<ParsedSplit> parsedSplits = Lists.newArrayList();
        ParsedSplit response = new ParsedSplit("FeatureName", 123, true, "off", Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1);
        parsedSplits.add(response);

        Mockito.when(splitFetcher.fetchAll()).thenReturn(parsedSplits);
        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher);
        List<String> splitNames = splitManager.splitNames();
        assertThat(splitNames.size(), is(equalTo(1)));
        assertThat(splitNames.get(0), is(equalTo(response.feature())));
    }

    private ParsedCondition getTestCondition(String treatment) {
        return new ParsedCondition(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(ConditionsTestUtil.partition(treatment, 10)));
    }

}
