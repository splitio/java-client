package io.split.storages.pluggable.adapters;

import com.google.common.collect.Lists;
import io.split.client.dtos.Partition;
import io.split.client.utils.Json;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.matchers.AllKeysMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.grammar.Treatments;
import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.domain.PrefixAdapter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserCustomSplitAdapterConsumerTest {

    private static final String SPLIT_NAME = "SplitName";
    private CustomStorageWrapper _customStorageWrapper;

    @Before
    public void setUp() {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
    }

    @Test
    public void testGetChangeNumber() {
        UserCustomSplitAdapterConsumer userCustomSplitAdapterConsumer = new UserCustomSplitAdapterConsumer(_customStorageWrapper);
        Mockito.when(_customStorageWrapper.get(PrefixAdapter.buildSplitChangeNumber())).thenReturn(getLongAsJson(120l));
        Assert.assertEquals(120l, userCustomSplitAdapterConsumer.getChangeNumber());
    }

    @Test
    public void testGetSplit() {
        UserCustomSplitAdapterConsumer userCustomSplitAdapterConsumer = new UserCustomSplitAdapterConsumer(_customStorageWrapper);
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(SPLIT_NAME, 123, false, Treatments.OFF, conditions, null, 1, 1);
        Mockito.when(_customStorageWrapper.get(PrefixAdapter.buildSplitKey(SPLIT_NAME))).thenReturn(getSplitsAsJson(parsedSplit));
        ParsedSplit result = userCustomSplitAdapterConsumer.get(SPLIT_NAME);
        Assert.assertEquals(parsedSplit, result);
    }

    private String getLongAsJson(long value) {
        System.out.println(Json.toJson(value));
        return Json.toJson(value);
    }

    private String getSplitsAsJson(ParsedSplit parsedSplits) {
        return Json.toJson(parsedSplits);
    }

    private Partition partition(String treatment, int size) {
        Partition p = new Partition();
        p.treatment = treatment;
        p.size = size;
        return p;
    }

}