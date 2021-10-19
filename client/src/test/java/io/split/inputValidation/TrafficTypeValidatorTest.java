package io.split.inputValidation;

import io.split.storages.SplitCacheConsumer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

public class TrafficTypeValidatorTest {

    @Test
    public void isValidWorks() {
        SplitCacheConsumer splitCacheConsumer = Mockito.mock(SplitCacheConsumer.class);

        Optional<String> result = TrafficTypeValidator.isValid("traffic_type_test", splitCacheConsumer, "test");
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("traffic_type_test", result.get());

        // when tt have upper case
        result = TrafficTypeValidator.isValid("trafficTypeTest", splitCacheConsumer, "test");

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("traffictypetest", result.get());

        // when tt is null
        result = TrafficTypeValidator.isValid(null, splitCacheConsumer, "test");

        Assert.assertFalse(result.isPresent());

        // when tt is empty
        result = TrafficTypeValidator.isValid("", splitCacheConsumer, "test");

        Assert.assertFalse(result.isPresent());
    }
}
