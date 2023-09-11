package io.split.client.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static io.split.inputValidation.FlagSetsValidator.cleanup;
import static io.split.inputValidation.FlagSetsValidator.areValid;

public class FlagSetsValidatorTest {

    @Test
    public void testEmptyFlagSets() {
        List<String> flagSets = new ArrayList<>();
        HashSet<String> cleanFlagSets = cleanup(flagSets);
        Assert.assertTrue(cleanFlagSets.isEmpty());
    }

    @Test
    public void testUpperFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add("Test1");
        flagSets.add("TEST2");
        HashSet<String> cleanFlagSets = cleanup(flagSets);
        Assert.assertTrue(cleanFlagSets.contains("test1"));
        Assert.assertTrue(cleanFlagSets.contains("test2"));
    }

    @Test
    public void testTrimFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add(" test1");
        flagSets.add(" test2 ");
        HashSet<String> cleanFlagSets = cleanup(flagSets);
        Assert.assertTrue(cleanFlagSets.contains("test1"));
        Assert.assertTrue(cleanFlagSets.contains("test2"));
    }

    @Test
    public void testRegexFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add(" test1");
        flagSets.add(" test-2 ");
        HashSet<String> cleanFlagSets = cleanup(flagSets);
        Assert.assertEquals(1, cleanFlagSets.size());
        Assert.assertTrue(cleanFlagSets.contains("test1"));
        Assert.assertFalse(cleanFlagSets.contains("test-2"));
    }

    @Test
    public void testDuplicateFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add(" test1");
        flagSets.add(" test1 ");
        HashSet<String> cleanFlagSets = cleanup(flagSets);
        Assert.assertEquals(1, cleanFlagSets.size());
        Assert.assertTrue(cleanFlagSets.contains("test1"));
    }

    @Test
    public void testIsValid(){
        Assert.assertTrue(areValid(Arrays.asList(" test1 ")).getValid());
        Assert.assertTrue(areValid(Arrays.asList("Test1 ")).getValid());
    }

    @Test
    public void testIsNotValid(){
        Assert.assertFalse(areValid(Arrays.asList(" test 1 ")).getValid());
        Assert.assertFalse(areValid(Arrays.asList("")).getValid());
        Assert.assertFalse(areValid(null).getValid());
    }
}