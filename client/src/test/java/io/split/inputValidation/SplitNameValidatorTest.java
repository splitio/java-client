package io.split.inputValidation;

import org.junit.Assert;
import org.junit.Test;

public class SplitNameValidatorTest {

    @Test
    public void isValidWorks() {
        InputValidationResult result = SplitNameValidator.isValid("split_name_test", "test");

        Assert.assertTrue(result.getSuccess());
        Assert.assertEquals("split_name_test", result.getValue());

        // when split name is null
        result = SplitNameValidator.isValid(null, "test");
        Assert.assertFalse(result.getSuccess());
        Assert.assertNull(result.getValue());

        // when split name is empty
        result = SplitNameValidator.isValid("", "test");
        Assert.assertFalse(result.getSuccess());
        Assert.assertNull(result.getValue());

        // when split name have empty spaces
        result = SplitNameValidator.isValid(" split name test ", "test");
        Assert.assertTrue(result.getSuccess());
        Assert.assertEquals("split name test", result.getValue());
    }
}
