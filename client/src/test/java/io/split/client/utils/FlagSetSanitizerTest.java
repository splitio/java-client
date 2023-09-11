package io.split.client.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static io.split.client.utils.FlagSetsValidator.cleanup;
import static io.split.client.utils.FlagSetsValidator.isValid;

public class FlagSetSanitizerTest {

    @Test
    public void testEmptyFlagSets() {
        List<String> flagSets = new ArrayList<>();
        List<String> cleanFlagSets = cleanup(flagSets);
        Assert.assertTrue(cleanFlagSets.isEmpty());
    }

    @Test
    public void testUpperFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add("Test1");
        flagSets.add("TEST2");
        List<String> cleanFlagSets = cleanup(flagSets);
        Assert.assertEquals("test1", cleanFlagSets.get(0));
        Assert.assertEquals("test2", cleanFlagSets.get(1));
    }

    @Test
    public void testTrimFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add(" test1");
        flagSets.add(" test2 ");
        List<String> cleanFlagSets = cleanup(flagSets);
        Assert.assertEquals("test1", cleanFlagSets.get(0));
        Assert.assertEquals("test2", cleanFlagSets.get(1));
    }

    @Test
    public void testRegexFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add(" test1");
        flagSets.add(" test-2 ");
        List<String> cleanFlagSets = cleanup(flagSets);
        Assert.assertEquals(1, cleanFlagSets.size());
        Assert.assertEquals("test1", cleanFlagSets.get(0));
    }

    @Test
    public void testDuplicateFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add(" test1");
        flagSets.add(" test1 ");
        List<String> cleanFlagSets = cleanup(flagSets);
        Assert.assertEquals(1, cleanFlagSets.size());
        Assert.assertEquals("test1", cleanFlagSets.get(0));
    }

    @Test
    public void testIsValid(){
        Assert.assertTrue(isValid(" test1 "));
    }

    @Test
    public void testIsNotValid(){
        Assert.assertFalse(isValid(" test 1 "));
        Assert.assertFalse(isValid("Test1 "));
        Assert.assertFalse(isValid(""));
        Assert.assertFalse(isValid(null));
    }
}