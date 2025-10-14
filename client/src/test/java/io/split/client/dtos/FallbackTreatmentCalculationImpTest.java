package io.split.client.dtos;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FallbackTreatmentCalculationImpTest {

    @Test
    public void TestWorks() {
        FallbackTreatmentsConfiguration fallbackTreatmentsConfiguration = new FallbackTreatmentsConfiguration(new FallbackTreatment("on"));
        FallbackTreatmentCalculator fallbackTreatmentCalculator = new FallbackTreatmentCalculatorImp(fallbackTreatmentsConfiguration);
        assertEquals("on", fallbackTreatmentCalculator.resolve("anyflag", "exception").getTreatment());
        assertEquals("fallback - exception", fallbackTreatmentCalculator.resolve("anyflag", "exception").getLabel());

        fallbackTreatmentsConfiguration = new FallbackTreatmentsConfiguration(new FallbackTreatment("on"),
                new HashMap<String, String>() {{ put("flag", "off"); }} );
        fallbackTreatmentCalculator = new FallbackTreatmentCalculatorImp(fallbackTreatmentsConfiguration);
        assertEquals("on", fallbackTreatmentCalculator.resolve("anyflag", "exception").getTreatment());
        assertEquals("fallback - exception", fallbackTreatmentCalculator.resolve("anyflag", "exception").getLabel());
        assertEquals("off", fallbackTreatmentCalculator.resolve("flag", "exception").getTreatment());
        assertEquals("fallback - exception", fallbackTreatmentCalculator.resolve("flag", "exception").getLabel());

        fallbackTreatmentsConfiguration = new FallbackTreatmentsConfiguration(
                new HashMap<String, FallbackTreatment>() {{ put("flag", new FallbackTreatment("off")); }} );
        fallbackTreatmentCalculator = new FallbackTreatmentCalculatorImp(fallbackTreatmentsConfiguration);
        assertEquals("control", fallbackTreatmentCalculator.resolve("anyflag", "exception").getTreatment());
        assertEquals("exception", fallbackTreatmentCalculator.resolve("anyflag", "exception").getLabel());
        assertEquals("off", fallbackTreatmentCalculator.resolve("flag", "exception").getTreatment());
        assertEquals("fallback - exception", fallbackTreatmentCalculator.resolve("flag", "exception").getLabel());
    }
}
