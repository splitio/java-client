package io.split.inputValidation;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class EventsValidatorTest {
    @Test
    public void propertiesAreValidWorks() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("prop1", 1);
        properties.put("prop2", 2L);
        properties.put("prop3", 7.56);
        properties.put("prop4", "something");
        properties.put("prop5", true);
        properties.put("prop6", null);
        properties.put(null, "value");
        properties.put("", "value");

        EventsValidator.EventValidatorResult result = EventsValidator.propertiesAreValid(properties);
        Assert.assertTrue(result.getSuccess());
        Assert.assertEquals(1063, result.getEventSize());
        Assert.assertEquals(6, result.getValue().size());

        // when properties size is > Event.MAX_PROPERTIES_LENGTH_BYTES
        properties = new HashMap<>();
        for (int i = 0; i <= (32 * 1024); i++) {
            properties.put("prop" + i, "something-" + i);
        }
        result = EventsValidator.propertiesAreValid(properties);
        Assert.assertFalse(result.getSuccess());
    }

    @Test
    public void typeIsValidWorks() {
        boolean result = EventsValidator.typeIsValid("event_type_id", "test");
        Assert.assertTrue(result);

        // when eventTypeId is null
        result = EventsValidator.typeIsValid(null, "test");
        Assert.assertFalse(result);

        // when eventTypeId is empty
        result = EventsValidator.typeIsValid("", "test");
        Assert.assertFalse(result);

        // when eventTypeId is does not match
        result = EventsValidator.typeIsValid("aksdjas!@#$@%#^$&%", "test");
        Assert.assertFalse(result);
    }
}
