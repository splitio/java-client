package io.split.client.testing;

import io.split.client.SplitClient;
import io.split.client.api.Key;
import io.split.grammar.Treatments;

import java.util.HashMap;
import java.util.Map;

public class SplitClientForTest implements SplitClient {
    private Map<String, String> _tests;

    public SplitClientForTest() {
        _tests = new HashMap<>();
    }

    public Map<String, String> tests() {
        return _tests;
    }

    public void clearTreatments() {
        _tests = new HashMap<>();
    }

    public void registerTreatments(Map<String, String> treatments) {
        _tests.putAll(treatments);
    }

    public void registerTreatment(String feature, String treatment) {
        _tests.put(feature, treatment);
    }

    public String getTreatment(String key, String feature) {
        return _tests.containsKey(feature)
                ? _tests.get(feature)
                : "control";
    }

    public String getTreatment(String key, String feature, Map<String, Object> attributes) {
        return this.getTreatment(key, feature);
    }

    public String getTreatment(Key key, String feature, Map<String, Object> attributes) {
        return _tests.containsKey(feature)
                ? _tests.get(feature)
                : Treatments.CONTROL;
    }
}
