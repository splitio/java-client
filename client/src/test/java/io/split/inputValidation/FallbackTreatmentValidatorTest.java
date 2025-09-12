package io.split.inputValidation;

import io.split.client.dtos.FallbackTreatment;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class FallbackTreatmentValidatorTest {

    @Test
    public void isValidTreatmentWorks() {
        Assert.assertEquals("123asHs_-sdf", FallbackTreatmentValidator.isValidTreatment("123asHs_-sdf", "test"));

        Assert.assertEquals(null, FallbackTreatmentValidator.isValidTreatment(new String(new char[101]).replace('\0', 'w'), "test"));
        Assert.assertEquals(null, FallbackTreatmentValidator.isValidTreatment("", "test"));
        Assert.assertEquals(null, FallbackTreatmentValidator.isValidTreatment(null, "test"));
        Assert.assertEquals(null, FallbackTreatmentValidator.isValidTreatment("12@3asHs_-sdf", "test"));
        Assert.assertEquals(null, FallbackTreatmentValidator.isValidTreatment("12#3asHs_-sdf", "test"));
        Assert.assertEquals(null, FallbackTreatmentValidator.isValidTreatment("12!3asHs_-sdf", "test"));
        Assert.assertEquals(null, FallbackTreatmentValidator.isValidTreatment("12^3asHs_-sdf", "test"));
    }

    @Test
    public void isValidByFlagTreatmentWorks() {
        HashMap<String, FallbackTreatment> byRef = new HashMap<String, FallbackTreatment>() {{ put("flag", new FallbackTreatment("12#2")); }};
        Assert.assertEquals(new HashMap<>(), FallbackTreatmentValidator.isValidByFlagTreatment(byRef, "test"));

        byRef = new HashMap<String, FallbackTreatment>() {{ put("flag", new FallbackTreatment("12%2")); }};
        Assert.assertEquals(new HashMap<>(), FallbackTreatmentValidator.isValidByFlagTreatment(byRef, "test"));

        byRef = new HashMap<String, FallbackTreatment>() {{ put("flag", new FallbackTreatment(new String(new char[101]).replace('\0', 'w'))); }};
        Assert.assertEquals(new HashMap<>(), FallbackTreatmentValidator.isValidByFlagTreatment(byRef, "test"));

        byRef = new HashMap<String, FallbackTreatment>() {{ put("flag", new FallbackTreatment("12&2")); }};
        Assert.assertEquals(new HashMap<>(), FallbackTreatmentValidator.isValidByFlagTreatment(byRef, "test"));

        byRef = new HashMap<String, FallbackTreatment>() {{ put("", new FallbackTreatment("on")); }};
        Assert.assertEquals(new HashMap<>(), FallbackTreatmentValidator.isValidByFlagTreatment(byRef, "test"));

        byRef = new HashMap<String, FallbackTreatment>() {{ put("12#dd", new FallbackTreatment("on")); }};
        Assert.assertEquals(new HashMap<>(), FallbackTreatmentValidator.isValidByFlagTreatment(byRef, "test"));

        byRef = new HashMap<String, FallbackTreatment>() {{ put(new String(new char[101]).replace('\0', 'w'), new FallbackTreatment("on")); }};
        Assert.assertEquals(new HashMap<>(), FallbackTreatmentValidator.isValidByFlagTreatment(byRef, "test"));

        byRef = new HashMap<String, FallbackTreatment>() {{ put("flag", new FallbackTreatment("123asHs_-sdf")); }};
        Assert.assertEquals("123asHs_-sdf", FallbackTreatmentValidator.isValidByFlagTreatment(byRef, "test").get("flag").getTreatment());
    }
}
