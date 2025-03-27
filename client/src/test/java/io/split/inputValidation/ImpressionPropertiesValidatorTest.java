package io.split.inputValidation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Assert;
import org.junit.Test;

public class ImpressionPropertiesValidatorTest {
    @Test
    public void propertiesAreValidWorks() {
        String properties = "{\"prop1\": 1, \"prop2\": 2L, \"prop3\": 7.56, \"prop4\": \"something\", \"prop5\": true, \"prop6\": null}";
        JsonObject propertiesJson = new JsonParser().parse(properties).getAsJsonObject();
        ImpressionPropertiesValidator.ImpressionPropertiesValidatorResult result = ImpressionPropertiesValidator.propertiesAreValid(propertiesJson);
        Assert.assertTrue(result.getSuccess());
        Assert.assertEquals(1065, result.getSize());
        Assert.assertEquals(6, result.getValue().size());

        // when properties size is > Event.MAX_PROPERTIES_LENGTH_BYTES
        for (int i = 7; i <= (32 * 1024); i++) {
            propertiesJson.addProperty("prop" + i, "something-" + i);
        }
        result = ImpressionPropertiesValidator.propertiesAreValid(propertiesJson);
        Assert.assertFalse(result.getSuccess());
    }
}
