package io.split.client.utils;

import io.split.inputValidation.FSValidatorResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.split.inputValidation.FlagSetsValidator.cleanup;
import static io.split.inputValidation.FlagSetsValidator.areValid;

public class FlagSetsValidatorTest {

    @Test
    public void testEmptyFlagSets() {
        List<String> flagSets = new ArrayList<>();
        FSValidatorResult cleanFlagSets = cleanup(flagSets);
        Assert.assertTrue(cleanFlagSets.getFlagSets().isEmpty());
    }

    @Test
    public void testUpperFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add("Test1");
        flagSets.add("TEST2");
        FSValidatorResult cleanFlagSets = cleanup(flagSets);
        Assert.assertTrue(cleanFlagSets.getFlagSets().contains("test1"));
        Assert.assertTrue(cleanFlagSets.getFlagSets().contains("test2"));
    }

    @Test
    public void testTrimFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add(" test1");
        flagSets.add(" test2 ");
        FSValidatorResult cleanFlagSets = cleanup(flagSets);
        Assert.assertTrue(cleanFlagSets.getFlagSets().contains("test1"));
        Assert.assertTrue(cleanFlagSets.getFlagSets().contains("test2"));
    }

    @Test
    public void testRegexFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add(" test1");
        flagSets.add(" test-2 ");
        FSValidatorResult cleanFlagSets = cleanup(flagSets);
        Assert.assertEquals(1, cleanFlagSets.getFlagSets().size());
        Assert.assertTrue(cleanFlagSets.getFlagSets().contains("test1"));
        Assert.assertFalse(cleanFlagSets.getFlagSets().contains("test-2"));
    }

    @Test
    public void testDuplicateFlagSets() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add(" test1");
        flagSets.add(" test1 ");
        FSValidatorResult cleanFlagSets = cleanup(flagSets);
        Assert.assertEquals(1, cleanFlagSets.getFlagSets().size());
        Assert.assertTrue(cleanFlagSets.getFlagSets().contains("test1"));
    }

    @Test
    public void testFlagSetsInOrder() {
        List<String> flagSets = new ArrayList<>();
        flagSets.add(" test3");
        flagSets.add(" test2");
        flagSets.add(" test1 ");
        flagSets.add(" 1test ");
        flagSets.add(" 2test ");
        FSValidatorResult cleanFlagSets = cleanup(flagSets);
        Assert.assertEquals(5, cleanFlagSets.getFlagSets().size());
        List<String> sets = new ArrayList<>(cleanFlagSets.getFlagSets());
        Assert.assertEquals("1test", sets.get(0));
        Assert.assertEquals("2test", sets.get(1));
        Assert.assertEquals("test1", sets.get(2));
        Assert.assertEquals("test2", sets.get(3));
        Assert.assertEquals("test3", sets.get(4));
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