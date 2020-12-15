package io.split.inputValidation;

import io.split.engine.cache.SplitCache;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TrafficTypeValidatorTest {

    @Test
    public void isValidWorks() {
        SplitCache splitCache = Mockito.mock(SplitCache.class);

        InputValidationResult result = TrafficTypeValidator.isValid("traffic_type_test", splitCache, "test");
        Assert.assertTrue(result.getSuccess());
        Assert.assertEquals("traffic_type_test", result.getValue());

        // when tt have upper case
        result = TrafficTypeValidator.isValid("trafficTypeTest", splitCache, "test");

        Assert.assertTrue(result.getSuccess());
        Assert.assertEquals("traffictypetest", result.getValue());

        // when tt is null
        result = TrafficTypeValidator.isValid(null, splitCache, "test");

        Assert.assertFalse(result.getSuccess());
        Assert.assertNull(result.getValue());

        // when tt is empty
        result = TrafficTypeValidator.isValid("", splitCache, "test");

        Assert.assertFalse(result.getSuccess());
        Assert.assertNull(result.getValue());
    }
}
