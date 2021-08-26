package io.split.storages.pluggable.domain;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrefixAdapterTest{

    private static final String SPLIT_NAME = "SplitName";
    private static final String TRAFFIC_TYPE = "TrafficType";
    private static final String DEFAULT_PREFIX = "SPLITIO.";
    private static final String SPLIT_PREFIX = "split.";
    private static final String SPLITS_PREFIX = "splits.";
    private static final String TRAFFIC_TYPE_PREFIX = "trafficType.";
    private static final String EVENTS = "events";
    private static final String IMPRESSIONS = "impressions";
    private static final String SEGMENT =  "segment." ;

    @Test
    public void testBuildSplitKey() {
        String prefix = PrefixAdapter.buildSplitKey(SPLIT_NAME);
        String expectedPrefix = String.format(DEFAULT_PREFIX+SPLIT_PREFIX+"{%s}", SPLIT_NAME);
        Assert.assertEquals(expectedPrefix,prefix);
    }

    @Test
    public void testBuildSplitGetChangeNumber() {
        String prefix = PrefixAdapter.buildSplitChangeNumber();
        String expectedPrefix = DEFAULT_PREFIX+ SPLITS_PREFIX +"till";
        Assert.assertEquals(expectedPrefix,prefix);
    }

    @Test
    public void testBuildGetAllSplit() {
        String prefix = PrefixAdapter.buildGetAllSplit();
        String expectedPrefix = DEFAULT_PREFIX+ "splits.*";
        Assert.assertEquals(expectedPrefix,prefix);
    }

    @Test
    public void testBuildTrafficTypeExists() {
        String prefix = PrefixAdapter.buildTrafficTypeExists(TRAFFIC_TYPE);
        String expectedPrefix = String.format(DEFAULT_PREFIX+TRAFFIC_TYPE_PREFIX+"{%s}", TRAFFIC_TYPE);
        Assert.assertEquals(expectedPrefix,prefix);
    }

    @Test
    public void testBuildFetchManySplits() {
        List<String> prefixes = PrefixAdapter.buildFetchManySplits(Stream.of(SPLIT_NAME+"1", SPLIT_NAME+"2").collect(Collectors.toList()));
        String expectedPrefix1 = String.format(DEFAULT_PREFIX+ SPLIT_PREFIX +"{%s}", SPLIT_NAME+"1");
        String expectedPrefix2 = String.format(DEFAULT_PREFIX+ SPLIT_PREFIX +"{%s}", SPLIT_NAME+"2");
        Assert.assertEquals(2,prefixes.size());
        Assert.assertEquals(expectedPrefix1,prefixes.get(0));
        Assert.assertEquals(expectedPrefix2,prefixes.get(1));

    }

    @Test
    public void testBuildEvents() {
        String expectedPrefix = DEFAULT_PREFIX + EVENTS;
        Assert.assertEquals(expectedPrefix, PrefixAdapter.buildEvent());
    }

    @Test
    public void testBuildImpressions() {
        String expectedPrefix = DEFAULT_PREFIX + IMPRESSIONS;
        Assert.assertEquals(expectedPrefix, PrefixAdapter.buildImpressions());
    }

    @Test
    public void testBuildSegments() {
        String expectedPrefix = DEFAULT_PREFIX + SEGMENT + "*";
        Assert.assertEquals(expectedPrefix, PrefixAdapter.buildSegmentAll());
    }

}