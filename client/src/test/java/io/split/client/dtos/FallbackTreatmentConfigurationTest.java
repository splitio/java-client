package io.split.client.dtos;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class FallbackTreatmentConfigurationTest {

    @Test
    public void TestWorks() {
        FallbackTreatmentsConfiguration fallbackTreatmentsConfiguration = new FallbackTreatmentsConfiguration(new FallbackTreatment("on"));
        assertEquals("on", fallbackTreatmentsConfiguration.getGlobalFallbackTreatment().getTreatment());
        assertEquals(null, fallbackTreatmentsConfiguration.getByFlagFallbackTreatment());

        fallbackTreatmentsConfiguration = new FallbackTreatmentsConfiguration(new FallbackTreatment("on", "{\"prop\":\"val\"}"),
                new HashMap<String, FallbackTreatment>() {{ put("flag", new FallbackTreatment("off", "{\"prop2\":\"val2\"}")); }} );
        assertEquals("on", fallbackTreatmentsConfiguration.getGlobalFallbackTreatment().getTreatment());
        assertEquals("{\"prop\":\"val\"}", fallbackTreatmentsConfiguration.getGlobalFallbackTreatment().getConfig());
        assertEquals(null, fallbackTreatmentsConfiguration.getGlobalFallbackTreatment().getLabel());
        assertEquals("off", fallbackTreatmentsConfiguration.getByFlagFallbackTreatment().get("flag").getTreatment());
        assertEquals("{\"prop2\":\"val2\"}", fallbackTreatmentsConfiguration.getByFlagFallbackTreatment().get("flag").getConfig());
        assertEquals(null, fallbackTreatmentsConfiguration.getByFlagFallbackTreatment().get("flag").getLabel());

        fallbackTreatmentsConfiguration = new FallbackTreatmentsConfiguration("on",
                new HashMap<String, String>() {{ put("flag", "off"); }} );
        assertEquals("on", fallbackTreatmentsConfiguration.getGlobalFallbackTreatment().getTreatment());
        assertEquals("off", fallbackTreatmentsConfiguration.getByFlagFallbackTreatment().get("flag").getTreatment());

    }
}
