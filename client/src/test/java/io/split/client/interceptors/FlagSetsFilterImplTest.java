package io.split.client.interceptors;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

public class FlagSetsFilterImplTest {

    @Test
    public void testIntersectSetsWithShouldFilter() {
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(new HashSet<>(Arrays.asList("a", "b")));
        Assert.assertTrue(flagSetsFilter.Intersect("a"));
        Assert.assertTrue(flagSetsFilter.Intersect(new HashSet<>(Arrays.asList("a", "c"))));
        Assert.assertFalse(flagSetsFilter.Intersect("c"));
        Assert.assertFalse(flagSetsFilter.Intersect(new HashSet<>(Arrays.asList("d", "c"))));
    }

    @Test
    public void testIntersectSetsWithShouldNotFilter() {
        FlagSetsFilter flagSetsFilter = new FlagSetsFilterImpl(new HashSet<>());
        Assert.assertTrue(flagSetsFilter.Intersect("a"));
        Assert.assertTrue(flagSetsFilter.Intersect(new HashSet<>(Arrays.asList("a", "c"))));
    }
}