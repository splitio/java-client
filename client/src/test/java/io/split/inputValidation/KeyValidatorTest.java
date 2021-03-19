package io.split.inputValidation;

import org.junit.Assert;
import org.junit.Test;

public class KeyValidatorTest {
    @Test
    public void isValidWorks() {
        boolean result = KeyValidator.isValid("key",  "propertyName", 5, "test");
        Assert.assertTrue(result);

        // when key is null
        result = KeyValidator.isValid(null,  "propertyName", 5, "test");
        Assert.assertFalse(result);

        // when key is empty
        result = KeyValidator.isValid("",  "propertyName", 5, "test");
        Assert.assertFalse(result);

        // when key is > maxStringLength
        result = KeyValidator.isValid("key",  "propertyName", 0, "test");
        Assert.assertFalse(result);
    }

    @Test
    public void bucketingKeyIsValidWorks() {
        boolean result = KeyValidator.bucketingKeyIsValid("bucketingKey", 20, "test");
        Assert.assertTrue(result);

        // when bucketingKey is null
        result = KeyValidator.bucketingKeyIsValid(null, 20, "test");
        Assert.assertTrue(result);

        // when bucketingKey is empty
        result = KeyValidator.bucketingKeyIsValid("", 20, "test");
        Assert.assertFalse(result);

        // when bucketingKey is > maxStringLength
        result = KeyValidator.bucketingKeyIsValid("", 5, "test");
        Assert.assertFalse(result);
    }
}
