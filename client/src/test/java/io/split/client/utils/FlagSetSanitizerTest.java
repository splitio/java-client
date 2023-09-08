package io.split.client.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class FlagSetSanitizerTest {

    @Test
    public void testEmptyFlagSets() {
        List<String> flagSets = new ArrayList<>();
        List<String> cleanFlagSets = FlagSetSanitizer.sanitizeFlagSet(flagSets);
        Assert.assertTrue(cleanFlagSets.isEmpty());
    }

}