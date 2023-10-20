package io.split.client.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.split.inputValidation.FlagSetsValidator.cleanup;

public class FlagSetsValidatorTest {

    @Test
    public void testEmptyFlagSets() {
        List<String> flagSets = new ArrayList<>();
        Assert.assertTrue(cleanup(flagSets).isEmpty());
    }

    @Test
    public void testUpperFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add("Test1");
        flagSets.add("TEST2");
        Set cleanFlagSets = cleanup(flagSets);
        Assert.assertTrue(cleanFlagSets.contains("test1"));
        Assert.assertTrue(cleanFlagSets.contains("test2"));
    }

    @Test
    public void testTrimFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add(" test1");
        flagSets.add(" test2 ");
        Set cleanFlagSets = cleanup(flagSets);
        Assert.assertTrue(cleanFlagSets.contains("test1"));
        Assert.assertTrue(cleanFlagSets.contains("test2"));
    }

    @Test
    public void testRegexFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add(" test1");
        flagSets.add(" test-2 ");
        Set cleanFlagSets = cleanup(flagSets);
        Assert.assertEquals(1, cleanFlagSets.size());
        Assert.assertTrue(cleanFlagSets.contains("test1"));
        Assert.assertFalse(cleanFlagSets.contains("test-2"));
    }

    @Test
    public void testDuplicateFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add(" test1");
        flagSets.add(" test1 ");
        Set cleanFlagSets = cleanup(flagSets);
        Assert.assertEquals(1, cleanFlagSets.size());
        Assert.assertTrue(cleanFlagSets.contains("test1"));
    }

    @Test
    public void testFlagSetsInOrder() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add(" test3");
        flagSets.add(" test2");
        flagSets.add(" test1 ");
        flagSets.add(" 1test ");
        flagSets.add(" 2test ");
        Set cleanFlagSets = cleanup(flagSets);
        Assert.assertEquals(5, cleanFlagSets.size());
        List<String> sets = new ArrayList<>(cleanFlagSets);
        Assert.assertEquals("1test", sets.get(0));
        Assert.assertEquals("2test", sets.get(1));
        Assert.assertEquals("test1", sets.get(2));
        Assert.assertEquals("test2", sets.get(3));
        Assert.assertEquals("test3", sets.get(4));
    }
}