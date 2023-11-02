package io.split.client.interceptors;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

public class FlagSetsFilterImplTest {

    @Test
    public void testIntersectSetsWithShouldFilter() {
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(new HashSet<>(Arrays.asList("a", "b")));
        Assert.assertTrue(flagSetsFilter.intersect("a"));
        Assert.assertTrue(flagSetsFilter.intersect(new HashSet<>(Arrays.asList("a", "c"))));
        Assert.assertFalse(flagSetsFilter.intersect("c"));
        Assert.assertFalse(flagSetsFilter.intersect(new HashSet<>(Arrays.asList("d", "c"))));
    }

    @Test
    public void testIntersectSetsWithShouldNotFilter() {
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(new HashSet<>());
        Assert.assertTrue(flagSetsFilter.intersect("a"));
        Assert.assertTrue(flagSetsFilter.intersect(new HashSet<>(Arrays.asList("a", "c"))));
    }
}