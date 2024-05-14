package io.split.engine.matchers.collections;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by adilaijaz on 4/18/17.
 */
public class ContainsAllOfSetMatcherTest {
    @Test
    public void worksForSets() {
        Set<String> set = new HashSet<>();
        set.add("first");
        set.add("second");

        ContainsAllOfSetMatcher matcher = new ContainsAllOfSetMatcher(set);

        Assert.assertFalse(matcher.match(null, null, null, null));

        Set<String> argument = new HashSet<>();
        Assert.assertFalse(matcher.match(argument, null, null, null));

        argument.add("second");
        Assert.assertFalse(matcher.match(argument, null, null, null));

        argument.add("first");
        Assert.assertTrue(matcher.match(argument, null, null, null));

        argument.add("third");
        Assert.assertTrue(matcher.match(argument, null, null, null));
    }

    @Test
    public void worksForLists() {
        List<String> list = new ArrayList<>();
        list.add("first");
        list.add("second");

        ContainsAllOfSetMatcher matcher = new ContainsAllOfSetMatcher(list);

        Assert.assertFalse(matcher.match(null, null, null, null));

        List<String> argument = new ArrayList<>();

        Assert.assertFalse(matcher.match(argument, null, null, null));

        argument.add("second");
        Assert.assertFalse(matcher.match(argument, null, null, null));

        argument.add("first");
        Assert.assertTrue(matcher.match(argument, null, null, null));

        argument.add("third");
        Assert.assertTrue(matcher.match(argument, null, null, null));
    }

    @Test
    public void worksForEmptyParamter() {
        List<String> list = new ArrayList<>();

        ContainsAllOfSetMatcher matcher = new ContainsAllOfSetMatcher(list);

        Assert.assertFalse(matcher.match(null, null, null, null));

        List<String> argument = new ArrayList<>();
        Assert.assertFalse(matcher.match(argument, null, null, null));

        argument.add("second");
        Assert.assertFalse(matcher.match(argument, null, null, null));

        argument.add("first");
        Assert.assertFalse(matcher.match(argument, null, null, null));

        argument.add("third");
        Assert.assertFalse(matcher.match(argument, null, null, null));
    }
}
