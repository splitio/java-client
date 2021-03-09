package io.split.inputValidation;

import io.split.cache.SplitCache;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

public class TrafficTypeValidatorTest {

    @Test
    public void isValidWorks() {
        SplitCache splitCache = Mockito.mock(SplitCache.class);

        Optional<String> result = TrafficTypeValidator.isValid("traffic_type_test", splitCache, "test");
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("traffic_type_test", result.get());

        // when tt have upper case
        result = TrafficTypeValidator.isValid("trafficTypeTest", splitCache, "test");

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("traffictypetest", result.get());

        // when tt is null
        result = TrafficTypeValidator.isValid(null, splitCache, "test");

        Assert.assertFalse(result.isPresent());

        // when tt is empty
        result = TrafficTypeValidator.isValid("", splitCache, "test");

        Assert.assertFalse(result.isPresent());
    }
}
