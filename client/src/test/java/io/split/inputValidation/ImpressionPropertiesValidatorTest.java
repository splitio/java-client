package io.split.inputValidation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.split.grammar.Treatments;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ImpressionPropertiesValidatorTest {

    @Test(expected = IllegalStateException.class)
    public void testConstructorException() {
        ImpressionPropertiesValidator iv = new ImpressionPropertiesValidator();
    }

    @Test
    public void propertiesAreValidWorks() {
        Map<String, Object> properties = new HashMap<String, Object>()
        {{
            put("prop1", 1);
            put("prop2", 2L);
            put("prop3", 7.56);
            put("prop4", "something");
            put("prop5", true);
            put("prop6", null);
        }};
        ImpressionPropertiesValidator.ImpressionPropertiesValidatorResult result = ImpressionPropertiesValidator.propertiesAreValid(properties);
        Assert.assertTrue(result.getSuccess());
        Assert.assertEquals(1063, result.getEventSize());
        Assert.assertEquals(6, result.getValue().size());

        // when properties size is > Event.MAX_PROPERTIES_LENGTH_BYTES
        for (int i = 7; i <= (32 * 1024); i++) {
            properties.put("prop" + i, "something-" + i);
        }
        result = ImpressionPropertiesValidator.propertiesAreValid(properties);
        Assert.assertFalse(result.getSuccess());
    }
}
