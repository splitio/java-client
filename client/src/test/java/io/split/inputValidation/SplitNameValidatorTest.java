package io.split.inputValidation;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class SplitNameValidatorTest {

    @Test
    public void isValidWorks() {
        Optional<String> result = SplitNameValidator.isValid("split_name_test", "test");

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("split_name_test", result.get());

        // when split name is null
        result = SplitNameValidator.isValid(null, "test");
        Assert.assertFalse(result.isPresent());

        // when split name is empty
        result = SplitNameValidator.isValid("", "test");
        Assert.assertFalse(result.isPresent());

        // test regex
        result = SplitNameValidator.isValid("te#fg", "test");
        Assert.assertFalse(result.isPresent());

        // test regex
        result = SplitNameValidator.isValid("te@fg", "test");
        Assert.assertFalse(result.isPresent());

        // test regex
        result = SplitNameValidator.isValid("te&fg", "test");
        Assert.assertFalse(result.isPresent());

        // test regex
        result = SplitNameValidator.isValid("te)fg", "test");
        Assert.assertFalse(result.isPresent());

        // test length
        result = SplitNameValidator.isValid(new String(new char[101]).replace('\0', 'w'), "test");
        Assert.assertFalse(result.isPresent());

        // when split name have empty spaces
        result = SplitNameValidator.isValid(" split-name-test ", "test");
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("split-name-test", result.get());
    }
}
